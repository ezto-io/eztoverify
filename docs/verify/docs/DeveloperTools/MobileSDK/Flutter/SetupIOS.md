---
id: SetupIOS
title: iOS
sidebar_label: iOS
---  

Integrating **ezto verify's SDK** into your **Flutter** project for **iOS** is straightforward. Follow our comprehensive documentation and sample code snippets to quickly implement user verification features.

<br />

## *1* Requirements
The minimum requirements needed in order to use the **ezto verify SDK** in your **Flutter** project. Ensure your development environment meets these prerequisites before proceeding with the integration.
- Dart SDK 3.4.0 and above
- Flutter SDK 3.22.0 and above
- iOS 13 and above

<br />

## *2* Installation 

To add the **ezto verify SDK** to your **Flutter** project, it is necessary to add the below code to your `pubspec.yaml` file in the dependency section. Make sure to use the latest version, which can be checked [here.](https://pub.dev/packages/eztoverify)

```javascript
dependencies:
    eztoverify: ^9.0.0+25091801
```

<br />

## *3* Permissions

This section lists the permissions that need to be declared in your `info.plist` file located in `<PROJECT_DIR>\ios\Runner\info.plist`. These permissions are required for the **ezto verify SDK** to function correctly, allowing it to access the necessary device features.

 ```xml
<key>NSFaceIDUsageDescription</key>
<string>FaceID is required for user authentication</string>
<key>NSMicrophoneUsageDescription</key>
<string>Mic access is required for user authentication</string>
<key>NSCameraUsageDescription</key>
<string>Camera access is required for user authentication</string>
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>Location access is required for user authentication</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>Location access is needed for user authentication.</string>

```

<!--- When using ezto Pass, ensure you add the below required usage to the `Release.entitlements` file located at `<PROJECT_DIR>\ios\Runner\Release.entitlements`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
    <dict>
        <key>com.apple.developer.nfc.readersession.formats</key>
        <array>
            <string>TAG</string>
        </array>
    </dict>
</plist> 
``` -->

- Add required usage to `Podfile` for the permission handler located at `<PROJECT_DIR>/ios/Podfile`:

```javascript

post_install do |installer|
    installer.pods_project.targets.each do |target|

        # Start of the permission_handler configuration
        target.build_configurations.each do |config|
            config.build_settings['GCC_PREPROCESSOR_DEFINITIONS'] ||= [
                '$(inherited)',
                'PERMISSION_LOCATION=1',
                'PERMISSION_CAMERA=1',
                'PERMISSION_MICROPHONE=1',
                'PERMISSION_NOTIFICATIONS=1'
            ]
        end
    end
end
```

<br />

## *4* Setup

This section explains how to set up the **ezto verify SDK** for different verification methods available. Currently, we have iOS setup available for [FIDO,](/docs/EV/WorkflowModules/Fido-passkeys/Register) UAE PASS Verification, and **DeepLink.**

### FIDO / Passkeys

To setup [FIDO or Passkeys,](/docs/EV/WorkflowModules/Fido-passkeys/Register) it is necessary to include the `webcredentials` along with your workspace URL to `ios/Runner.entitlements`: 

```javascript
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>aps-environment</key>
	<string>development</string>
	<key>com.apple.developer.associated-domains</key>
	<array>
		<string>webcredentials:example.ezto.io</string>
        <!-- Replace example.ezto.io with your ezto verify workspace url -->
        <!-- Go to Dashboard > Settings > General, copy the URL. -->
	</array>
</dict>
</plist>

```

<br/>

### UAE PASS Verification

> This setup is optional and is only required when using **UAE Pass.**

To enable UAE PASS Verification, it is required to add usage descriptions `info.plist` located in `<PROJECT_DIR>\ios\Runner\info.plist`:

```javascript
<key>LSApplicationQueriesSchemes</key>
<array>
    <string>uaepass</string>
    <string>uaepassstg</string>
</array>
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        // Add your bundle_id         
        <array>
            <string>{{bundle_id}}</string>
        </array>
        <key>CFBundleURLName</key>
        <string>sdk_scheme</string>
    </dict>
</array>
```

<br/>

### DeepLink

To setup the **DeepLink** functionality, it is required to add the below code to `AppDelegate.swift` located at `<PROJECT_DIR>\ios\Runner\AppDelegate.swift` :

```javascript
import app_links

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {

    // Retrieve the link from parameters
    if let url = AppLinks.shared.getLink(launchOptions: launchOptions) {
      // We have a link, propagate it to your Flutter app or not
      AppLinks.shared.handleLink(url: url)
      return true // Returning true will stop the propagation to other packages
    }

    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }
}
```

* Add the `applinks` with your workspace URL to `ios/Runner.entitlements`:

```javascript
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>aps-environment</key>
    <string>development</string>
    <key>com.apple.developer.associated-domains</key>
    <array>
        <string>applinks:example.ezto.io</string>
        <!-- Replace example.ezto.io with your ezto verify workspace url -->
        <!-- Go to Dashboard > Settings > General, copy the URL. -->
    </array>
</dict>
</plist>
```