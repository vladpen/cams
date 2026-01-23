package com.vladpen.onvif

import com.vladpen.StreamDataModel
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sqrt

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
    private val commandInterval get() = stream.ptzRateLimit // Use stream-specific rate limit
    
    override fun onGestureStart() {
        android.util.Log.d("PTZ_GESTURE", "Gesture started")
        // Initialize gesture tracking
        moveJob?.cancel()
    }
    
    override fun onGestureMove(panSpeed: Float, tiltSpeed: Float) {
        android.util.Log.d("PTZ_GESTURE", "Gesture move: pan=$panSpeed, tilt=$tiltSpeed")
        
        // Calculate distance from center (0.0 to 1.0)
        val distance = sqrt(panSpeed * panSpeed + tiltSpeed * tiltSpeed).coerceAtMost(1.0f)
        
        // Apply variable speed based on distance
        val speedMultiplier = calculateSpeedMultiplier(distance)
        
        // Apply speed scaling to movement
        currentPanSpeed = panSpeed * speedMultiplier
        currentTiltSpeed = tiltSpeed * speedMultiplier
        
        android.util.Log.d("PTZ_SPEED", "=== SPEED DEBUG ===")
        android.util.Log.d("PTZ_SPEED", "Distance from center: $distance")
        android.util.Log.d("PTZ_SPEED", "Speed multiplier: $speedMultiplier")
        android.util.Log.d("PTZ_SPEED", "Original pan/tilt: $panSpeed, $tiltSpeed")
        android.util.Log.d("PTZ_SPEED", "Scaled pan/tilt: $currentPanSpeed, $currentTiltSpeed")
        android.util.Log.d("PTZ_SPEED", "==================")
        
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
                val invertedPanSpeed = if (stream.invertHorizontalPTZ) -currentPanSpeed else currentPanSpeed
                val invertedTiltSpeed = if (stream.invertVerticalPTZ) -currentTiltSpeed else currentTiltSpeed
                
                // Determine movement direction
                val direction = when {
                    abs(invertedPanSpeed) > abs(invertedTiltSpeed) -> {
                        if (invertedPanSpeed > 0) PTZDirection.RIGHT else PTZDirection.LEFT
                    }
                    else -> {
                        if (invertedTiltSpeed > 0) PTZDirection.UP else PTZDirection.DOWN
                    }
                }
                
                // Use the calculated speed with multiplier
                val speed = kotlin.math.max(abs(invertedPanSpeed), abs(invertedTiltSpeed)) * 20f
                val clampedSpeed = kotlin.math.min(speed, 1.0f)
                val finalSpeed = kotlin.math.max(clampedSpeed, 0.1f) // Minimum 0.1 for precise control
                
                android.util.Log.d("PTZ_SPEED", "FINAL SPEED CALCULATION:")
                android.util.Log.d("PTZ_SPEED", "Raw speed: $speed")
                android.util.Log.d("PTZ_SPEED", "Clamped speed: $clampedSpeed") 
                android.util.Log.d("PTZ_SPEED", "Final speed sent to camera: $finalSpeed")
                android.util.Log.d("PTZ_SPEED", "Speed multiplier applied: $speedMultiplier")
                
                ptzController.continuousMove(direction, finalSpeed)
                
            } catch (e: Exception) {
                // Handle PTZ command errors silently
            }
        }
    }
    
    // Speed scaling function: slow near center, fast at edges
    private fun calculateSpeedMultiplier(distance: Float): Float {
        return when {
            distance < 0.1f -> 0.1f  // Minimum speed for precise control
            distance < 0.3f -> 0.1f + (distance - 0.1f) * 2.0f  // Linear ramp up
            else -> 0.5f + (distance - 0.3f) * 0.714f  // Continue to max 1.0
        }.coerceIn(0.1f, 1.0f)
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

data class PTZVector(
    val pan: Float,
    val tilt: Float,
    val speed: Float = 1.0f  // Speed multiplier (0.1 to 1.0)
)

data class PTZVelocity(
    val pan: Float,    // Velocity with speed applied
    val tilt: Float    // Velocity with speed applied
)
