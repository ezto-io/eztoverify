---
id: Angular
title: Angular
sidebar_label: Angular
--- 

Integrating **ezto verify's** Web SDK into your **Angular** project is seamless. Follow the steps below to add the necessary files and initiate the verification process within your **Angular** application.

##  Implement the SDK in Component

Set up your **Angular** component to use the **ezto verify SDK**:

```ts title="typescript"
// app.component.ts

import { Component } from '@angular/core';

declare var eztoverify: any; // Declare the SDK to avoid TypeScript errors

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  makeRequest() {
    const ez = new eztoverify();
    const metadata = { 
      request: JSON.stringify(
        {
            "user": {
                "metadata": {
                    "workflow": "" // onboarding or verification
                },
                "email": "example@gmail.com"
            }
        }
      ) // Build the request body using the API builder, and input it here.
    };
    const cnfg = {
      api: "http://xxxxxxxxxxxxxxxx/realms/api/ezto_web_push/0fbece21-c11d-48cb-913d-30a0b2e807be/xxxxxx/register",
      apiVersion:"1"
    };
    const callback = (response: any) => {
      console.log('Response:', response);
      // Handle the response here
    };
    ez.request(metadata, cnfg, callback);
  }
}


```

## Add a Button to Trigger the Request

In your component's template, add a button to trigger the request:

```html
<div>
  <button (click)="makeRequest()">Start verification</button>
</div>
```