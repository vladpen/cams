# ONVIF PTZ Control and Motion Detection Design

## Architecture Overview

The ONVIF integration will be implemented as a separate module that interfaces with the existing camera management system. The design follows a layered architecture to maintain separation of concerns and minimize impact on existing RTSP functionality.

## Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer                                 │
├─────────────────────────────────────────────────────────────┤
│ VideoActivity │ EditActivity │ PTZControlView │ MotionIndicator │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                 Service Layer                               │
├─────────────────────────────────────────────────────────────┤
│ ONVIFManager │ PTZController │ MotionEventService │ Discovery │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                Protocol Layer                               │
├─────────────────────────────────────────────────────────────┤
│    ONVIF SOAP Client    │    WS-Discovery    │   Events     │
└─────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. ONVIFManager
- **Purpose**: Central coordinator for all ONVIF operations
- **Responsibilities**:
  - Manage ONVIF device connections
  - Coordinate between PTZ and motion detection services
  - Handle authentication and session management
  - Cache device capabilities

### 2. ONVIFDiscovery
- **Purpose**: Network discovery of ONVIF devices
- **Implementation**: WS-Discovery protocol over UDP multicast
- **Key Methods**:
  - `discoverDevices()`: Scan network for ONVIF cameras
  - `getDeviceInfo()`: Retrieve device capabilities and services

### 3. PTZController
- **Purpose**: Handle PTZ operations
- **Key Methods**:
  - `continuousMove(direction, speed)`
  - `stop()`
  - `zoom(factor)`
  - `gotoPreset(presetId)`
  - `setPreset(name, position)`

### 4. MotionEventService
- **Purpose**: Handle ONVIF motion detection events
- **Implementation**: SOAP event subscription with callback handling
- **Key Methods**:
  - `subscribeToMotionEvents()`
  - `unsubscribeFromMotionEvents()`
  - `handleMotionEvent(event)`

## Data Models

### ONVIFDevice
```kotlin
data class ONVIFDevice(
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val onvifPort: Int,
    val rtspUrl: String,
    val capabilities: DeviceCapabilities,
    val credentials: ONVIFCredentials?
)
```

### DeviceCapabilities
```kotlin
data class DeviceCapabilities(
    val supportsPTZ: Boolean,
    val supportsMotionEvents: Boolean,
    val ptzCapabilities: PTZCapabilities?,
    val eventCapabilities: EventCapabilities?
)
```

### PTZCapabilities
```kotlin
data class PTZCapabilities(
    val supportsPan: Boolean,
    val supportsTilt: Boolean,
    val supportsZoom: Boolean,
    val supportsPresets: Boolean,
    val maxPresets: Int
)
```

## Sequence Diagrams

### ONVIF Device Discovery
```
User -> EditActivity: Tap "Discover ONVIF"
EditActivity -> ONVIFDiscovery: discoverDevices()
ONVIFDiscovery -> Network: WS-Discovery probe
Network -> ONVIFDiscovery: Device responses
ONVIFDiscovery -> ONVIFManager: getDeviceCapabilities()
ONVIFManager -> EditActivity: List<ONVIFDevice>
EditActivity -> User: Display discovered cameras
```

### PTZ Control Flow
```
User -> VideoActivity: Touch PTZ control
VideoActivity -> PTZController: continuousMove(direction)
PTZController -> ONVIFManager: sendPTZCommand()
ONVIFManager -> ONVIF Device: SOAP PTZ request
User -> VideoActivity: Release PTZ control
VideoActivity -> PTZController: stop()
PTZController -> ONVIF Device: SOAP stop request
```

### Motion Event Handling
```
ONVIFManager -> MotionEventService: subscribeToMotionEvents()
MotionEventService -> ONVIF Device: Subscribe to events
ONVIF Device -> MotionEventService: Motion event callback
MotionEventService -> VideoActivity: notifyMotionDetected()
VideoActivity -> User: Show motion indicator
MotionEventService -> NotificationManager: Send local notification
```

## UI Design

### PTZ Control Overlay
- **Location**: Overlay on video surface in VideoActivity
- **Controls**: 
  - Directional pad (up, down, left, right)
  - Zoom buttons (+, -)
  - Preset dropdown menu
- **Behavior**: 
  - Show only when camera supports PTZ
  - Auto-hide after 5 seconds of inactivity
  - Touch and hold for continuous movement

### Motion Detection Indicator
- **Visual**: Red border around video frame when motion detected
- **Duration**: 3-second fade-out animation
- **Settings**: Toggle in camera edit screen

### Discovery Interface
- **Location**: New button in EditActivity
- **Flow**: 
  1. Show progress dialog during discovery
  2. Display list of found cameras
  3. Allow selection to auto-populate camera settings

## Implementation Considerations

### ONVIF Protocol Implementation
- **Library**: Use existing SOAP libraries (ksoap2-android or custom implementation)
- **Authentication**: Support WS-UsernameToken and HTTP Digest
- **Error Handling**: Graceful degradation when ONVIF features unavailable

### Performance Optimization
- **Connection Pooling**: Reuse ONVIF connections for multiple operations
- **Background Processing**: All ONVIF operations on background threads
- **Caching**: Cache device capabilities to avoid repeated queries

### Security Considerations
- **Credential Storage**: Encrypt ONVIF credentials in SharedPreferences
- **Network Security**: Validate SSL certificates for HTTPS ONVIF endpoints
- **Input Validation**: Sanitize all ONVIF responses

### Integration with Existing Code
- **Minimal Changes**: Extend existing SourceData model with ONVIF properties
- **Backward Compatibility**: Maintain existing RTSP-only functionality
- **Optional Features**: ONVIF features are additive, not required

## Error Handling Strategy

### Network Errors
- **Discovery Timeout**: 10-second timeout with retry option
- **Connection Failures**: Show user-friendly error messages
- **Authentication Errors**: Prompt for credential re-entry

### ONVIF Protocol Errors
- **Unsupported Operations**: Gracefully disable unavailable features
- **Malformed Responses**: Log errors and continue with available functionality
- **Version Compatibility**: Support ONVIF Profile S baseline

## Testing Strategy

### Unit Tests
- Mock ONVIF responses for PTZ operations
- Test motion event parsing and handling
- Validate discovery protocol implementation

### Integration Tests
- Test with real ONVIF cameras when available
- Verify compatibility with major camera manufacturers
- Test network discovery across different network configurations

## Dependencies

### New Libraries Required
- **SOAP Client**: For ONVIF protocol communication
- **WS-Discovery**: For network device discovery
- **XML Parser**: For ONVIF response parsing (if not using existing)

### Estimated Impact
- **APK Size**: +200KB for ONVIF libraries
- **Permissions**: No additional permissions required
- **Compatibility**: Maintains existing Android API level support
