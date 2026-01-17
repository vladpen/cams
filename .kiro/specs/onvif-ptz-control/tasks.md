# ONVIF PTZ Control and Motion Detection Implementation Tasks

## Phase 1: Foundation and Dependencies

- [x] **Add ONVIF dependencies to build.gradle**
  - Add SOAP client library (ksoap2-android or equivalent)
  - Add XML parsing dependencies if needed
  - Update proguard rules for new libraries

- [x] **Create ONVIF data models**
  - Implement ONVIFDevice data class
  - Implement DeviceCapabilities data class
  - Implement PTZCapabilities data class
  - Implement EventCapabilities data class
  - Implement ONVIFCredentials data class

- [x] **Extend existing SourceData model**
  - Add ONVIF-specific properties to SourceData
  - Add database migration for new ONVIF fields
  - Update SourceAdapter to handle ONVIF properties

## Phase 2: ONVIF Protocol Implementation

- [ ] **Implement ONVIF SOAP client**
  - Create ONVIFSoapClient class for basic SOAP communication
  - Implement authentication (WS-UsernameToken and HTTP Digest)
  - Add error handling and timeout management

- [ ] **Implement WS-Discovery for device discovery**
  - Create ONVIFDiscovery class
  - Implement UDP multicast probe/match protocol
  - Add device information parsing from discovery responses

- [ ] **Implement device capabilities detection**
  - Query device services and capabilities
  - Parse PTZ capabilities from device responses
  - Parse event capabilities from device responses

## Phase 3: PTZ Control Implementation

- [ ] **Create PTZController class**
  - Implement continuousMove() method
  - Implement stop() method
  - Implement zoom() method
  - Add PTZ command validation and error handling

- [ ] **Implement preset management**
  - Add gotoPreset() method
  - Add setPreset() method
  - Implement preset storage in local database
  - Add preset UI management

- [ ] **Create PTZ control UI overlay**
  - Design PTZControlView custom view
  - Implement directional pad with touch handling
  - Add zoom controls (+/- buttons)
  - Add preset dropdown menu
  - Implement auto-hide behavior

## Phase 4: Motion Detection Events

- [ ] **Investigate ONVIF motion detection capabilities**
  - Research ONVIF event specification
  - Test with available ONVIF cameras
  - Document findings and implementation approach

- [ ] **Implement MotionEventService**
  - Create event subscription mechanism
  - Implement SOAP event callback handling
  - Add motion event parsing and validation

- [ ] **Create motion detection UI indicators**
  - Implement MotionIndicator overlay view
  - Add red border animation for motion detection
  - Implement 3-second fade-out animation
  - Add motion detection toggle in camera settings

## Phase 5: Integration and UI Updates

- [ ] **Update EditActivity for ONVIF discovery**
  - Add "Discover ONVIF Cameras" button
  - Implement discovery progress dialog
  - Add discovered camera selection interface
  - Auto-populate camera settings from selected device

- [ ] **Update VideoActivity for PTZ controls**
  - Integrate PTZControlView overlay
  - Add PTZ control visibility logic based on capabilities
  - Implement motion indicator integration
  - Add PTZ control state management

- [ ] **Implement ONVIFManager coordinator**
  - Create central ONVIF operations manager
  - Add connection pooling and session management
  - Implement capability caching
  - Add background thread management

## Phase 6: Security and Performance

- [ ] **Implement security measures**
  - Add credential encryption for ONVIF passwords
  - Implement SSL certificate validation
  - Add input validation for ONVIF responses
  - Sanitize all user inputs for ONVIF operations

- [ ] **Add performance optimizations**
  - Implement connection pooling for ONVIF requests
  - Add response caching for device capabilities
  - Optimize UI thread usage for ONVIF operations
  - Add network timeout and retry logic

- [ ] **Implement comprehensive error handling**
  - Add user-friendly error messages for common failures
  - Implement graceful degradation when ONVIF unavailable
  - Add logging for debugging ONVIF issues
  - Handle network connectivity changes

## Phase 7: Testing and Validation

- [ ] **Create unit tests**
  - Test ONVIF protocol implementation with mocked responses
  - Test PTZ control logic and validation
  - Test motion event parsing and handling
  - Test discovery protocol implementation

- [ ] **Perform integration testing**
  - Test with real ONVIF cameras from different manufacturers
  - Validate PTZ control responsiveness and accuracy
  - Test motion detection event reliability
  - Verify discovery works across network configurations

- [ ] **Add compatibility testing**
  - Test ONVIF Profile S compliance
  - Verify backward compatibility with existing RTSP functionality
  - Test on different Android versions and devices
  - Validate performance impact on video playback

## Phase 8: Documentation and Finalization

- [ ] **Update user documentation**
  - Add ONVIF setup instructions to README
  - Document PTZ control usage
  - Add motion detection configuration guide
  - Update troubleshooting section

- [ ] **Code cleanup and optimization**
  - Remove debug logging and test code
  - Optimize imports and dependencies
  - Add comprehensive code comments
  - Perform final code review

- [ ] **Prepare release**
  - Update version numbers and changelog
  - Test final APK build with all features
  - Verify all ONVIF features work in release build
  - Create release notes for ONVIF features

## Dependencies and Resources

### External Dependencies
- ONVIF-compatible IP cameras for testing
- Network access for WS-Discovery testing
- SOAP client library documentation

### Development Resources
- ONVIF Profile S specification document
- WS-Discovery protocol specification
- Example ONVIF SOAP requests/responses

### Estimated Timeline
- **Phase 1-2**: 2-3 days (Foundation and protocol)
- **Phase 3**: 2-3 days (PTZ implementation)
- **Phase 4**: 2-4 days (Motion detection investigation and implementation)
- **Phase 5**: 2-3 days (UI integration)
- **Phase 6**: 1-2 days (Security and performance)
- **Phase 7**: 2-3 days (Testing)
- **Phase 8**: 1 day (Documentation and finalization)

**Total Estimated Time**: 12-19 days
