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
}
