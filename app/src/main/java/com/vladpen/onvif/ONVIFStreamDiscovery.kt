package com.vladpen.onvif

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaProfile(
    val token: String,
    val name: String,
    val videoConfig: VideoConfiguration
)

data class VideoConfiguration(
    val width: Int,
    val height: Int,
    val frameRate: Int,
    val bitrate: Int
)

data class StreamDiscoveryResult(
    val primaryRtspUrl: String,
    val secondaryRtspUrl: String?,
    val profiles: List<MediaProfile>
)

class ONVIFStreamDiscovery {
    companion object {
        private const val MEDIA_NAMESPACE = "http://www.onvif.org/ver10/media/wsdl"
    }
    
    suspend fun discoverStreams(onvifUrl: String, credentials: ONVIFCredentials?): StreamDiscoveryResult = withContext(Dispatchers.IO) {
        Log.d("ONVIF", "Starting stream discovery for URL: $onvifUrl")
        
        // For GetProfiles, we need to use the Media service endpoint
        // Try common Media service paths
        val mediaUrls = listOf(
            onvifUrl.replace("/onvif/device_service", "/onvif/Media"),
            onvifUrl.replace("/onvif/device_service", "/onvif/media_service"), 
            onvifUrl.replace("/onvif/device_service", "/onvif/Media2"),
            onvifUrl // fallback to original URL
        )
        
        var profilesResponse: ONVIFResponse? = null
        var workingClient: ONVIFSoapClient? = null
        
        for (mediaUrl in mediaUrls) {
            try {
                Log.d("ONVIF", "Trying Media endpoint: $mediaUrl")
                val client = ONVIFSoapClient(mediaUrl, credentials)
                profilesResponse = client.sendRequest("GetProfiles", MEDIA_NAMESPACE)
                if (profilesResponse != null) {
                    Log.d("ONVIF", "Success with endpoint: $mediaUrl")
                    workingClient = client
                    break
                }
            } catch (e: Exception) {
                Log.d("ONVIF", "Failed with $mediaUrl: ${e.message}")
            }
        }
        
        if (profilesResponse == null || workingClient == null) {
            throw Exception("Failed to get profiles from any Media service endpoint")
        }
        
        val profiles = parseProfiles(profilesResponse)
        if (profiles.isEmpty()) {
            throw Exception("No media profiles found")
        }
        
        // Select best profiles
        val primaryProfile = selectHighestQualityProfile(profiles)
        val secondaryProfile = selectSecondaryProfile(profiles)
        
        // Use the working client for getStreamUri calls
        val primaryRtsp = getStreamUri(workingClient, primaryProfile.token)
        val secondaryRtsp = secondaryProfile?.let { getStreamUri(workingClient, it.token) }
        
        Log.d("ONVIF", "Discovery complete - Primary RTSP: $primaryRtsp, Secondary RTSP: $secondaryRtsp")
        
        StreamDiscoveryResult(
            primaryRtspUrl = primaryRtsp,
            secondaryRtspUrl = secondaryRtsp,
            profiles = profiles
        )
    }
    
    private suspend fun getStreamUri(client: ONVIFSoapClient, profileToken: String): String {
        Log.d("ONVIF", "Getting stream URI for profile token: $profileToken")
        
        val streamSetup = mapOf(
            "Stream" to "RTP-Unicast",
            "Transport" to mapOf("Protocol" to "RTSP")
        )
        val params = mapOf(
            "StreamSetup" to streamSetup,
            "ProfileToken" to profileToken
        )
        
        val response = client.sendRequest("GetStreamUri", MEDIA_NAMESPACE, params)
            ?: throw Exception("Failed to get stream URI")
        
        // Try multiple ways to extract the URI
        val uri = response.getProperty("Uri") 
            ?: response.getProperty("MediaUri.Uri")
            ?: response.getProperty("MediaUri")?.let { 
                Log.d("ONVIF", "Found MediaUri object, trying to extract Uri property")
                response.getProperty("MediaUri.Uri") 
            }
            ?: response.getPropertyAsString("Uri")
            ?: extractRtspFromText(response.debugDumpProperties())
            
        Log.d("ONVIF", "Raw response properties check:")
        Log.d("ONVIF", "- Uri: ${response.getProperty("Uri")}")
        Log.d("ONVIF", "- MediaUri: ${response.getProperty("MediaUri")}")
        Log.d("ONVIF", "- MediaUri.Uri: ${response.getProperty("MediaUri.Uri")}")
        Log.d("ONVIF", response.debugDumpProperties())
            
        Log.d("ONVIF", "Found stream URI: '$uri'")
        
        return uri ?: throw Exception("No stream URI found in response")
    }
    
    private fun extractRtspFromText(text: String): String? {
        // Extract RTSP URL from concatenated text like "rtsp://user:pass@host/pathfalsefalsePT0S"
        val rtspRegex = Regex("(rtsp://[^\\s]+?)(?:false|true|PT\\d+S)")
        val match = rtspRegex.find(text)
        return match?.groupValues?.get(1)
    }
    
    private fun parseProfiles(response: ONVIFResponse): List<MediaProfile> {
        val profiles = mutableListOf<MediaProfile>()
        
        // Try different possible profile element names
        val profileNodes = response.getPropertyList("Profiles") 
            ?: response.getPropertyList("Profile")
            ?: response.getPropertyList("trt:Profiles")
            ?: response.getPropertyList("trt:Profile")
        
        Log.d("ONVIF", "Profile nodes found: ${profileNodes?.size ?: 0}")
        
        if (profileNodes == null) {
            Log.d("ONVIF", "No profile nodes found, checking response structure...")
            return profiles
        }
        
        for (profileNode in profileNodes) {
            // Try different ways to get the token attribute
            val token = profileNode.getProperty("token") 
                ?: profileNode.getProperty("@token")
                ?: profileNode.getPropertyAsString("token")
                ?: profileNode.getAttribute("token")
                
            Log.d("ONVIF", "Processing profile with token: '$token'")
            
            if (token.isNullOrEmpty()) {
                Log.d("ONVIF", "Skipping profile without token")
                continue
            }
            
            val name = profileNode.getProperty("Name") ?: "Profile"
            
            val videoConfig = parseVideoConfiguration(profileNode)
            profiles.add(MediaProfile(token, name, videoConfig))
        }
        
        Log.d("ONVIF", "Parsed ${profiles.size} profiles")
        return profiles
    }
    
    private fun parseVideoConfiguration(profileNode: ONVIFResponse): VideoConfiguration {
        val videoEncoder = profileNode.getProperty("VideoEncoderConfiguration")
        
        val width = profileNode.getProperty("Resolution.Width")?.toIntOrNull() ?: 1920
        val height = profileNode.getProperty("Resolution.Height")?.toIntOrNull() ?: 1080
        val frameRate = profileNode.getProperty("RateControl.FrameRateLimit")?.toIntOrNull() ?: 25
        val bitrate = profileNode.getProperty("RateControl.BitrateLimit")?.toIntOrNull() ?: 2000
        
        return VideoConfiguration(width, height, frameRate, bitrate)
    }
    
    private fun selectHighestQualityProfile(profiles: List<MediaProfile>): MediaProfile {
        return profiles.maxByOrNull { profile ->
            profile.videoConfig.width * profile.videoConfig.height * profile.videoConfig.frameRate
        } ?: profiles.first()
    }
    
    private fun selectSecondaryProfile(profiles: List<MediaProfile>): MediaProfile? {
        if (profiles.size < 2) return null
        
        val sorted = profiles.sortedByDescending { profile ->
            profile.videoConfig.width * profile.videoConfig.height
        }
        
        return sorted.getOrNull(1)
    }
}
