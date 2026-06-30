---
id: Integration
title: Integration
sidebar_label: Integration
--- 

This section covers the fundamental setup and configuration required to integrate the SDK into your application. It includes the code needed to initialize and utilize the core functionalities provided by the SDK.

## Base Usage

Implement the `EztoService` interface and override its methods to enable communication between the SDK and your app. This setup allows the SDK to interact with your application, handling key events such as permissions, errors, and transaction updates.

```javascript
import { onPushReceived, sendFcmToken } from 'io.ezto.verify.react';

const McWrapper = NativeModules.EztoVerifyServiceImpl;
const eventEmitter = new NativeEventEmitter(McWrapper);

export const getFcmToken = async () => {
  sendFcmToken("token", "pushType");
}
// Call the getFcmToken function in the main init function.

export function MyListener(this: any) {
  this.eventListener = eventEmitter.addListener(
    'onClosed', // Don't change the eventType
    (event: any) => {
      //Transaction has completed
    }
  );
  this.eventListener = eventEmitter.addListener(
    'onError', // Don't change the eventType
    (event: any) => {
      //SDK has thrown some error
    }
  );
  this.eventListener = eventEmitter.addListener(
    'onResult', // Don't change the eventType
    (event: any) => {
        //This will be called if the Result Hook notification way in Ezto dashboard is set to Mobile_Sdk
        //Only called if the authentication flow is completed successfully
    }
  );
  this.eventListener = eventEmitter.addListener(
    'onPermissionDenied', // Don't change the eventType
    (event: any) => {
        //The user has denied runtime permission
    }
  );
  this.eventListener = eventEmitter.addListener(
    'onTransactionRequest', // Don't change the eventType
    (event: any) => {
       onPushReceived(event.payload, {{encryption key}});
    }
  );
}
```

#### Callback Functions

- `sendFcmToken` - The SDK relies on the token and push type. The application needs to obtain the token, push type and invoke a specific function within its main init function, providing the token and type.
- `onClose` - Called when the transaction has completed.
- `onError` - Called when the SDK encounters an error.
- `onResult` - Called if the Result Hook notification way in ezto dashboard is set to Mobile_Sdk. Only called if the authentication flow is completed successfully.
- `onPermissionDenied` - Called when the user denies runtime permission. Return true if all requested permissions are granted, else return false.
- `onTransactionRequest` -  This function is called to get the payload data.

<br/>

## Force Update

The `forceUpdate` function ensures that your app can enforce updates when necessary. By passing true to this function, the app will check for available updates and initiate a forced update if one is required, ensuring that you are always on the latest version.

```java
import {eztoInit} from'io.ezto.verify.react';
// If app's force update needed, pass bool true
eztoInit(true)
```
<br/>

## Push Support

This functionality enables an application to handle and process push notifications. It includes setting up the necessary configurations and methods to receive, decrypt, and respond to push messages sent to the app. 

**ezto verify's SDK** push support involves using the `onPushReceived` method on the `EztoVerify` instance to handle incoming push notifications securely with the provided encryption key. To get the encryption key, check [here](/verify/docs/EV/AdvancedSettings/Keys/).

```javascript
import { onPushReceived } from 'io.ezto.verify.react';

messaging().onMessage(async (remoteMessage) => {
    //Firebase can be utilized as the push service for push support.
    onPushReceived(remoteMessage.data, {{encryption key}});
})
```

<br/>

## QR Support

This functionality allows an application to scan and process QR codes. It involves setting up the necessary configurations and methods to read QR codes and perform actions based on the data contained within them. 

**ezto verify's SDK** QR support involves using the `launchQrScanner` method on the `EztoVerify` instance to enable secure QR code scanning within your app, utilizing the provided encryption key for secure processing.

To launch the QR scanner, use the below code. To get the encryption key, check [here](/verify/docs/EV/AdvancedSettings/Keys/).

```javascript
import { launchQrScanner } from 'io.ezto.verify.react';

//Firebase can be utilized as the push service for push support.
launchQrScanner({{encryption key}});
```

<br/>

## DeepLink Support

This allows your app to handle and process deep links, which are URLs that direct users to specific content or features within your app. 

Use the `onDeeplinkRequest` method on the `EztoVerify` instance to manage incoming deep link requests and handle them appropriately within your application.

```javascript
import { onDeeplinkRequest } from 'io.ezto.verify.react';

Linking.getInitialURL().then((url) => {
  onDeeplinkRequest(url);
})
```