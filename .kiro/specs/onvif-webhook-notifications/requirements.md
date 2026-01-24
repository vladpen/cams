# Requirements: ONVIF Webhook Background Notification System

## Overview
Enable the app to receive ONVIF motion detection events from cameras via webhook push notifications while running in the background, and display Android notifications to alert the user.

**Important:** This feature only works on local networks where the device and cameras are on the same network segment. The HTTP server is only started after successfully registering the webhook with at least one camera.

## User Stories

### US-1: Background Service Management
**As a** user  
**I want** the app to run a background service that listens for camera motion events  
**So that** I can receive notifications even when the app is not in the foreground

WHEN the user enables motion notifications for a camera  
THE SYSTEM SHALL start a foreground service with a persistent notification

WHEN the foreground service is running  
THE SYSTEM SHALL display a status notification showing "Monitoring N cameras for motion"

WHEN the user disables motion notifications for all cameras  
THE SYSTEM SHALL stop the foreground service

WHEN the system terminates the service due to resource constraints  
THE SYSTEM SHALL attempt to restart the service automatically

### US-2: HTTP Webhook Server
**As a** camera  
**I want** to send motion detection events to the app via HTTP POST  
**So that** the app can receive real-time motion notifications

WHEN the background service starts  
THE SYSTEM SHALL detect the device's local IP address

WHEN the device has a valid local IP address  
THE SYSTEM SHALL attempt to register webhooks with enabled cameras

WHEN webhook registration succeeds for at least one camera  
THE SYSTEM SHALL start the HTTP server on an available port

WHEN the HTTP server starts successfully  
THE SYSTEM SHALL log the server URL (IP:port) for diagnostics

WHEN webhook registration fails for all cameras  
THE SYSTEM SHALL NOT start the HTTP server and SHALL display an error notification

WHEN the HTTP server receives a POST request to /webhook/{cameraId}  
THE SYSTEM SHALL parse the ONVIF notification message

WHEN the HTTP server receives an invalid request  
THE SYSTEM SHALL return HTTP 400 Bad Request

WHEN the HTTP server receives a valid motion event  
THE SYSTEM SHALL return HTTP 200 OK

WHEN all webhook subscriptions are unregistered  
THE SYSTEM SHALL stop the HTTP server

### US-3: ONVIF Webhook Registration
**As a** user  
**I want** the app to automatically register its webhook URL with cameras  
**So that** I don't have to manually configure the camera

WHEN the background service starts  
THE SYSTEM SHALL detect the device's local IP address

WHEN the device IP is detected  
THE SYSTEM SHALL register webhook subscriptions with all enabled cameras

WHEN registering a webhook with a camera  
THE SYSTEM SHALL use the ONVIF CreateNotification method with the webhook URL

WHEN the device IP address changes  
THE SYSTEM SHALL re-register webhooks with the new IP address

WHEN webhook registration fails  
THE SYSTEM SHALL log the error and retry after 60 seconds

WHEN the app is closed or service stops  
THE SYSTEM SHALL unsubscribe webhooks from all cameras

### US-4: Motion Event Processing
**As a** user  
**I want** to receive Android notifications when motion is detected  
**So that** I am alerted to activity on my cameras

WHEN a motion detection event is received  
THE SYSTEM SHALL parse the ONVIF notification message to extract camera ID and event type

WHEN a motion start event is detected  
THE SYSTEM SHALL display an Android notification with camera name and timestamp

WHEN a motion stop event is detected  
THE SYSTEM SHALL update or dismiss the corresponding notification

WHEN multiple cameras detect motion simultaneously  
THE SYSTEM SHALL display separate notifications for each camera

WHEN the user taps a motion notification  
THE SYSTEM SHALL open the app and display the corresponding camera stream

### US-5: Configuration and Permissions
**As a** user  
**I want** to configure which cameras send motion notifications  
**So that** I can control which cameras I monitor

WHEN viewing a camera's configuration  
THE SYSTEM SHALL display a toggle for "Motion Notifications"

WHEN the user enables motion notifications  
THE SYSTEM SHALL request POST_NOTIFICATIONS permission (Android 13+)

WHEN the user enables motion notifications without network access  
THE SYSTEM SHALL display a warning that the device must be on the same local network as cameras

WHEN the user enables motion notifications  
THE SYSTEM SHALL save the preference and start/update the background service

WHEN the device is not on a local network  
THE SYSTEM SHALL display a notification that motion monitoring requires local network connectivity

### US-6: Network Connectivity
**As a** user  
**I want** the app to handle network changes gracefully  
**So that** notifications continue working when my device reconnects to WiFi

WHEN the device connects to a WiFi network  
THE SYSTEM SHALL detect the IP address change and re-register webhooks

WHEN the device disconnects from WiFi  
THE SYSTEM SHALL pause webhook monitoring and display a notification

WHEN the device switches to mobile data  
THE SYSTEM SHALL pause webhook monitoring (cameras cannot reach mobile IPs)

WHEN the device reconnects to the same WiFi network  
THE SYSTEM SHALL resume webhook monitoring automatically

### US-7: Error Handling and Diagnostics
**As a** user  
**I want** to see the status of motion monitoring  
**So that** I can troubleshoot connection issues

WHEN viewing the app settings  
THE SYSTEM SHALL display the current webhook server status (running/stopped)

WHEN the webhook server is running  
THE SYSTEM SHALL display the current IP address and port

WHEN webhook registration fails for a camera  
THE SYSTEM SHALL display an error indicator next to that camera

WHEN the user taps the error indicator  
THE SYSTEM SHALL show detailed error information and troubleshooting steps

## Acceptance Criteria

### AC-1: Service Lifecycle
- [ ] Foreground service starts when at least one camera has notifications enabled
- [ ] Service displays persistent notification with monitoring status
- [ ] Service stops when all cameras have notifications disabled
- [ ] Service survives app closure and device sleep

### AC-2: HTTP Server
- [ ] HTTP server only starts after successful webhook registration
- [ ] HTTP server binds to available port (8000-8100 range)
- [ ] Server accepts POST requests with ONVIF notification payloads
- [ ] Server validates ONVIF message format
- [ ] Server responds with appropriate HTTP status codes
- [ ] Server stops when all webhooks are unregistered

### AC-3: Webhook Management
- [ ] Webhooks registered using ONVIF CreateNotification
- [ ] Webhook URL includes unique camera identifier
- [ ] Webhooks renewed before expiration (every 8 minutes for 10-minute subscription)
- [ ] Webhooks unsubscribed on service stop

### AC-4: Notifications
- [ ] Android notification displayed within 1 second of motion event
- [ ] Notification includes camera name, timestamp, and thumbnail (if available)
- [ ] Notification tap opens app to camera view
- [ ] Notifications respect Android Do Not Disturb settings

### AC-5: Network Handling
- [ ] IP address changes detected within 30 seconds
- [ ] Webhooks re-registered within 60 seconds of IP change
- [ ] Service pauses gracefully when network unavailable
- [ ] Service resumes automatically when network restored

### AC-6: Battery and Performance
- [ ] Service uses < 5% battery per hour when idle
- [ ] HTTP server uses minimal CPU when not receiving events
- [ ] Service does not prevent device sleep
- [ ] Service releases resources properly on stop

## Non-Functional Requirements

### NFR-1: Security
- HTTP server only accepts connections from configured camera IP addresses
- Webhook URLs include random token to prevent unauthorized access
- Service validates ONVIF message signatures (if supported by camera)

### NFR-2: Reliability
- Service automatically restarts if killed by system
- Webhook subscriptions renewed before expiration
- Failed webhook registrations retried with exponential backoff

### NFR-3: Compatibility
- Works with Android 8.0 (API 26) and above
- Compatible with Thingino cameras using onvif_notify_server
- Compatible with standard ONVIF Profile S cameras supporting notifications
- Only functions on local networks (WiFi/Ethernet on same subnet as cameras)

### NFR-4: Usability
- Setup requires minimal user configuration
- Clear error messages for common issues (wrong network, firewall, etc.)
- Status indicators show monitoring health at a glance
- Clear messaging that feature requires local network connectivity

## Out of Scope
- Motion detection via video analysis (use existing SFTP method)
- Support for cameras without ONVIF notification support
- Cloud-based relay for mobile data connectivity
- Video recording triggered by motion events
- Integration with third-party notification services
