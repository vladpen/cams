package com.vladpen.cams

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.vladpen.*
import com.vladpen.Effects.edgeToEdge
import com.vladpen.Utils.decodeString
import com.vladpen.Utils.encodeString
import com.vladpen.Utils.parseUrl
import com.vladpen.Utils.replacePassword
import com.vladpen.cams.databinding.ActivityEditBinding
import com.vladpen.onvif.*
import kotlinx.coroutines.*

class EditActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditBinding.inflate(layoutInflater) }
    private var streamId: Int = -1
    private val streams by lazy { StreamData.getAll() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        edgeToEdge(binding.root)
        initActivity()
    }

    private fun initActivity() {
        streamId = intent.getIntExtra("streamId", -1)
        val stream = StreamData.getById(streamId)
        if (stream == null) {
            streamId = -1
            binding.toolbar.tvLabel.text = getString(R.string.cam_add)
            binding.tvDeleteLink.visibility = View.GONE
            binding.tvCopyLink.visibility = View.GONE
            if (StreamData.copyStreamId >= 0) {
                binding.tvPasteLink.visibility = View.VISIBLE
                binding.tvPasteLink.setOnClickListener {
                    paste()
                }
            }
            binding.llChannelBox.layoutParams.height = 0
            binding.llSftpBox.layoutParams.height = 0
            binding.rbEditTcp.isChecked = true
        } else {
            binding.toolbar.tvLabel.text = stream.name

            binding.etEditName.setText(stream.name)
            binding.etEditUrl.setText(safeUrl(stream.url))
            binding.etEditChannel.setText(safeUrl(stream.url2))
            binding.etEditSftpUrl.setText(safeUrl(stream.sftp))
            binding.rbEditTcp.isChecked = stream.tcp
            binding.rbEditUdp.isChecked = !stream.tcp
            binding.cbAlert.isChecked = stream.alert == true

            // Populate ONVIF fields if this is an ONVIF device
            if (stream.isOnvifDevice) {
                binding.etOnvifUrl.setText(stream.onvifServiceUrl ?: "")
                stream.onvifCredentials?.let { creds ->
                    binding.etOnvifUsername.setText(creds.username)
                    binding.etOnvifPassword.setText(creds.password)
                }
                // Show ONVIF configuration
                toggleOnvifConfiguration()
            }

            val startup = SourceData.getStartup()
            binding.cbStartup.isChecked = (
                    startup != null && startup.type == "stream" && startup.id == streamId
            )

            binding.tvDeleteLink.setOnClickListener {
                delete()
            }
            binding.tvPasteLink.visibility = View.GONE
            binding.tvCopyLink.visibility = View.VISIBLE
            binding.tvCopyLink.setOnClickListener {
                copy()
            }
            if (stream.url2 == null) {
                binding.llChannelBox.layoutParams.height = 0
            } else {
                binding.tvAddChannel.visibility = View.GONE
            }
            if (stream.sftp == null) {
                binding.llSftpBox.layoutParams.height = 0
            } else {
                binding.tvSftpLabel.visibility = View.VISIBLE
                binding.llSftpBox.visibility = View.VISIBLE
                binding.tvAddSftp.visibility = View.GONE
            }
        }
        binding.tvAddChannel.setOnClickListener {
            Effects.fadeOut(arrayOf(binding.tvAddChannel))
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
        binding.tvAddSftp.setOnClickListener {
            showSftp()
        }
        binding.tvDiscoverOnvif.setOnClickListener {
            discoverOnvifDevices()
        }
        binding.tvAddOnvif.setOnClickListener {
            toggleOnvifConfiguration()
        }
        binding.btnSave.setOnClickListener {
            save()
        }
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
        this.onBackPressedDispatcher.addCallback(callback)

        Alert.init(this, binding.toolbar.btnAlert)
    }

    private fun safeUrl(url: String?): String {
        return if (url != null) replacePassword(url, "***") else ""
    }

    private fun getEncodedUrl(newUrl: String, oldUrl: String?): String {
        val new = parseUrl(newUrl)
        val old = parseUrl(oldUrl)
        if (new != null && old != null && new.password == "***")
            return replacePassword(newUrl, encodeString(decodeString(old.password)))
        if (new != null && new.password != "")
            return replacePassword(newUrl, encodeString(decodeString(new.password)))
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
        val oldStream = if (streamId >= 0)
            StreamData.getById(streamId)
        else
            StreamData.getById(StreamData.copyStreamId)

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

        // Handle ONVIF configuration
        val onvifUrl = binding.etOnvifUrl.text.toString().trim()
        val onvifUsername = binding.etOnvifUsername.text.toString().trim()
        val onvifPassword = binding.etOnvifPassword.text.toString().trim()
        
        val isOnvifDevice = onvifUrl.isNotEmpty()
        val onvifCredentials = if (onvifUsername.isNotEmpty() && onvifPassword.isNotEmpty()) {
            ONVIFCredentials(onvifUsername, onvifPassword)
        } else null

        var newStream = StreamDataModel(
            binding.etEditName.text.toString().trim(),
            streamUrl,
            if (channelUrl != "") channelUrl else null,
            binding.rbEditTcp.isChecked,
            if (sftpUrl != "") sftpUrl else null,
            if (binding.cbAlert.isChecked && sftpUrl != "") true else null,
            isOnvifDevice = isOnvifDevice,
            onvifServiceUrl = if (isOnvifDevice) onvifUrl else null,
            deviceCapabilities = if (isOnvifDevice) {
                // Default capabilities - will be updated when device is tested
                DeviceCapabilities(true, true, 
                    PTZCapabilities(true, true, true, true, 10),
                    EventCapabilities(true, false, 5))
            } else null
        )
        
        // Apply credentials if provided
        if (onvifCredentials != null) {
            newStream = newStream.withCredentials(onvifCredentials)
        }
        if (streamId < 0)
            streamId = StreamData.add(newStream)
        else
            StreamData.update(streamId, newStream)

        SourceData.setStartup(binding.cbStartup.isChecked, "stream", streamId)

        Alert.checkAvailability()
        back()
    }

    private fun validate(url: String, channelUrl: String): Boolean {
        val name = binding.etEditName.text.toString().trim()
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
        if (channelUrl != "" && channelUrl == url) {
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
                Alert.checkAvailability()
                back()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun copy() {
        StreamData.copyStreamId = streamId
        Effects.fadeOut(arrayOf(binding.tvCopyLink))
    }

    private fun paste() {
        Effects.fadeOut(arrayOf(binding.tvPasteLink))
        val stream = StreamData.getById(StreamData.copyStreamId) ?: return

        binding.etEditName.setText(stream.name, TextView.BufferType.EDITABLE)
        binding.etEditUrl.setText(safeUrl(stream.url), TextView.BufferType.EDITABLE)
        binding.etEditSftpUrl.setText(safeUrl(stream.sftp), TextView.BufferType.EDITABLE)
        binding.rbEditTcp.isChecked = stream.tcp
        binding.rbEditUdp.isChecked = !stream.tcp
        binding.cbAlert.isChecked = stream.alert == true
        if (stream.sftp !=null)
            showSftp()
    }

    private fun showSftp() {
        binding.tvSftpLabel.visibility = View.VISIBLE
        Effects.fadeOut(arrayOf(binding.tvAddSftp))
        Effects.toggle(binding.llSftpBox)
    }

    private fun back() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun discoverOnvifDevices() {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Discovering ONVIF Cameras")
            .setMessage("Scanning network for ONVIF devices...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val devices = withContext(Dispatchers.IO) {
                    ONVIFManager.getInstance().discoverDevices()
                }
                
                progressDialog.dismiss()
                
                if (devices.isEmpty()) {
                    AlertDialog.Builder(this@EditActivity)
                        .setTitle("No Devices Found")
                        .setMessage("No ONVIF cameras were discovered on the network.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    showDeviceSelectionDialog(devices)
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@EditActivity)
                    .setTitle("Discovery Error")
                    .setMessage("Error discovering ONVIF devices: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showDeviceSelectionDialog(devices: List<ONVIFDevice>) {
        val deviceNames = devices.map { "${it.name} (${it.ipAddress})" }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Select ONVIF Camera")
            .setItems(deviceNames) { _, which ->
                val selectedDevice = devices[which]
                populateFromOnvifDevice(selectedDevice)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun populateFromOnvifDevice(device: ONVIFDevice) {
        // Auto-populate camera settings from ONVIF device
        binding.etEditName.setText(device.name)
        
        // Generate RTSP URL (simplified - may need device-specific logic)
        val rtspUrl = "rtsp://${device.ipAddress}/stream1"
        binding.etEditUrl.setText(rtspUrl)
        
        // Show credentials dialog if needed
        if (device.credentials == null) {
            showCredentialsDialog(device)
        }
    }

    private fun showCredentialsDialog(device: ONVIFDevice) {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null)
        // Simplified credentials dialog - would need proper layout
        AlertDialog.Builder(this)
            .setTitle("ONVIF Credentials")
            .setMessage("Enter credentials for ${device.name}")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                // Handle credentials input
            }
            .setNegativeButton("Skip", null)
            .show()
    }

    private fun toggleOnvifConfiguration() {
        val isVisible = binding.tvOnvifLabel.visibility == View.VISIBLE
        
        if (isVisible) {
            // Hide ONVIF fields
            binding.tvOnvifLabel.visibility = View.GONE
            binding.etOnvifUrl.visibility = View.GONE
            binding.llOnvifCredentials.visibility = View.GONE
            binding.tvAddOnvif.text = "Add ONVIF Configuration"
        } else {
            // Show ONVIF fields
            binding.tvOnvifLabel.visibility = View.VISIBLE
            binding.etOnvifUrl.visibility = View.VISIBLE
            binding.llOnvifCredentials.visibility = View.VISIBLE
            binding.tvAddOnvif.text = "Hide ONVIF Configuration"
        }
    }
}