package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vladpen.*
import com.vladpen.cams.databinding.ActivityFilesBinding

class FilesActivity: AppCompatActivity() {
    private val binding by lazy { ActivityFilesBinding.inflate(layoutInflater) }
    private lateinit var remotePath: String
    private var streamId: Int = -1
    private lateinit var stream: StreamDataModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        streamId = intent.getIntExtra("streamId", -1)
        stream = StreamData.getById(streamId) ?: return

        val sftpData = FileData.parseSftpUrl(stream.sftp)
        if (sftpData == null) {
            videoScreen()
            return
        }
        remotePath = intent.getStringExtra("remotePath") ?: sftpData.path

        binding.toolbar.btnBack.setOnClickListener {
            if (remotePath == sftpData.path) {
                videoScreen()
            } else {
                val filesIntent = Intent(this, FilesActivity::class.java)
                    .putExtra("streamId", streamId)
                    .putExtra("remotePath", FileData.getParentPath(remotePath))
                startActivity(filesIntent)
           }
        }
        binding.toolbar.tvToolbarLabel.text = stream.name
        binding.toolbar.tvToolbarLink.text = getString(R.string.live)
        binding.toolbar.tvToolbarLink.visibility = View.VISIBLE
        binding.toolbar.tvToolbarLink.setOnClickListener {
            videoScreen()
        }
        binding.toolbar.tvToolbarLink.setTextColor(getColor(R.color.live_link))

        val files = FileData(this, stream.sftp).getFiles(remotePath)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = FilesAdapter(files, remotePath, streamId, stream.sftp)
    }

    private fun videoScreen() {
        val videoIntent = Intent(this, VideoActivity::class.java)
            .putExtra("streamId", streamId)
        startActivity(videoIntent)
    }
}
