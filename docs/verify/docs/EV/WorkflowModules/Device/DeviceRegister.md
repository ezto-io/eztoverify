---
id: DeviceRegister
title: Device Register
sidebar_label: Device Register
--- 

**Device Register** helps enrolling a specific device for a particular user, ensuring that the device is recognized and linked to the user's account.

> This flow supports both **Mobile** and **Desktop** applications.


### Prerequisites

* Download the **ezto verify mobile** app before proceeding with the flow.

### How to Setup

*1* Navigate to **Workspace > Workflows,** and click on the flow created for **Device Register.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Device Register** to add it.

![Device Register](https://images.tango.us/workflows/74d2ffeb-7c3e-476f-be35-8fc3ef072917/steps/14b498b7-188d-4355-a61c-2e1962287f2e/a60c6505-a62b-4547-b4a4-95dd091afb5a.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/74d2ffeb-7c3e-476f-be35-8fc3ef072917/steps/a20e043c-69ed-457c-b8c2-eb833ba572b9/25ebecfd-ba3d-4e75-a413-8f7f471e89ab.png)

### Manage General

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="167">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Display Name<span class="required-text">*</span></td>
         <td>Set a name to the workflow for easy identification.</td>
      </tr>
      <tr>
         <td>Description</td>
         <td>Summarize the purpose and functionality of this workflow module.</td>
      </tr>
      <tr>
         <td>Enable</td>
         <td>This toggle is enabled by default. When enabled, the particular workflow module begins its execution. Conversely, if disabled, the workflow module remains inactive and won't proceed.</td>
      </tr>
      <tr>
         <td>Rule</td>
         <td>This determines the criteria for triggering the particular workflow module. When the selected rule is evaluated, and is satisfied, the workflow proceeds as provided. If not, the workflow will not be executed. <strong>Note: If no rules are set, the workflow defaults to its standard action without any rule. For more information about the rules, refer <a href="/verify/docs/EV/ControlIntegration/WorkflowsLevel/">here.</a></strong></td>
      </tr>
   </tbody>
</table>

*4* Then, click on the **Configs** tab to set the necessary configurations.

![Configs](https://images.tango.us/workflows/74d2ffeb-7c3e-476f-be35-8fc3ef072917/steps/461c128c-2a06-4af5-b6d5-94bf1506dda3/1d019357-8650-48dd-8087-305c78ce7c29.png)

### Manage Configs

<table>
   <thead>
      <tr>
         <th width="249">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Allow Multi Device Registration</td>
         <td>This configuration permits multiple devices to be registered for biometric verification under the same user's account.</td>
      </tr>
   </tbody>
   <tbody>
      <tr>
         <td>Max Device Count</td>
         <td>Set the maximum number of devices the user can register, with a value ranging from 1 to 10.</td>
      </tr>
      <tr>
         <td>Clear Data</td>
         <td>Removes existing data upon initiating the process, ensuring a fresh start for the new flow.</td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Device Register** includes the following parameters:

| Name | Description |
|------|-------------|
| `device_registered` | This denotes whether the device registration is completed. The value will be "True" if the registration is successfully completed and "False" if it is not.|
| `push_token` | Token used for push notifications or communication with the device. |
| `device_registered_at` | This provides a timestamp, in Unix epoch format, indicating the exact time when the device registration was completed. This allows you to know precisely when the registration event occurred. | 

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

> Make sure to configure the `"allow_new_device": true` in the body of the respective **API.**

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way (Mobile)** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Begin by scanning the **QR code** with the **ezto verify** mobile app**.**

> This depends on what you have configured for the mobile notification way, i.e, **QR** code or **Push** integration. To know the difference between **QR** code or **Push,** refer [here](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile/).

![QR](https://images.tango.us/workflows/74d2ffeb-7c3e-476f-be35-8fc3ef072917/steps/2d8e3ba5-f9d8-42de-8494-edb33a5e6ceb/342666e8-ce4b-4ebc-84c7-946c694139a7.png)

*5* Once you scan the QR code, it will take a few seconds to register the new device.

![ezto verify](https://images.tango.us/workflows/74d2ffeb-7c3e-476f-be35-8fc3ef072917/steps/c0ac9d4a-8e2a-4205-9c94-c00ed9e5cb34/68e05e57-ea38-4f85-aab4-8bd88ce7048e.png)

*6* The process will be completed and the device will be registered successfully.

![Completed](https://images.tango.us/workflows/74d2ffeb-7c3e-476f-be35-8fc3ef072917/steps/67eeedb7-070b-4ef7-936d-f740de221398/7087d55c-f631-491f-a626-3fa4380de784.jpeg)

*7* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*8* Here is a sample response for **Device Register:**

``` json
{
   "workflow": "verification",
   "user_id": "mark@gmail.com",
   "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   // highlight-start
   "device_registered_at": "1719569072",
   "device_registered": "true/false",
   "push_token": "cIeSFhRDQoxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
   // highlight-end
}
```