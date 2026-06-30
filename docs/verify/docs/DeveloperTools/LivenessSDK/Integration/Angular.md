---
id: Angular
title: Angular
sidebar_label: Angular
--- 

Integrating **ezto verify's** Liveness SDK into your **Angular** project is a streamlined process. Follow these steps to add the necessary files and initiate the liveness process within your **Angular** application.

##  Implement the SDK in Component

Set up your **Angular** component to use the **ezto verify Liveness SDK**:

```html
  <!-- In the <head> -->
  <link href="https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/liveness/styles/liveness.css" rel="stylesheet" />
```


```ts title="typescript"
// app.component.ts

import { Component, AfterViewInit, OnDestroy } from '@angular/core';

@Component({
  selector: 'app-liveness',
  template: `<button (click)="retry()">Retry</button>`,
})
export class LivenessComponent implements AfterViewInit, OnDestroy {
  private livenessSDK: any;

  async ngAfterViewInit() {
    // Dynamically import as ES module
    // @ts-ignore
    const sdkModule = await import('https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/liveness/dist/liveness.min.js');
    const Liveness = sdkModule.default;

    const config = {
      fingerCount: { enabled: true, expected: 5 },
      blink: { enabled: true, expected: 2 },
      speech: { enabled: true, expected: "Hat" },
      movement: { enabled: true },
    };

    this.livenessSDK = new Liveness(config);

    this.livenessSDK.onLoaded = () => { /* Loaded */ };
    this.livenessSDK.onCompleted = (flows, meta) => { /* Success */ };
    this.livenessSDK.onError = (err) => { /* Error */ };
    this.livenessSDK.onFileProcessed = (videoFile) => { /* Video file */ };

    this.livenessSDK.init();
    this.livenessSDK.startLiveness();
  }

  retry() {
    if (this.livenessSDK) {
      this.livenessSDK.retry();
    }
  }

  ngOnDestroy() {
    if (this.livenessSDK && this.livenessSDK.destroy) {
      this.livenessSDK.destroy();
    }
  }
}
```

## Add a Button to Trigger the Request

In your component's template, add a button to trigger the request:

```html
 <div>
      {/* Usually, the SDK injects its own UI, but you can create your own container if needed */}
      <div id="liveness"></div>
      <button onClick={() => livenessRef.current && livenessRef.current.retry()}>
        Retry Liveness Check
      </button>
 </div>
```