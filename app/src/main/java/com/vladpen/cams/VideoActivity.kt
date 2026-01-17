package com.vladpen.cams

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import com.vladpen.*
import com.vladpen.Effects.edgeToEdge
import com.vladpen.cams.databinding.ActivityVideoBinding
import com.vladpen.onvif.*
import kotlinx.coroutines.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer
import java.io.IOException

class VideoActivity : AppCompatActivity(), Layout, Player {
    private val binding by lazy { ActivityVideoBinding.inflate(layoutInflater) }
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override lateinit var rootView: View
    override lateinit var layoutListener: ViewTreeObserver.OnGlobalLayoutListener
    override var insets: Insets? = null
    override var hideBars = false

    override lateinit var libVlc: LibVLC
    override lateinit var mediaPlayer: MediaPlayer
    override var volume = 0

    private lateinit var stream: StreamDataModel
    private lateinit var gestureDetector: VideoGestureDetector
    private var gestureInProgress = false
    private var streamId: Int = -1 // -1 means "no stream"
    private lateinit var remotePath: String // relative SFTP path
    private val seekStep: Long = 10000 // milliseconds
    private val watchdogInterval: Long = 10000 // milliseconds
    private var isPlaying = true

    // ONVIF properties
    private var ptzControlView: PTZControlView? = null
    private var motionIndicator: MotionIndicator? = null
    private var onvifManager: ONVIFManager? = null
    private val onvifScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        edgeToEdge(binding.root) { innerPadding ->
            insets = innerPadding
        }
        streamId = intent.getIntExtra("streamId", -1)
        stream = StreamData.getById(streamId) ?: return
        remotePath = intent.getStringExtra("remotePath") ?: return

        initActivity()
        initLayout(binding.root)
    }

    private fun initActivity() {
        initToolbar()
        initVideoBar()
        initMute()

        gestureDetector = VideoGestureDetector(binding.flVideoBox)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        this.onBackPressedDispatcher.addCallback(callback)

        Alert.init(this, binding.toolbar.btnAlert)
        
        // Initialize ONVIF if this is an ONVIF device
        initOnvifFeatures()
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            back()
        }
    }

    private fun back() {
        val intent = Intent(this, FilesActivity::class.java)
            .putExtra("streamId", streamId)
            .putExtra("remotePath", FileData.getParentPath(remotePath))
        startActivity(intent)
    }

    override fun resizeLayout() {
        resizeVideo(binding.flVideoBox)
        gestureDetector.reset()
        initBars()
    }

    private fun streamsScreen() {
        val intent = Intent(this, StreamsActivity::class.java)
        if (GroupData.backGroupId > -1) {
            intent.putExtra("type", "group")
            intent.putExtra("id", GroupData.backGroupId)
        } else {
            intent.putExtra("type", "stream")
            intent.putExtra("id", streamId)
        }
        startActivity(intent)
    }

    private fun initToolbar() {
        binding.toolbar.tvLabel.text = stream.name
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
        if (stream.sftp == null) {
            return
        }
        binding.toolbar.btnLink.setImageResource(R.drawable.ic_outline_videocam_24)
        binding.toolbar.btnLink.contentDescription = getString(R.string.back)
        binding.toolbar.btnLink.visibility = View.VISIBLE
        binding.toolbar.btnLink.setOnClickListener {
            streamsScreen()
        }
    }

    private fun initVideoBar() {
        val ext = FileData.getExtension(remotePath).lowercase()
        if (ext == "jpg" || ext == "jpeg" || ext == "png") {
            binding.videoBar.btnSeekBack.visibility = View.GONE
            binding.videoBar.tvSpeed.visibility = View.GONE
        }
        binding.videoBar.btnPlay.setOnClickListener {
            if (isPlaying) {
                isPlaying = false
                mediaPlayer.pause()
                it.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
            } else {
                isPlaying = true
                mediaPlayer.play()
                it.setBackgroundResource(R.drawable.ic_baseline_pause_24)
            }
            initBars()
        }
        binding.videoBar.btnPrevFile.setOnClickListener {
            next(false)
        }
        binding.videoBar.btnSeekBack.setOnClickListener {
            dropRate() // prevent lost keyframe
            if (mediaPlayer.time > seekStep) {
                // we can't use the "position" here (file size changes during playback)
                mediaPlayer.time -= seekStep
            } else {
                // rewind to the beginning
                mediaPlayer.stop()
                start()  // activity
            }
            initBars()
        }
        binding.videoBar.btnNextFile.setOnClickListener {
            next()
        }
        binding.videoBar.tvSpeed.setOnClickListener {
            if (mediaPlayer.rate < 2f) {
                mediaPlayer.rate = 4f
                "4x".also { binding.videoBar.tvSpeed.text = it }
            } else {
                dropRate()
            }
            initBars()
        }
        "1x".also { binding.videoBar.tvSpeed.text = it } // "also" makes linter happy
        binding.videoBar.llVideoCtrl.visibility = View.VISIBLE
    }

    private fun dropRate() {
        mediaPlayer.rate = 1f
        "1x".also { binding.videoBar.tvSpeed.text = it }
    }

    private fun initMute() {
        binding.videoBar.btnMute.setOnClickListener {
            var mute = StreamData.getMute()
            mute = if (mute == 0) 1 else 0
            setMute(mute)
            StreamData.setMute(mute)
            initBars()
        }
    }

    private fun setMute(mute: Int) {
        if (mute == 1) {
            mediaPlayer.volume = 0
            binding.videoBar.btnMute.setImageResource(R.drawable.ic_baseline_volume_off_24)
        } else {
            mediaPlayer.volume = 100
            binding.videoBar.btnMute.setImageResource(R.drawable.ic_baseline_volume_on_24)
        }
    }

    private fun start() {
        try {
            start(FileData.getTmpFile(remotePath).absolutePath)  // player
            setMute(StreamData.getMute())

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun next(fwd: Boolean = true) {
        remotePath = FileData(stream.sftp).getNext(remotePath, fwd)
        if (remotePath != "") {
            start()  // activity
        } else { // Last file was played, let's show live video
            streamsScreen()
        }
    }

    private fun initBars() {
        Effects.cancel()
        binding.toolbar.root.visibility = View.VISIBLE
        binding.videoBar.root.visibility = View.VISIBLE
        if (hideBars) {
            Effects.delayedFadeOut(arrayOf(binding.toolbar.root, binding.videoBar.root))
        }
    }

    private val runnable = object : Runnable {
        override fun run() {
            if (isPlaying)
                watchdog()
            handler.postDelayed(this, watchdogInterval)
        }
    }

    override fun onStart() {
        super.onStart()
        initPlayer(this, binding.videoLayout, false)
        start()
        handler.postDelayed(runnable, watchdogInterval)
    }

    override fun onStop() {
        super.onStop()
        release()
        handler.removeCallbacks(runnable)
    }

    override fun onBufferReady(isAudioAvailable: Boolean) {
        super.onBufferReady(isAudioAvailable)
        binding.progressBar.pbLoading.visibility = View.GONE
        if (isAudioAvailable) {
            binding.videoBar.btnMute.visibility = View.VISIBLE
        }
        if (!isPlaying)
            mediaPlayer.pause()
    }

    override fun onEndReached() {
        if (isPlaying)
            next()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val res = gestureDetector.onTouchEvent(event)
        if (res)
            gestureInProgress = true

        if (event.action == MotionEvent.ACTION_UP) {
            if (!gestureInProgress)
                initBars()
            else
                gestureInProgress = false
        }
        return res || super.onTouchEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        initLayout(binding.root)
    }

    private fun initOnvifFeatures() {
        // Check if this stream has ONVIF capabilities
        if (!stream.isOnvifDevice || stream.onvifServiceUrl == null) {
            return
        }

        onvifManager = ONVIFManager.getInstance()
        
        // Initialize PTZ controller if supported
        if (stream.deviceCapabilities?.supportsPTZ == true) {
            initPTZControls()
        }
        
        // Initialize motion detection if supported
        if (stream.deviceCapabilities?.supportsMotionEvents == true) {
            initMotionDetection()
        }
    }

    private fun initPTZControls() {
        val serviceUrl = stream.onvifServiceUrl ?: return
        val credentials = stream.onvifCredentials
        
        // Create PTZ control view
        ptzControlView = PTZControlView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Add to video container
        binding.flVideoBox.addView(ptzControlView)
        
        // Initialize PTZ controller
        onvifScope.launch {
            val deviceId = stream.url // Use URL as device ID for now
            val controller = onvifManager?.initializePTZController(deviceId, serviceUrl, credentials)
            ptzControlView?.setPTZController(controller)
            
            // Set up PTZ control listener
            ptzControlView?.setListener(object : PTZControlView.PTZControlListener {
                override fun onPTZCommand(direction: PTZDirection, isPressed: Boolean) {
                    onvifScope.launch {
                        if (isPressed) {
                            onvifManager?.performPTZMove(deviceId, direction)
                        } else {
                            onvifManager?.stopPTZMovement(deviceId)
                        }
                    }
                }

                override fun onZoomCommand(factor: Float) {
                    onvifScope.launch {
                        onvifManager?.performZoom(deviceId, factor)
                    }
                }
            })
        }
    }

    private fun initMotionDetection() {
        val serviceUrl = stream.onvifServiceUrl ?: return
        val credentials = stream.onvifCredentials
        
        // Create motion indicator
        motionIndicator = MotionIndicator(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Add to video container
        binding.flVideoBox.addView(motionIndicator)
        
        // Initialize motion event service
        onvifScope.launch {
            val deviceId = stream.url // Use URL as device ID for now
            val service = onvifManager?.initializeMotionEventService(deviceId, serviceUrl, credentials)
            
            // Set up ONVIF manager listener for motion events
            onvifManager?.setListener(object : ONVIFManager.ONVIFManagerListener {
                override fun onDeviceDiscovered(devices: List<ONVIFDevice>) {}
                
                override fun onMotionDetected(deviceId: String) {
                    if (deviceId == stream.url) {
                        runOnUiThread {
                            motionIndicator?.showMotionDetected()
                        }
                    }
                }
                
                override fun onMotionStopped(deviceId: String) {
                    if (deviceId == stream.url) {
                        runOnUiThread {
                            motionIndicator?.hideMotionDetected()
                        }
                    }
                }
                
                override fun onPTZError(deviceId: String, error: String) {
                    // Handle PTZ errors
                }
            })
            
            // Subscribe to motion events
            onvifManager?.subscribeToMotionEvents(deviceId)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Show PTZ controls on touch
        if (event.action == MotionEvent.ACTION_DOWN) {
            ptzControlView?.show()
        }
        
        val res = gestureDetector.onTouchEvent(event)
        if (res)
            gestureInProgress = true

        if (event.action == MotionEvent.ACTION_UP) {
            if (!gestureInProgress)
                initBars()
            else
                gestureInProgress = false
        }
        return res || super.onTouchEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        onvifScope.cancel()
        
        // Cleanup ONVIF resources
        stream.url.let { deviceId ->
            onvifScope.launch {
                onvifManager?.unsubscribeFromMotionEvents(deviceId)
            }
        }
    }
}
