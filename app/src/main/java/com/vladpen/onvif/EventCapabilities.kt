package com.vladpen.onvif

data class EventCapabilities(
    val supportsMotionDetection: Boolean,
    val supportsTamperDetection: Boolean,
    val maxEventSubscriptions: Int
)
