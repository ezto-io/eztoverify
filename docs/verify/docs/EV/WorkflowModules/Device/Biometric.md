---
id: Biometric
title: Biometric
sidebar_label: Biometric
--- 

**Biometric** verification involves confirming an individual's unique physiological or behavioral characteristics to ensure their identity is accurately verified.

> This flow supports only **Mobile** applications.

### Prerequisites

* Download the **ezto verify mobile** app before proceeding with the flow.

### How to Setup

*1* Navigate to **Workspace > Workflows,** and click on the flow created for **Biometric.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Biometric** to add it.

![Biometric](https://images.tango.us/workflows/804c69d8-b101-4510-9599-141e5640fbf5/steps/f733bba5-5133-497a-b9cb-31de8a43374c/9975110c-7857-45a8-a542-0fb96998d003.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/804c69d8-b101-4510-9599-141e5640fbf5/steps/82849e86-3b5c-4531-8bf0-b4b82643bbed/c5b780a8-008c-4e4d-9dac-8e2b507e3f02.png)

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

![Configs](https://images.tango.us/workflows/804c69d8-b101-4510-9599-141e5640fbf5/steps/0029cf88-5084-44f1-b9f9-7cea604a6741/c0a519e4-89b4-492d-928e-3fc458651373.png)

### Manage Configs

(<span class="required-text">*</span>) Indicates an action to be mandatory.

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
      <tr>
         <td>Max Device Count</td>
         <td>You can specify the maximum number of devices that a user is allowed to register, with the acceptable range for this value being between 1 and 10.</td>
      </tr>
      <tr>
         <td>Clear Data</td>
         <td>Removes existing data upon initiating the process, ensuring a fresh start for the new flow.</td>
      </tr>
      <tr>
         <td>Keys<span class="required-text">*</span></td>
         <td>Choose the key to enable hashing of biometric data. Only HS256 with 256-bit hashing is supported for now. <strong>Note that,  changing this key will invalidate all registered biometrics, requiring users to re-register. For more information about keys, refer <a href="/verify/docs/EV/AdvancedSettings/Keys/">here</a>.</strong> </td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Biometric** includes the following parameters:

| Name | Description |
|------|-------------|
| `biometric_device` | Specifies the type of biometric device used for verification.|
| `biometric_verified` | This denotes whether the biometric data associated with the user has been successfully verified. The value will be "True" if the verification is successfully completed and "False" if it is not. |
| `biometric_verified_at` |This provides a timestamp, in Unix epoch format, indicating the exact time when the biometric data was verified. This allows you to know precisely when the event occurred.  |

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

> Make sure to configure the `"allow_new_device": true` in the body of the respective **API.**

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way (Mobile)** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Begin by scanning the **QR code** with the **ezto verify** mobile app and verify the **Biometric.**

> This depends on what you have configured for the mobile notification way, i.e, **QR** code or **Push** integration. To know the difference between **QR** code or **Push,** refer [here.](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile/)

![QR](https://images.tango.us/workflows/804c69d8-b101-4510-9599-141e5640fbf5/steps/f2093c2c-341b-4ecf-a10e-2d98368e69f2/03d98c64-c25e-4a8b-84aa-d810d423911f.png)

*5* The verification process will be completed if the provided data is valid.

![Completed](https://images.tango.us/workflows/804c69d8-b101-4510-9599-141e5640fbf5/steps/484e25da-2433-41f5-9ba5-e74b4dc36038/86ad2ade-f3f7-499d-ac08-bf9cd2b7d90d.png)

*6* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*7* Here is a sample response for **Biometric:**

```json
{
  "workflow": "verification",
  "user_id": "mark@gmail.com",
  "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
    // highlight-start
  "biometric_device": "{\"model\":\"motorola motorola edge 40\",\"isPhysicalDevice\":true,\"osVersion\":\"14\",\"platform\":\"Android\",\"id\":\"13654178-6e53-449d-959c-02833d4256de\"}",
  "biometric_verified": "true/false",
  "biometric_verified_at": "1719569072"
   // highlight-end
}
```