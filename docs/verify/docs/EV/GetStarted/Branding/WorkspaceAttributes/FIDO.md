---
id: FIDO
title: FIDO
sidebar_label: FIDO
--- 

FIDO is a validation mechanism that ensures an app belongs to the specified domain. It helps maintain security and integrity by verifying whether an application is correctly associated with the domain it claims to represent.

## Configuring FIDO

*1* Navigate to **Workspace** and click on **Settings.**

*2* Click on the **Workspace Attributes** tab.

*3* Locate **FIDO** and click on it.

*4* You will be redirected to the **FIDO Configuration** page.

![FIDO](https://images.tango.us/workflows/c4fc81ff-a44e-4f42-98a6-423e37a28ccd/steps/6a6bdada-f403-4e0b-9757-7d8570fdd976/28682ee4-938c-465d-a6a1-7aff0b988a30.png)

*5* The FIDO configuration options will be displayed.

*6* Provide the **FIDO Asset Link for iOS**, and **FIDO Asset Link for Android**.

![Configurations](https://images.tango.us/workflows/c4fc81ff-a44e-4f42-98a6-423e37a28ccd/steps/8d801c11-dcb3-4a99-aa48-1b30891476a1/c92d2bbd-7d21-44bb-93fc-08e7ae6c5523.png)

## Manage FIDO Configs

| Name | Description |
|------|-------------|
| FIDO Asset Link for iOS | The asset link used to validate the iOS application’s association with the given domain. |
| FIDO Asset Link for Android |  The asset link used to validate the Android application’s association with the given domain. |

> **To generate the asset links for your platform:**  
> - **iOS:** Follow Apple's [Associated Domains](https://developer.apple.com/documentation/safariservices/supporting_associated_domains) guide.  
> - **Android:** Use Google's [Digital Asset Links](https://developers.google.com/digital-asset-links/v1/getting-started) tool. 

*7* Once all configurations are completed, click on **SAVE**.

#### For further assistance or related topics, you may explore the following resource:

- [FIDO Alliance](https://fidoalliance.org/)
- [Passkeys Overview](https://fidoalliance.org/passkeys/)
- [Apple’s Passkey Implementation](https://developer.apple.com/documentation/authenticationservices/implementing_user_authentication_with_sign_in_with_apple).  
- [Google Digital Asset Links](https://developers.google.com/digital-asset-links/v1/getting-started).  
- [Passkeys for Android](https://developer.android.com/training/sign-in/passkeys).
- [WebAuthn](https://webauthn.io/)