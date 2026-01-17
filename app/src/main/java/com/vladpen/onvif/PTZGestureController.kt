package com.vladpen.onvif

import com.vladpen.StreamDataModel
import kotlinx.coroutines.*
import kotlin.math.abs

class PTZGestureController(
    private val ptzController: PTZController,
    private val stream: StreamDataModel
) : TouchGestureOverlay.PTZGestureListener {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var moveJob: Job? = null
    
    private var basePosition = PTZPosition(0f, 0f)
    private var currentPanSpeed = 0f
    private var currentTiltSpeed = 0f
    
    // Rate limiting
    private var lastCommandTime = 0L
    private val commandInterval = 200L // 200ms between commands (5 commands per second)
    
    override fun onGestureStart() {
        android.util.Log.d("PTZ_GESTURE", "Gesture started")
        // Initialize gesture tracking
        moveJob?.cancel()
    }
    
    override fun onGestureMove(panSpeed: Float, tiltSpeed: Float) {
        android.util.Log.d("PTZ_GESTURE", "Gesture move: pan=$panSpeed, tilt=$tiltSpeed")
        currentPanSpeed = panSpeed
        currentTiltSpeed = tiltSpeed
        
        // Rate limiting - only send command if enough time has passed
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCommandTime < commandInterval) {
            android.util.Log.d("PTZ_GESTURE", "Rate limited - skipping command")
            return
        }
        lastCommandTime = currentTime
        
        moveJob?.cancel()
        moveJob = scope.launch {
            try {
                android.util.Log.d("PTZ_GESTURE", "Sending PTZ move command")
                
                // Apply inversion settings from stream configuration
                val invertedPanSpeed = if (stream.invertHorizontalPTZ) -panSpeed else panSpeed
                val invertedTiltSpeed = if (stream.invertVerticalPTZ) -tiltSpeed else tiltSpeed
                
                // Determine movement direction
                val direction = when {
                    abs(invertedPanSpeed) > abs(invertedTiltSpeed) -> {
                        if (invertedPanSpeed > 0) PTZDirection.RIGHT else PTZDirection.LEFT
                    }
                    else -> {
                        if (invertedTiltSpeed > 0) PTZDirection.UP else PTZDirection.DOWN
                    }
                }
                
                // Increase speed multiplier for more responsive movement
                val speed = kotlin.math.max(abs(invertedPanSpeed), abs(invertedTiltSpeed)) * 20f // 20x multiplier
                val clampedSpeed = kotlin.math.min(speed, 1.0f) // Cap at 1.0
                // Use minimum speed of 0.3 for noticeable movement
                val finalSpeed = kotlin.math.max(clampedSpeed, 0.3f)
                android.util.Log.d("PTZ_GESTURE", "Calculated speed: $speed, clamped: $clampedSpeed, final: $finalSpeed")
                ptzController.continuousMove(direction, finalSpeed)
                
            } catch (e: Exception) {
                // Handle PTZ command errors silently
            }
        }
    }
    
    override fun onGestureEnd() {
        android.util.Log.d("PTZ_GESTURE", "Gesture ended, stopping PTZ movement")
        moveJob?.cancel()
        scope.launch {
            try {
                ptzController.stop()
                android.util.Log.d("PTZ_GESTURE", "PTZ movement stopped")
            } catch (e: Exception) {
                android.util.Log.e("PTZ_GESTURE", "Error stopping PTZ movement: ${e.message}")
            }
        }
        
        // Update base position for next gesture
        basePosition = PTZPosition(
            basePosition.pan + currentPanSpeed * 0.1f,
            basePosition.tilt + currentTiltSpeed * 0.1f
        )
        
        currentPanSpeed = 0f
        currentTiltSpeed = 0f
    }
    
    fun cleanup() {
        scope.cancel()
    }
}

data class PTZPosition(
    val pan: Float,
    val tilt: Float
)
