# ONVIF PTZ Control and Automatic Stream Discovery Technical Design

## Architecture Overview

The enhanced ONVIF system provides automatic RTSP stream discovery and touch-based PTZ control. Users only need to enter an ONVIF URL, and the system automatically discovers and configures RTSP streams while providing intuitive gesture-based camera control.

## Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    VideoActivity                            │
│  ┌─────────────────────────────────────────────────────────┤
│  │              FullscreenVideoView                        │
│  │  ┌─────────────────────────────────────────────────────┤
│  │  │            TouchGestureOverlay                      │
│  │  │  - Captures touch events                           │
│  │  │  - Calculates gesture vectors                      │
│  │  │  - Shows visual indicators                         │
│  │  └─────────────────────────────────────────────────────┤
│  │              PTZGestureController                       │
│  │  - Translates gestures to PTZ commands                │
│  │  - Manages proportional movement                       │
│  │  - Handles position tracking                           │
│  └─────────────────────────────────────────────────────────┤
│                 ONVIFManager                               │
│  - Executes PTZ commands                                   │
│  - Manages ONVIF connections                               │
│  - Discovers RTSP streams automatically                    │
└─────────────────────────────────────────────────────────────┘
```

## Automatic Stream Discovery System

### ONVIF Stream Discovery Flow
```kotlin
class ONVIFStreamDiscovery {
    suspend fun discoverStreams(onvifUrl: String, credentials: ONVIFCredentials): StreamDiscoveryResult {
        // 1. Connect to ONVIF device
        val deviceClient = ONVIFSoapClient(onvifUrl, credentials)
        
        // 2. Get available media profiles
        val profiles = deviceClient.sendRequest("GetProfiles", MEDIA_NAMESPACE)
        
        // 3. Select best profiles for primary/secondary streams
        val primaryProfile = selectHighestQualityProfile(profiles)
        val secondaryProfile = selectSecondaryProfile(profiles)
        
        // 4. Get RTSP URLs for selected profiles
        val primaryRtsp = getStreamUri(deviceClient, primaryProfile.token)
        val secondaryRtsp = secondaryProfile?.let { getStreamUri(deviceClient, it.token) }
        
        return StreamDiscoveryResult(
            primaryRtspUrl = primaryRtsp,
            secondaryRtspUrl = secondaryRtsp,
            profiles = profiles
        )
    }
    
    private suspend fun getStreamUri(client: ONVIFSoapClient, profileToken: String): String {
        val streamSetup = mapOf(
            "Stream" to "RTP-Unicast",
            "Transport" to mapOf("Protocol" to "RTSP")
        )
        val params = mapOf(
            "StreamSetup" to streamSetup,
            "ProfileToken" to profileToken
        )
        
        val response = client.sendRequest("GetStreamUri", MEDIA_NAMESPACE, params)
        return response.getProperty("Uri") ?: throw Exception("No stream URI found")
    }
}
```

### Profile Selection Logic
```kotlin
data class MediaProfile(
    val token: String,
    val name: String,
    val videoConfig: VideoConfiguration
)

data class VideoConfiguration(
    val width: Int,
    val height: Int,
    val frameRate: Int,
    val bitrate: Int
)

fun selectHighestQualityProfile(profiles: List<MediaProfile>): MediaProfile {
    return profiles.maxByOrNull { profile ->
        profile.videoConfig.width * profile.videoConfig.height * profile.videoConfig.frameRate
    } ?: profiles.first()
}

fun selectSecondaryProfile(profiles: List<MediaProfile>): MediaProfile? {
    if (profiles.size < 2) return null
    
    val sorted = profiles.sortedByDescending { profile ->
        profile.videoConfig.width * profile.videoConfig.height
    }
    
    return sorted.getOrNull(1) // Second highest quality
}
```

## Touch Gesture System

### Gesture Detection
- **Center Area**: 100dp radius around screen center for initial touch detection (not pixel-perfect)
- **PTZ Dot**: Visual indicator that appears under user's finger
- **Movement Threshold**: 5dp minimum movement to trigger PTZ commands
- **Maximum Range**: Screen edges define maximum PTZ movement

### PTZ Dot Behavior
```kotlin
sealed class PTZDotState {
    object Hidden : PTZDotState()
    data class Visible(val x: Float, val y: Float) : PTZDotState()
    data class Dragging(val x: Float, val y: Float) : PTZDotState()
    data class Returning(val fromX: Float, val fromY: Float, val progress: Float) : PTZDotState()
    data class Fading(val alpha: Float) : PTZDotState()
}
```

### Animation System
- **Dot Appearance**: Instant appearance under finger when touch detected
- **Dot Following**: Real-time position updates during drag
- **Return Animation**: Smooth slide back to center over 300ms using interpolation
- **Fade Animation**: Alpha fade from 100% to 0% over 200ms after reaching center
- **Re-appearance**: Instant show when center touched again

### Coordinate Mapping with Variable Speed
```kotlin
// Convert touch coordinates to PTZ values with distance-based speed scaling
fun touchToPTZ(touchX: Float, touchY: Float, centerX: Float, centerY: Float): PTZVector {
    val deltaX = (touchX - centerX) / (screenWidth / 2)  // -1.0 to 1.0
    val deltaY = (centerY - touchY) / (screenHeight / 2) // -1.0 to 1.0 (inverted)
    
    // Calculate distance from center (0.0 to 1.0)
    val distance = sqrt(deltaX * deltaX + deltaY * deltaY).coerceAtMost(1.0f)
    
    // Apply speed scaling based on distance
    val speedMultiplier = calculateSpeedMultiplier(distance)
    
    return PTZVector(
        pan = (deltaX * speedMultiplier).coerceIn(-1.0f, 1.0f),
        tilt = (deltaY * speedMultiplier).coerceIn(-1.0f, 1.0f),
        speed = speedMultiplier
    )
}

// Speed scaling function: slow near center, fast at edges
fun calculateSpeedMultiplier(distance: Float): Float {
    return when {
        distance < 0.1f -> 0.1f  // Minimum speed for precise control
        distance < 0.3f -> 0.1f + (distance - 0.1f) * 2.0f  // Linear ramp up
        else -> 0.5f + (distance - 0.3f) * 0.714f  // Continue to max 1.0
    }.coerceIn(0.1f, 1.0f)
}
```

### Movement Calculation with Variable Speed
- **Proportional Control**: Movement speed proportional to distance from center
- **Speed Scaling**: 0.1x speed near center (0-10% radius), scaling to 1.0x at edges
- **Smooth Transition**: Linear speed ramp from 10% to 30% radius, then gradual to 100%
- **Deadzone**: 10% deadzone around center to prevent jitter
- **Acceleration Curve**: Exponential curve for fine control near center
- **Position Tracking**: Track cumulative movement for return-to-center

## Visual Feedback System

### PTZ Dot Indicator
- **Appearance**: Semi-transparent white circle (30dp diameter) that appears under finger
- **Following**: Dot position updates in real-time during drag gestures
- **Return Animation**: Smooth slide back to center using cubic bezier interpolation
- **Fade Animation**: Alpha transition from opaque to transparent over 200ms

### Center Area Indicator
- **Center Crosshair**: Subtle crosshair at screen center (only visible during active gesture)
- **Center Zone**: Visual indication of touch-sensitive area (100dp radius)
- **Range Boundary**: Optional outer circle showing maximum movement range

### Animation States
```kotlin
class PTZDotAnimator {
    fun startReturnAnimation(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val currentX = fromX + (toX - fromX) * progress
                val currentY = fromY + (toY - fromY) * progress
                updateDotPosition(currentX, currentY)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startFadeAnimation()
                }
            })
        }.start()
    }
    
    fun startFadeAnimation() {
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 200
            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Float
                updateDotAlpha(alpha)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    hideDot()
                }
            })
        }.start()
    }
}
```

### PTZ Command Generation with Variable Speed

#### Continuous Movement with Speed Control
```kotlin
class PTZGestureController {
    private var basePosition = PTZPosition(0f, 0f)
    private var currentOffset = PTZVector(0f, 0f)
    
    fun onGestureMove(vector: PTZVector) {
        currentOffset = vector
        val targetPosition = basePosition + vector
        
        // Apply speed scaling to PTZ velocity
        val scaledVelocity = PTZVelocity(
            pan = vector.pan * vector.speed,
            tilt = vector.tilt * vector.speed
        )
        
        ptzController.continuousMove(scaledVelocity)
    }
    
    fun onGestureEnd() {
        basePosition += currentOffset
        currentOffset = PTZVector(0f, 0f)
        ptzController.stop()
    }
}

data class PTZVector(
    val pan: Float,
    val tilt: Float,
    val speed: Float = 1.0f  // Speed multiplier (0.1 to 1.0)
)

data class PTZVelocity(
    val pan: Float,    // Velocity with speed applied
    val tilt: Float    // Velocity with speed applied
)
```

### Position Tracking
- **Base Position**: Camera position when gesture starts
- **Current Offset**: Current gesture displacement from base
- **Target Position**: Base + Offset for absolute positioning
- **Return Path**: Smooth interpolation back to center when requested

## Fullscreen Video Implementation

### Layout Changes
```xml
<!-- VideoActivity Layout -->
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="false">
    
    <VideoView
        android:id="@+id/videoView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
    
    <com.vladpen.onvif.TouchGestureOverlay
        android:id="@+id/ptzOverlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
        
</FrameLayout>
```

### System UI Management
- **Immersive Mode**: Hide status bar and navigation bar
- **Stable Layout**: Prevent layout shifts when system UI changes
- **Edge-to-Edge**: Video content extends to screen edges

## Touch Event Processing

### Event Flow
```kotlin
class TouchGestureOverlay : View {
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isCenterTouch(event.x, event.y)) {
                    startPTZGesture(event.x, event.y)
                    showIndicators()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                updatePTZGesture(event.x, event.y)
                updateIndicators()
            }
            MotionEvent.ACTION_UP -> {
                endPTZGesture()
                scheduleHideIndicators()
            }
        }
        return true
    }
}
```

### Gesture Recognition
- **Initial Touch**: Must be within center radius to activate PTZ
- **Movement Tracking**: Continuous position updates during drag
- **Multi-touch**: Ignore additional fingers, use only first touch
- **Edge Cases**: Handle touch outside screen bounds gracefully

### Performance Considerations

#### Optimization Strategies
- **Command Throttling**: Limit PTZ commands to 10Hz to prevent overload
- **Speed Calculation**: Efficient distance calculation using squared distance where possible
- **Gesture Smoothing**: Apply low-pass filter to reduce jitter while preserving speed changes
- **Efficient Rendering**: Use hardware acceleration for overlay graphics
- **Memory Management**: Recycle touch event objects

### Threading Model
- **UI Thread**: Touch event processing and visual updates
- **Background Thread**: ONVIF command execution
- **Render Thread**: Video surface rendering (unchanged)

## Error Handling

### Network Issues
- **Connection Loss**: Show warning overlay, queue commands for retry
- **Timeout**: Provide visual feedback, allow manual retry
- **Authentication**: Graceful fallback to non-PTZ mode

### Touch System Issues
- **Calibration**: Auto-detect screen center, allow manual adjustment
- **Sensitivity**: Configurable gesture sensitivity settings
- **Accessibility**: Support for alternative input methods

## Integration Points

### EditActivity Integration
```kotlin
class EditActivity {
    private fun onOnvifUrlChanged(onvifUrl: String) {
        if (onvifUrl.startsWith("onvif://")) {
            // Show progress indicator
            binding.progressDiscovery.visibility = View.VISIBLE
            binding.etEditUrl.isEnabled = false
            
            // Start stream discovery
            lifecycleScope.launch {
                try {
                    val parsed = ONVIFUrlParser.parseOnvifUrl(onvifUrl)
                    val discovery = ONVIFStreamDiscovery()
                    val result = discovery.discoverStreams(parsed.serviceUrl, parsed.credentials)
                    
                    // Auto-populate RTSP URLs
                    binding.etEditUrl.setText(result.primaryRtspUrl)
                    binding.etEditUrl2.setText(result.secondaryRtspUrl ?: "")
                    
                    // Show success message
                    showDiscoverySuccess(result.profiles.size)
                    
                } catch (e: Exception) {
                    // Show error and allow manual entry
                    showDiscoveryError(e.message)
                    binding.etEditUrl.isEnabled = true
                } finally {
                    binding.progressDiscovery.visibility = View.GONE
                }
            }
        }
    }
}
```

### User Experience Flow
1. **ONVIF URL Entry**: User enters `onvif://username:password@ip:port/path`
2. **Automatic Discovery**: System shows progress and discovers streams
3. **Auto-Population**: RTSP URLs are automatically filled in
4. **Fallback**: If discovery fails, manual RTSP entry is still available
5. **Dual Channel**: Secondary stream automatically configured if available

### Existing Components
- **VideoActivity**: Minimal changes, add overlay and gesture controller
- **ONVIFManager**: No changes to PTZ command interface
- **PTZController**: Enhanced with position tracking methods

### New Components
- **TouchGestureOverlay**: Custom view for gesture capture and visual feedback
- **PTZGestureController**: Business logic for gesture-to-PTZ translation
- **FullscreenManager**: System UI management for immersive experience
