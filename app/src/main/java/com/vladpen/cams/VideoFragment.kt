package com.vladpen.cams

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vladpen.StreamData
import com.vladpen.StreamDataModel
import com.vladpen.Utils
import com.vladpen.cams.databinding.FragmentVideoBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.IOException

class VideoFragment : Fragment() {
    private val binding by lazy { FragmentVideoBinding.inflate(layoutInflater) }

    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var videoLayout: VLCVideoLayout

    private var streamId: Int = -1
    private lateinit var stream: StreamDataModel

    companion object {
        /**
         * Factory method to create a new instance of this fragment using the provided parameters
         *
         * @param streamId
         * @return A new instance of this fragment
         */
        @JvmStatic
        fun newInstance(streamId: Int) =
            VideoFragment().apply {
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
        initFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding.root.setOnClickListener {
            val intent = Intent(context, VideoActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("streamId", streamId)
            context?.startActivity(intent)
        }
        return binding.root
    }

    private fun initFragment() {
        videoLayout = binding.videoLayout
        libVlc = LibVLC(requireContext(), ArrayList<String>().apply {
            if (stream.tcp)
                add("--rtsp-tcp")
            if (!StreamData.logConnections)
                add("--verbose=-1")
        })
        mediaPlayer = MediaPlayer(libVlc)
    }

    private fun play() {
        try {
            val media = Media(libVlc, Uri.parse(Utils.decodeUrl(stream.url)))

            media.apply {
                setHWDecoderEnabled(false, false)
                addOption(":network-caching=300")
                mediaPlayer.media = this
            }.release()

            mediaPlayer.play()
            mediaPlayer.volume = 0

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()

        mediaPlayer.attachViews(videoLayout, null, false, false)
        mediaPlayer.volume = 0
        play()

        mediaPlayer.setEventListener {
            if (it.type == MediaPlayer.Event.Buffering && it.buffering == 100f)
                binding.pbLoading.visibility = View.GONE
        }
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
}