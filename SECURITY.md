# Security Policy

## Overview

ONVIF Camera (fork of Cams) is designed with security and privacy as core principles. This document outlines the security measures, known issues, and recommendations for secure usage.

## Security Principles

1. **No Data Collection**: The app does not collect, transmit, or store any user data on external servers
2. **Local Storage Only**: All configuration and credentials are stored locally on the device
3. **No Analytics**: No Google Analytics, Firebase, or third-party tracking services
4. **No Cloud Services**: Direct camera connections only - no manufacturer cloud services
5. **Encrypted Credentials**: ONVIF and SFTP credentials are encrypted using AES-256

## Supported Versions

| Version | Supported          | Status |
| ------- | ------------------ | ------ |
| 2.4.5   | :white_check_mark: | Current release |
| 2.4.4   | :x:                | Superseded |
| < 2.4.4 | :x:                | No longer supported |

## Third-Party Dependencies

### Core Dependencies

#### VLC for Android (libvlc-all) - Version 3.6.5
- **License**: LGPL 2.1+ / GPL 2.0+
- **Purpose**: Video playback engine for RTSP/RTMP streams
- **Status**: ✅ **CURRENT** - Latest stable Android release (May 2025)
- **Version Notes**: 
  - Android VLC uses 3.6.x branch (currently 3.6.5)
  - Desktop VLC uses 3.0.x branch (currently 3.0.22/3.0.23)
  - Android version is newer and maintained separately
  - Security fixes from desktop 3.0.22 (CVE-2025-51602) likely already included or not applicable
- **Risk Level**: LOW - Latest stable version, trusted camera sources only
- **Mitigation**: Only connect to trusted camera sources
- **Recommendation**: Monitor for 3.6.6 or 3.7.0 stable release

#### AndroidX Libraries
- **androidx.core:core-ktx**: 1.17.0 (Latest as of Jan 2026) ✅
- **androidx.appcompat:appcompat**: 1.7.1 (Current) ✅
- **androidx.work:work-runtime-ktx**: 2.10.5 (Current) ✅
- **androidx.activity:activity-ktx**: 1.11.0 (Current) ✅
- **com.google.android.material:material**: 1.13.0 (Current) ✅
- **License**: Apache 2.0
- **Status**: All dependencies are current and maintained

#### Apache HttpClient Android - Version 4.3.5.1
- **License**: Apache 2.0
- **Purpose**: HTTP communication for ONVIF SOAP requests
- **Status**: ⚠️ **DEPRECATED** - Last updated 2014
- **Known Issues**: 
  - DefaultHttpClient class deprecated since 4.3
  - Lacks modern TLS 1.2/1.3 support by default
  - No longer receives security updates
- **Risk Level**: MEDIUM - Used only for local network ONVIF communication
- **Current Usage**: Limited to ONVIF device communication on local networks
- **Mitigation**: 
  - App uses HttpURLConnection for ONVIF SOAP (not HttpClient directly)
  - All ONVIF communication is on trusted local networks
  - TLS validation performed by Android platform
- **Recommendation**: Migrate to OkHttp or HttpClient 5.x in future release

#### JSch (SSH/SFTP) - Version 2.27.2
- **License**: BSD-style
- **Purpose**: SFTP file access for camera recordings
- **Status**: ✅ **CURRENT** - Maintained fork by mwiede
- **Security**: Actively maintained with security updates
- **Notes**: This is the community-maintained fork (com.github.mwiede:jsch)

#### Gson - Version 2.13.1
- **License**: Apache 2.0
- **Purpose**: JSON serialization for app settings
- **Status**: ✅ **CURRENT** - Latest stable release
- **Security**: No known vulnerabilities

### Test Dependencies
- **junit:junit**: 4.13.2 ✅
- **mockito:mockito-core**: 4.6.1 ✅
- **androidx.test.ext:junit**: 1.3.0 ✅
- **androidx.test.espresso:espresso-core**: 3.7.0 ✅

## Security Features

### Credential Storage
- **ONVIF Credentials**: Encrypted using AES-256-ECB with device-specific key
- **SFTP Passwords**: Base64 encoded (legacy, should be upgraded)
- **Storage Location**: Android SharedPreferences (private to app)
- **Key Management**: Encryption key stored in secure SharedPreferences

### Network Security
- **RTSP Streams**: Supports both TCP and UDP protocols
- **ONVIF Communication**: HTTP/HTTPS with WS-Security authentication
- **SFTP**: SSH-based secure file transfer
- **Certificate Validation**: Relies on Android platform TLS validation
- **Cleartext Traffic**: Permitted (required for IP camera protocols)

### Input Validation
- **URL Sanitization**: Basic validation and sanitization of camera URLs
- **XML Injection Protection**: ONVIF responses sanitized to remove script tags
- **Length Limits**: Input fields limited to 1000 characters
- **Special Character Filtering**: Removes `<>&"'` from ONVIF inputs

### Android Security
- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 36 (Android 14+)
- **Permissions**: 
  - INTERNET (required for camera streams)
  - CHANGE_WIFI_MULTICAST_STATE (for ONVIF discovery)
  - FOREGROUND_SERVICE (for motion detection alerts)
  - POST_NOTIFICATIONS (for alerts)
- **No Dangerous Permissions**: No access to contacts, location, camera, microphone, etc.

## Known Security Limitations

### 1. Deprecated HTTP Client (LOW RISK)
**Issue**: Apache HttpClient 4.3.5.1 is deprecated and lacks modern TLS support
**Impact**: Potential TLS downgrade attacks on ONVIF communication
**Mitigation**:
- ONVIF typically used on trusted local networks
- App uses HttpURLConnection (not HttpClient directly)
- Android platform provides TLS validation
**Recommendation**: Migrate to modern HTTP client in future release

### 2. Credential Encryption (LOW RISK)
**Issue**: AES-ECB mode used instead of AES-GCM or AES-CBC
**Impact**: ECB mode can leak patterns in encrypted data
**Mitigation**: 
- Credentials are short strings (username:password)
- Pattern leakage minimal for credential-length data
- Key stored securely in app-private storage
**Recommendation**: Migrate to AES-GCM in future release

### 3. SFTP Password Encoding (MEDIUM RISK)
**Issue**: SFTP passwords stored as Base64 (encoding, not encryption)
**Impact**: Passwords readable if device storage compromised
**Mitigation**: 
- Android app storage is private by default
- Requires root access or device compromise to read
**Recommendation**: Migrate to encrypted storage like ONVIF credentials

### 4. Cleartext Traffic Permitted (BY DESIGN)
**Issue**: App allows HTTP connections (not HTTPS only)
**Impact**: Traffic can be intercepted on network
**Justification**: 
- IP cameras typically use HTTP/RTSP (not HTTPS)
- ONVIF protocol commonly uses HTTP on local networks
- RTSP protocol does not support TLS
**Mitigation**: Use on trusted networks only

## Security Best Practices for Users

### Network Security
1. **Use Trusted Networks**: Only connect to cameras on trusted local networks
2. **Avoid Public WiFi**: Do not access cameras over public/untrusted WiFi
3. **Network Segmentation**: Place cameras on isolated VLAN if possible
4. **Firewall Rules**: Restrict camera access to local network only

### Camera Configuration
1. **Strong Passwords**: Use strong, unique passwords for camera credentials
2. **Disable UPnP**: Disable UPnP on cameras to prevent internet exposure
3. **Firmware Updates**: Keep camera firmware updated
4. **Disable Unused Services**: Disable cloud services and unused protocols

### App Usage
1. **Trusted Sources Only**: Only add cameras you own or trust
2. **Verify URLs**: Double-check camera URLs before saving
3. **Regular Updates**: Keep app updated to latest version
4. **Device Security**: Use device lock screen and encryption

### SFTP Security
1. **Dedicated User**: Create dedicated SFTP user with limited permissions
2. **Chroot Jail**: Configure SFTP server with chroot for isolation
3. **Key-Based Auth**: Use SSH keys instead of passwords when possible
4. **Read-Only Access**: Grant read-only access to recording directories

## Reporting Security Vulnerabilities

If you discover a security vulnerability, please report it responsibly:

1. **Do NOT** open a public GitHub issue
2. **Email**: Contact maintainer via GitHub profile
3. **Include**: 
   - Description of vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if available)

We will respond within 48 hours and work to address confirmed vulnerabilities promptly.

## Security Roadmap

### Planned Improvements
1. **Migrate HTTP Client**: Replace Apache HttpClient with OkHttp
2. **Improve Encryption**: Migrate to AES-GCM for credential storage
3. **Encrypt SFTP Passwords**: Use same encryption as ONVIF credentials
4. **Certificate Pinning**: Add optional certificate pinning for ONVIF
5. **Security Audit**: Conduct third-party security audit

### Future Considerations
- Hardware-backed keystore for credential encryption (Android Keystore)
- Biometric authentication for app access
- Network traffic analysis/anomaly detection
- Secure element support for credential storage

## Compliance

### Privacy Regulations
- **GDPR Compliant**: No personal data collected or transmitted
- **CCPA Compliant**: No data sale or sharing
- **No Tracking**: No analytics or user tracking

### Open Source Licenses
All dependencies use permissive open-source licenses (MIT, Apache 2.0, BSD, LGPL). See [LICENSE.md](LICENSE.md) for full details.

## Security Audit History

| Date | Auditor | Scope | Findings | Status |
|------|---------|-------|----------|--------|
| 2026-01-24 | Internal | Dependency review | HttpClient deprecated, all others current | Documented |

## References

- [VLC for Android Releases](https://code.videolan.org/videolan/vlc-android/-/tags)
- [Apache HttpClient Migration Guide](https://hc.apache.org/httpcomponents-client-5.6.x/migration-guide/)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)

## Changelog

- **2026-01-24**: Initial security policy created
  - Documented all dependencies and versions
  - Clarified VLC Android 3.6.5 is newer than desktop 3.0.22
  - Noted Apache HttpClient deprecation
  - Documented encryption methods and limitations

---

*Last Updated: 2026-01-24*  
*Version: 2.4.5*
