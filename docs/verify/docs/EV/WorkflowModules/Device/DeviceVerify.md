---
id: DeviceVerify
title: Device Verify
sidebar_label: Device Verify
--- 

**Device Verify** helps confirm the identity of a specific device for a particular user, ensuring that the device is authorized to access the user's account.

> This flow supports both **Mobile** and **Desktop** applications.

### Prerequisites

* Download the **ezto verify mobile** app before proceeding with the flow.
* Before proceeding with this flow, ensure you have completed the **Device Register** flow as it is mandatory.

### How to Setup

*1* Navigate to **Workspace > Workflows,** and click on the flow created for **Device Verify.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Device Verify** to add it.

![Device Verify](https://images.tango.us/workflows/68684518-c4de-4a9f-9a88-72205af36d8b/steps/82b6e135-7b30-444d-8f28-f0952dc0bfdb/6defd088-0413-4e3b-ae22-3c63e337fe21.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/68684518-c4de-4a9f-9a88-72205af36d8b/steps/df503d92-d5cd-4db6-a494-d2cad6586185/69fdd6be-e40e-4eae-8d1b-46e0f9b3089d.png)

### Manage General

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="147">Name</th>
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
         <td>This determines the criteria for triggering the particular workflow module. When the selected rule is evaluated, and is satisfied, the workflow proceeds as provided. If not, the workflow will not be executed. <strong>Note: If no rules are set, the workflow defaults to its standard action without any rule. For more information about the rules, refer <a href="/verify/docs/EV/ControlIntegration/WorkflowsLevel/">here</a>.</strong></td>
      </tr>
   </tbody>
</table>

*4* After you've configured everything needed, click on **SAVE.**

*5* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Device Verify** includes the following parameters:

| Name | Description |
|------|-------------|
| `device_verified` | This denotes whether the device verification is completed. The value will be "True" if the verification is successfully completed and "False" if it is not.|
| `device_verified_at` | This provides a timestamp, in Unix epoch format, indicating the exact time when the device verification was completed. This allows you to know precisely when the verification event occurred. | 

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

> Make sure to configure the `"allow_new_device": true` in the body of the respective **API.**

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way (Mobile)** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Begin by scanning the **QR code** with the **ezto verify** mobile app**.**

> This depends on what you have configured for the mobile notification way, i.e, **QR** code or **Push** integration. To know the difference between **QR** code or **Push,** refer [here.](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile/)

![QR](https://images.tango.us/workflows/68684518-c4de-4a9f-9a88-72205af36d8b/steps/4fdaa2a4-cd18-4f0e-b791-57e5ef5b5550/7e1e761b-c837-44cf-824b-5ba3834b36d8.png)

*5* Once you scan the QR code, it will take a few seconds to verify the new device.

![ezto verify](https://images.tango.us/workflows/68684518-c4de-4a9f-9a88-72205af36d8b/steps/8a534b1b-41c0-48d5-bc90-462c1ad14c2a/35f2ed29-b142-4c99-80ac-82c5d8462e66.png)

*6* The process will be completed and the device will be verified successfully.

![Completed](https://images.tango.us/workflows/68684518-c4de-4a9f-9a88-72205af36d8b/steps/f58e25cf-99c7-47e7-8b39-89a1934b3e3e/09ba7a5f-8f5b-45c3-808f-d351af7f5b1e.png)

*7* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*8* Here is a sample response for **Device Verify:**

```json
{
  "workflow": "verification",
  "user_id": "mark@gmail.com",
  "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   // highlight-start
  "device_verified": "true/false",
  "device_verified_at": "1719569072"
   // highlight-end
}

```