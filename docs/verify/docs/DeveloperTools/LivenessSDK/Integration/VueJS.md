---
id: VueJS
title: Vue JS
sidebar_label: Vue JS
--- 

Integrating **ezto verify's** Liveness SDK into your **Vue.js** project is a streamlined process. Follow these steps to add the necessary files and initiate the liveness process within your **Vue** application.

## Implement the SDK in Component

Set up your **Vue.js** component to utilize the **ezto verify Liveness SDK:**

```html title="javascript"
<template>
  <button @click="retry">Retry Liveness Check</button>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue';

const livenessSDK = ref(null);

onMounted(async () => {
  // Import as an ES Module
  const sdkModule = await import('https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/liveness/dist/liveness.min.js');
  const Liveness = sdkModule.default;

  const config = {
    fingerCount: { enabled: true, expected: 5 },
    blink: { enabled: true, expected: 2 },
    speech: { enabled: true, expected: "Hat" },
    movement: { enabled: true },
  };

  livenessSDK.value = new Liveness(config);

  livenessSDK.value.onLoaded = () => { /* Loaded */ };
  livenessSDK.value.onCompleted = (flows, meta) => { /* Success */ };
  livenessSDK.value.onError = (err) => { /* Error */ };
  livenessSDK.value.onFileProcessed = (videoFile) => { /* Video file */ };

  livenessSDK.value.init();
  livenessSDK.value.startLiveness();
});

onUnmounted(() => {
  if (livenessSDK.value && livenessSDK.value.destroy) {
    livenessSDK.value.destroy();
  }
});

function retry() {
  if (livenessSDK.value) livenessSDK.value.retry();
}
</script>

<!-- In your index.html <head>: -->
<!-- <link href="https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/liveness/styles/liveness.css" rel="stylesheet" /> -->
```
