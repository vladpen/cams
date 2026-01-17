package com.vladpen.onvif

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vladpen.cams.MainApp.Companion.context

data class PTZPreset(
    val id: String,
    val name: String,
    val deviceId: String,
    val presetToken: String
)

object PTZPresetManager {
    private const val PRESETS_FILE_NAME = "ptz_presets.json"
    private var presets = mutableListOf<PTZPreset>()

    fun getPresetsForDevice(deviceId: String): List<PTZPreset> {
        loadPresets()
        return presets.filter { it.deviceId == deviceId }
    }

    fun addPreset(preset: PTZPreset) {
        loadPresets()
        // Remove existing preset with same name for this device
        presets.removeAll { it.deviceId == preset.deviceId && it.name == preset.name }
        presets.add(preset)
        savePresets()
    }

    fun removePreset(deviceId: String, presetName: String) {
        loadPresets()
        presets.removeAll { it.deviceId == deviceId && it.name == presetName }
        savePresets()
    }

    fun getPreset(deviceId: String, presetName: String): PTZPreset? {
        loadPresets()
        return presets.find { it.deviceId == deviceId && it.name == presetName }
    }

    private fun loadPresets() {
        if (presets.isNotEmpty()) return

        try {
            context.openFileInput(PRESETS_FILE_NAME).use { inputStream ->
                val json = inputStream.bufferedReader().use { it.readText() }
                val listType = object : TypeToken<List<PTZPreset>>() {}.type
                presets = Gson().fromJson<List<PTZPreset>>(json, listType).toMutableList()
            }
        } catch (e: Exception) {
            presets = mutableListOf()
        }
    }

    private fun savePresets() {
        try {
            context.openFileOutput(PRESETS_FILE_NAME, Context.MODE_PRIVATE).use {
                it.write(Gson().toJson(presets).toByteArray())
            }
        } catch (e: Exception) {
            // Handle save error
        }
    }
}
