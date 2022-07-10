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
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.IOException

class VideoActivity : AppCompatActivity(), MediaPlayer.EventListener {
    private val binding by lazy { ActivityVideoBinding.inflate(layoutInflater) }

    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var videoLayout: VLCVideoLayout
    private lateinit var stream: StreamDataModel

    private lateinit var gestureDetector: VideoGestureDetector
    private var gestureInProgress = false

    private var streamId: Int = -1 // -1 means "no stream"
    private var remotePath: String = "" // relative SFTP path
    private val seekStep: Long = 10000 // milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        streamId = intent.getIntExtra("streamId", -1)
        remotePath = intent.getStringExtra("remotePath") ?: ""

        stream = StreamData.getById(streamId) ?: return

        videoLayout = binding.videoLayout

        libVlc = LibVLC(this, ArrayList<String>().apply {
            if (stream.tcp && remotePath == "")
                add("--rtsp-tcp")
            if (!StreamData.logConnections)
                add("--verbose=-1")
        })
        mediaPlayer = MediaPlayer(libVlc)
        mediaPlayer.setEventListener(this)

        initToolbar()
        if (remotePath != "")
            initVideoBar()
        initMute()

        gestureDetector = VideoGestureDetector(videoLayout)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        this.onBackPressedDispatcher.addCallback(callback)

        if (remotePath != "")
            return
        val isLocal = Utils.isUrlLocal(stream.url)
        NetworkState(isLocal).observe(this) { isConnected ->
            if (isConnected) {
                binding.tvAlert.visibility = View.GONE
                mediaPlayer.stop()
                mediaPlayer.play()
            } else {
                binding.tvAlert.visibility = View.VISIBLE
                binding.tvAlert.text =
                    if (isLocal) getString(R.string.no_wifi) else getString(R.string.no_internet)
            }
        }
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            back()
        }
    }

    private fun back() {
        if (remotePath == "") {
            if (GroupData.currentGroupId > -1) {
                groupScreen()
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        } else {
            val intent = Intent(this, FilesActivity::class.java)
                .putExtra("streamId", streamId)
                .putExtra("remotePath", FileData.getParentPath(remotePath))
            startActivity(intent)
        }
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
        finish()
        intent.putExtra("streamId", streamId).putExtra("remotePath", "")
        startActivity(intent)
    }

    private fun initToolbar() {
        binding.toolbar.tvToolbarLabel.text = stream.name
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }

        if (stream.sftp == null)
            return

        if (remotePath == "") {
            binding.toolbar.tvToolbarLink.text = getString(R.string.files)
            binding.toolbar.tvToolbarLink.setTextColor(getColor(R.color.files_link))
        } else {
            if (GroupData.currentGroupId > -1) {
                binding.toolbar.tvToolbarLink.text = getString(R.string.group)
                binding.toolbar.tvToolbarLink.setTextColor(getColor(R.color.group_link))
            } else {
                binding.toolbar.tvToolbarLink.text = getString(R.string.live)
                binding.toolbar.tvToolbarLink.setTextColor(getColor(R.color.live_link))
            }
        }
        binding.toolbar.tvToolbarLink.setOnClickListener {
            if (remotePath == "") {
                filesScreen()
            } else {
                if (GroupData.currentGroupId > -1)
                    groupScreen()
                else
                    videoScreen()
            }
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
            // we can't use the "position" here (file size changes during playback)
            mediaPlayer.time -= seekStep
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
        "1x".also { binding.videoBar.tvSpeed.text = it } // makes linter happy
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
            binding.videoBar.btnMute.setImageResource(R.drawable.ic_baseline_volume_up_24)
        }
    }

    private fun play() {
        try {
            val media =
                if (remotePath == "")
                    Media(libVlc, Uri.parse(Utils.decodeUrl(stream.url)))
                else
                    Media(libVlc, FileData.getTmpFile(remotePath).absolutePath)

            media.apply {
                setHWDecoderEnabled(false, false)
                addOption(":network-caching=300")
                mediaPlayer.media = this
            }.release()

            mediaPlayer.play()

            setMute(StreamData.getMute())

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun next(fwd: Boolean = true) {
        remotePath = FileData(stream.sftp).getNext(remotePath, fwd)
        if (remotePath != "") {
            binding.videoBar.btnPlay.setBackgroundResource(R.drawable.ic_baseline_pause_24)
            play()
        } else { // The most recent file was played, let's show live video
            videoScreen()
        }
    }

    private fun initBars(isLandscape: Boolean? = null) {
        val landscape = isLandscape ?: (binding.root.width > binding.root.height)

        Effects.cancel()
        binding.toolbar.root.visibility = View.VISIBLE
        binding.videoBar.root.visibility = View.VISIBLE
        if (landscape)
            Effects.delayedFadeOut(arrayOf(binding.toolbar.root, binding.videoBar.root))
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer.attachViews(videoLayout, null, false, false)
        play()
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
            if (mediaPlayer.audioTracksCount > 0)
                binding.videoBar.btnMute.visibility = View.VISIBLE
            initBars()
        } else if (ev.type == MediaPlayer.Event.EndReached && remotePath != "") {
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
        gestureDetector.reset()
        initBars(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }
}