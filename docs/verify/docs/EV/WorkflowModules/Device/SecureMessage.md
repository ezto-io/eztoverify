---
id: SecureMessage
title: Secure Message
sidebar_label: Secure Message
--- 

This feature enables you to send secure messages to users' registered devices, ensuring that sensitive information is communicated safely and directly.

> This flow supports only **Mobile** applications.

### Prerequisites

* Download the **ezto verify mobile** app before proceeding with the flow.
* Make sure that the **Verification Way** is set to **Mobile or Desktop**, and the **Result Notification Way** is configured as `Mobile_sdk` or `Desktop_sdk`

### How to Setup

*1* Navigate to **Workspace > Workflows,** and click on the flow created for **Secure Message.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Secure Message** to add it.

![Secure Message](https://images.tango.us/workflows/195d6c25-7ae7-4805-81d8-b9bb6d94fab7/steps/db9f5978-f41f-4818-a22a-5997ed42cfab/281d707c-2fdb-4608-b7d8-4492a5029d93.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/195d6c25-7ae7-4805-81d8-b9bb6d94fab7/steps/07e27539-14ab-4ee1-a129-c0ca98193910/be477816-f0f0-4b35-801b-9c0007006d1a.png)

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

*4* Then, click on the **Configs** tab to set the necessary configurations.

![Configs](https://images.tango.us/workflows/195d6c25-7ae7-4805-81d8-b9bb6d94fab7/steps/8b438d24-576a-48f9-b8e2-848526932519/4c84b799-db14-4108-b033-3aa4ebc76a72.png)

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
         <td>Device Details</td>
         <td>When enabled, includes device details in the response upon message delivery to a device.</td>
      </tr>
      <tr>
         <td>Location</td>
         <td>When enabled, includes device location in the response upon message delivery to a device.</td>
      </tr>
      <tr>
         <td>IP Address</td>
         <td>When enabled, includes device IP address in the response upon message delivery to a device.</td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Secure Message** includes the following parameters:

| Name | Description |
|------|-------------|
| `sm_read_at` | This provides a timestamp, in Unix epoch format, indicating when the secure message was read.| 
| `sm_read_location` | Location or context where the secure message was read. |
| `sm_read_ip` | IP address from which the secure message was accessed. |
| `sm_read_device` | Device used to access and read the secure message.|
| `sm_read_confirmation` | Confirmation or acknowledgment of the secure message being read.|

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

> Make sure to configure the `"allow_new_device": true` in the body of the respective **API.**

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way (Mobile)** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Begin by scanning the **QR code** with the **ezto verify** mobile app**.**

> This depends on what you have configured for the mobile notification way, i.e, **QR** code or **Push** integration. To know the difference between **QR** code or **Push,** refer [here.](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile/)

![QR](https://images.tango.us/workflows/195d6c25-7ae7-4805-81d8-b9bb6d94fab7/steps/5221711f-0301-44a3-b635-82877b3aecc2/18bab515-b850-4fb8-ba20-d93f7ceddc99.png)

*5* If you are prompted with a location access permission request, please grant it.

![Allow](https://images.tango.us/workflows/195d6c25-7ae7-4805-81d8-b9bb6d94fab7/steps/b840c520-d560-4c0b-9b1a-be7dfe751c55/33077968-a964-4742-aaa8-862b7f16773c.jpeg)

*6* The secure message with configured details will be displayed, click on the **OK** button.

![Completed](https://images.tango.us/workflows/195d6c25-7ae7-4805-81d8-b9bb6d94fab7/steps/34ce2548-370b-42c9-aec9-ab0f9afee6e2/31e4e8a8-7412-49ef-a865-ab6ba0145f03.png?mark-x=540&mark-y=222&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz0zNTgmaD0xNjcmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)

*7* The verification process will be completed.

![Verification Completed](https://images.tango.us/workflows/195d6c25-7ae7-4805-81d8-b9bb6d94fab7/steps/668d764e-aab0-45c0-b90c-e1fa8fe0cf22/15fa6eac-9c2d-4e7a-ae7e-158dd8f48f6a.png)

*8* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*9* Here is a sample response for **Secure Message:**

```json
{
   "workflow": "verification",
   "user_id": "mark@gmail.com",
   "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   // highlight-start
   "sm_read_at": "1719569072",
   "sm_read_location": "{\"accuracy\":12.678000450134277,\"lat\":12.9686521,\"long\":80.2482644}",
   "sm_read_ip": "000.000.000.000",
   "sm_read_device": "{\"model\":\"motorola motorola edge 40\",\"isPhysicalDevice\":true,\"osVersion\":\"14\",\"platform\":\"Android\",\"id\":\"13654178-6e53-449d-959c-02833d4256de\"}",
   "sm_read_confirmation": "yes/no"
   // highlight-end
}

```