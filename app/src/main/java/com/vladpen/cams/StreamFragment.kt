package com.vladpen.cams

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.vladpen.StreamData
import com.vladpen.StreamDataModel
import com.vladpen.Player
import com.vladpen.cams.databinding.FragmentStreamBinding
import com.vladpen.onvif.*
import kotlinx.coroutines.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

class StreamFragment : Fragment(), Player {
    private val binding by lazy { FragmentStreamBinding.inflate(layoutInflater) }

    override lateinit var libVlc: LibVLC
    override lateinit var mediaPlayer: MediaPlayer
    override var volume = 0

    lateinit var stream: StreamDataModel
    var streamId: Int = -1
        private set
    
    // PTZ controls
    private var ptzGestureController: PTZGestureController? = null
    private val ptzScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        /**
         * Factory method to create a new instance of this fragment using the provided parameters
         *
         * @param streamId
         * @return A new instance of this fragment
         */
        @JvmStatic
        fun newInstance(streamId: Int) =
            StreamFragment().apply {
                arguments = Bundle().apply {
                    putInt("streamId", streamId)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            streamId = it.getInt("streamId", -1)
        }
        stream = StreamData.getById(streamId) ?: return
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setupPTZControls()
        return binding.root
    }

    override fun onBufferReady(isAudioAvailable: Boolean) {
        super.onBufferReady(isAudioAvailable)

        (activity as StreamsActivity).hideLoading(streamId)
        if (isAudioAvailable) {
            (activity as StreamsActivity).showMute()
        }
    }

    fun initPlayer() {
        initPlayer(requireContext(), binding.videoLayout, stream.tcp)
    }

    fun getFrame(): FrameLayout {
        return binding.root.parent as FrameLayout
    }
    
    private fun setupPTZControls() {
        android.util.Log.d("STREAM_PTZ", "Setting up PTZ controls for stream $streamId")
        android.util.Log.d("STREAM_PTZ", "ONVIF URL: ${stream.onvifServiceUrl}")
        android.util.Log.d("STREAM_PTZ", "ONVIF Credentials: ${stream.onvifCredentials != null}")
        
        // Always set up the overlay for touch detection
        binding.ptzOverlay.isClickable = true
        binding.ptzOverlay.isFocusable = true
        android.util.Log.d("STREAM_PTZ", "PTZ overlay configured")
        
        // Initialize PTZ controls if ONVIF is configured
        if (stream.onvifServiceUrl != null && stream.onvifCredentials != null) {
            android.util.Log.d("STREAM_PTZ", "Initializing ONVIF PTZ controller")
            android.util.Log.d("STREAM_PTZ", "Service URL: ${stream.onvifServiceUrl}")
            android.util.Log.d("STREAM_PTZ", "Username: ${stream.onvifCredentials?.username}")
            ptzScope.launch {
                try {
                    val ptzController = PTZController(stream.onvifServiceUrl!!, stream.onvifCredentials)
                    android.util.Log.d("STREAM_PTZ", "PTZ controller created, initializing...")
                    if (ptzController.initialize()) {
                        ptzGestureController = PTZGestureController(ptzController, stream)
                        binding.ptzOverlay.setGestureListener(ptzGestureController)
                        android.util.Log.d("STREAM_PTZ", "PTZ controller initialized successfully")
                    } else {
                        android.util.Log.d("STREAM_PTZ", "PTZ controller initialization failed")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("STREAM_PTZ", "PTZ initialization error: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
            android.util.Log.d("STREAM_PTZ", "No ONVIF configuration found")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ptzGestureController?.cleanup()
        ptzScope.cancel()
    }
}
