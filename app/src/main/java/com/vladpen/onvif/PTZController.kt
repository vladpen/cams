package com.vladpen.onvif

import kotlinx.coroutines.*

class PTZController(
    private val serviceUrl: String,
    private val credentials: ONVIFCredentials?
) {
    companion object {
        private const val PTZ_NAMESPACE = "http://www.onvif.org/ver20/ptz/wsdl"
        private const val MEDIA_NAMESPACE = "http://www.onvif.org/ver10/media/wsdl"
    }

    private var profileToken: String? = null
    private var mediaServiceUrl: String? = null
    private var ptzServiceUrl: String? = null

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        // Determine service URLs based on device service URL
        val baseUrl = serviceUrl.substringBeforeLast("/")
        mediaServiceUrl = "$baseUrl/media_service"
        ptzServiceUrl = "$baseUrl/ptz_service"
        
        profileToken = getFirstProfile()
        profileToken != null
    }

    private suspend fun getFirstProfile(): String? {
        val mediaUrl = mediaServiceUrl ?: return null
        val mediaSoapClient = ONVIFSoapClient(mediaUrl, credentials)
        
        val response = mediaSoapClient.sendRequest("GetProfiles", MEDIA_NAMESPACE)
        return response?.let { parseProfileTokenFromResponse(it) }
    }

    private fun parseProfileTokenFromResponse(response: ONVIFResponse): String? {
        // Try to get profile token from the response
        return response.getProperty("token") ?: 
               response.getProperty("ProfileToken") ?:
               "Profile_0" // Default fallback
    }

    suspend fun continuousMove(direction: PTZDirection, speed: Float = 0.5f): Boolean = withContext(Dispatchers.IO) {
        android.util.Log.d("PTZ_CONTROLLER", "continuousMove called: direction=$direction, speed=$speed")
        val token = profileToken ?: return@withContext false
        val ptzUrl = ptzServiceUrl ?: return@withContext false
        
        val ptzSoapClient = ONVIFSoapClient(ptzUrl, credentials)
        
        val (panSpeed, tiltSpeed) = when (direction) {
            PTZDirection.LEFT -> Pair(-speed, 0f)
            PTZDirection.RIGHT -> Pair(speed, 0f)
            PTZDirection.UP -> Pair(0f, speed)
            PTZDirection.DOWN -> Pair(0f, -speed)
        }
        
        val moveParams = mapOf(
            "ProfileToken" to token,
            "Velocity" to """
                <PanTilt x="$panSpeed" y="$tiltSpeed"/>
                <Zoom x="0.0"/>
            """.trimIndent()
        )
        
        val response = ptzSoapClient.sendRequest("ContinuousMove", PTZ_NAMESPACE, moveParams)
        android.util.Log.d("PTZ_CONTROLLER", "ContinuousMove response: ${response != null}")
        return@withContext response != null
    }

    suspend fun stop(): Boolean = withContext(Dispatchers.IO) {
        android.util.Log.d("PTZ_CONTROLLER", "stop() called")
        val token = profileToken ?: return@withContext false
        val ptzUrl = ptzServiceUrl ?: return@withContext false
        
        val ptzSoapClient = ONVIFSoapClient(ptzUrl, credentials)
        val stopParams = mapOf("ProfileToken" to token)
        
        val response = ptzSoapClient.sendRequest("Stop", PTZ_NAMESPACE, stopParams)
        android.util.Log.d("PTZ_CONTROLLER", "Stop response: ${response != null}")
        return@withContext response != null
    }

    suspend fun zoom(direction: ZoomDirection, speed: Float = 0.5f): Boolean = withContext(Dispatchers.IO) {
        val token = profileToken ?: return@withContext false
        val ptzUrl = ptzServiceUrl ?: return@withContext false
        
        val ptzSoapClient = ONVIFSoapClient(ptzUrl, credentials)
        
        val zoomSpeed = when (direction) {
            ZoomDirection.IN -> speed
            ZoomDirection.OUT -> -speed
        }
        
        val zoomParams = mapOf(
            "ProfileToken" to token,
            "Velocity" to """
                <PanTilt x="0.0" y="0.0"/>
                <Zoom x="$zoomSpeed"/>
            """.trimIndent()
        )
        
        val response = ptzSoapClient.sendRequest("ContinuousMove", PTZ_NAMESPACE, zoomParams)
        return@withContext response != null
    }

    suspend fun gotoPreset(presetToken: String): Boolean = withContext(Dispatchers.IO) {
        val token = profileToken ?: return@withContext false
        val ptzUrl = ptzServiceUrl ?: return@withContext false
        
        val ptzSoapClient = ONVIFSoapClient(ptzUrl, credentials)
        val presetParams = mapOf(
            "ProfileToken" to token,
            "PresetToken" to presetToken
        )
        
        val response = ptzSoapClient.sendRequest("GotoPreset", PTZ_NAMESPACE, presetParams)
        return@withContext response != null
    }

    suspend fun setPreset(presetName: String): Boolean = withContext(Dispatchers.IO) {
        val token = profileToken ?: return@withContext false
        val ptzUrl = ptzServiceUrl ?: return@withContext false
        
        val ptzSoapClient = ONVIFSoapClient(ptzUrl, credentials)
        val presetParams = mapOf(
            "ProfileToken" to token,
            "PresetName" to presetName
        )
        
        val response = ptzSoapClient.sendRequest("SetPreset", PTZ_NAMESPACE, presetParams)
        return@withContext response != null
    }
}

enum class PTZDirection {
    LEFT, RIGHT, UP, DOWN
}

enum class ZoomDirection {
    IN, OUT
}
