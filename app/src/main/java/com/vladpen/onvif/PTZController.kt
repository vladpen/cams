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
        android.util.Log.d("PTZ_CONTROLLER", "=== PTZ INITIALIZATION START ===")
        android.util.Log.d("PTZ_CONTROLLER", "Service URL: $serviceUrl")
        android.util.Log.d("PTZ_CONTROLLER", "Username: ${credentials?.username}")
        
        // Discover service URLs from GetCapabilities
        val deviceClient = ONVIFSoapClient(serviceUrl, credentials)
        val capabilitiesResponse = deviceClient.sendRequest("GetCapabilities", "http://www.onvif.org/ver10/device/wsdl")
        
        if (capabilitiesResponse != null) {
            // Try to extract PTZ XAddr from capabilities - look for PTZ.XAddr specifically
            val ptzXAddr = capabilitiesResponse.getProperty("PTZ.XAddr")
            android.util.Log.d("PTZ_CONTROLLER", "Raw PTZ.XAddr from capabilities: $ptzXAddr")
            
            ptzServiceUrl = if (ptzXAddr != null && ptzXAddr.isNotEmpty()) {
                // Use hostname instead of IP if XAddr contains IP
                if (ptzXAddr.contains("10.0.0.") || ptzXAddr.contains("192.168.")) {
                    val path = ptzXAddr.substringAfter(":80").substringAfter(":8080")
                    val baseUrl = serviceUrl.substringBefore("/onvif/")
                    "$baseUrl$path"
                } else {
                    ptzXAddr
                }
            } else {
                tryCommonPtzPaths()
            }
            
            mediaServiceUrl = capabilitiesResponse.getProperty("Media.XAddr")
                ?: tryCommonMediaPaths()
            
            android.util.Log.d("PTZ_CONTROLLER", "Final PTZ URL: $ptzServiceUrl")
            android.util.Log.d("PTZ_CONTROLLER", "Final Media URL: $mediaServiceUrl")
        } else {
            // Fallback to common paths
            android.util.Log.d("PTZ_CONTROLLER", "GetCapabilities failed, trying common paths")
            ptzServiceUrl = tryCommonPtzPaths()
            mediaServiceUrl = tryCommonMediaPaths()
            android.util.Log.d("PTZ_CONTROLLER", "Fallback PTZ URL: $ptzServiceUrl")
            android.util.Log.d("PTZ_CONTROLLER", "Fallback Media URL: $mediaServiceUrl")
        }
        
        profileToken = getFirstProfile()
        val success = profileToken != null && ptzServiceUrl != null
        android.util.Log.d("PTZ_CONTROLLER", "Profile token: $profileToken")
        android.util.Log.d("PTZ_CONTROLLER", "=== PTZ INITIALIZATION ${if (success) "SUCCEEDED" else "FAILED"} ===")
        success
    }
    
    private fun tryCommonPtzPaths(): String {
        // Thingino cameras use ptz_service endpoint
        val baseUrl = serviceUrl.substringBefore("/onvif/")
        return "$baseUrl/onvif/ptz_service"
    }
    
    private fun tryCommonMediaPaths(): String {
        // Extract base URL properly - remove /onvif/device_service to get http://host:port
        val baseUrl = serviceUrl.substringBefore("/onvif/")
        return "$baseUrl/onvif/media_service"
    }

    private suspend fun getFirstProfile(): String? {
        val mediaUrl = mediaServiceUrl ?: return null
        
        // Try multiple possible media endpoints
        val mediaUrls = listOf(
            mediaUrl,
            mediaUrl.replace("/Media", "/media_service"),
            mediaUrl.replace("/media_service", "/Media")
        ).distinct()
        
        for (url in mediaUrls) {
            try {
                android.util.Log.d("PTZ_CONTROLLER", "Trying media endpoint: $url")
                val mediaSoapClient = ONVIFSoapClient(url, credentials)
                val response = mediaSoapClient.sendRequest("GetProfiles", MEDIA_NAMESPACE)
                
                if (response != null) {
                    val token = parseProfileTokenFromResponse(response)
                    if (token != null) {
                        android.util.Log.d("PTZ_CONTROLLER", "Found profile token: $token")
                        return token
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d("PTZ_CONTROLLER", "Failed with $url: ${e.message}")
            }
        }
        
        return null
    }

    private fun parseProfileTokenFromResponse(response: ONVIFResponse): String? {
        // Try multiple ways to extract profile token
        val token = response.getProperty("token") 
            ?: response.getProperty("ProfileToken")
            ?: response.getAttribute("token")
        
        if (token != null) {
            android.util.Log.d("PTZ_CONTROLLER", "Extracted profile token: $token")
            return token
        }
        
        // Try to get from Profiles list
        val profiles = response.getPropertyList("Profiles") 
            ?: response.getPropertyList("Profile")
        
        if (profiles != null && profiles.isNotEmpty()) {
            val firstProfile = profiles[0]
            val profileToken = firstProfile.getProperty("token") 
                ?: firstProfile.getAttribute("token")
            android.util.Log.d("PTZ_CONTROLLER", "Extracted token from profile list: $profileToken")
            return profileToken
        }
        
        android.util.Log.d("PTZ_CONTROLLER", "Using fallback profile token")
        return "Profile_0" // Default fallback
    }

    suspend fun continuousMove(direction: PTZDirection, speed: Float = 0.5f): Boolean = withContext(Dispatchers.IO) {
        android.util.Log.d("PTZ_CONTROLLER", "=== PTZ MOVE START ===")
        android.util.Log.d("PTZ_CONTROLLER", "Direction: $direction, Speed: $speed")
        android.util.Log.d("PTZ_CONTROLLER", "Profile Token: $profileToken")
        android.util.Log.d("PTZ_CONTROLLER", "PTZ Service URL: $ptzServiceUrl")
        
        val token = profileToken ?: run {
            android.util.Log.e("PTZ_CONTROLLER", "No profile token available")
            return@withContext false
        }
        val ptzUrl = ptzServiceUrl ?: run {
            android.util.Log.e("PTZ_CONTROLLER", "No PTZ service URL available")
            return@withContext false
        }
        
        val ptzSoapClient = ONVIFSoapClient(ptzUrl, credentials)
        
        // Use RelativeMove instead of ContinuousMove (Thingino cameras don't support ContinuousMove)
        val (panDistance, tiltDistance) = when (direction) {
            PTZDirection.LEFT -> Pair(-0.1f, 0f)
            PTZDirection.RIGHT -> Pair(0.1f, 0f)
            PTZDirection.UP -> Pair(0f, 0.1f)
            PTZDirection.DOWN -> Pair(0f, -0.1f)
        }
        
        android.util.Log.d("PTZ_CONTROLLER", "Pan Distance: $panDistance, Tilt Distance: $tiltDistance")
        
        val moveParams = mapOf(
            "ProfileToken" to token,
            "Translation" to """
                <PanTilt x="$panDistance" y="$tiltDistance"/>
                <Zoom x="0.0"/>
            """.trimIndent(),
            "Speed" to """
                <PanTilt x="$speed" y="$speed"/>
                <Zoom x="$speed"/>
            """.trimIndent()
        )
        
        android.util.Log.d("PTZ_CONTROLLER", "Sending RelativeMove request...")
        val response = ptzSoapClient.sendRequest("RelativeMove", PTZ_NAMESPACE, moveParams)
        val success = response != null
        android.util.Log.d("PTZ_CONTROLLER", "=== PTZ MOVE ${if (success) "SUCCESS" else "FAILED"} ===")
        return@withContext success
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
