class IproovErrorService {
  String feedbackCodeToReason(String feedBackCode) {
    switch (feedBackCode.toUpperCase()) {
      case "TOO_MUCH_MOVEMENT":
      case "tooMuchMovement":
        return buildMessage("Too much movement detected. Please keep still");
      case "TOO_BRIGHT":
      case "tooBright":
        return buildMessage(
          "The environment is too bright. Please move somewhere darker",
        );
      case "TOO_DARK":
      case "tooDark":
        return buildMessage(
          "The environment is too dark. Please move somewhere brighter",
        );
      case "MISALIGNED_FACE":
      case "misalignedFace":
        return buildMessage(
          "Please keep your face in the oval",
        );
      case "EYES_CLOSED":
      case "eyesClosed":
        return buildMessage(
          "Please keep your eyes open",
        );
      case "FACE_TOO_FAR":
      case "faceTooFar":
        return buildMessage(
          "Please move your face closer to the screen",
        );
      case "FACE_TOO_CLOSE":
      case "faceTooClose":
        return buildMessage(
          "Please move your face farther from the screen",
        );
      case "SUNGLASSES":
      case "sunglasses":
        return buildMessage(
          "Please remove your glasses",
        );
      case "OBSCURED_FACE":
      case "obscuredFace":
        return buildMessage("Please remove any face coverings");
      case "USER_TIMEOUT":
      case "userTimeout":
        return "Request timed out. Please try again!";
      case "NOT_SUPPORTED":
      case "notSupported":
        return "Unable to complete request. Device is not supported";
      case "UNKNOWN":
      case "unknown":
      default:
        return "Unable to complete request. Please try again!";
    }
  }

  String buildMessage(String message) {
    return "Unable to complete request $message and try again!";
  }
}
