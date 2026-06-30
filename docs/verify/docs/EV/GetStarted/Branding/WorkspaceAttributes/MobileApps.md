---
id: MobileApps
title: Mobile Apps
sidebar_label: Mobile Apps
--- 

The **Mobile Apps** section allows you to manage registered applications, enforce version updates. This ensures that users always access the latest, most secure version of your mobile application. 

## Secret Key for Verification  

- At the top of the page, you'll see a **Secret Key**, which is used to **encrypt and secure the verification payload** during communication between the mobile application and ezto verify.

![Secret Key](https://images.tango.us/workflows/42930509-c407-405e-af4e-a34b112ec662/steps/b4afe4c2-80ce-4561-bd64-3be970797481/39d89224-b943-41a0-849f-f34aab53da86.png?mark-x=83&mark-y=51&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz0xNTQ5Jmg9MjIzJmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)

- You can generate a new **Secret Key** by clicking the **generate icon** next to it. 
> To learn about how the encryption functions, click [here.](/docs/EV/EncryptionAndDecryption/EncryptRequest)

![Generate Secret Key](https://images.tango.us/workflows/42930509-c407-405e-af4e-a34b112ec662/steps/da3a7305-b1b6-43cc-8d59-d4934a58d1f7/307f342d-2b93-4f24-bfac-d80edd674feb.png)

- The newly generated **Secret Key** will appear.
:::note
**Once the secret key is generated, you can rotate it by clicking the reload icon. Make sure to update the new key across all platforms where it is used to prevent any interruptions.**
:::
![Generated](https://images.tango.us/workflows/42930509-c407-405e-af4e-a34b112ec662/steps/461bae90-13d1-429e-9041-638ce533ff5b/15a7fb8c-7025-49b0-a81c-8c0bdd09c955.png)

## Managing Registered Apps  

The **Registered Apps** section lists all applications that have been added to the **ezto verify.** This allows you to control updates, enforce version requirements, and set up redirection in case the app is not available for download.  

### Adding a New App  
To register a new mobile application,

*1* Click on the **+ADD APP** button.

![ADD APP](https://images.tango.us/workflows/42930509-c407-405e-af4e-a34b112ec662/steps/07bc7196-4d29-4b94-af3f-ec291da37f0d/e5a3feef-544b-4d3e-93d0-c5a99d1d1347.png)

*2* The mobile app configurations will be displayed.

![Configs](https://images.tango.us/workflows/42930509-c407-405e-af4e-a34b112ec662/steps/7eba807f-32b8-41f3-aff6-9cf401beb9fb/5fc800b0-35fa-4d54-9ad2-16e04d69d32f.png)

*3* Enter the **App Name**, this is the display name of your mobile application.  

*4* Provide the **App Version** of your app. This is used to enforce updates if an older version is detected. 

*5* Provide the respective platform's **App URL,** which will redirect users to the application on the App Store (iOS) or Google Play Store (Android). 

*6* Lastly, choose between **iOS** and **Android** to specify which platform the app is built for.  

*7* Click **SAVE** to complete the registration.  

![SAVE](https://images.tango.us/workflows/42930509-c407-405e-af4e-a34b112ec662/steps/98d43a2d-7e40-4326-9c0a-099882451f05/746c7f9d-5b33-40c0-a2dd-06d485799eab.png)

*8* Once saved, the registered application will appear in the **Registered Apps** list.

![List](https://images.tango.us/workflows/42930509-c407-405e-af4e-a34b112ec662/steps/7487579f-da21-4e87-a555-d5164430b7a8/4379fb8b-36a6-41cd-b14b-fbd3870d5fd8.png)