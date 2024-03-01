package com.vladpen.cams

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.vladpen.*
import com.vladpen.cams.databinding.ActivityVideoBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.IOException

private const val ASPECT_RATIO = 16f / 9f

class VideoActivity : AppCompatActivity(), MediaPlayer.EventListener {
    private val binding by lazy { ActivityVideoBinding.inflate(layoutInflater) }

    private lateinit var stream: StreamDataModel
    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var layoutListener: ViewTreeObserver.OnGlobalLayoutListener
    private lateinit var gestureDetector: VideoGestureDetector
    private var gestureInProgress = false
    private var streamId: Int = -1 // -1 means "no stream"
    private var remotePath: String? = null // relative SFTP path
    private val seekStep: Long = 10000 // milliseconds
    private var isBuffered = false
    private var hideBars = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()

        layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            resizeLayout() // also reset gestureDetector
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun initActivity() {
        streamId = intent.getIntExtra("streamId", -1)
        remotePath = intent.getStringExtra("remotePath")

        stream = StreamData.getById(streamId) ?: return

        libVlc = LibVLC(this, ArrayList<String>().apply {
            if (stream.tcp && remotePath == null)
                add("--rtsp-tcp")
            if (!StreamData.logConnections)
                add("--verbose=-1")
            add("--image-duration=1") // for "events" slideshow
        })
        mediaPlayer = MediaPlayer(libVlc)
        mediaPlayer.setEventListener(this)

        initToolbar()
        if (remotePath != null)
            initVideoBar()
        else if (stream.url2 != null)
            initChannel()
        initMute()

        gestureDetector = VideoGestureDetector(binding.clScreenBox, binding.flVideoBox)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        this.onBackPressedDispatcher.addCallback(callback)

        if (remotePath == null)
            observeNetworkState()

        Alert.init(this, binding.toolbar.btnAlert)
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            back()
        }
    }

    private fun back() {
        if (remotePath == null) {
            if (GroupData.currentGroupId > -1) {
                groupScreen()
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        } else {
            val intent = Intent(this, FilesActivity::class.java)
                .putExtra("streamId", streamId)
                .putExtra("remotePath", FileData.getParentPath(remotePath!!))
            startActivity(intent)
        }
    }

    private fun resizeLayout() {
        val frameWidth: Int
        val frameHeight: Int
        val rootAspectRatio = binding.root.width.toFloat() / binding.root.height.toFloat()
        if (rootAspectRatio > ASPECT_RATIO) { // vertical margins
            frameHeight = binding.root.height
            frameWidth = (frameHeight * ASPECT_RATIO).toInt()
            hideBars = true
        } else { // horizontal margins
            frameWidth = binding.root.width
            frameHeight = (frameWidth / ASPECT_RATIO).toInt()
            hideBars = false
        }
        val params = binding.flVideoBox.layoutParams
        params.width = frameWidth
        params.height = frameHeight
        binding.flVideoBox.layoutParams = params

        gestureDetector.reset()
        initBars()
    }

    private fun filesScreen() {
        val intent = Intent(this, FilesActivity::class.java)
            .putExtra("streamId", streamId)
        startActivity(intent)
    }

    private fun groupScreen() {
        val intent = Intent(this, GroupActivity::class.java)
            .putExtra("groupId", GroupData.currentGroupId)
        startActivity(intent)
    }

    private fun videoScreen() {
        val intent = Intent(this, VideoActivity::class.java)
            .putExtra("streamId", streamId)
        // finish()
        // intent.putExtra("streamId", streamId)
        startActivity(intent)
    }

    private fun initToolbar() {
        binding.toolbar.tvLabel.text = stream.name
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }

        if (stream.sftp == null)
            return

        if (remotePath != null) {
            binding.toolbar.btnLink.setImageResource(R.drawable.ic_outline_videocam_24)
            binding.toolbar.btnLink.contentDescription = getString(R.string.back)
        }
        binding.toolbar.btnLink.visibility = View.VISIBLE
        binding.toolbar.btnLink.setOnClickListener {
            if (remotePath == null)
                filesScreen()
            else if (GroupData.currentGroupId > -1)
                groupScreen()
            else
                videoScreen()
        }
    }

    private fun initVideoBar() {
        binding.videoBar.btnPlay.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                it.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
            } else {
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
                start()
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

    private fun initChannel() {
        var channel = StreamData.getChannel()
        binding.videoBar.tvChannel.text = Utils.getChannelButton(channel)
        binding.videoBar.tvChannel.visibility = View.VISIBLE
        binding.videoBar.tvChannel.setOnClickListener {
            channel = if (channel != 1) 1 else 0
            StreamData.setChannel(channel)
            binding.videoBar.tvChannel.text = Utils.getChannelButton(channel)
            binding.videoBar.tvChannel.visibility = View.GONE
            binding.pbLoading.visibility = View.VISIBLE
            mediaPlayer.stop()
            start()
        }
    }

    private fun start() {
        try {
            val media =
                if (remotePath == null)
                    Media(libVlc, Uri.parse(getUrl()))
                else
                    Media(libVlc, FileData.getTmpFile(remotePath!!).absolutePath)

            media.apply {
                mediaPlayer.media = this
            }.release()

            mediaPlayer.play()
            setMute(StreamData.getMute())

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun next(fwd: Boolean = true) {
        remotePath = FileData(stream.sftp).getNext(remotePath!!, fwd)
        if (remotePath != "") {
            binding.videoBar.btnPlay.setBackgroundResource(R.drawable.ic_baseline_pause_24)
            start()
        } else { // Last file was played, let's show live video
            videoScreen()
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

    override fun onStart() {
        super.onStart()
        mediaPlayer.attachViews(binding.videoLayout, null, false, false)
        start()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.stop()
        mediaPlayer.detachViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        libVlc.release()
    }

    override fun onEvent(ev: MediaPlayer.Event) {
        if (ev.type == MediaPlayer.Event.Buffering && ev.buffering == 100f) {
            binding.pbLoading.visibility = View.GONE
            if (mediaPlayer.audioTracksCount > 0)  // use (media.tracks.size > 1) for 4.X versions
                binding.videoBar.btnMute.visibility = View.VISIBLE
            if (stream.url2 != null)
                binding.videoBar.tvChannel.visibility = View.VISIBLE
            initBars()
            isBuffered = true
        } else if (ev.type == MediaPlayer.Event.EndReached && remotePath != null) {
            next()
        }
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
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun observeNetworkState() {
        var lastConnected = true
        NetworkState().observe(this) { isConnected ->
            if (isConnected && !lastConnected && isBuffered) {
                mediaPlayer.stop()
                mediaPlayer.play()
                setMute(StreamData.getMute())
            }
            lastConnected = isConnected
        }
    }

    private fun getUrl(): String {
        val url = if (stream.url2 != null && StreamData.getChannel() == 1)
            stream.url2!!
        else
            stream.url
        return Utils.getFullUrl(url, 554, "rtsp")
    }
}
