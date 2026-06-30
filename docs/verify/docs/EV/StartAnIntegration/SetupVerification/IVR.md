---
id: IVR
title: IVR
sidebar_label: IVR
--- 

import TabItem from "@theme/TabItem";
import Tabs from '@theme/Tabs';
import VideoPlayer from "@site/src/components/docs/VideoPlayer";


It is a verification method used to confirm a user's identity by interacting with them through a phone call. It typically involves automated systems that prompt users to respond or enter information using their phone's keypad.

> Every application execution requires an **Onboarding** and **Verification** workflow. The onboarding workflow (can be any) will be used for a new user. You can configure it as **"None"** for a user who has already been onboarded. Please note that selecting the "None" option for the onboarding workflow will automatically redirect the user to the verification workflow, irrespective of the users availability.

### Access Application

*1* Navigate to **Workspace,** and click on **Applications.**

*2* Choose the application you wish to execute the flow with configuration as **IVR Verification.**

*3* Click on the **Configuration** tab, and select  **Mobile** in the **Verification Way.**

![IVR](https://images.tango.us/workflows/9416db5d-9b91-4569-95ba-67847c0c5523/steps/4b25046f-9980-4783-a3b5-c423d5575f94/a4690221-f3cb-4ec5-9465-4464d073cb21.png)

*4* You will be displayed with default settings, for **Twilio Config** you can select Default or Custom from the drop-down and based on the chosen, the configurations will vary.

<Tabs>
<TabItem value="Default" label="Default" default>

The following are the default configurations for **IVR Verification.**

![Default](https://images.tango.us/workflows/9416db5d-9b91-4569-95ba-67847c0c5523/steps/fefc7014-1479-4852-9c13-99808348be6d/18dfbd77-ea5c-46ba-b561-1642809f7237.png)

### Default settings

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
         <td>IVR Provider</td>
         <td>Choose <strong>Twilio</strong> as the <strong>IVR</strong> provider for initiating <strong>IVR</strong> verification, as it is currently the only supported option.</td>
      </tr>
      <tr>
         <td>Twilio Config</td>
         <td>Choose the method to configure Twilio. If custom details are  not provided, the default configuration will be used.</td>
      </tr>
      <tr>
         <td>User Metadata Attribute<span class="required-text">*</span></td>
         <td>Specify the user metadata attribute that should be mapped from the API.</td>
      </tr>
   </tbody>
</table>
</TabItem>

<TabItem value="Custom" label="Custom" default>

When setting up a custom **Twilio** configuration, you integrate your own Twilio account credentials and configurations into the verification system.

![Custom](https://images.tango.us/workflows/9416db5d-9b91-4569-95ba-67847c0c5523/steps/1310dc66-df13-48b2-99e2-b43995c00237/3e74414b-7526-4547-b8c1-27f02c9efecf.png)

### Custom settings

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="246">Name</th>
         <th>Description </th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Twilio Config</td>
         <td>Choose "Custom" for <strong>Twilio</strong> configuration.</td>
      </tr>
      <tr>
         <td>Twilio Flow Rest API<span class="required-text">*</span></td>
         <td>Specify the <strong>Twilio</strong> <strong>IVR Flow Rest API</strong> to be used.</td>
      </tr>
      <tr>
         <td>From<span class="required-text">*</span></td>
         <td>Set the <strong>Twilio</strong> phone number from which calls will originate.</td>
      </tr>
      <tr>
         <td>Account SID<span class="required-text">*</span></td>
         <td>Provide the <strong>Twilio</strong> <strong>Account SID</strong> for authentication.</td>
      </tr>
      <tr>
         <td>Auth Token<span class="required-text">*</span></td>
         <td>Enter the <strong>Twilio Auth Token</strong> for authentication purposes.</td>
      </tr>
      <tr>
         <td>User Metadata Attribute<span class="required-text">*</span></td>
         <td>Specify the user metadata attribute that should be mapped from the API.</td>
      </tr>
   </tbody>
</table>
</TabItem>
</Tabs>

*5* After configuring the required **Verification Way,** ensure to `SAVE` the configurations.

*6* Then, configure the **Result Notification Way.** This is necessary to proceed further in order to obtain the desired result or response. To know in detail, see [here.](/verify/docs/EV/StartAnIntegration/SetupResultNotification/Hook/)

### How it Works

:::note
 * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
 * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the  [**cURL commands**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands), or can simply test the flow using the test mode.
 * Ensure to provide the PIN in the API request body.
:::

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Make sure to include the request key under the user metadata and customize the IVR-related messages accordingly.

![Metadata](https://images.tango.us/workflows/9416db5d-9b91-4569-95ba-67847c0c5523/steps/de345a53-9ec7-4dbd-95bf-1c4aafcd7d34/b0df1385-23aa-464c-8425-9d7a8e63daca.png?mark-x=1051&mark-y=163&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz0xNzgmaD0zNCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)
 
*3* Upon initiating the API call, you will receive an IVR verification phone call. Please enter the **PIN** followed by the sign **(#),** e.g., 1234#.

#### Here is a sample IVR call:

<VideoPlayer name="IVR"/>

*4* After completing the verification process, view the exact **response type** for the configured **Result Notification Type,** refer [here](/verify/docs/EV/StartAnIntegration/SetupResultNotification/Hook/).

> Note that, the results will be different based on the configured **Result Notification Type.**

*5* To view a sample response for **IVR Verification Way,** refer to our [API Builder.](/verify/docs/DeveloperTools/API/BuildyourIntegration/TriggerRequest/v1/)