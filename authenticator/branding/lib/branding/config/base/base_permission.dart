abstract class BasePermission {
  final List<String> androidPermissions = [
    "INTERNET",
    "USE_BIOMETRIC",
    "POST_NOTIFICATIONS",
    "CAMERA",
    "RECORD_AUDIO",
    "VIDEO_CAPTURE",
    "AUDIO_CAPTURE",
    "ACCESS_FINE_LOCATION",
    "ACCESS_COARSE_LOCATION",
    "MODIFY_AUDIO_SETTINGS",
  ];

  final Map<String, String> applePermissions = {
    "NSCameraUsageDescription":
        "Camera access is required for user verification",
    "NSFaceIDUsageDescription": "FaceID is required for user verification",
    "NSMicrophoneUsageDescription":
        "Mic access is required for user verification",
    "NSLocationAlwaysAndWhenInUseUsageDescription":
        "Location access is required for user verification",
    "NSLocationWhenInUseUsageDescription":
        "Location access is needed for user verification."
  };
}
