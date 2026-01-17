package com.vladpen.onvif

import org.junit.Test
import org.junit.Assert.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Unit tests for ONVIF PTZ functionality that can run directly
 */
class ONVIFPTZTests {

    private val testCameraUrl = "http://10.0.0.51:80/onvif/device_service"
    private val testUsername = "thingino"
    private val testPassword = "AjVHVSUPOoZQkGG2U"

    @Test
    fun testONVIFConnectivity() {
        println("=== Testing ONVIF Connectivity ===")
        
        try {
            val credentials = ONVIFCredentials(testUsername, testPassword)
            val soapClient = ONVIFSoapClient(testCameraUrl, credentials)
            
            val deviceInfo = soapClient.sendRequest("GetDeviceInformation")
            
            assertNotNull("Device information should not be null", deviceInfo)
            assertTrue("Device info should contain manufacturer", deviceInfo!!.contains("Manufacturer"))
            
            println("✅ ONVIF connectivity test PASSED")
            println("Device info: ${deviceInfo.substring(0, minOf(200, deviceInfo.length))}...")
            
        } catch (e: Exception) {
            fail("ONVIF connectivity test failed: ${e.message}")
        }
    }

    @Test
    fun testPTZController() {
        println("=== Testing PTZ Controller ===")
        
        try {
            val credentials = ONVIFCredentials(testUsername, testPassword)
            val ptzController = PTZController(testCameraUrl, credentials)
            
            // Test initialization
            val initResult = ptzController.initialize()
            assertTrue("PTZ controller should initialize successfully", initResult)
            println("✅ PTZ initialization: $initResult")
            
            // Test continuous move
            val moveResult = ptzController.continuousMove(0.1f, 0.0f, 0.0f)
            assertTrue("PTZ move should succeed", moveResult)
            println("✅ PTZ move test: $moveResult")
            
            // Wait a moment then stop
            Thread.sleep(1000)
            
            val stopResult = ptzController.stop()
            assertTrue("PTZ stop should succeed", stopResult)
            println("✅ PTZ stop test: $stopResult")
            
        } catch (e: Exception) {
            fail("PTZ controller test failed: ${e.message}")
        }
    }

    @Test
    fun testWSSecurityAuthentication() {
        println("=== Testing WS-Security Authentication ===")
        
        try {
            // Test with correct credentials
            val correctCreds = ONVIFCredentials(testUsername, testPassword)
            val soapClient = ONVIFSoapClient(testCameraUrl, correctCreds)
            
            val result = soapClient.sendRequest("GetDeviceInformation")
            assertNotNull("Request with correct credentials should succeed", result)
            println("✅ Correct credentials test PASSED")
            
            // Test with wrong credentials
            val wrongCreds = ONVIFCredentials("wrong", "wrong")
            val wrongSoapClient = ONVIFSoapClient(testCameraUrl, wrongCreds)
            
            val wrongResult = wrongSoapClient.sendRequest("GetDeviceInformation")
            assertNull("Request with wrong credentials should fail", wrongResult)
            println("✅ Wrong credentials test PASSED (correctly failed)")
            
        } catch (e: Exception) {
            fail("WS-Security authentication test failed: ${e.message}")
        }
    }

    @Test
    fun testPTZCapabilities() {
        println("=== Testing PTZ Capabilities ===")
        
        try {
            val credentials = ONVIFCredentials(testUsername, testPassword)
            val soapClient = ONVIFSoapClient(testCameraUrl, credentials)
            
            // Test GetProfiles
            val profiles = soapClient.sendRequest("GetProfiles", "http://www.onvif.org/ver10/media/wsdl")
            assertNotNull("GetProfiles should return data", profiles)
            println("✅ GetProfiles test PASSED")
            
            // Test GetCapabilities
            val capabilities = soapClient.sendRequest("GetCapabilities")
            assertNotNull("GetCapabilities should return data", capabilities)
            println("✅ GetCapabilities test PASSED")
            
        } catch (e: Exception) {
            fail("PTZ capabilities test failed: ${e.message}")
        }
    }

    @Test
    fun testDirectHTTPConnection() {
        println("=== Testing Direct HTTP Connection ===")
        
        try {
            val url = URL(testCameraUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            
            val soapRequest = """<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
    <soap:Body>
        <tds:GetDeviceInformation xmlns:tds="http://www.onvif.org/ver10/device/wsdl"/>
    </soap:Body>
</soap:Envelope>"""
            
            connection.outputStream.use { it.write(soapRequest.toByteArray()) }
            
            val responseCode = connection.responseCode
            println("HTTP Response Code: $responseCode")
            
            // 401 is expected without authentication, 200 means success
            assertTrue("Response should be 200 or 401", responseCode == 200 || responseCode == 401)
            println("✅ Direct HTTP connection test PASSED")
            
        } catch (e: Exception) {
            fail("Direct HTTP connection test failed: ${e.message}")
        }
    }
}
