# ONVIF PTZ Control Technical Design

## Architecture Overview

The touch-based PTZ control system uses a gesture-driven approach where users interact directly with the video surface to control camera movement. The system provides intuitive, proportional control with visual feedback.

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
└─────────────────────────────────────────────────────────────┘
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

### Coordinate Mapping
```kotlin
// Convert touch coordinates to PTZ values
fun touchToPTZ(touchX: Float, touchY: Float, centerX: Float, centerY: Float): PTZVector {
    val deltaX = (touchX - centerX) / (screenWidth / 2)  // -1.0 to 1.0
    val deltaY = (centerY - touchY) / (screenHeight / 2) // -1.0 to 1.0 (inverted)
    return PTZVector(
        pan = deltaX.coerceIn(-1.0f, 1.0f),
        tilt = deltaY.coerceIn(-1.0f, 1.0f)
    )
}
```

### Movement Calculation
- **Proportional Control**: Movement speed proportional to distance from center
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

## PTZ Command Generation

### Continuous Movement
```kotlin
class PTZGestureController {
    private var basePosition = PTZPosition(0f, 0f)
    private var currentOffset = PTZVector(0f, 0f)
    
    fun onGestureMove(vector: PTZVector) {
        currentOffset = vector
        val targetPosition = basePosition + vector
        ptzController.continuousMove(targetPosition)
    }
    
    fun onGestureEnd() {
        basePosition += currentOffset
        currentOffset = PTZVector(0f, 0f)
        ptzController.stop()
    }
}
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

## Performance Considerations

### Optimization Strategies
- **Command Throttling**: Limit PTZ commands to 10Hz to prevent overload
- **Gesture Smoothing**: Apply low-pass filter to reduce jitter
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

### Existing Components
- **VideoActivity**: Minimal changes, add overlay and gesture controller
- **ONVIFManager**: No changes to PTZ command interface
- **PTZController**: Enhanced with position tracking methods

### New Components
- **TouchGestureOverlay**: Custom view for gesture capture and visual feedback
- **PTZGestureController**: Business logic for gesture-to-PTZ translation
- **FullscreenManager**: System UI management for immersive experience
