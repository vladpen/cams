package com.vladpen.onvif

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class ONVIFManager private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: ONVIFManager? = null
        
        fun getInstance(): ONVIFManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ONVIFManager().also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val deviceCache = ConcurrentHashMap<String, ONVIFDevice>()
    private val ptzControllers = ConcurrentHashMap<String, PTZController>()
    private val motionServices = ConcurrentHashMap<String, MotionEventService>()
    private val discovery = ONVIFDiscovery()

    interface ONVIFManagerListener {
        fun onDeviceDiscovered(devices: List<ONVIFDevice>)
        fun onMotionDetected(deviceId: String)
        fun onMotionStopped(deviceId: String)
        fun onPTZError(deviceId: String, error: String)
    }

    private var listener: ONVIFManagerListener? = null

    fun setListener(listener: ONVIFManagerListener?) {
        this.listener = listener
    }

    suspend fun discoverDevices(): List<ONVIFDevice> = withContext(Dispatchers.IO) {
        try {
            val devices = discovery.discoverDevices()
            
            // Cache discovered devices
            devices.forEach { device ->
                deviceCache[device.deviceId] = device
            }
            
            listener?.onDeviceDiscovered(devices)
            devices
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun initializePTZController(deviceId: String, serviceUrl: String, credentials: ONVIFCredentials?): PTZController? {
        return try {
            val controller = PTZController(serviceUrl, credentials)
            ptzControllers[deviceId] = controller
            
            // Initialize controller in background
            scope.launch {
                val success = controller.initialize()
                if (!success) {
                    ptzControllers.remove(deviceId)
                    listener?.onPTZError(deviceId, "Failed to initialize PTZ controller")
                }
            }
            
            controller
        } catch (e: Exception) {
            listener?.onPTZError(deviceId, "Error creating PTZ controller: ${e.message}")
            null
        }
    }

    fun getPTZController(deviceId: String): PTZController? {
        return ptzControllers[deviceId]
    }

    fun initializeMotionEventService(deviceId: String, serviceUrl: String, credentials: ONVIFCredentials?): MotionEventService? {
        return try {
            val service = MotionEventService(serviceUrl, credentials)
            service.setListener(object : MotionEventService.MotionEventListener {
                override fun onMotionDetected(deviceId: String) {
                    listener?.onMotionDetected(deviceId)
                }

                override fun onMotionStopped(deviceId: String) {
                    listener?.onMotionStopped(deviceId)
                }
            })
            
            motionServices[deviceId] = service
            service
        } catch (e: Exception) {
            null
        }
    }

    fun getMotionEventService(deviceId: String): MotionEventService? {
        return motionServices[deviceId]
    }

    suspend fun subscribeToMotionEvents(deviceId: String): Boolean {
        return motionServices[deviceId]?.subscribeToMotionEvents(deviceId) ?: false
    }

    suspend fun unsubscribeFromMotionEvents(deviceId: String): Boolean {
        return motionServices[deviceId]?.unsubscribeFromMotionEvents(deviceId) ?: false
    }

    fun getCachedDevice(deviceId: String): ONVIFDevice? {
        return deviceCache[deviceId]
    }

    fun cacheDevice(device: ONVIFDevice) {
        deviceCache[device.deviceId] = device
    }

    fun removeDevice(deviceId: String) {
        deviceCache.remove(deviceId)
        ptzControllers.remove(deviceId)?.let { 
            // Cleanup PTZ controller if needed
        }
        motionServices.remove(deviceId)?.cleanup()
    }

    fun cleanup() {
        scope.cancel()
        deviceCache.clear()
        ptzControllers.clear()
        motionServices.values.forEach { it.cleanup() }
        motionServices.clear()
    }

    // Convenience methods for common operations
    suspend fun performPTZMove(deviceId: String, direction: PTZDirection, speed: Float = 0.5f): Boolean {
        return ptzControllers[deviceId]?.continuousMove(direction, speed) ?: false
    }

    suspend fun stopPTZMovement(deviceId: String): Boolean {
        return ptzControllers[deviceId]?.stop() ?: false
    }

    suspend fun performZoom(deviceId: String, factor: Float): Boolean {
        return ptzControllers[deviceId]?.zoom(factor) ?: false
    }

    suspend fun gotoPreset(deviceId: String, presetToken: String): Boolean {
        return ptzControllers[deviceId]?.gotoPreset(presetToken) ?: false
    }

    suspend fun setPreset(deviceId: String, name: String): String? {
        return ptzControllers[deviceId]?.setPreset(name)
    }
}
