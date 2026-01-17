package com.vladpen.onvif

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

class ONVIFDebugTester(private val context: Context) {
    
    fun runAllTests() {
        Log.d("ONVIF_TEST", "=== Starting ONVIF Debug Tests ===")
        
        // Test 1: Basic connectivity
        testBasicConnectivity()
        
        // Test 2: SOAP client creation
        testSoapClientCreation()
        
        // Test 3: Manager initialization
        testManagerInitialization()
        
        // Test 4: PTZ controller creation
        testPTZControllerCreation()
        
        Log.d("ONVIF_TEST", "=== ONVIF Debug Tests Complete ===")
    }
    
    private fun testBasicConnectivity() {
        Log.d("ONVIF_TEST", "--- Test 1: Basic Connectivity ---")
        
        val testUrl = "http://httpbin.org/post"
        val soapClient = ONVIFSoapClient(testUrl, null)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ONVIF_TEST", "Testing basic HTTP connectivity...")
                val response = soapClient.sendRequest("TestMethod")
                Log.d("ONVIF_TEST", "Basic connectivity test result: ${if (response != null) "SUCCESS" else "FAILED"}")
            } catch (e: Exception) {
                Log.e("ONVIF_TEST", "Basic connectivity test failed: ${e.message}")
            }
        }
    }
    
    private fun testSoapClientCreation() {
        Log.d("ONVIF_TEST", "--- Test 2: SOAP Client Creation ---")
        
        try {
            val testUrl = "http://192.168.1.100:8080/onvif/device_service"
            val credentials = ONVIFCredentials("admin", "password")
            val soapClient = ONVIFSoapClient(testUrl, credentials)
            Log.d("ONVIF_TEST", "SOAP client creation: SUCCESS")
            
            // Test SOAP envelope creation
            Log.d("ONVIF_TEST", "Testing SOAP envelope creation...")
            // This would require making the createSoapEnvelope method public for testing
            Log.d("ONVIF_TEST", "SOAP envelope test: SKIPPED (method private)")
            
        } catch (e: Exception) {
            Log.e("ONVIF_TEST", "SOAP client creation failed: ${e.message}")
        }
    }
    
    private fun testManagerInitialization() {
        Log.d("ONVIF_TEST", "--- Test 3: Manager Initialization ---")
        
        try {
            val manager = ONVIFManager.getInstance()
            Log.d("ONVIF_TEST", "ONVIF Manager creation: SUCCESS")
            
            // Test device caching
            val testDevice = ONVIFDevice(
                deviceId = "test-device",
                name = "Test Camera",
                ipAddress = "192.168.1.100",
                onvifPort = 8080,
                rtspUrl = "rtsp://192.168.1.100:554/stream1",
                capabilities = DeviceCapabilities(true, true, 
                    PTZCapabilities(true, true, true, true, 10),
                    EventCapabilities(true, false, 5)),
                credentials = null
            )
            
            manager.cacheDevice(testDevice)
            val cachedDevice = manager.getCachedDevice("test-device")
            Log.d("ONVIF_TEST", "Device caching test: ${if (cachedDevice != null) "SUCCESS" else "FAILED"}")
            
        } catch (e: Exception) {
            Log.e("ONVIF_TEST", "Manager initialization failed: ${e.message}")
        }
    }
    
    private fun testPTZControllerCreation() {
        Log.d("ONVIF_TEST", "--- Test 4: PTZ Controller Creation ---")
        
        try {
            val testUrl = "http://192.168.1.100:8080/onvif/device_service"
            val credentials = ONVIFCredentials("admin", "password")
            val controller = PTZController(testUrl, credentials)
            Log.d("ONVIF_TEST", "PTZ Controller creation: SUCCESS")
            
            // Test initialization
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("ONVIF_TEST", "Testing PTZ controller initialization...")
                    val initResult = controller.initialize()
                    Log.d("ONVIF_TEST", "PTZ Controller initialization: ${if (initResult) "SUCCESS" else "FAILED"}")
                    
                    // Test PTZ commands
                    Log.d("ONVIF_TEST", "Testing PTZ move command...")
                    val moveResult = controller.continuousMove(PTZDirection.UP, 0.5f)
                    Log.d("ONVIF_TEST", "PTZ move command: ${if (moveResult) "SUCCESS" else "FAILED"}")
                    
                    Log.d("ONVIF_TEST", "Testing PTZ stop command...")
                    val stopResult = controller.stop()
                    Log.d("ONVIF_TEST", "PTZ stop command: ${if (stopResult) "SUCCESS" else "FAILED"}")
                    
                } catch (e: Exception) {
                    Log.e("ONVIF_TEST", "PTZ controller test failed: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e("ONVIF_TEST", "PTZ Controller creation failed: ${e.message}")
        }
    }
    
    fun testRealCamera(onvifUrl: String, username: String, password: String) {
        Log.d("ONVIF_TEST", "=== Testing Real Camera ===")
        Log.d("ONVIF_TEST", "URL: $onvifUrl")
        Log.d("ONVIF_TEST", "Username: $username")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val credentials = ONVIFCredentials(username, password)
                val soapClient = ONVIFSoapClient(onvifUrl, credentials)
                
                // Test GetDeviceInformation
                Log.d("ONVIF_TEST", "Testing GetDeviceInformation...")
                val deviceInfo = soapClient.sendRequest("GetDeviceInformation")
                Log.d("ONVIF_TEST", "GetDeviceInformation result: ${if (deviceInfo != null) "SUCCESS" else "FAILED"}")
                
                // Test GetCapabilities
                Log.d("ONVIF_TEST", "Testing GetCapabilities...")
                val capabilities = soapClient.sendRequest("GetCapabilities")
                Log.d("ONVIF_TEST", "GetCapabilities result: ${if (capabilities != null) "SUCCESS" else "FAILED"}")
                
                // Test GetProfiles
                Log.d("ONVIF_TEST", "Testing GetProfiles...")
                val profiles = soapClient.sendRequest("GetProfiles", "http://www.onvif.org/ver10/media/wsdl")
                Log.d("ONVIF_TEST", "GetProfiles result: ${if (profiles != null) "SUCCESS" else "FAILED"}")
                
            } catch (e: Exception) {
                Log.e("ONVIF_TEST", "Real camera test failed: ${e.message}")
            }
        }
    }
}
