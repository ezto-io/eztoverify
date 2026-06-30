---
id: DesktopApps
title: Desktop Apps
sidebar_label: Desktop Apps
--- 

The **Desktop Apps** section allows you to manage registered applications and enforce version updates, ensuring users always access the latest, most secure version of your desktop application.

## Secret Key for Verification  

- At the top of the page, you'll see a **Secret Key**, which is used to **encrypt and secure the verification payload** during communication between the desktop application and ezto verify.

![Secret Key](https://images.tango.us/workflows/cd83de4f-0972-4640-887f-4b81ced53d48/steps/f8030aae-b184-4431-9b4d-5e8eb4ddc4f8/876a6138-c95b-47b6-a175-1c5338ff773e.png?mark-x=83&mark-y=51&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz0xNTQ5Jmg9MjIzJmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)

- You can generate a new **Secret Key** by clicking the **generate icon** next to it. 
> To learn about how the encryption functions, click [here.](/docs/EV/EncryptionAndDecryption/EncryptRequest)

![Generate Secret Key](https://images.tango.us/workflows/cd83de4f-0972-4640-887f-4b81ced53d48/steps/d36e2631-1160-4e5f-bcc3-5ed75b9ef5b9/60b71b0d-84e2-4abf-bfca-4b2d5e08a461.png)

- The newly generated **Secret Key** will appear.
:::note
**Once the secret key is generated, you can rotate it by clicking the reload icon. Make sure to update the new key across all platforms where it is used to prevent any interruptions.**
:::
![Generated](https://images.tango.us/workflows/cd83de4f-0972-4640-887f-4b81ced53d48/steps/8d5bd0f3-a10c-4281-a5d5-98a7503bcf4e/4a0b02e4-d1b8-45c4-8c6f-33e7347a65d4.png)

## Managing Registered Desktop Apps  

The **Registered Apps** section lists all applications that have been added to the **ezto verify.** This allows you to control updates, enforce version requirements, and set up redirection in case the app is not available for download.  

### Adding a New Desktop App  
To register a new desktop application,

*1* Click on the **+ADD APP** button.

![ADD APP](https://images.tango.us/workflows/cd83de4f-0972-4640-887f-4b81ced53d48/steps/84e751fb-838f-4f13-b4bb-c75d4ccc94f6/07ed4a79-9221-475f-a9d4-156d6ca41841.png)

*2* The desktop app configurations will be displayed.

![Configs](https://images.tango.us/workflows/cd83de4f-0972-4640-887f-4b81ced53d48/steps/f6b945df-c09b-4668-8d8b-d9c61ddc8b5a/c9c3238a-524c-4bae-8d3c-96e155311637.png)

*3* Enter the **App Name**, this is the display name of your desktop application.  

*4* Provide the **App Version** of your app. This is used to enforce updates if an older version is detected. 

*5* Provide the respective platform's **App URL,** which will redirect users to the application on the App Store (macOS) or the download link for Windows.

*6* Lastly, choose between **macos** and **windows** to specify which platform the app is built for.  

*7* Click **SAVE** to complete the registration.  

![SAVE](https://images.tango.us/workflows/cd83de4f-0972-4640-887f-4b81ced53d48/steps/44598707-9bc4-4170-8599-5c5cdcba7afb/8b7260f4-223f-4d2e-adcc-d76915631528.png?mark-x=408&mark-y=411&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz04OTAmaD0zNTImZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)

*8* Once saved, the registered application will appear in the **Registered Apps** list.

![List](https://images.tango.us/workflows/cd83de4f-0972-4640-887f-4b81ced53d48/steps/c4706b62-2638-4270-867d-ac47b7184f6e/7a6e1d40-999f-4445-8ea5-3116e5fbdb49.png)