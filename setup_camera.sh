#!/bin/bash

echo "Setting up Thingino camera in the app..."

# Launch the app
adb shell am start -n com.vladpen.cams/.MainActivity
sleep 3

# Tap the "ADD CAMERA" button (center coordinates from UI dump: bounds="[405,997][675,1117]")
echo "Tapping ADD CAMERA button..."
adb shell input tap 540 1057
sleep 2

# Enter camera name
echo "Entering camera name..."
adb shell input text "Thingino%sCamera"
sleep 1

# Tab to next field and enter RTSP URL
echo "Entering RTSP URL..."
adb shell input keyevent 61
sleep 1
adb shell input text "rtsp://thingino:AjVHVSUPOoZQkGG2U@10.0.0.51:554/ch0"
sleep 1

# Scroll down to find ONVIF fields
echo "Scrolling to ONVIF fields..."
adb shell input swipe 500 1500 500 800
sleep 1

# Enter ONVIF service URL with credentials
echo "Entering ONVIF URL..."
adb shell input keyevent 61
adb shell input keyevent 61
sleep 1
adb shell input text "onvif://thingino:AjVHVSUPOoZQkGG2U@10.0.0.51:80/onvif/device_service"
sleep 1

# Save the configuration
echo "Saving configuration..."
adb shell input keyevent 4  # Back key to save
sleep 2

# Tap on the newly created camera to open video
echo "Opening video stream..."
adb shell input tap 540 300  # Tap in the recycler view area
sleep 3

echo "Setup complete! The video should now be open with PTZ controls."
echo "Touch the center of the screen to see the PTZ dot."
