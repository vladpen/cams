# Design: ONVIF Webhook Background Notification System

## Architecture Overview

**Network Scope:** This system only operates on local networks where the Android device and cameras are on the same network segment (e.g., 10.0.0.0/24). The HTTP server is conditionally started only after successful webhook registration with at least one camera.

```
┌─────────────────────────────────────────────────────────────┐
│                      Android Device                          │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │           ONVIF Camera App (UI)                    │    │
│  │  - Camera configuration                            │    │
│  │  - Enable/disable motion notifications             │    │
│  │  - View monitoring status                          │    │
│  └─────────────────┬──────────────────────────────────┘    │
│                    │ Start/Stop Service                     │
│                    │ Configure cameras                      │
│                    ▼                                         │
│  ┌────────────────────────────────────────────────────┐    │
│  │    MotionNotificationService (Foreground)          │    │
│  │  - Manages HTTP server lifecycle                   │    │
│  │  - Monitors network changes                        │    │
│  │  - Manages webhook subscriptions                   │    │
│  └─────┬──────────────────────┬───────────────────────┘    │
│        │                      │                             │
│        ▼                      ▼                             │
│  ┌──────────────┐      ┌──────────────────┐               │
│  │ HTTP Server  │      │ Webhook Manager  │               │
│  │ (NanoHTTPD)  │      │ - Register       │               │
│  │              │      │ - Renew          │               │
│  │ Port: 8000+  │      │ - Unsubscribe    │               │
│  └──────┬───────┘      └────────┬─────────┘               │
│         │                       │                          │
│         │ Webhook POST          │ ONVIF SOAP               │
└─────────┼───────────────────────┼──────────────────────────┘
          │                       │
          │                       │
┌─────────┼───────────────────────┼──────────────────────────┐
│         │                       │                          │
│         │                       ▼                          │
│  ┌──────▼──────────────────────────────────────┐          │
│  │     Thingino Camera / ONVIF Camera          │          │
│  │                                              │          │
│  │  ┌────────────────────────────────────┐    │          │
│  │  │  onvif_notify_server               │    │          │
│  │  │  - Watches motion files            │    │          │
│  │  │  - Sends HTTP POST to webhook      │    │          │
│  │  └────────────────────────────────────┘    │          │
│  │                                              │          │
│  │  ┌────────────────────────────────────┐    │          │
│  │  │  Motion Detection (prudynt)        │    │          │
│  │  │  - Writes to motion.active         │    │          │
│  │  └────────────────────────────────────┘    │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
│                    Camera (10.0.0.68)                       │
└─────────────────────────────────────────────────────────────┘
```

## Component Design

### 1. MotionNotificationService (Foreground Service)

**Responsibilities:**
- Manage service lifecycle
- Start/stop HTTP server
- Monitor network connectivity
- Coordinate webhook subscriptions
- Display foreground notification

**Key Methods:**
```kotlin
class MotionNotificationService : Service() {
    private var httpServer: WebhookHttpServer? = null
    private var webhookManager: WebhookManager? = null
    private var networkMonitor: NetworkMonitor? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    fun startMonitoring(cameras: List<CameraConfig>)
    fun stopMonitoring()
    fun updateCameraList(cameras: List<CameraConfig>)
    private fun createForegroundNotification(): Notification
    private fun onNetworkChanged(newIp: String?)
}
```

**Lifecycle:**
1. Started via `startForegroundService()` from MainActivity
2. Immediately calls `startForeground()` with persistent notification
3. Detects local IP address and validates network connectivity
4. Attempts webhook registration with enabled cameras
5. Only starts HTTP server if at least one webhook registration succeeds
6. Registers network callback for IP changes
7. Runs until explicitly stopped or all cameras disabled
8. Stops HTTP server when all webhooks are unregistered

### 2. WebhookHttpServer (HTTP Server)

**Technology:** NanoHTTPD (lightweight embedded HTTP server)

**Important:** The HTTP server is only instantiated and started after at least one webhook is successfully registered with a camera. If all webhooks are unregistered, the server is stopped to conserve resources.

**Responsibilities:**
- Listen for incoming HTTP POST requests from local network only
- Parse ONVIF notification messages
- Validate request source and format
- Emit motion events to service

**Endpoint Structure:**
```
POST /webhook/{cameraId}/{token}
Content-Type: application/soap+xml

Body: ONVIF notification message
```

**Key Methods:**
```kotlin
class WebhookHttpServer(
    private val port: Int,
    private val eventCallback: (String, MotionEvent) -> Unit
) : NanoHTTPD(port) {
    
    override fun serve(session: IHTTPSession): Response
    private fun parseOnvifNotification(body: String): MotionEvent?
    private fun validateRequest(session: IHTTPSession): Boolean
    private fun generateToken(cameraId: String): String
    fun getWebhookUrl(cameraId: String): String
    fun start() // Only called after successful webhook registration
    fun stop()  // Called when all webhooks unregistered
}
```

**Port Selection:**
- Try ports 8000-8100 until one binds successfully
- Store selected port in SharedPreferences
- Prefer previously used port for consistency
- Only bind to local network interface (not 0.0.0.0)

**Security:**
- Validate source IP matches configured camera IP (local network only)
- Require random token in URL path
- Reject requests without valid ONVIF message structure
- Only bind to local network interface to prevent external access

### 3. WebhookManager (Webhook Subscription Manager)

**Responsibilities:**
- Register webhook subscriptions with cameras
- Renew subscriptions before expiration
- Unsubscribe on service stop
- Handle registration failures

**Key Methods:**
```kotlin
class WebhookManager(
    private val onvifClient: ONVIFSoapClient
) {
    private val subscriptions = ConcurrentHashMap<String, WebhookSubscription>()
    private val renewalJobs = ConcurrentHashMap<String, Job>()
    
    suspend fun registerWebhook(
        cameraId: String,
        webhookUrl: String,
        credentials: ONVIFCredentials?
    ): Result<String>
    
    suspend fun unregisterWebhook(cameraId: String): Result<Unit>
    
    private suspend fun renewWebhook(cameraId: String)
    
    private fun scheduleRenewal(cameraId: String, subscriptionId: String)
}

data class WebhookSubscription(
    val subscriptionId: String,
    val cameraId: String,
    val webhookUrl: String,
    val expiresAt: Long,
    val isActive: Boolean
)
```

**Subscription Lifecycle:**
1. Call ONVIF `CreateNotification` with webhook URL
2. Set subscription duration to 10 minutes
3. Schedule renewal job for 8 minutes
4. Renewal job calls `Renew` to extend subscription
5. On service stop, call `Unsubscribe` for cleanup

**Error Handling:**
- Retry failed registrations with exponential backoff (1s, 2s, 4s, 8s, 16s, 60s max)
- Log errors to app's error log
- Display error indicator in UI
- Continue monitoring other cameras if one fails

### 4. NetworkMonitor (Network Change Detector)

**Responsibilities:**
- Detect IP address changes
- Monitor WiFi connectivity
- Trigger webhook re-registration

**Key Methods:**
```kotlin
class NetworkMonitor(
    private val context: Context,
    private val callback: (NetworkState) -> Unit
) {
    private val connectivityManager: ConnectivityManager
    private val networkCallback: ConnectivityManager.NetworkCallback
    
    fun start()
    fun stop()
    fun getCurrentIpAddress(): String?
    fun isOnLocalNetwork(): Boolean
}

sealed class NetworkState {
    data class Connected(val ipAddress: String) : NetworkState()
    object Disconnected : NetworkState()
    object MobileData : NetworkState()
}
```

**Implementation:**
- Use `ConnectivityManager.NetworkCallback` for network changes
- Poll IP address every 30 seconds to detect DHCP changes
- Use `NetworkInterface.getNetworkInterfaces()` to get local IP
- Filter for WiFi/Ethernet interfaces only (exclude mobile data)
- Validate IP is in private address space (10.x, 172.16-31.x, 192.168.x)

### 5. MotionNotificationManager (Notification Display)

**Responsibilities:**
- Create and display motion notifications
- Update notification content
- Handle notification taps

**Key Methods:**
```kotlin
class MotionNotificationManager(private val context: Context) {
    
    fun showMotionNotification(
        cameraId: String,
        cameraName: String,
        timestamp: Long
    ): Int
    
    fun dismissMotionNotification(notificationId: Int)
    
    fun updateMotionNotification(
        notificationId: Int,
        content: String
    )
    
    private fun createNotificationChannel()
    
    private fun createPendingIntent(cameraId: String): PendingIntent
}
```

**Notification Design:**
- **Title:** "Motion detected: {Camera Name}"
- **Content:** Timestamp of detection
- **Icon:** Motion sensor icon
- **Action:** Tap to open camera view
- **Channel:** "Motion Alerts" (high priority)
- **Sound:** Default notification sound
- **Vibration:** Short vibration pattern

### 6. ONVIF Message Parser

**Responsibilities:**
- Parse ONVIF SOAP notification messages
- Extract event type and camera information
- Validate message structure

**Key Methods:**
```kotlin
object OnvifNotificationParser {
    
    fun parse(soapMessage: String): MotionEvent?
    
    private fun extractTopic(xml: String): String?
    
    private fun extractSourceValue(xml: String): String?
    
    private fun isMotionEvent(topic: String): Boolean
}

data class MotionEvent(
    val topic: String,
    val sourceToken: String,
    val isMotionStart: Boolean,
    val timestamp: Long
)
```

**Expected ONVIF Message Format:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
  <soap:Body>
    <wsnt:Notify xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2">
      <wsnt:NotificationMessage>
        <wsnt:Topic>tns1:VideoSource/MotionAlarm</wsnt:Topic>
        <wsnt:Message>
          <tt:Message>
            <tt:Source>
              <tt:SimpleItem Name="VideoSourceConfigurationToken" Value="VideoSourceToken"/>
            </tt:Source>
            <tt:Data>
              <tt:SimpleItem Name="State" Value="true"/>
            </tt:Data>
          </tt:Message>
        </wsnt:Message>
      </wsnt:NotificationMessage>
    </wsnt:Notify>
  </soap:Body>
</soap:Envelope>
```

## Data Flow

### Sequence: Service Startup

```
User -> MainActivity: Enable motion notifications
MainActivity -> MotionNotificationService: startForegroundService()
MotionNotificationService -> MotionNotificationService: startForeground()
MotionNotificationService -> NetworkMonitor: start()
NetworkMonitor --> MotionNotificationService: IP address (10.0.0.50)
MotionNotificationService -> WebhookManager: registerWebhooks(cameras, IP)
loop For each camera
    WebhookManager -> Camera: CreateNotification(webhook URL)
    Camera --> WebhookManager: Subscription ID or Error
end
alt At least one webhook registered successfully
    MotionNotificationService -> WebhookHttpServer: start(port)
    WebhookHttpServer -> WebhookHttpServer: bind to port 8000
    WebhookHttpServer --> MotionNotificationService: Server started
    WebhookManager -> WebhookManager: scheduleRenewal(8 min)
else All webhooks failed
    MotionNotificationService -> MotionNotificationService: Display error notification
    MotionNotificationService -> MotionNotificationService: Retry after 60s
end
MotionNotificationService --> MainActivity: Service started
```

### Sequence: Motion Event Received

```
Camera -> Camera: Motion detected
Camera -> onvif_notify_server: Write to motion.active
onvif_notify_server -> WebhookHttpServer: POST /webhook/{id}/{token}
WebhookHttpServer -> WebhookHttpServer: Validate request
WebhookHttpServer -> OnvifNotificationParser: parse(body)
OnvifNotificationParser --> WebhookHttpServer: MotionEvent
WebhookHttpServer -> MotionNotificationService: onMotionEvent(cameraId, event)
MotionNotificationService -> MotionNotificationManager: showMotionNotification()
MotionNotificationManager -> Android: Display notification
WebhookHttpServer --> Camera: HTTP 200 OK
```

### Sequence: Network Change

```
Android -> NetworkMonitor: Network changed
NetworkMonitor -> NetworkMonitor: getCurrentIpAddress()
NetworkMonitor --> MotionNotificationService: Connected(10.0.0.51)
MotionNotificationService -> WebhookHttpServer: stop()
MotionNotificationService -> WebhookManager: unregisterAll()
loop For each camera
    WebhookManager -> Camera: Unsubscribe(subscription ID)
end
MotionNotificationService -> WebhookManager: registerWebhooks(cameras, new IP)
loop For each camera
    WebhookManager -> Camera: CreateNotification(new webhook URL)
    Camera --> WebhookManager: New subscription ID or Error
end
alt At least one webhook registered successfully
    MotionNotificationService -> WebhookHttpServer: start(port, new IP)
    WebhookHttpServer --> MotionNotificationService: Server started
else All webhooks failed
    MotionNotificationService -> MotionNotificationService: Display error notification
end
```

## Configuration Storage

**SharedPreferences Keys:**
```kotlin
object MotionNotificationPrefs {
    const val PREF_NAME = "motion_notifications"
    const val KEY_ENABLED_CAMERAS = "enabled_cameras" // JSON array of camera IDs
    const val KEY_SERVER_PORT = "server_port"
    const val KEY_LAST_IP = "last_ip_address"
    const val KEY_WEBHOOK_TOKENS = "webhook_tokens" // JSON map of camera ID -> token
}
```

**Per-Camera Configuration:**
```kotlin
data class CameraConfig(
    val id: String,
    val name: String,
    val onvifServiceUrl: String,
    val credentials: ONVIFCredentials?,
    val ipAddress: String,
    val motionNotificationsEnabled: Boolean
)
```

## Error Handling Strategy

### Error Categories

1. **Network Errors**
   - No WiFi connection → Pause service, show notification
   - IP address unavailable → Retry every 30 seconds
   - Camera unreachable → Mark camera as offline, continue monitoring others

2. **ONVIF Errors**
   - CreateNotification fails → Retry with backoff, log error
   - Camera doesn't support notifications → Disable for that camera, show error in UI
   - Subscription expired → Attempt re-registration

3. **HTTP Server Errors**
   - Port already in use → Try next port in range
   - All ports in use → Show error, stop service
   - Malformed request → Log and return 400, continue serving

4. **Android System Errors**
   - Service killed by system → Restart via START_STICKY
   - Notification permission denied → Show permission request dialog
   - Battery optimization → Prompt user to disable for app

## Performance Considerations

### Battery Optimization
- Use `WakeLock` only during webhook registration (< 30 seconds)
- HTTP server uses minimal CPU when idle (event-driven)
- Network monitor uses system callbacks (no polling)
- Webhook renewal scheduled with `AlarmManager` (exact timing not required)

### Memory Management
- Limit concurrent webhook registrations to 5
- Clear old notifications after 10 minutes
- Release HTTP server resources on service stop
- Use weak references for callbacks to prevent leaks

### Network Efficiency
- Batch webhook registrations when possible
- Reuse ONVIF SOAP client connections
- Set reasonable timeouts (5s connect, 10s read)
- Cancel pending operations on service stop

## Security Considerations

### Webhook Security
- Generate random 32-character token per camera
- Include token in webhook URL path
- Validate source IP matches camera IP
- Reject requests without valid ONVIF structure

### Credential Storage
- Use existing encrypted credential storage
- Never log credentials or tokens
- Clear credentials from memory after use

### Network Security
- HTTP server only binds to local network interface (not 0.0.0.0)
- Only accepts connections from private IP address space
- No external/internet access required or supported
- Firewall-friendly (inbound only from camera IPs on local network)
- Service pauses when device switches to mobile data

## Testing Strategy

### Unit Tests
- OnvifNotificationParser: Parse valid/invalid messages
- WebhookManager: Subscription lifecycle
- NetworkMonitor: IP address detection
- Token generation and validation

### Integration Tests
- HTTP server receives and processes webhook
- Service survives app closure
- Network change triggers re-registration
- Notifications displayed correctly

### Manual Testing
- Test with real Thingino camera
- Verify notifications on different Android versions
- Test network disconnection/reconnection
- Verify battery usage over 24 hours
- Test with multiple cameras simultaneously

## Dependencies

### New Libraries
- **NanoHTTPD** (2.3.1) - Lightweight HTTP server
  - License: BSD-3-Clause
  - Size: ~100KB
  - Purpose: Webhook HTTP server

### Existing Libraries
- ONVIFSoapClient (already implemented)
- Android WorkManager (for service restart)
- Kotlin Coroutines (for async operations)

## Implementation Considerations

### Android API Requirements
- Minimum SDK: 26 (Android 8.0) - Foreground service support
- Target SDK: 36 (Android 14+) - Current target
- Permissions required:
  - `FOREGROUND_SERVICE`
  - `POST_NOTIFICATIONS` (Android 13+)
  - `ACCESS_NETWORK_STATE`
  - `INTERNET`

### Backward Compatibility
- Graceful degradation for cameras without ONVIF notification support
- Fallback to existing SFTP motion detection
- Clear messaging when feature unavailable
- Clear messaging that feature only works on local networks

### Limitations
- **Local network only**: Feature does not work when device is on mobile data or different network than cameras
- **HTTP server conditional**: Server only runs when webhooks are successfully registered
- **No cloud relay**: No support for remote access or cloud-based notification relay
- **Private IP only**: Only supports cameras and devices on private IP address space (RFC 1918)

### Future Enhancements (Out of Scope)
- Video snapshot in notification
- Notification grouping for multiple cameras
- Custom notification sounds per camera
- Motion event history/log
- Integration with Android Auto/Wear OS
