package com.vladpen.onvif

import kotlinx.coroutines.*

class PTZController(
    private val serviceUrl: String,
    private val credentials: ONVIFCredentials?
) {
    companion object {
        private const val PTZ_NAMESPACE = "http://www.onvif.org/ver20/ptz/wsdl"
    }

    private val soapClient = ONVIFSoapClient(serviceUrl, credentials)
    private var profileToken: String? = null

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        profileToken = getFirstProfile()
        profileToken != null
    }

    suspend fun continuousMove(direction: PTZDirection, speed: Float = 0.5f): Boolean = withContext(Dispatchers.IO) {
        val token = profileToken ?: return@withContext false
        
        val velocity = when (direction) {
            PTZDirection.UP -> mapOf("PanTilt" to mapOf("x" to 0.0, "y" to speed))
            PTZDirection.DOWN -> mapOf("PanTilt" to mapOf("x" to 0.0, "y" to -speed))
            PTZDirection.LEFT -> mapOf("PanTilt" to mapOf("x" to -speed, "y" to 0.0))
            PTZDirection.RIGHT -> mapOf("PanTilt" to mapOf("x" to speed, "y" to 0.0))
        }

        val params = mapOf(
            "ProfileToken" to token,
            "Velocity" to velocity
        )

        soapClient.sendRequest("ContinuousMove", PTZ_NAMESPACE, params) != null
    }

    suspend fun stop(): Boolean = withContext(Dispatchers.IO) {
        val token = profileToken ?: return@withContext false
        
        val params = mapOf(
            "ProfileToken" to token,
            "PanTilt" to true,
            "Zoom" to true
        )

        soapClient.sendRequest("Stop", PTZ_NAMESPACE, params) != null
    }

    suspend fun zoom(factor: Float): Boolean = withContext(Dispatchers.IO) {
        val token = profileToken ?: return@withContext false
        
        val velocity = mapOf("Zoom" to mapOf("x" to factor))
        val params = mapOf(
            "ProfileToken" to token,
            "Velocity" to velocity
        )

        soapClient.sendRequest("ContinuousMove", PTZ_NAMESPACE, params) != null
    }

    suspend fun gotoPreset(presetToken: String): Boolean = withContext(Dispatchers.IO) {
        val token = profileToken ?: return@withContext false
        
        val params = mapOf(
            "ProfileToken" to token,
            "PresetToken" to presetToken
        )

        soapClient.sendRequest("GotoPreset", PTZ_NAMESPACE, params) != null
    }

    suspend fun setPreset(name: String): String? = withContext(Dispatchers.IO) {
        val token = profileToken ?: return@withContext null
        
        val params = mapOf(
            "ProfileToken" to token,
            "PresetName" to name
        )

        val response = soapClient.sendRequest("SetPreset", PTZ_NAMESPACE, params)
        val presetToken = response?.getPropertyAsString("PresetToken")
        
        // Save preset locally
        presetToken?.let { token ->
            val deviceId = serviceUrl // Use service URL as device ID
            val preset = PTZPreset(
                id = "${deviceId}_${name}",
                name = name,
                deviceId = deviceId,
                presetToken = token
            )
            PTZPresetManager.addPreset(preset)
        }
        
        presetToken
    }

    suspend fun getPresets(): List<PTZPreset> = withContext(Dispatchers.IO) {
        val deviceId = serviceUrl // Use service URL as device ID
        PTZPresetManager.getPresetsForDevice(deviceId)
    }

    private fun getFirstProfile(): String? {
        return try {
            val response = soapClient.sendRequest("GetProfiles", "http://www.onvif.org/ver10/media/wsdl")
            response?.getProperty("Profiles")?.toString()?.let { profiles ->
                // Extract first profile token - simplified parsing
                val tokenStart = profiles.indexOf("token=\"") + 7
                val tokenEnd = profiles.indexOf("\"", tokenStart)
                if (tokenStart > 6 && tokenEnd > tokenStart) {
                    profiles.substring(tokenStart, tokenEnd)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

enum class PTZDirection {
    UP, DOWN, LEFT, RIGHT
}
