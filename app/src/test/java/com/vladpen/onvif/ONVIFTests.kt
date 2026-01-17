package com.vladpen.onvif

import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

class ONVIFSecurityTest {

    @Test
    fun testInputValidation() {
        val maliciousInput = "<script>alert('xss')</script>test"
        val sanitized = ONVIFSecurity.validateInput(maliciousInput)
        
        assertFalse("Should remove script tags", sanitized.contains("<script>"))
        assertTrue("Should keep safe content", sanitized.contains("test"))
    }

    @Test
    fun testUrlValidation() {
        assertTrue("Valid HTTP URL", ONVIFSecurity.isValidUrl("http://192.168.1.100:8080/onvif"))
        assertTrue("Valid HTTPS URL", ONVIFSecurity.isValidUrl("https://camera.local/onvif"))
        assertFalse("Invalid protocol", ONVIFSecurity.isValidUrl("ftp://192.168.1.100"))
        assertFalse("Malformed URL", ONVIFSecurity.isValidUrl("not-a-url"))
    }

    @Test
    fun testResponseSanitization() {
        val maliciousResponse = """
            <response>
                <data>Valid content</data>
                <script>alert('xss')</script>
                <img src="x" onerror="javascript:alert('xss')">
            </response>
        """.trimIndent()
        
        val sanitized = ONVIFSecurity.sanitizeDeviceResponse(maliciousResponse)
        
        assertFalse("Should remove script tags", sanitized.contains("<script>"))
        assertFalse("Should remove javascript URLs", sanitized.contains("javascript:"))
        assertTrue("Should keep valid content", sanitized.contains("Valid content"))
    }
}

class PTZControllerTest {

    @Test
    fun testPTZDirectionMapping() {
        // Test that PTZ directions map correctly
        val directions = PTZDirection.values()
        assertEquals("Should have 4 directions", 4, directions.size)
        
        assertTrue("Should contain UP", directions.contains(PTZDirection.UP))
        assertTrue("Should contain DOWN", directions.contains(PTZDirection.DOWN))
        assertTrue("Should contain LEFT", directions.contains(PTZDirection.LEFT))
        assertTrue("Should contain RIGHT", directions.contains(PTZDirection.RIGHT))
    }
}

class ONVIFDeviceTest {

    @Test
    fun testDeviceCreation() {
        val capabilities = DeviceCapabilities(
            supportsPTZ = true,
            supportsMotionEvents = true,
            ptzCapabilities = PTZCapabilities(true, true, true, true, 10),
            eventCapabilities = EventCapabilities(true, false, 5)
        )
        
        val credentials = ONVIFCredentials("admin", "password")
        
        val device = ONVIFDevice(
            deviceId = "test-device-1",
            name = "Test Camera",
            ipAddress = "192.168.1.100",
            onvifPort = 8080,
            rtspUrl = "rtsp://192.168.1.100:554/stream1",
            capabilities = capabilities,
            credentials = credentials
        )
        
        assertEquals("Device ID should match", "test-device-1", device.deviceId)
        assertEquals("Name should match", "Test Camera", device.name)
        assertEquals("IP should match", "192.168.1.100", device.ipAddress)
        assertEquals("Port should match", 8080, device.onvifPort)
        assertTrue("Should support PTZ", device.capabilities.supportsPTZ)
        assertTrue("Should support motion events", device.capabilities.supportsMotionEvents)
        assertNotNull("Should have credentials", device.credentials)
    }
}

class DeviceCapabilitiesTest {

    @Test
    fun testCapabilitiesWithPTZ() {
        val ptzCaps = PTZCapabilities(
            supportsPan = true,
            supportsTilt = true,
            supportsZoom = true,
            supportsPresets = true,
            maxPresets = 20
        )
        
        val capabilities = DeviceCapabilities(
            supportsPTZ = true,
            supportsMotionEvents = false,
            ptzCapabilities = ptzCaps,
            eventCapabilities = null
        )
        
        assertTrue("Should support PTZ", capabilities.supportsPTZ)
        assertFalse("Should not support motion events", capabilities.supportsMotionEvents)
        assertNotNull("Should have PTZ capabilities", capabilities.ptzCapabilities)
        assertNull("Should not have event capabilities", capabilities.eventCapabilities)
        
        assertEquals("Max presets should be 20", 20, ptzCaps.maxPresets)
    }

    @Test
    fun testCapabilitiesWithEvents() {
        val eventCaps = EventCapabilities(
            supportsMotionDetection = true,
            supportsTamperDetection = false,
            maxEventSubscriptions = 5
        )
        
        val capabilities = DeviceCapabilities(
            supportsPTZ = false,
            supportsMotionEvents = true,
            ptzCapabilities = null,
            eventCapabilities = eventCaps
        )
        
        assertFalse("Should not support PTZ", capabilities.supportsPTZ)
        assertTrue("Should support motion events", capabilities.supportsMotionEvents)
        assertNull("Should not have PTZ capabilities", capabilities.ptzCapabilities)
        assertNotNull("Should have event capabilities", capabilities.eventCapabilities)
        
        assertTrue("Should support motion detection", eventCaps.supportsMotionDetection)
        assertFalse("Should not support tamper detection", eventCaps.supportsTamperDetection)
    }
}
