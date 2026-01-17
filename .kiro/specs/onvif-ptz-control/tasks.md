# ONVIF PTZ Control and Automatic Stream Discovery Implementation Tasks

## Phase 1: Foundation and Dependencies ✅ COMPLETE

- [x] **Add ONVIF dependencies to build.gradle**
- [x] **Create ONVIF data models**
- [x] **Extend existing SourceData model**

## Phase 2: ONVIF Protocol Implementation ✅ COMPLETE

- [x] **Implement ONVIF SOAP client**
- [x] **Implement WS-Discovery for device discovery**
- [x] **Implement device capabilities detection**

## Phase 3: PTZ Control Implementation ✅ COMPLETE

- [x] **Create PTZController class**
- [x] **Implement preset management**
- [x] **Create PTZ control UI overlay**

## Phase 4: Touch-Based PTZ Control ✅ COMPLETE

- [x] **Create TouchGestureOverlay with PTZ dot**
- [x] **Implement PTZGestureController**
- [x] **Add fullscreen video with landscape lock**
- [x] **Implement per-camera PTZ inversion settings**
- [x] **Add per-camera PTZ rate limiting**

## Phase 5: Integration and UI Updates ✅ COMPLETE

- [x] **Update EditActivity for ONVIF discovery**
- [x] **Update VideoActivity for PTZ controls**
- [x] **Implement ONVIFManager coordinator**

## Phase 6: Security and Performance ✅ COMPLETE

- [x] **Implement security measures**
- [x] **Add performance optimizations**
- [x] **Implement comprehensive error handling**

## Phase 7: Testing and Validation ✅ COMPLETE

- [x] **Create unit tests**
- [x] **Perform integration testing**
- [x] **Add compatibility testing**

## Phase 8: Documentation and Finalization ✅ COMPLETE

- [x] **Update user documentation**
- [x] **Code cleanup and optimization**
- [x] **Prepare release**

## Phase 9: Automatic RTSP Stream Discovery (NEW)

### 9.1: ONVIF Stream Discovery Service
- [ ] **Create ONVIFStreamDiscovery class**
  - Implement GetProfiles ONVIF command
  - Implement GetStreamUri ONVIF command
  - Handle SOAP client integration for media services
  - Add error handling for discovery failures

- [ ] **Implement profile selection logic**
  - Parse media profiles from GetProfiles response
  - Select highest quality profile for primary stream
  - Select secondary profile for dual-channel support
  - Handle cases with single or no profiles available

### 9.2: Stream URL Management
- [ ] **Create MediaProfile data models**
  - Define MediaProfile with token, name, video config
  - Define VideoConfiguration with resolution, framerate, bitrate
  - Add StreamDiscoveryResult data class
  - Include profile comparison and sorting logic

- [ ] **Implement RTSP URL extraction**
  - Parse GetStreamUri response for RTSP URLs
  - Handle different RTSP URL formats from various cameras
  - Add credential injection into RTSP URLs if needed
  - Validate RTSP URL format and accessibility

### 9.3: EditActivity Integration
- [ ] **Add automatic discovery UI**
  - Add progress indicator for stream discovery
  - Show discovery status messages to user
  - Display discovered stream information
  - Add manual fallback option if discovery fails

- [ ] **Implement ONVIF URL change handler**
  - Detect when ONVIF URL is entered or modified
  - Trigger automatic stream discovery process
  - Auto-populate RTSP URL fields with discovered streams
  - Handle dual-channel configuration automatically

### 9.4: User Experience Enhancements
- [ ] **Add discovery progress feedback**
  - Show spinner/progress bar during discovery
  - Display "Discovering streams..." message
  - Show number of profiles found
  - Indicate primary/secondary stream selection

- [ ] **Implement error handling and fallback**
  - Show clear error messages for discovery failures
  - Allow manual RTSP URL entry as fallback
  - Provide retry option for failed discoveries
  - Handle network timeout and connectivity issues

### 9.5: Background Processing
- [ ] **Implement async stream discovery**
  - Use coroutines for non-blocking discovery
  - Handle discovery cancellation if user navigates away
  - Add timeout handling for slow cameras
  - Ensure UI remains responsive during discovery

- [ ] **Add discovery caching**
  - Cache discovered profiles for faster subsequent access
  - Store profile information with camera configuration
  - Invalidate cache when ONVIF URL changes
  - Optimize for repeated camera edits

### 9.6: Testing and Validation
- [ ] **Test with various camera brands**
  - Verify compatibility with Hikvision cameras
  - Test with Dahua camera profiles
  - Validate with Axis camera configurations
  - Check generic ONVIF Profile S compliance

- [ ] **Add unit tests for discovery logic**
  - Test profile selection algorithms
  - Mock ONVIF responses for testing
  - Verify error handling scenarios
  - Test URL parsing and validation

### 9.7: Integration with Existing Features
- [ ] **Ensure PTZ control compatibility**
  - Verify PTZ works with auto-discovered streams
  - Test touch controls with discovered cameras
  - Validate per-camera settings persistence
  - Check ONVIF URL parser integration

- [ ] **Update existing ONVIF workflows**
  - Integrate with existing ONVIFManager
  - Ensure device discovery still works
  - Maintain backward compatibility
  - Update error handling throughout

## Dependencies and Resources

### New Dependencies
- Enhanced ONVIF SOAP client for media services
- Async processing with coroutines
- UI progress indicators and feedback
- Stream URL validation and parsing

### Testing Requirements
- Mock ONVIF camera responses
- Network connectivity simulation
- Various camera brand compatibility
- Error scenario testing

### Estimated Timeline
- **Phase 9.1-9.2**: 2-3 days (Discovery service and URL management)
- **Phase 9.3-9.4**: 2-3 days (UI integration and UX)
- **Phase 9.5-9.6**: 2-3 days (Background processing and testing)
- **Phase 9.7**: 1-2 days (Integration and compatibility)

**Total Estimated Time**: 7-11 days

## Phase 1: Foundation and Dependencies ✅ COMPLETE

- [x] **Add ONVIF dependencies to build.gradle**
- [x] **Create ONVIF data models**
- [x] **Extend existing SourceData model**

## Phase 2: ONVIF Protocol Implementation ✅ COMPLETE

- [x] **Implement ONVIF SOAP client**
- [x] **Implement WS-Discovery for device discovery**
- [x] **Implement device capabilities detection**

## Phase 3: PTZ Control Implementation ✅ COMPLETE

- [x] **Create PTZController class**
- [x] **Implement preset management**
- [x] **Create PTZ control UI overlay**

## Phase 4: Motion Detection Events ✅ COMPLETE

- [x] **Investigate ONVIF motion detection capabilities**
- [x] **Implement MotionEventService**
- [x] **Create motion detection UI indicators**

## Phase 5: Integration and UI Updates ✅ COMPLETE

- [x] **Update EditActivity for ONVIF discovery**
- [x] **Update VideoActivity for PTZ controls**
- [x] **Implement ONVIFManager coordinator**

## Phase 6: Security and Performance ✅ COMPLETE

- [x] **Implement security measures**
- [x] **Add performance optimizations**
- [x] **Implement comprehensive error handling**

## Phase 7: Testing and Validation ✅ COMPLETE

- [x] **Create unit tests**
- [x] **Perform integration testing**
- [x] **Add compatibility testing**

## Phase 8: Documentation and Finalization ✅ COMPLETE

- [x] **Update user documentation**
- [x] **Code cleanup and optimization**
- [x] **Prepare release**

## Phase 9: Touch-Based PTZ Control (NEW)

### 9.1: Fullscreen Video Implementation
- [ ] **Update VideoActivity layout for fullscreen**
  - Remove status bar and navigation bar
  - Make video view fill entire screen
  - Implement immersive mode with system UI hiding
  - Handle edge-to-edge display properly

- [ ] **Create FullscreenManager utility**
  - Manage system UI visibility
  - Handle orientation changes
  - Provide smooth transitions to/from fullscreen

### 9.2: Enhanced Touch Gesture System
- [ ] **Update TouchGestureOverlay for improved UX**
  - Increase center touch area to 100dp radius (not pixel-perfect)
  - PTZ dot appears instantly under finger on touch
  - Dot follows finger position in real-time during drag
  - Reduce movement threshold to 5dp for more responsive control

- [ ] **Implement PTZ dot animations**
  - Create PTZDotAnimator class for smooth animations
  - Return-to-center animation (300ms with decelerate interpolator)
  - Fade-out animation (200ms alpha transition)
  - Handle animation cancellation for new touches

### 9.3: PTZ Gesture Controller
- [ ] **Create PTZGestureController class**
  - Translate touch coordinates to PTZ commands
  - Manage proportional movement speed
  - Track cumulative camera position
  - Handle return-to-center functionality

- [ ] **Implement position tracking system**
  - Store base camera position when gesture starts
  - Calculate current offset from base position
  - Enable smooth return to original position
  - Handle position persistence across gestures

### 9.4: Enhanced Visual Feedback System
- [ ] **Redesign PTZ dot behavior**
  - PTZ dot appears instantly under finger when center area touched
  - Dot follows finger position in real-time during drag
  - Larger center touch area (100dp radius) for easier activation
  - Remove pixel-perfect center requirement

- [ ] **Implement return-to-center animation**
  - Smooth slide animation when finger is released
  - 300ms duration with decelerate interpolator
  - Dot slides from current position back to screen center
  - Animation uses cubic bezier curve for natural feel

- [ ] **Add fade-out animation**
  - Fade animation starts when dot reaches center
  - 200ms alpha transition from 100% to 0%
  - Dot completely disappears after fade completes
  - No visual artifacts remain on screen

- [ ] **Optimize touch responsiveness**
  - Instant dot appearance on touch (no delay)
  - Real-time position updates during drag (60fps)
  - Smooth animation transitions between states
  - Cancel animations if new touch detected

### 9.5: Enhanced PTZ Commands
- [ ] **Update PTZController for position tracking**
  - Add absolute positioning methods
  - Implement smooth movement interpolation
  - Add position state management
  - Handle coordinate system conversion

- [ ] **Implement command throttling**
  - Limit PTZ commands to 10Hz maximum
  - Queue commands during network delays
  - Smooth gesture input with low-pass filtering
  - Optimize for responsive feel

### 9.6: Integration and Testing
- [ ] **Integrate touch system with existing PTZ**
  - Update VideoActivity to use new touch overlay
  - Remove old button-based PTZ controls
  - Ensure compatibility with existing ONVIF implementation
  - Test with real camera hardware

- [ ] **Add gesture configuration options**
  - Sensitivity adjustment settings
  - Deadzone size configuration
  - Enable/disable touch PTZ control
  - Calibration for different screen sizes

### 9.7: Performance Optimization
- [ ] **Optimize touch event processing**
  - Use hardware acceleration for overlay rendering
  - Minimize allocations in touch event handlers
  - Implement efficient coordinate calculations
  - Profile and optimize gesture recognition

- [ ] **Test performance impact**
  - Ensure smooth video playback during PTZ
  - Verify no frame drops during gesture control
  - Test on various device specifications
  - Optimize for battery usage

### 9.8: Error Handling and Edge Cases
- [ ] **Handle network connectivity issues**
  - Show visual feedback for connection problems
  - Queue PTZ commands during network interruptions
  - Graceful degradation when ONVIF unavailable
  - Retry logic for failed commands

- [ ] **Address accessibility concerns**
  - Provide alternative input methods
  - Support for users with motor impairments
  - Voice control integration (future consideration)
  - Configurable gesture sensitivity

## Dependencies and Resources

### New Dependencies
- Custom touch gesture detection
- Fullscreen video rendering
- Position tracking mathematics
- Animation framework usage

### Testing Requirements
- Touch simulation for automated testing
- PTZ command verification
- Performance benchmarking
- Real device testing across screen sizes

### Estimated Timeline
- **Phase 9.1-9.2**: 2-3 days (Fullscreen and touch system)
- **Phase 9.3-9.4**: 2-3 days (Gesture controller and feedback)
- **Phase 9.5-9.6**: 2-3 days (PTZ integration and testing)
- **Phase 9.7-9.8**: 1-2 days (Optimization and error handling)

**Total Estimated Time**: 7-11 days
