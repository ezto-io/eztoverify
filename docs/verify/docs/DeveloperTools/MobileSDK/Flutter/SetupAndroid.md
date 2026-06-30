---
id: SetupAndroid
title: Android
sidebar_label: Android
--- 

Integrating **ezto verify's SDK** into your **Flutter** project for **Android** is straightforward. Follow our comprehensive documentation and sample code snippets to quickly implement user verification features.

<br />

## *1* Requirements

The minimum requirements needed in order to use the **ezto verify SDK** in your **Flutter** project. Ensure your development environment meets these prerequisites before proceeding with the integration.
- Dart SDK 3.4.0 and above
- Flutter SDK 3.22.0 and above
- Android 5 (API level 21)

<br />

## *2* Installation 

To add the **ezto verify SDK** to your **Flutter** project, it is necessary to add the below code to your `pubspec.yaml` file in the dependency section. Make sure to use the latest version, which can be checked [here.](https://pub.dev/packages/eztoverify)

```javascript 
dependencies:
    eztoverify: ^9.0.0+25091801
```

<br />

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
```
<br/>

## *4* Setup

This section explains how to set up the **ezto verify SDK** for different verification methods available. Currently, we have Android setup available for [Biometric,](/docs/EV/WorkflowModules/Device/Biometric) [FIDO,](/docs/EV/WorkflowModules/Fido-passkeys/Register) UAE PASS Verification, and **DeepLink.**

For the **ezto verify SDK,** the following requirement must be added to the `android/build.gradle` file.

```javascript
allprojects {
    repositories {
        google()
        mavenCentral()
    }

    // Add this
    subprojects {
        afterEvaluate { project ->
            if (project.hasProperty('android')) {
                project.android {
                    if (namespace == null) {
                        namespace = project.group
                    }
                }
            }
        }
    }
}
```

### Biometric 
To enable [Biometric](/docs/EV/WorkflowModules/Device/Biometric) in your **Flutter** project. It covers the necessary changes to your activity class and configuration in the `build.gradle` file.

* If you are using `FlutterActivity` directly, change it to `FlutterFragmentActivity` in your `AndroidManifest.xml.`

* If you are using a custom activity, update your `MainActivity.kt`:

```javascript
import io.flutter.embedding.android.FlutterFragmentActivity

class MainActivity: FlutterFragmentActivity() {
    // ...
}
```

or `MainActivity.java:`

```java
import io.flutter.embedding.android.FlutterFragmentActivity;

public class MainActivity extends FlutterFragmentActivity {
    // ...
}
```

<br/>

### FIDO / Passkeys

To setup [FIDO or Passkeys,](/docs/EV/WorkflowModules/Fido-passkeys/Register) it is necessary to include the `proguard-rules.pro` to your project to ensure proper functionality.

```javascript
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
```

<br/>

### UAE PASS Verification

> This setup is optional and is only required when using **UAE Pass.**

To enable UAE PASS Verification, it is required to add the `queries` section  to your `AndroidManifest.xml` file located in `<PROJECT_DIR>\android\app\src\main\AndroidManifest.xml`:

```javascript
<queries>
  <package android:name="ae.uaepass.mainapp"/>
  <package android:name="ae.uaepass.mainapp.stg"/>
</queries>
```

### DeepLink

To setup the **DeepLink** functionality, it is required to intent filters and configurations in the `AndroidManifest.xml` file located in `<PROJECT_DIR>\android\app\src\main\AndroidManifest.xml` to handle deep links correctly.

```xml
<!-- By default, flutter Activity is set with android:launchMode="singleTop". This is perfectly fine and expected, but this launches another instance of your app, specifically for the requested view.
If you don't want this behaviour, you can set android:launchMode="singleInstance" in your AndroidManifest.xml to avoid another flutter warmup. -->
<activity
    android:launchMode="singleInstance">

    <!-- Make sure you explicitly set android:autoVerify to "true". -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <!-- If a user clicks on a shared link that uses the "http" scheme, your
            app should be able to delegate that traffic to "https". -->
        <data android:scheme="https" />

        <!-- Include one or more domains that should be verified. -->
        <!-- Replace example.ezto.io with your ezto verify workspace url -->
        <!-- Go to Dashboard > Settings > General, copy the URL. -->
        <data android:host="example.ezto.io" />
        <data android:path="/auth/deeplink.html" />
    </intent-filter>
</activity>        
```