---
id: Desktop
title: Desktop
sidebar_label: Desktop
--- 

It is a verification method used to confirm a user's identity by executing the verification flow through the desktop application. This process typically involves guiding users through steps on their desktop to verify their identity securely and accurately.

> Every application execution requires an **Onboarding** and **Verification** workflow. The onboarding workflow (can be any) will be used for a new user. You can configure it as **"None"** for a user who has already been onboarded. Please note that selecting the "None" option for the onboarding workflow will automatically redirect the user to the verification workflow, irrespective of the users availability.

### Prerequisites

Install the **ezto verify** desktop app and make sure the notification is enabled for the app in your desktop.

### Access Application

*1* Navigate to **Workspace,** and click on **Applications.**

*2* Choose the application you wish to execute the flow with configuration as **Desktop.**

*3* Click on the **Configuration** tab, and select **Desktop** in the **Verification Way.**

![Desktop](https://images.tango.us/workflows/8f40ec76-ea34-46c8-af0f-d2d443a0ec0e/steps/17baa97a-2b0b-4b3e-8f49-5558cbc19d7c/2723158a-9d88-49ea-9346-89bc1b89658c.png)

*4* You will be displayed with the desktop settings.

![Desktop](https://images.tango.us/workflows/8f40ec76-ea34-46c8-af0f-d2d443a0ec0e/steps/3fa1be3c-21fc-41e8-9a92-ba7e5f7d2698/176e904e-6687-441c-9b08-2fceae5801c3.png)

### Desktop settings

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="236">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Link Expiry</td>
         <td>Set the duration after which the link will expire: 2 days (2d), 2 hours (2h), 2 minutes (2m), or 2 seconds (2s). The default is currently set to 120 seconds.</td>
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
         <td>Encryption</td>
         <td>Enable this option to encrypt the payload sent to the mobile <strong>SDK</strong> for secure data transmission.</td>
      </tr>
   </tbody>
</table>

*5* After configuring the required **Verification Way,** ensure to `SAVE` the configurations.

*6* Then, configure the **Result Notification Way.** This is necessary to proceed further in order to obtain the desired result or response. To know in detail, see [here.](/verify/docs/EV/StartAnIntegration/SetupResultNotification/Hook/) (Make sure to configure it as `Desktop_app`)

### How it Works

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the  [**cURL commands**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands), or can simply test the flow using the test mode.
> * Ensure to configure the **Onboarding workflow** with Device Register. It is mandatory to register the device, before proceeding with the verification flow.

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a transaction link will be generated in the API body.

*3* Paste the link in an incognito browser, you will be prompted to open the **ezto verify desktop app** and grant access.

*4* Upon granting access, the desktop app will open, initiating the onboarding process (i.e., **Device Register**).

*5* Once the onboarding process is completed, return to the **API tool,** input your **user ID** in the request body, and trigger the API again.

*6* You will receive a **Push** notification to initiate the verification flow.


*7* After completing the verification process, view the exact **response type** for the configured **Result Notification Type,** refer [here.](/verify/docs/EV/StartAnIntegration/SetupResultNotification/Hook/)

> Note that, the results will be different based on the configured **Result Notification Type.**

*8* To view a sample response for **Desktop Verification Way,** refer to our [API Builder.](/verify/docs/DeveloperTools/API/BuildyourIntegration/TriggerRequest/v1/)