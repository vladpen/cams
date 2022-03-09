package com.vladpen.cams

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.appcompat.app.AppCompatActivity
import com.vladpen.StreamData
import com.vladpen.cams.databinding.ActivityVideoBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class VideoActivity : AppCompatActivity(), MediaPlayer.EventListener {
    private val binding by lazy { ActivityVideoBinding.inflate(layoutInflater) }

    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var videoLayout: VLCVideoLayout
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f

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
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
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

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Let the ScaleGestureDetector inspect all events.
        scaleGestureDetector.onTouchEvent(ev)
        return true
    }

    inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaleFactor *= scaleGestureDetector.scaleFactor
            scaleFactor = max(1f, min(scaleFactor, 10.0f))
            videoLayout.scaleX = scaleFactor
            videoLayout.scaleY = scaleFactor
            return true
        }
    }
}
