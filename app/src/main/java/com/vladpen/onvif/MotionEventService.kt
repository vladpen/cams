package com.vladpen.onvif

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class MotionEventService(
    private val serviceUrl: String,
    private val credentials: ONVIFCredentials?
) {
    companion object {
        private const val EVENT_NAMESPACE = "http://www.onvif.org/ver10/events/wsdl"
    }

    private val soapClient = ONVIFSoapClient(serviceUrl, credentials)
    private val subscriptions = ConcurrentHashMap<String, EventSubscription>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    interface MotionEventListener {
        fun onMotionDetected(deviceId: String)
        fun onMotionStopped(deviceId: String)
    }

    private var listener: MotionEventListener? = null

    fun setListener(listener: MotionEventListener?) {
        this.listener = listener
    }

    suspend fun subscribeToMotionEvents(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("MOTION_EVENT", "=== Subscribing to motion events for device: $deviceId ===")
            android.util.Log.d("MOTION_EVENT", "Service URL: $serviceUrl")
            
            // First, check if device supports motion events
            android.util.Log.d("MOTION_EVENT", "Checking event capabilities...")
            val capabilities = getEventCapabilities()
            android.util.Log.d("MOTION_EVENT", "Supports motion detection: ${capabilities.supportsMotionDetection}")
            android.util.Log.d("MOTION_EVENT", "Max subscriptions: ${capabilities.maxEventSubscriptions}")
            
            if (capabilities.supportsMotionDetection) {
                // Try PullPoint subscription (preferred method)
                android.util.Log.d("MOTION_EVENT", "Creating event subscription...")
                val subscriptionId = createEventSubscription()
                android.util.Log.d("MOTION_EVENT", "Subscription ID: $subscriptionId")
                
                if (subscriptionId != null) {
                    val subscription = EventSubscription(
                        id = subscriptionId,
                        deviceId = deviceId,
                        isActive = true,
                        useFastPolling = false
                    )
                    subscriptions[deviceId] = subscription
                    
                    android.util.Log.d("MOTION_EVENT", "Starting event polling...")
                    startEventPolling(deviceId, subscriptionId)
                    android.util.Log.d("MOTION_EVENT", "Motion event subscription successful!")
                    return@withContext true
                }
            }
            
            // Fallback to fast polling method
            android.util.Log.d("MOTION_EVENT", "PullPoint subscription not available, using fast polling fallback")
            val subscription = EventSubscription(
                id = "fast-poll-$deviceId",
                deviceId = deviceId,
                isActive = true,
                useFastPolling = true
            )
            subscriptions[deviceId] = subscription
            startFastPolling(deviceId)
            android.util.Log.d("MOTION_EVENT", "Fast polling motion detection started")
            true
            
        } catch (e: Exception) {
            android.util.Log.e("MOTION_EVENT", "Error subscribing to motion events: ${e.message}", e)
            false
        }
    }

    suspend fun unsubscribeFromMotionEvents(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        val subscription = subscriptions[deviceId] ?: return@withContext false
        
        try {
            val params = mapOf("SubscriptionReference" to subscription.id)
            val response = soapClient.sendRequest("Unsubscribe", EVENT_NAMESPACE, params)
            
            subscription.isActive = false
            subscriptions.remove(deviceId)
            
            response != null
        } catch (e: Exception) {
            false
        }
    }

    private fun getEventCapabilities(): EventCapabilities {
        return try {
            android.util.Log.d("MOTION_EVENT", "Getting event capabilities...")
            val response = soapClient.sendRequest("GetEventProperties", EVENT_NAMESPACE)
            android.util.Log.d("MOTION_EVENT", "GetEventProperties response: $response")
            
            // Parse response to determine motion detection support
            val supportsMotion = response?.toString()?.contains("MotionDetection") == true
            android.util.Log.d("MOTION_EVENT", "Response contains MotionDetection: $supportsMotion")
            
            EventCapabilities(
                supportsMotionDetection = supportsMotion,
                supportsTamperDetection = false,
                maxEventSubscriptions = 5
            )
        } catch (e: Exception) {
            android.util.Log.e("MOTION_EVENT", "Error getting event capabilities: ${e.message}", e)
            EventCapabilities(false, false, 0)
        }
    }

    private fun createEventSubscription(): String? {
        return try {
            android.util.Log.d("MOTION_EVENT", "Creating pull point subscription...")
            val params = mapOf(
                "Filter" to mapOf(
                    "TopicExpression" to "tns1:RuleEngine/CellMotionDetector/Motion"
                ),
                "InitialTerminationTime" to "PT60M" // 60 minutes
            )
            
            android.util.Log.d("MOTION_EVENT", "Subscription params: $params")
            val response = soapClient.sendRequest("CreatePullPointSubscription", EVENT_NAMESPACE, params)
            android.util.Log.d("MOTION_EVENT", "CreatePullPointSubscription response: $response")
            
            val subscriptionRef = response?.getPropertyAsString("SubscriptionReference")
            android.util.Log.d("MOTION_EVENT", "Extracted subscription reference: $subscriptionRef")
            subscriptionRef
        } catch (e: Exception) {
            android.util.Log.e("MOTION_EVENT", "Error creating subscription: ${e.message}", e)
            null
        }
    }

    private fun startFastPolling(deviceId: String) {
        android.util.Log.d("MOTION_EVENT", "Starting fast polling for device: $deviceId")
        var lastMotionState = false
        
        scope.launch {
            while (subscriptions[deviceId]?.isActive == true) {
                try {
                    // Poll motion alarm status every 5 seconds
                    val motionDetected = checkMotionAlarmStatus()
                    
                    // Only trigger callbacks on state change
                    if (motionDetected != lastMotionState) {
                        android.util.Log.d("MOTION_EVENT", "Motion state changed: $lastMotionState -> $motionDetected")
                        if (motionDetected) {
                            android.util.Log.d("MOTION_EVENT", "*** MOTION DETECTED (fast poll) for device: $deviceId ***")
                            listener?.onMotionDetected(deviceId)
                        } else {
                            android.util.Log.d("MOTION_EVENT", "*** MOTION STOPPED (fast poll) for device: $deviceId ***")
                            listener?.onMotionStopped(deviceId)
                        }
                        lastMotionState = motionDetected
                    }
                    
                    delay(5000) // Poll every 5 seconds
                } catch (e: Exception) {
                    android.util.Log.e("MOTION_EVENT", "Error in fast polling: ${e.message}", e)
                    delay(5000)
                }
            }
            android.util.Log.d("MOTION_EVENT", "Fast polling stopped for device: $deviceId")
        }
    }
    
    private fun checkMotionAlarmStatus(): Boolean {
        return try {
            // Try multiple methods to detect motion
            
            // Method 1: Check via GetStatus on device service
            val statusResponse = soapClient.sendRequest("GetStatus", "http://www.onvif.org/ver10/device/wsdl")
            if (statusResponse != null) {
                val statusStr = statusResponse.toString()
                android.util.Log.d("MOTION_EVENT", "GetStatus response: $statusStr")
                if (statusStr.contains("MotionAlarm") && statusStr.contains("true")) {
                    return true
                }
            }
            
            // Method 2: Try GetEventProperties and check for active motion
            val eventResponse = soapClient.sendRequest("GetEventProperties", EVENT_NAMESPACE)
            if (eventResponse != null) {
                val eventStr = eventResponse.toString()
                if (eventStr.contains("MotionAlarm") || eventStr.contains("CellMotionDetector")) {
                    // Check if there's an active state indicator
                    if (eventStr.contains("State") && eventStr.contains("true")) {
                        return true
                    }
                }
            }
            
            false
        } catch (e: Exception) {
            android.util.Log.e("MOTION_EVENT", "Error checking motion alarm status: ${e.message}")
            false
        }
    }

    private fun startEventPolling(deviceId: String, subscriptionId: String) {
        android.util.Log.d("MOTION_EVENT", "Starting event polling for device: $deviceId")
        scope.launch {
            while (subscriptions[deviceId]?.isActive == true) {
                try {
                    val params = mapOf(
                        "Timeout" to "PT5S",
                        "MessageLimit" to 10
                    )
                    
                    android.util.Log.d("MOTION_EVENT", "Polling for messages...")
                    val response = soapClient.sendRequest("PullMessages", EVENT_NAMESPACE, params)
                    android.util.Log.d("MOTION_EVENT", "PullMessages response: $response")
                    
                    response?.let { parseEventMessages(it, deviceId) }
                    
                    delay(1000) // Poll every second
                } catch (e: Exception) {
                    android.util.Log.e("MOTION_EVENT", "Error polling events: ${e.message}", e)
                    delay(5000) // Wait longer on error
                }
            }
            android.util.Log.d("MOTION_EVENT", "Event polling stopped for device: $deviceId")
        }
    }

    private fun parseEventMessages(response: ONVIFResponse, deviceId: String) {
        try {
            android.util.Log.d("MOTION_EVENT", "Parsing event messages for device: $deviceId")
            // Simplified event parsing
            val messages = response.getProperty("NotificationMessage")
            android.util.Log.d("MOTION_EVENT", "Notification messages: $messages")
            
            if (messages != null) {
                when {
                    messages.contains("Motion") && messages.contains("true") -> {
                        android.util.Log.d("MOTION_EVENT", "*** MOTION DETECTED for device: $deviceId ***")
                        listener?.onMotionDetected(deviceId)
                    }
                    messages.contains("Motion") && messages.contains("false") -> {
                        android.util.Log.d("MOTION_EVENT", "*** MOTION STOPPED for device: $deviceId ***")
                        listener?.onMotionStopped(deviceId)
                    }
                    else -> {
                        android.util.Log.d("MOTION_EVENT", "Event message does not contain motion state")
                    }
                }
            } else {
                android.util.Log.d("MOTION_EVENT", "No notification messages in response")
            }
        } catch (e: Exception) {
            android.util.Log.e("MOTION_EVENT", "Error parsing event messages: ${e.message}", e)
        }
    }

    fun cleanup() {
        scope.cancel()
        subscriptions.clear()
    }

    private data class EventSubscription(
        val id: String,
        val deviceId: String,
        var isActive: Boolean,
        val useFastPolling: Boolean = false
    )
}
