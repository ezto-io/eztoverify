---
id: setup
title: Liveness Setup
sidebar_label: Setup
---

Integrating **ezto verify’s Liveness SDK** into your project is straightforward and requires no build steps. Use the CDN-based module import and simple configuration to get started.

> The Liveness SDK supports tests such as finger counting, blinking, speech recognition, and movement detection. Each test can be easily enabled or customized through the configuration object.

<br />

## _1_ Import Liveness SDK via CDN

- Add the `Liveness` class by including a `<script type="module">` in your HTML, loading the SDK directly from the CDN

```html
<script type="module">
  import Liveness from "https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/liveness/dist/liveness.min.js";
  // Your integration code goes here
</script>
```

<br />

## _2_ Add liveness CSS

Include the following link tag in your HTML to load the necessary CSS file:

```html
<link
  href="https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/liveness/styles/liveness.css"
  rel="stylesheet"
/>
```

<br />

## _3_ Add the element

- Add the below element where the liveness ui to be rendered.

```html
<div id="liveness"></div>
```

## _4_ Configure the SDK

- Set up a configuration object to specify which liveness checks you want to include:

```javascript
const config = {
  fingerCount: {
    enabled: true, // Enable finger counting test
    expected: 5, // Expect user to show 5 fingers
  },
  blink: {
    enabled: true, // Enable blink test
    expected: 2, // Expect user to blink twice
  },
  speech: {
    enabled: true, // Enable speech test
    expected: "Hat", // Expect user to say the word "Hat"
  },
  movement: {
    enabled: true, // Enable random movement test
  },
};
```

## _5_ Initialize & Use the SDK

- Initialize the SDK, set up event handlers for completion, errors, loading, and file-processed events, then call the main methods:

```javascript
let livenessSDK = new Liveness(config);

// Event handlers (define actions for each event)
livenessSDK.onLoaded = () => {
  // Called when the SDK loads successfully
};

livenessSDK.onCompleted = (completedFlows, metadata) => {
  // Called when the liveness check completes successfully
};

livenessSDK.onError = (error) => {
  // Handle any errors here
};

livenessSDK.onFileProcessed = (videoFile) => {
  // Process the captured video file here
};

// Initialize on page load
livenessSDK.init();

// Start the liveness process
livenessSDK.startLiveness();

// Optionally, to reset and retry the liveness check:
livenessSDK.retry();
```

> After receiving the `onLoaded` callback, you must call either `startLiveness()` or `retry()` to begin the liveness check process.

## _6_  Receive Video from Callback
- After receiving the video file from the `onFileProcessed` callback function, call the ezto verify Liveness API with the required authentication and form data.


## Possible errors 

These errors can occur in any of the workflows mentioned on this page. They typically relate to system issues, request failures, or access restrictions that may disrupt the process.

| Error                           | Message                                                                                     |
|---------------------------------|---------------------------------------------------------------------------------------------|
| **InitError**                   | Failed to Initialize.                                                                       |
| **UIRenderError**               | Failed to render the UI.                                                                    |
| **RemoveUIError**               | Failed to remove the UI.                                                                    |
| **videoRecorderError**          | Video stream is null, cannot stop recording.                                                |
| **FaceNotCenteredError**        | Face not centered for long time.                                                            |                                          
| **NotAllowedError**             | Camera access denied. Please allow camera permissions and try again.                        |
| **PermissionDeniedError**       | Camera access denied. Please allow camera permissions and try again.                        |
| **NotFoundError**               | No camera found. Please connect a camera and try again.                                     |
| **DevicesNotFoundError**        | No camera found. Please connect a camera and try again.                                     |
| **NotReadableError**            | Camera is already in use by another application.                                            |
| **TrackStartError**             | Camera is already in use by another application.                                            |
| **OverconstrainedError**        | Camera does not support the requested configuration.                                        |
| **ConstraintNotSatisfiedError** | Camera does not support the requested configuration.                                        |
| **NotSupportedError**           | Camera access is not supported in this browser.                                             |
| **AbortError**                  | Camera access was aborted.                                                                  |
| **SecurityError**               | Camera access blocked due to security restrictions.                                         |
