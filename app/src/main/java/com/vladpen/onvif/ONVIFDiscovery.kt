package com.vladpen.onvif

import kotlinx.coroutines.*
import java.net.*
import java.util.*

class ONVIFDiscovery {
    companion object {
        private const val MULTICAST_ADDRESS = "239.255.255.250"
        private const val MULTICAST_PORT = 3702
        private const val DISCOVERY_TIMEOUT = 10000L
    }

    suspend fun discoverDevices(): List<ONVIFDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<ONVIFDevice>()
        
        try {
            val socket = DatagramSocket()
            socket.soTimeout = DISCOVERY_TIMEOUT.toInt()
            
            val probeMessage = createProbeMessage()
            val group = InetAddress.getByName(MULTICAST_ADDRESS)
            val packet = DatagramPacket(
                probeMessage.toByteArray(),
                probeMessage.length,
                group,
                MULTICAST_PORT
            )
            
            socket.send(packet)
            
            // Listen for responses
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT) {
                try {
                    val buffer = ByteArray(8192)
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)
                    
                    val response = String(responsePacket.data, 0, responsePacket.length)
                    parseProbeMatch(response)?.let { device ->
                        if (!devices.any { it.deviceId == device.deviceId }) {
                            devices.add(device)
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    break
                }
            }
            
            socket.close()
        } catch (e: Exception) {
            // Log error but return partial results
        }
        
        devices
    }

    private fun createProbeMessage(): String {
        val uuid = UUID.randomUUID().toString()
        return """<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsd="http://schemas.xmlsoap.org/ws/2005/04/discovery" xmlns:tns="http://www.onvif.org/ver10/network/wsdl">
    <soap:Header>
        <wsa:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action>
        <wsa:MessageID>urn:uuid:$uuid</wsa:MessageID>
        <wsa:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To>
    </soap:Header>
    <soap:Body>
        <wsd:Probe>
            <wsd:Types>tns:NetworkVideoTransmitter</wsd:Types>
        </wsd:Probe>
    </soap:Body>
</soap:Envelope>"""
    }

    private fun parseProbeMatch(response: String): ONVIFDevice? {
        return try {
            // Simple XML parsing for essential fields
            val deviceId = extractXmlValue(response, "wsa:EndpointReference", "wsa:Address") ?: return null
            val xAddrs = extractXmlValue(response, "wsd:XAddrs") ?: return null
            
            // Extract IP and port from XAddrs
            val url = xAddrs.split(" ").firstOrNull() ?: return null
            val uri = URI(url)
            val ipAddress = uri.host
            val port = if (uri.port != -1) uri.port else 80
            
            // Get device info
            val deviceInfo = getDeviceInfo(url)
            
            ONVIFDevice(
                deviceId = deviceId,
                name = deviceInfo?.name ?: "ONVIF Camera",
                ipAddress = ipAddress,
                onvifPort = port,
                rtspUrl = "", // Will be populated later
                capabilities = deviceInfo?.capabilities ?: DeviceCapabilities(false, false, null, null),
                credentials = null
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractXmlValue(xml: String, vararg tags: String): String? {
        var content = xml
        for (tag in tags) {
            val startTag = "<$tag>"
            val endTag = "</$tag>"
            val start = content.indexOf(startTag)
            val end = content.indexOf(endTag)
            if (start == -1 || end == -1) return null
            content = content.substring(start + startTag.length, end)
        }
        return content.trim()
    }

    private fun getDeviceInfo(serviceUrl: String): DeviceInfo? {
        return try {
            val client = ONVIFSoapClient(serviceUrl, null)
            val response = client.sendRequest("GetDeviceInformation")
            
            response?.let {
                val manufacturer = it.getProperty("Manufacturer") ?: "Unknown"
                val model = it.getProperty("Model") ?: "Camera"
                val name = "$manufacturer $model"
                val capabilities = getDeviceCapabilities(serviceUrl)
                DeviceInfo(name, capabilities)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getDeviceCapabilities(serviceUrl: String): DeviceCapabilities {
        return try {
            val client = ONVIFSoapClient(serviceUrl, null)
            val response = client.sendRequest("GetCapabilities")
            
            response?.let {
                val responseStr = it.toString()
                val ptzSupported = responseStr.contains("PTZ")
                val eventSupported = responseStr.contains("Events")
                
                DeviceCapabilities(
                    supportsPTZ = ptzSupported,
                    supportsMotionEvents = eventSupported,
                    ptzCapabilities = if (ptzSupported) PTZCapabilities(true, true, true, true, 10) else null,
                    eventCapabilities = if (eventSupported) EventCapabilities(true, false, 5) else null
                )
            } ?: DeviceCapabilities(false, false, null, null)
        } catch (e: Exception) {
            DeviceCapabilities(false, false, null, null)
        }
    }

    private fun extractCapabilityUrl(response: ONVIFResponse, capability: String): String? {
        // Simplified capability URL extraction
        return try {
            response.getProperty(capability)
        } catch (e: Exception) {
            null
        }
    }

    private data class DeviceInfo(
        val name: String,
        val capabilities: DeviceCapabilities
    )
}
