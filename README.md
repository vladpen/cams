## Cams

> **Note**: This is a fork of the [original cams repository](https://github.com/vladpen/cams) with additional features including ONVIF PTZ control support.

![Cams](https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/5_cover.jpg)

Simple Android mobile application for playing RTSP streams from IP cameras.

Features:

- View RTSP streams from any IP cameras, including H.265+.
- Simultaneous viewing of multiple streams.
- 20x image zoom.
- Support for dual-channel cameras.
- View video recordings or images via SFTP protocol.
- Motion detection alerts configuration.
- High connection speed.
- Extremely simple navigation and control.
- Maximum security and data privacy.
- TCP/UDP protocol switching.
  This option is important when viewing cameras over the internet, where UDP may not be supported or work poorly.
- **ONVIF PTZ Control**: Touch-based pan, tilt, and zoom control for ONVIF-compatible cameras.
- **ONVIF Device Discovery**: Automatic detection of ONVIF cameras on the network.
- **Touch Gesture Control**: Intuitive touch-based PTZ control with visual feedback.

<img src="https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/1_main_ru.jpg"
alt="Main screen"
width="200">&nbsp;
<img src="https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/2_edit_ru.jpg"
alt="Edit screen"
width="200">&nbsp;
<img src="https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/3_files_ru.jpg"
alt="Files screen"
width="200">&nbsp;
<img src="https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/4_video_ru.jpg"
alt="Video screen"
width="200">

The application is written for joint use with the [python-rtsp-server](https://github.com/vladpen/python-rtsp-server) server,
but works perfectly standalone thanks to the ability to connect to any IP cameras, as well as video recorders that support SFTP.

Plays most types of video streams (not just RTSP).
The screenshot above shows an image from a real video camera and three test videos in "Group" mode.

*IMPORTANT. The application is focused on security and data privacy, so it does not collect or process any user information.
Data is not sent to any servers, including Google's technical infrastructure and manufacturers' "cloud" storage.*

## Installation

APK file can be built yourself, [downloaded from Github](https://github.com/vladpen/cams/raw/main/app/release/app-armeabi-v7a-release.apk),
installed using [F-Droid](https://f-droid.org/ru/packages/com.vladpen.cams/) (32-bit platforms only)
or [RuStore](https://www.rustore.ru/catalog/app/com.vladpen.cams).
Supports armeabi-v7a architecture (used in most modern mobile phones), arm64-v8a, x86-64 and x86.

## Configuration

To connect to a video camera, you need to enter its URL in the "Address" field as specified by the manufacturer. Usually it looks like this:
```
[rtsp://][<user>:<password>@]<IP>[:<port>][/<path>]
```
Parameters in square brackets are optional (depends on camera settings).

For dual-channel cameras, you can additionally specify the address of the second channel.
For example, for Hikvision cameras and their derivatives, the path will look like this:
```
ISAPI/Streaming/Channels/<channel number>
```
Then the first channel (high resolution) will have number 101, and the second (low resolution) — 102.

Low resolution channels can be used to speed up image loading,
to save traffic and to reduce device processor load.
This is especially convenient for viewing groups of cameras at low connection speeds.
During playback, channels can be switched using the K1/K2 button in the lower right corner of the screen.
On camera group screens, K2 is used by default.

Also, to reduce load, playback of cameras that go beyond the screen boundaries when zooming is paused.

SFTP server or video recorder address looks like this:
```
[sftp://]<user>:<password>@<IP>[:<port>][/<path>]
```
WARNING! It is strongly not recommended to use administrator access credentials.
For SFTP server, it's better to create chroot, for example, as described [here](https://wiki.archlinux.org/title/SFTP_chroot).

**Tip:** you can use emoji as icons in camera names.
For example, the screenshots above use icons from the standard mobile phone set.

## ONVIF Setup

This fork includes enhanced ONVIF support for automatic camera discovery and PTZ control.

### Touch-Based PTZ Control

For ONVIF cameras with PTZ capabilities:
- Touch the center of the video screen to activate PTZ control
- Drag your finger to pan and tilt the camera
- The PTZ dot follows your finger movement with visual feedback
- Release to stop movement - the dot smoothly returns to center
- Per-camera settings for PTZ inversion and rate limiting
- Works in fullscreen landscape mode for optimal control

### ONVIF Configuration

Configure ONVIF cameras using the single URL format:
```
onvif://username:password@camera-ip:port/onvif/device_service
```

### Supported ONVIF Features

- **Device Discovery**: WS-Discovery protocol for automatic camera detection
- **Touch PTZ Control**: Intuitive gesture-based pan, tilt, and zoom
- **Per-Camera Settings**: Individual PTZ inversion and rate limiting configuration
- **Profile S Compliance**: Compatible with ONVIF Profile S cameras
- **Secure Authentication**: WS-UsernameToken and HTTP Digest authentication

## Motion Detection Alerts

Optionally, the application can notify about camera motion detector triggers.
Alerts are triggered when a new image from the camera appears in the specified SFTP server folder.
For this function to work, you need to configure cameras and image storage server.
These settings are described in detail in the parallel project [Cams-PWA](https://github.com/vladpen/cams-pwa).

Detailed discussion of the application: [habr.com/ru/post/654915](https://habr.com/ru/post/654915/)
and server: [habr.com/ru/post/597363](https://habr.com/ru/post/597363/).

[<img src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
alt="Get it on Github"
height="80">](https://github.com/vladpen/cams/tree/main/app/release)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/packages/com.vladpen.cams/)

&nbsp; [<img src="https://user-images.githubusercontent.com/3853013/194689050-e6da2f21-9aa3-4662-9b7d-7293b140f22f.svg"
alt="Доступно в RuStore"
height="57">](https://apps.rustore.ru/app/com.vladpen.cams)

*Copyright (c) 2022-2025 vladpen under MIT license. Use it with absolutely no warranty.*
