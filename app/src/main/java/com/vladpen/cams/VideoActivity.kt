package com.vladpen.cams

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.vladpen.cams.databinding.ActivityVideoBinding
import com.vladpen.onvif.*
import com.vladpen.*
import kotlinx.coroutines.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

class VideoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityVideoBinding.inflate(layoutInflater) }
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var stream: StreamDataModel
    private var streamId: Int = -1
    private lateinit var remotePath: String
    
    // Touch-based PTZ components
    private lateinit var fullscreenManager: FullscreenManager
    private var ptzGestureController: PTZGestureController? = null
    private val ptzScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("VIDEO_ACTIVITY", "New VideoActivity onCreate called")
        setContentView(binding.root)
        
        // Initialize fullscreen manager
        fullscreenManager = FullscreenManager(this)
        fullscreenManager.enableFullscreen()
        
        streamId = intent.getIntExtra("streamId", -1)
        stream = StreamData.getById(streamId) ?: return
        remotePath = intent.getStringExtra("remotePath") ?: return
        
        android.util.Log.d("VIDEO_ACTIVITY", "Setting up PTZ overlay")
        android.util.Log.d("VIDEO_ACTIVITY", "Stream has ONVIF: ${stream.onvifServiceUrl != null}")
        setupPTZControls()
        initPlayer()
        initBackButton()
    }
    
    private fun setupPTZControls() {
        android.util.Log.d("VIDEO_ACTIVITY", "Setting up PTZ controls")
        android.util.Log.d("VIDEO_ACTIVITY", "ONVIF URL: ${stream.onvifServiceUrl}")
        android.util.Log.d("VIDEO_ACTIVITY", "ONVIF Credentials: ${stream.onvifCredentials != null}")
        
        // Always set up the overlay for touch detection, even without ONVIF
        binding.ptzOverlay.isClickable = true
        binding.ptzOverlay.isFocusable = true
        android.util.Log.d("VIDEO_ACTIVITY", "PTZ overlay configured")
        
        // Initialize PTZ controls if ONVIF is configured
        if (stream.onvifServiceUrl != null && stream.onvifCredentials != null) {
            android.util.Log.d("VIDEO_ACTIVITY", "Initializing ONVIF PTZ controller")
            ptzScope.launch {
                try {
                    val ptzController = PTZController(stream.onvifServiceUrl!!, stream.onvifCredentials)
                    if (ptzController.initialize()) {
                        ptzGestureController = PTZGestureController(ptzController, stream)
                        binding.ptzOverlay.setGestureListener(ptzGestureController)
                        android.util.Log.d("VIDEO_ACTIVITY", "PTZ controller initialized successfully")
                    } else {
                        android.util.Log.d("VIDEO_ACTIVITY", "PTZ controller initialization failed")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VIDEO_ACTIVITY", "PTZ initialization error: ${e.message}")
                }
            }
        } else {
            android.util.Log.d("VIDEO_ACTIVITY", "No ONVIF configuration found")
        }
    }

    private fun initPlayer() {
        try {
            libVlc = LibVLC(this)
            mediaPlayer = MediaPlayer(libVlc)
            mediaPlayer.attachViews(binding.videoLayout, null, false, false)
            
            val media = org.videolan.libvlc.Media(libVlc, stream.url)
            mediaPlayer.media = media
            
            mediaPlayer.play()
            
        } catch (e: Exception) {
            finish()
        }
    }
    
    private fun initBackButton() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle orientation changes if needed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ptzGestureController?.cleanup()
        ptzScope.cancel()
        fullscreenManager.disableFullscreen()
        
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.detachViews()
            mediaPlayer.release()
        }
        if (::libVlc.isInitialized) {
            libVlc.release()
        }
    }
}
