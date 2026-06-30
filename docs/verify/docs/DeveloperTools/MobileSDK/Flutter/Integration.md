---
id: Integration
title: Integration
sidebar_label: Integration
--- 

This section covers the fundamental setup and configuration required to integrate the SDK into your application. It includes the code needed to initialize and utilize the core functionalities provided by the SDK.

## Base Usage

This involves wrapping your `MaterialApp` widget with `EztoVerifyApp` and configuring various callbacks and settings to manage permissions, errors, and transactions within your **Flutter** app.

```javascript
EztoVerifyApp(
    forceUpdate: const ForceUpdate(
        android: UpgradeConfig(shouldForceUpdate: true),
        ios: UpgradeConfig(shouldForceUpdate: true)
        // App force update need call this
    ),
    onPermissionDenied: (deniedPermission) async {
        // Handle permission denial
        return true;
    },
    onResult: (String reqId, String authReqId) async {
        // Handle successful authentication result
    },
    onPushTokenRequest: () async {
        return GetTokenModel(
            // Provide FCM token
            pushToken: "token",
            pushType: PushSupport.firebase,
        );
    },
    onError: (reason) {
        // Handle SDK error
    },
    onClose: (reason) {
        // Handle transaction closure
    },
    onTransactionRequest: (data, isFromDeeplink) async {
        // For deeplinks
        EztoVerify(
            encryptionKey: key,
            ).onPushReceived(
        data: data);
    },
    child: MaterialApp(
        // ... Your app configuration
    ),
);
```

#### Callback Functions

- `forceUpdate` - If given true, the respective app will check for available update and force update will be initiated.
- `onPermissionDenied` - Called when the user denies runtime permission. Return true if all requested permissions are granted, else return false.
- `onResult` - Called if the Result Hook notification way in Ezto dashboard is set to Mobile_Sdk. Only called if the authentication flow is completed successfully.
- `onPushTokenRequest` - SDK has requested the token and push type. App should get the token, push type and return it.
- `onError` - Called when the SDK encounters an error.
- `onClose` - Called when the transaction has completed.
- `onTransactionRequest` - This function is called to get the payload data.


<br/>

## Push Support

This functionality enables an application to handle and process push notifications. It includes setting up the necessary configurations and methods to receive, decrypt, and respond to push messages sent to the app. 

**ezto verify's SDK** push support involves using the `onPushReceived` method on the `EztoVerify` instance to handle incoming push notifications securely with the provided encryption key. To get the encryption key, check [here](/verify/docs/EV/AdvancedSettings/Keys/).

```javascript
 FirebaseMessaging.onMessageOpenedApp.listen((message) async {
    EztoVerify(
        encryptionKey: {{encryption key}}, // replace this
        ).onPushReceived(
        data: message.data,
    );
});
```

<br/>

## QR Support

This functionality allows an application to scan and process QR codes. It involves setting up the necessary configurations and methods to read QR codes and perform actions based on the data contained within them. 

**ezto verify's SDK** QR support involves using the `scanQr` method on the `EztoVerify` instance to enable secure QR code scanning within your app, utilizing the provided encryption key for secure processing.

To launch the QR scanner, use the below code. To get the encryption key, check [here](/verify/docs/EV/AdvancedSettings/Keys/).

```javascript
EztoVerify(
    encryptionKey: {{encryption key}},
).scanQr();
```

## Byte Trigger Support

This functionality allows the application to handle QR codes in the form of raw bytes. It involves converting the base64 image to a byte array and passing it to the SDK for secure processing and transaction initiation.

**ezto verify's SDK** supports processing QR codes from byte arrays using the `qrFromBytes` method on the `EztoVerify` instance. This is particularly useful when QR codes are received as images rather than scanned directly from the camera.

```javascript
EztoVerify().qrFromBytes(bytes: qrCode);
```