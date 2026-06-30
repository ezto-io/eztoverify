---
id: NextJS
title: Next JS
sidebar_label: Next JS
--- 


Integrating **ezto verify's** Liveness SDK into your **Next.js** project is a streamlined process. Follow these steps to add the necessary files and initiate the liveness process within your **Next.js** application.

## Implement the SDK in Component

Set up your **Next.js** component to use the **ezto verify Liveness SDK:**

```js title="javascript"

import React, { useEffect, useRef } from "react";

// Add this to your _app.js or index.html head for the CSS
// <link href="https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/liveness/styles/liveness.css" rel="stylesheet" />

export default function Liveness() {
  const livenessRef = useRef(null);
  const [livenessLoaded, setLivenessLoaded] = useState(false);

  useEffect(() => {
    let livenessSDK;

    async function loadSDK() {
      // Dynamically import the SDK as an ES module
      const sdkModule = await import(
        "https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/liveness/dist/liveness.min.js"
      );
      const Liveness = sdkModule.default;

      const config = {
        fingerCount: { enabled: true, expected: 5 },
        blink: { enabled: true, expected: 2 },
        speech: { enabled: true, expected: "Hat" },
        movement: { enabled: true },
      };

      livenessSDK = new Liveness(config);

      livenessSDK.onLoaded = () => { /* Loaded */  setLivenessLoaded(true)};
      livenessSDK.onCompleted = (flows, meta) => { /* Success */ };
      livenessSDK.onError = (err) => { /* Error */ };
      livenessSDK.onFileProcessed = (videoFile) => { /* Video file */ };

      livenessSDK.init();
      livenessRef.current = livenessSDK;
    }

    loadSDK();

    // Cleanup
    return () => {
      if (livenessRef.current && livenessRef.current.destroy) {
        livenessRef.current.destroy();
      }
      livenessRef.current = null;
    };
  }, []);

  return (
    <div>
      {/* Usually, the SDK injects its own UI, but you can create your own container if needed */}
      <div id="liveness"></div>
      {livenessLoaded && (
        <button onClick={() => livenessRef.current && livenessRef.current.startLiveness()}>
            start Liveness Check
        </button>
        <button onClick={() => livenessRef.current && livenessRef.current.retry()}>
            Retry Liveness Check
        </button> 
      )}
    </div>
  );
}
```