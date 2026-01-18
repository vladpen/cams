package com.vladpen.onvif

import android.util.Log
import kotlinx.coroutines.runBlocking

class ONVIFTest {
    companion object {
        fun testStreamDiscovery() {
            runBlocking {
                try {
                    Log.d("ONVIF_TEST", "Starting ONVIF test...")
                    
                    val onvifUrl = "onvif://thingino:AjVHVSUPOoZQkGG2U@10.0.0.51:80/onvif/device_service"
                    val parsed = ONVIFUrlParser.parseOnvifUrl(onvifUrl)
                    
                    if (parsed == null) {
                        Log.e("ONVIF_TEST", "Failed to parse ONVIF URL")
                        return@runBlocking
                    }
                    
                    Log.d("ONVIF_TEST", "Parsed URL - Service: ${parsed.serviceUrl}")
                    Log.d("ONVIF_TEST", "Credentials - User: ${parsed.credentials?.username}")
                    
                    val discovery = ONVIFStreamDiscovery()
                    val result = discovery.discoverStreams(parsed.serviceUrl, parsed.credentials)
                    
                    Log.d("ONVIF_TEST", "SUCCESS!")
                    Log.d("ONVIF_TEST", "Primary RTSP: ${result.primaryRtspUrl}")
                    Log.d("ONVIF_TEST", "Secondary RTSP: ${result.secondaryRtspUrl}")
                    Log.d("ONVIF_TEST", "Profiles found: ${result.profiles.size}")
                    
                } catch (e: Exception) {
                    Log.e("ONVIF_TEST", "Test failed: ${e.message}", e)
                }
            }
        }
    }
}
