package com.vladpen.onvif

data class ONVIFDevice(
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val onvifPort: Int,
    val rtspUrl: String,
    val capabilities: DeviceCapabilities,
    val credentials: ONVIFCredentials?
)
