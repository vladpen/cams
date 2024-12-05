package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vladpen.*
import com.vladpen.Effects.edgeToEdge
import com.vladpen.cams.databinding.ActivityFilesBinding

class FilesActivity: AppCompatActivity() {
    private val binding by lazy { ActivityFilesBinding.inflate(layoutInflater) }
    private lateinit var remotePath: String
    private var streamId: Int = -1
    private lateinit var stream: StreamDataModel
    private var sftpData: UrlDataModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        edgeToEdge(binding.root)
        initActivity()
    }

    private fun initActivity() {
        streamId = intent.getIntExtra("streamId", -1)
        stream = StreamData.getById(streamId) ?: return

        sftpData = Utils.parseUrl(stream.sftp, 22, "sftp")
        if (sftpData == null) {
            streamsScreen()
            return
        }
        remotePath = intent.getStringExtra("remotePath") ?: sftpData!!.path
        remotePath = Utils.addTrailingSlash(remotePath)

        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
        this.onBackPressedDispatcher.addCallback(callback)
        binding.toolbar.tvLabel.text = stream.name

        binding.toolbar.btnLink.setImageResource(R.drawable.ic_outline_videocam_24)
        binding.toolbar.btnLink.contentDescription = getString(R.string.back)
        binding.toolbar.btnLink.visibility = View.VISIBLE
        binding.toolbar.btnLink.setOnClickListener {
            streamsScreen()
        }
        val files = FileData(stream.sftp).getAll(remotePath)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = FileAdapter(files, remotePath, streamId, stream.sftp) {
            it: Intent -> startActivity(it)
        }
        Alert.init(this, binding.toolbar.btnAlert)
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            back()
        }
    }

    private fun back() {
        val path = Utils.addTrailingSlash(sftpData?.path)
        if (sftpData == null || remotePath == path || remotePath == "/") {
            streamsScreen()
        } else {
            val intent = Intent(this, FilesActivity::class.java)
                .putExtra("streamId", streamId)
                .putExtra("remotePath", FileData.getParentPath(remotePath))
            startActivity(intent)
        }
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
}