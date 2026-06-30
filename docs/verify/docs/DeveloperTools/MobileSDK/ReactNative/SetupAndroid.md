---
id: SetupAndroid
title: Android
sidebar_label: Android
--- 

Integrating **ezto verify's SDK** into your **React Native** project for **Android** is straightforward. Follow our comprehensive documentation and sample code snippets to quickly implement user verification features.

<br />

## *1* Requirements

The minimum requirements needed in order to use the **ezto verify SDK** in your **React Native** project. Ensure your development environment meets these prerequisites before proceeding with the integration.
- Android 14 (API level 34)

<br />

## *2* Installation 

* To add the **ezto verify SDK** to your **React Native** project, it is necessary to add `io.ezto.verify.react` using the npm command.

``` shell
npm i io.ezto.verify.react
```

* Run `yarn` to install the dependencies. After that, add the following lines to the `build.gradle` file:

```javascript
dependencies {
    implementation("io.ezto.verify:sdk:5.0.0")
    implementation("com.iproov.sdk:iproov:9.0.3")
}
```

* Add the following code to the `repositories` section in `build.gradle`:

```javascript
allprojects {
    repositories {
        // For Iproov
        maven {
            url "https://raw.githubusercontent.com/iProov/android/master/maven/"
        }
    }
}
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

This section explains how to set up the **ezto verify SDK** for different verification methods available. Currently, we have iOS setup available for [FIDO,](/docs/EV/WorkflowModules/Fido-passkeys/Register) UAE PASS Verification, and **DeepLink.**

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

To enable UAE PASS Verification, it is required to add the `queries` section to your `AndroidManifest.xml` file located in `<PROJECT_DIR>\android\app\src\main\AndroidManifest.xml`:

```javascript
<queries>
  <package android:name="ae.uaepass.mainapp"/>
  <package android:name="ae.uaepass.mainapp.stg"/>
</queries>
```

<br/>

### DeepLink

To setup the **DeepLink** functionality, it is required to intent filters and configurations in the `AndroidManifest.xml` file located in `<PROJECT_DIR>\android\app\src\main\AndroidManifest.xml` to handle deep links correctly.

```xml
<!-- By default, flutter Activity is set with android:launchMode="singleTop". This is perfectly fine and expected, but this launches another instance of your app, specifically for the requested view.
If you don't want this behaviour, you can set android:launchMode="singleInstance" in your AndroidManifest.xml and avoid another flutter warmup. -->
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