---
id: Mobile
title: Mobile
sidebar_label: Mobile
--- 

import TabItem from "@theme/TabItem";
import Tabs from '@theme/Tabs';


Verification via the **Mobile** involves the process use of mobile devices, such as smartphones, as a means to confirm a user's identity and grant them access to the application using the [ezto verify's](https://ezto.io/) mobile app.

> Every application execution requires an **Onboarding** and **Verification** workflow. The onboarding workflow (can be any) will be used for a new user. You can configure it as **"None"** for a user who has already been onboarded. Please note that selecting the "None" option for the onboarding workflow will automatically redirect the user to the verification workflow, irrespective of the users availability.

### Access Application

*1* Navigate to **Workspace,** and click on **Applications.**

*2* Choose the application you wish to execute the flow with configuration as **Mobile.**

*3* Click on the **Configuration** tab, and select  **Mobile** in the **Verification Way.**

![Mobile](https://images.tango.us/workflows/66d8bcd2-8c8e-43b7-b7e3-0f07aeea4b21/steps/f48fe673-7401-45a2-9d8d-53c56af6d899/e8eee6d7-6987-4e20-be3c-19b202d13091.png)

*4* You will be displayed with default settings, i.e, **Link Expiry** and **Notification Type** ,  you can select your preferred **Notification Type** from the drop-down and based on the chosen, the configurations will vary.

> There is currently two methods of **Notification Type** available for **"Mobile" Verification Way.**

<Tabs>

<TabItem value="Push" label="Push" default>

If this method is selected as the **Notification Type,** the verification will be sent as a push notification to the registered mobile device.

> **Push notifications are supported by both Firebase and HMS.**

![Push](https://images.tango.us/workflows/66d8bcd2-8c8e-43b7-b7e3-0f07aeea4b21/steps/db55e472-d1e9-42a1-8763-6cfba1ec5b75/4a9b6c40-1ecd-4835-a61d-5052cbc3814e.png)

### Push settings

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="268">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Mobile App Integration Type<span class="required-text">*</span></td>
         <td>Choose how you want to integrate verification. Select "SDK" if you intend to integrate with your own app, or choose "ezto verify app" if you prefer to use our official app from app stores.</td>
      </tr>
      <tr>
         <td>Push Notification Title<span class="required-text">*</span></td>
         <td>Specify the title for the push notification that will be displayed on the app side.</td>
      </tr>
      <tr>
         <td>Push Notification Body<span class="required-text">*</span></td>
         <td>Provide the content for the push notification message.</td>
      </tr>
      <tr>
         <td>Enable Proximity (BETA)</td>
         <td>Turn this on to verify the proximity of devices. This feature is currently supported only for the <strong>Web to Device</strong> flow.</td>
      </tr>
      <tr>
         <td>Encryption</td>
         <td>Enable this option to encrypt the payload sent to the mobile <strong>SDK</strong> for secure data transmission.</td>
      </tr>
   </tbody>
</table>

</TabItem>

<TabItem value="QR" label="QR">

If this method is selected as the **Notification Type,** the verification will be sent as a QR code to the registered mobile device.

![QR](https://images.tango.us/workflows/66d8bcd2-8c8e-43b7-b7e3-0f07aeea4b21/steps/33c04431-5e3b-485c-a68a-d41a5fa9533a/4035b24f-8f88-434f-8aa4-25a09591a418.png)

### QR settings

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="269">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Mobile App Integration Type<span class="required-text">*</span></td>
         <td>Choose how you want to integrate verification. Select "SDK" if you intend to integrate with your own app, or choose "ezto verify app" if you prefer to use our official app from app stores.</td>
      </tr>
      <tr>
         <td>Enable Proximity (BETA)</td>
         <td>Turn this on to verify the proximity of devices. This feature is currently supported only for the <strong>Web to Device</strong> flow.</td>
      </tr>
      <tr>
         <td>Encryption</td>
         <td>Enable this option to encrypt the payload sent to the mobile <strong>SDK</strong> for secure data transmission.</td>
      </tr>
   </tbody>
</table>

</TabItem>
</Tabs>

*5* After configuring the required **Verification Way,** ensure to `SAVE` the configurations.
*6* Then, configure the **Result Notification Way.** This is necessary to proceed further in order to obtain the desired result or response. To know in detail, see [here.](/verify/docs/EV/StartAnIntegration/SetupResultNotification/Hook/)

### How it Works

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the [**cURL commands.**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands)

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* After completing the verification process, view the exact **response type** for the configured **Result Notification Type,** refer [here.](/verify/docs/EV/StartAnIntegration/SetupResultNotification/Hook/)

> Note that, the results will be different based on the configured **Result Notification Type.**

*5* To view a sample response for **Mobile Verification Way,** refer to our [API Builder.](/verify/docs/DeveloperTools/API/BuildyourIntegration/TriggerRequest/v1/)