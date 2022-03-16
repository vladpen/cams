package com.vladpen.cams

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import com.vladpen.StreamData
import com.vladpen.VideoGestureDetector
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

    private lateinit var gestureDetector: VideoGestureDetector

    private var position: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        position = intent.getIntExtra("position", -1)

        val stream = StreamData.getByPosition(position)
        if (stream == null) {
            position = -1
            return
        }

        binding.toolbar.tvToolbarLabel.text = stream.name
        binding.toolbar.btnBack.setOnClickListener {
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
        }

        videoLayout = binding.videoLayout

        libVlc = LibVLC(this, ArrayList<String>().apply {
            if (stream.tcp) {
                add("--rtsp-tcp")
            }
        })
        mediaPlayer = MediaPlayer(libVlc)
        mediaPlayer.setEventListener(this)

        mediaPlayer.attachViews(videoLayout, null, false, false)

        try {
            val uri = Uri.parse(stream.url)
            Media(libVlc, uri).apply {
                setHWDecoderEnabled(true, false)
                addOption(":network-caching=150")
                mediaPlayer.media = this
            }.release()

            mediaPlayer.play()

        } catch (e: IOException) {
            e.printStackTrace()
        }

        gestureDetector = VideoGestureDetector(this, videoLayout)
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
        }
    }

    override fun onTouchEvent(event: MotionEvent) =
        gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        gestureDetector.reset()
    }
}