package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.vladpen.StreamData
import com.vladpen.StreamDataModel
import com.vladpen.cams.databinding.ActivityEditBinding

class EditActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditBinding.inflate(layoutInflater) }
    private val streams by lazy { StreamData.getStreams(this) }
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
            binding.toolbar.tvToolbarLabel.text = getString(R.string.cam_add)
        } else {
            binding.toolbar.tvToolbarLabel.text = stream.name

            binding.etEditName.setText(stream.name)
            binding.etEditUrl.setText(stream.url)
            binding.scEditTcp.isChecked = !stream.tcp

            binding.tvDeleteLink.visibility = View.VISIBLE
            binding.tvDeleteLink.setOnClickListener {
                delete()
            }
        }
        binding.btnSave.setOnClickListener {
            save()
        }
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
    }

    private fun save() {
        if (!validate()) {
            return
        }
        StreamData.save(this, position, StreamDataModel(
            binding.etEditName.text.toString().trim(),
            binding.etEditUrl.text.toString().trim(),
            !binding.scEditTcp.isChecked
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
            if (i == position) {
                break
            }
            if (streams[i].name == name) {
                binding.etEditName.error = getString(R.string.err_cam_exists)
                ok = false
            }
            if (streams[i].name == url) {
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
                StreamData.delete(this, position)
                back()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun back() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}