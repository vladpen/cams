# ONVIF PTZ Control Requirements

## User Stories

### Camera Discovery
WHEN a user opens the camera configuration screen  
THE SYSTEM SHALL provide an option to discover ONVIF cameras on the network

WHEN a user selects "Discover ONVIF Cameras"  
THE SYSTEM SHALL scan the local network for ONVIF-compatible devices  
AND display a list of discovered cameras with their names and IP addresses

WHEN a user selects a discovered ONVIF camera  
THE SYSTEM SHALL automatically populate the camera configuration with RTSP URL and ONVIF service endpoint

### PTZ Control Interface
WHEN a user is viewing a camera stream that supports PTZ  
THE SYSTEM SHALL display PTZ control buttons on the video interface

WHEN a user taps directional arrows (up, down, left, right)  
THE SYSTEM SHALL send continuous move commands to the camera in the selected direction

WHEN a user releases a directional arrow  
THE SYSTEM SHALL send a stop command to halt camera movement

WHEN a user taps zoom in (+) or zoom out (-)  
THE SYSTEM SHALL send zoom commands to the camera

### PTZ Capabilities Detection
WHEN the app connects to an ONVIF camera  
THE SYSTEM SHALL query the camera's PTZ capabilities  
AND only display controls for supported movements (pan, tilt, zoom)

WHEN a camera does not support PTZ  
THE SYSTEM SHALL not display PTZ controls

### Preset Management
WHEN a user long-presses on the video while PTZ controls are visible  
THE SYSTEM SHALL show options to save current position as a preset

WHEN a user saves a preset  
THE SYSTEM SHALL store the preset with a user-defined name

WHEN a user accesses preset menu  
THE SYSTEM SHALL display saved presets and allow navigation to them

### Error Handling
WHEN ONVIF discovery fails  
THE SYSTEM SHALL display an error message indicating network issues or no cameras found

WHEN PTZ commands fail  
THE SYSTEM SHALL show a brief error notification without disrupting video playbook

WHEN ONVIF authentication fails  
THE SYSTEM SHALL prompt for credentials and retry the connection

### ONVIF Motion Detection Events
WHEN the app connects to an ONVIF camera  
THE SYSTEM SHALL investigate and determine if the camera supports ONVIF motion detection events

WHEN an ONVIF camera supports motion detection events  
THE SYSTEM SHALL subscribe to motion detection notifications from the camera

WHEN a motion detection event is received from an ONVIF camera  
THE SYSTEM SHALL display a visual indicator on the camera stream (e.g., red border or motion icon)

WHEN motion detection events are active  
THE SYSTEM SHALL provide an option to enable/disable motion event notifications in camera settings

WHEN motion detection events are enabled  
THE SYSTEM SHALL send local notifications to the user when motion is detected

WHEN investigating ONVIF motion detection capabilities  
THE SYSTEM SHALL log findings about event support and implementation requirements for future development

## Acceptance Criteria

- PTZ controls appear only for ONVIF cameras with PTZ capabilities
- Camera movement is smooth and responsive to user input
- Discovery finds cameras within 10 seconds on local network
- PTZ controls do not interfere with existing video playback functionality
- Preset positions are saved persistently across app sessions
- All PTZ operations work with standard ONVIF Profile S cameras
- Motion detection events are investigated and implemented if supported by ONVIF standard
- Motion detection visual indicators do not obstruct video viewing
- Motion event notifications can be toggled on/off per camera
- Investigation findings are documented for future reference
