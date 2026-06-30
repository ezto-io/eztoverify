---
id: Android
title: Android
sidebar_label: Android
--- 


Integrating the **ezto verify's SDK** into your **Native** project for **Android** is a straightforward process. Follow our comprehensive documentation and sample code snippets to quickly implement user verification features.

<br />

## *1* Requirements

The minimum requirements needed in order to use the **ezto verify SDK** in your **Native** project. Ensure your development environment meets these prerequisites before proceeding with the integration.
- Android Studio
- Android 14 (API level 34)

<br />

## *2* Installation 

* To add the **ezto verify SDK** to your **Native** project, it is necessary to add the dependency to the `dependencies` section in `build.gradle.kts`.

```java
dependencies {
  implementation("io.ezto.verify:sdk:7.0.0")
}
```

* If you are using Face Register and Face Verify workflow modules, it is necessary to add the dependency to the `dependencies` section in `build.gradle.kts`.

```java
dependencies {
  implementation("com.iproov.sdk:iproov:9.0.3")
}
```

<br/>

## *3* Permissions 

This section lists the permissions that need to be declared in your `AndroidManifest.xml` file located in `<PROJECT_DIR>\android\app\src\main\AndroidManifest.xml`. These permissions are required for the **ezto verify SDK** to function correctly, allowing it to access the necessary device features.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.VIDEO_CAPTURE" />
<uses-permission android:name="android.permission.AUDIO_CAPTURE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<!-- Below is required if using the ezto Pass Verification or De-Register ezto Pass workflow modules -->
<uses-permission android:name="android.permission.NFC" />
```

<br/>

## *4* Integration

### Base Usage

Implement the `EztoService` interface and override its methods to enable communication between the SDK and your app. This setup allows the SDK to interact with your application, handling key events such as permissions, errors, and transaction updates.

```java
class MyListener: EztoService {
    override suspend fun onPushTokenRequest(): String? {
        //SDK has requested token
        //Get token from firebase and return it
       return GetTokenModel(
            "token",
            PushSupport.firebase
        );
    }

    override fun onResult(reqId: String, authReqId: String) {
      //This will be called if the Result Hook notification way in Ezto dashboard is set to Mobile_Sdk
      //Only called if the authentication flow is completed successfully
    }

    override suspend fun onPermissionDenied(deniedPermissions: List<String>): Boolean {
        //The user has denied runtime permission 
        //The app can try again and return true if all requested permissions are granted, else return false
        return false
    }

    override fun onError(error: ErrorCode) {
       //This will be called in case of any errors during the flow
    }

    override fun onClosed(reason: CloseReason) {
       //This will be called if the sdk bottom sheet is closed
    }

    override fun onTransactionRequest(payload: Map<String,String>) {
        //This will be get deeplink payload data
        //Pass encryption key if the the push payload is encrypted
        Ezto.getInstance().onPushReceived(
            context = this,
            listener = this,
            message = payload,
            encryptionKey = {{encryption key}},
        )
    }
}
```
#### Callback Functions

- `onPushTokenRequest` - SDK has requested the token and push type. App should get the token, push type and return it.
- `onResult` - Called if the Result Hook notification way in Ezto dashboard is set to Mobile_Sdk. Only called if the authentication flow is completed successfully.
- `onPermissionDenied` - Called when the user denies runtime permission. Return true if all requested permissions are granted, else return false.
- `onError` - Called when the SDK encounters an error.
- `onClose` - Called when the transaction has completed.
- `onTransactionRequest` - This function is called to get the payload data.

<br/>

### Force Update

Use the `eztoInit` function in the main `init` function to enforce app updates when required. By passing `true` to this function, your app will check for and initiate a forced update, ensuring users are always on the latest version of the app.

```java
// If app's force update needed, pass bool true.
Ezto.getInstance().eztoInit(this, true)
```

<br/>

### Push Support

Integrate push notifications by configuring the SDK to handle messages in both foreground and background states. 

Use `onMessageReceived` to manage notifications when the app is active. To get the encryption key, check [here](/verify/docs/EV/AdvancedSettings/Keys/).

```java
class MessagingService : FirebaseMessagingService() {
    //This method is overriden from FirebaseMessagingService and will be called if new push is received
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        //Pass encryption key if the the push payload is encrypted
        Ezto.getInstance().onPushReceived(
            context = this,
            listener = instanceOfMyListener,
            message = message.data, 
            encryptionKey = {{encryption key}},
        )
    }
}
```

Use `onBackgroundPushReceived` to process when the app is inactive or in the background. To get the encryption key, check [here](/verify/docs/EV/AdvancedSettings/Keys/).

```java
class MainActivity: EztoService {
    override fun onCreate(savedInstanceState: Bundle?) {
        Ezto.getInstance().onBackgroundPushReceived(
            context = this,
            listener = instanceOfMyListener,
            intent = intent, 
            encryptionKey = {{encryption key}},
        )
    }

    //...
    //Overriden methods of EztoService
}
```

<br/>


### QR Support

Launch the QR scanner within your app using the `launchQrScanner` method. This functionality enables secure QR code scanning, allowing your app to read and process QR codes using the provided encryption key. To get the encryption key, check [here.](/verify/docs/EV/AdvancedSettings/Keys/)

```java
Ezto.getInstance().launchQrScanner(
    context = this,
    listener = instanceOfMyListener,
    encryptionKey = {{encryption key}},
```

<br/>

### DeepLink Support

This allows your app to handle and process deep links, which are URLs that direct users to specific content or features within your app. 

Use the `onDeeplinkRequest` method on the `EztoVerify` instance to manage incoming deep link requests and handle them appropriately within your application.

```java
 Ezto.getInstance().onDeeplinkRequest(intent)
``` 