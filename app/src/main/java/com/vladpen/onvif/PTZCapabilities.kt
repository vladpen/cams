package com.vladpen.onvif

data class PTZCapabilities(
    val supportsPan: Boolean,
    val supportsTilt: Boolean,
    val supportsZoom: Boolean,
    val supportsPresets: Boolean,
    val maxPresets: Int
)
