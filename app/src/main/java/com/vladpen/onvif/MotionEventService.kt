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
            // First, check if device supports motion events
            val capabilities = getEventCapabilities()
            if (!capabilities.supportsMotionDetection) {
                return@withContext false
            }

            // Create subscription
            val subscriptionId = createEventSubscription()
            if (subscriptionId != null) {
                val subscription = EventSubscription(
                    id = subscriptionId,
                    deviceId = deviceId,
                    isActive = true
                )
                subscriptions[deviceId] = subscription
                
                // Start polling for events (simplified approach)
                startEventPolling(deviceId, subscriptionId)
                true
            } else {
                false
            }
        } catch (e: Exception) {
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
            val response = soapClient.sendRequest("GetEventProperties", EVENT_NAMESPACE)
            
            // Parse response to determine motion detection support
            val supportsMotion = response?.toString()?.contains("MotionDetection") == true
            
            EventCapabilities(
                supportsMotionDetection = supportsMotion,
                supportsTamperDetection = false,
                maxEventSubscriptions = 5
            )
        } catch (e: Exception) {
            EventCapabilities(false, false, 0)
        }
    }

    private fun createEventSubscription(): String? {
        return try {
            val params = mapOf(
                "Filter" to mapOf(
                    "TopicExpression" to "tns1:RuleEngine/CellMotionDetector/Motion"
                ),
                "InitialTerminationTime" to "PT60M" // 60 minutes
            )
            
            val response = soapClient.sendRequest("CreatePullPointSubscription", EVENT_NAMESPACE, params)
            response?.getPropertyAsString("SubscriptionReference")
        } catch (e: Exception) {
            null
        }
    }

    private fun startEventPolling(deviceId: String, subscriptionId: String) {
        scope.launch {
            while (subscriptions[deviceId]?.isActive == true) {
                try {
                    val params = mapOf(
                        "Timeout" to "PT5S",
                        "MessageLimit" to 10
                    )
                    
                    val response = soapClient.sendRequest("PullMessages", EVENT_NAMESPACE, params)
                    response?.let { parseEventMessages(it, deviceId) }
                    
                    delay(1000) // Poll every second
                } catch (e: Exception) {
                    delay(5000) // Wait longer on error
                }
            }
        }
    }

    private fun parseEventMessages(response: ONVIFResponse, deviceId: String) {
        try {
            // Simplified event parsing
            val messages = response.getProperty("NotificationMessage")
            if (messages != null) {
                when {
                    messages.contains("Motion") && messages.contains("true") -> {
                        listener?.onMotionDetected(deviceId)
                    }
                    messages.contains("Motion") && messages.contains("false") -> {
                        listener?.onMotionStopped(deviceId)
                    }
                }
            }
        } catch (e: Exception) {
            // Log parsing error
        }
    }

    fun cleanup() {
        scope.cancel()
        subscriptions.clear()
    }

    private data class EventSubscription(
        val id: String,
        val deviceId: String,
        var isActive: Boolean
    )
}
