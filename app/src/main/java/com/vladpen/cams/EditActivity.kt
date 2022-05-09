package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.vladpen.StreamData
import com.vladpen.StreamDataModel
import com.vladpen.Utils
import com.vladpen.cams.databinding.ActivityEditBinding

class EditActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditBinding.inflate(layoutInflater) }
    private var streamId: Int = -1
    private val streams by lazy { StreamData.getStreams(this) }

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
        } else {
            binding.toolbar.tvToolbarLabel.text = stream.name

            binding.etEditName.setText(stream.name)
            binding.etEditUrl.setText(safeUrl(stream.url))
            binding.etEditSftpUrl.setText(safeUrl(stream.sftp))
            binding.scEditTcp.isChecked = !stream.tcp

            binding.tvDeleteLink.setOnClickListener {
                delete()
            }
        }
        binding.btnSave.setOnClickListener {
            save()
        }
        if (streams.count() > 0) {
            binding.toolbar.btnBack.setOnClickListener {
                back()
            }
        } else {
            binding.toolbar.btnBack.visibility = View.GONE
            binding.toolbar.tvToolbarLabel.setPadding(30)
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
            return Utils.replacePassword(newUrl, Utils.encodeString(this, new.password))
        return newUrl
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (streams.count() > 0)
                back()
            else
                finishAffinity()
        }
    }

    private fun save() {
        if (!validate())
            return

        val stream = StreamData.getById(streamId)

        var streamUrl = binding.etEditUrl.text.toString().trim()
        streamUrl = getEncodedUrl(streamUrl, stream?.url)

        var sftpUrl = binding.etEditSftpUrl.text.toString().trim()
        sftpUrl = getEncodedUrl(sftpUrl, stream?.sftp)

        StreamData.save(this, streamId, StreamDataModel(
            binding.etEditName.text.toString().trim(),
            streamUrl,
            !binding.scEditTcp.isChecked,
            if (sftpUrl != "") sftpUrl else null,
            stream?.id
        ))
        back()
    }

    private fun validate(): Boolean {
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
        return ok
    }

    private fun delete() {
        AlertDialog.Builder(this)
            .setMessage(R.string.cam_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                StreamData.delete(this, streamId)
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