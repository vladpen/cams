package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.vladpen.*
import com.vladpen.cams.databinding.ActivityEditBinding

class EditActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditBinding.inflate(layoutInflater) }
    private var streamId: Int = -1
    private val streams by lazy { StreamData.getAll() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        streamId = intent.getIntExtra("streamId", -1)

        val stream = StreamData.getById(streamId)
        if (stream == null) {
            streamId = -1
            binding.toolbar.tvToolbarLabel.text = getString(R.string.cam_add)
            binding.tvDeleteLink.visibility = View.GONE
            binding.llChannelBox.layoutParams.height = 0
        } else {
            binding.toolbar.tvToolbarLabel.text = stream.name

            binding.etEditName.setText(stream.name)
            binding.etEditUrl.setText(safeUrl(stream.url))
            binding.etEditChannel.setText(safeUrl(stream.url2))
            binding.etEditSftpUrl.setText(safeUrl(stream.sftp))
            binding.scEditTcp.isChecked = !stream.tcp

            binding.tvDeleteLink.setOnClickListener {
                delete()
            }
            if (stream.url2 == null)
                binding.llChannelBox.layoutParams.height = 0
            else
                binding.tvAddChannel.visibility = View.GONE
        }
        binding.tvAddChannel.setOnClickListener {
            binding.tvAddChannel.visibility = View.GONE
            binding.tvDelChannel.visibility = View.VISIBLE
            binding.etEditChannel.setText(binding.etEditUrl.text.toString().trim())
            Effects.toggle(binding.llChannelBox)
        }
        binding.tvDelChannel.setOnClickListener {
            binding.tvDelChannel.visibility = View.GONE
            binding.tvAddChannel.visibility = View.VISIBLE
            binding.etEditChannel.setText("")
            Effects.toggle(binding.llChannelBox)
        }
        binding.btnSave.setOnClickListener {
            save()
        }
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
        this.onBackPressedDispatcher.addCallback(callback)
    }

    private fun safeUrl(url: String?): String {
        return if (url != null) Utils.replacePassword(url, "***") else ""
    }

    private fun getEncodedUrl(newUrl: String, oldUrl: String?): String {
        val new = Utils.parseUrl(newUrl)
        val old = Utils.parseUrl(oldUrl)
        if (new != null && old != null && new.password == "***")
            return Utils.replacePassword(newUrl, old.password)
        if (new != null && new.password != "")
            return Utils.replacePassword(newUrl, Utils.encodeString(new.password))
        return newUrl
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (streams.isNotEmpty())
                back()
            else
                finishAffinity()
        }
    }

    private fun save() {
        val oldStream = StreamData.getById(streamId)

        var streamUrl = binding.etEditUrl.text.toString().trim()
        streamUrl = getEncodedUrl(streamUrl, oldStream?.url)

        var channelUrl = binding.etEditChannel.text.toString().trim()
        val oldChannelUrl = if (oldStream?.url2 != null) oldStream.url2
            else if (oldStream?.url != null) oldStream.url
            else streamUrl
        channelUrl = getEncodedUrl(channelUrl, oldChannelUrl)

        var sftpUrl = binding.etEditSftpUrl.text.toString().trim()
        sftpUrl = getEncodedUrl(sftpUrl, oldStream?.sftp)

        if (!validate(streamUrl, channelUrl))
            return

        val newStream = StreamDataModel(
            binding.etEditName.text.toString().trim(),
            streamUrl,
            if (channelUrl != "") channelUrl else null,
            !binding.scEditTcp.isChecked,
            if (sftpUrl != "") sftpUrl else null
        )
        if (streamId < 0) {
            StreamData.add(newStream)
        } else {
            StreamData.update(streamId, newStream)
        }
        back()
    }

    private fun validate(streamUrl: String, channelUrl: String): Boolean {
        val name = binding.etEditName.text.toString().trim()
        val url = binding.etEditUrl.text.toString().trim()
        var ok = true

        if (name.isEmpty() || name.length > 255) {
            binding.etEditName.error = getString(R.string.err_invalid)
            ok = false
        }
        if (url.isEmpty() || url.length > 255) {
            binding.etEditUrl.error = getString(R.string.err_invalid)
            ok = false
        }
        for (i in streams.indices) {
            if (i == streamId)
                break

            if (streams[i].name == name) {
                binding.etEditName.error = getString(R.string.err_cam_exists)
                ok = false
            }
            if (streams[i].url == url) {
                binding.etEditUrl.error = getString(R.string.err_cam_exists)
                ok = false
            }
        }
        if (channelUrl != "" && channelUrl == streamUrl) {
            binding.etEditChannel.error = getString(R.string.err_channels_equal)
            ok = false
        }
        return ok
    }

    private fun delete() {
        AlertDialog.Builder(this)
            .setMessage(R.string.cam_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                StreamData.delete(streamId)
                back()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun back() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}