---
id: Svelte
title: Svelte
sidebar_label: Svelte
--- 

Integrating **ezto verify's** Liveness SDK into your **Svelte** project is a streamlined process. Follow these steps to add the necessary files and initiate the liveness process within your **Svelte** application.

## Implement the SDK in Component

Set up your **Svelte** component to utilize the **ezto verify Liveness SDK:**

```html title="javascript"

<script>
  let livenessSDK;

  async function start() {
    const sdkModule = await import('https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/liveness/dist/liveness.min.js');
    const Liveness = sdkModule.default;

    const config = {
      fingerCount: { enabled: true, expected: 5 },
      blink: { enabled: true, expected: 2 },
      speech: { enabled: true, expected: "Hat" },
      movement: { enabled: true },
    };

    livenessSDK = new Liveness(config);

    livenessSDK.onLoaded = () => { /* Loaded */ };
    livenessSDK.onCompleted = (flows, meta) => { /* Success */ };
    livenessSDK.onError = (err) => { /* Error */ };
    livenessSDK.onFileProcessed = (videoFile) => { /* Video file */ };

    livenessSDK.init();
    livenessSDK.startLiveness();
  }

  function retry() {
    if (livenessSDK && livenessSDK.retry) {
      livenessSDK.retry();
    }
  }

  // Start on mount
  onMount(() => { start(); });

  // Optionally, clean up on destroy
  onDestroy(() => {
    if (livenessSDK && livenessSDK.destroy) livenessSDK.destroy();
  });
</script>

<button on:click={retry}>Retry</button>

<!-- index.html <head>: -->
<!-- <link href="https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/liveness/styles/liveness.css" rel="stylesheet" /> -->
```