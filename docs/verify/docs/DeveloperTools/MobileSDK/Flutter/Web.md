---
id: Web
title: Web
sidebar_label: Web
---

Integrating **ezto verify's SDK** into your **Flutter** project for **Web** is straightforward. Follow our documentation and sample code snippets to quickly implement verification features.

## _1_ Installation

To add the **ezto verify SDK** to your **Flutter** project, it is necessary to add the below code to your `pubspec.yaml` file in the dependency section. Make sure to use the latest version, which can be checked [here.](https://pub.dev/packages/ezto_verify_flutter_web_sdk)

```javascript
dependencies:
    ezto_verify_flutter_web_sdk: ^1.0.0+25021401
```

Ensure you have the correct path set based on your project structure.

<br /> 


## _2_ Implementation

To initiate the verification process, use the following code snippet:

```javascript
import 'package:ezto_verify_flutter_web_sdk/ezto_verify_web_sdk.dart';

EztoVerifyWebSdk().request(
  // Required - Provide the API endpoint, use the API Builder to generate the endpoint
  api: "${api}",
  
  // Optional - Defaults to version 1 if not provided
  apiVersion: "${apiVersion}",
  
  // Required - Provide the API request body, use the API Builder to generate the request body
  request: {
    "user": {
      "metadata": {
        "workflow": "" // onboarding or verification
      },
      "email": "example@gmail.com"
    }
  },
  
  onComplete: (Result result) {
    if (result.success) {
      // Handle successful verification result
    }
  },
);
```

> **To find details for the `api`, `apiVersion`, and `request` attributes in the code snippet, check the API Builder. For v0, refer [here](/docs/DeveloperTools/API/BuildyourIntegration/TriggerRequest/V0), and for v1, refer [here](/docs/DeveloperTools/API/BuildyourIntegration/TriggerRequest/V1).**

Replace `api` with your verification API endpoint. The `onComplete` callback handles the response once verification is completed.
