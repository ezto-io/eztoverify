---
id: SelfieRegistration
title: Selfie Registration
sidebar_label: Selfie Registration
--- 

The **Selfie Registration** allows users to securely store their facial template for future use during liveness checks, ensuring a smooth and secure verification process.

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Selfie Registration.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Selfie Registration** to add it.

![Selfie Registration](https://images.tango.us/workflows/a84d0a1b-6ee3-44b4-a932-abb4d9a4602a/steps/e6ee832c-d531-45ad-9317-314dcae69bb1/04e08141-cfe5-48a5-9785-c87fc514ebb9.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/a84d0a1b-6ee3-44b4-a932-abb4d9a4602a/steps/bc9d51df-ed25-4bc5-8625-e55eadbe348b/78cdc9de-5ff4-4e59-a808-9e9d6efdc8fc.png)

### Manage General

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="153">Name</th>
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
         <td>Skip</td>
         <td>Allows the user to skip this specific workflow module during execution. When selected, it provides an option to bypass the particular workflow without affecting other modules in the sequence.</td>
      </tr>
      <tr>
         <td>Rule</td>
         <td>This determines the criteria for triggering the particular workflow module. When the selected rule is evaluated, and is satisfied, the workflow proceeds as provided. If not, the workflow will not be executed. <strong>Note: If no rules are set, the workflow defaults to its standard action without any rule. For more information about the rules, refer <a href="/verify/docs/EV/ControlIntegration/WorkflowsLevel/">here.</a></strong></td>
      </tr>
   </tbody>
</table>

*4* Then, click on the **Configs** tab to set the necessary configurations.

![Configs-1](https://images.tango.us/workflows/a84d0a1b-6ee3-44b4-a932-abb4d9a4602a/steps/0ff30443-78bd-4978-a2b2-c9ecf3f6fb4e/77470452-6ea2-4710-9d36-c912ad406ad2.png)

*5* Enable the toggle for **Geo Tagging for Selfie Registration** to display all related configurations.

![Configs-2](https://images.tango.us/workflows/a84d0a1b-6ee3-44b4-a932-abb4d9a4602a/steps/43800e7e-a5e7-469b-b463-9f93aabcc9b8/026b21e7-6ff0-4f9d-b589-ea76494b43cc.png)

### Manage Configs

<table>
   <thead>
      <tr>
         <th width="302">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Enable Geo Tagging for Selfie Registration</td>
         <td>Enabling this toggle displays the options and captures location details in the response.</td>
      </tr>
      <tr>
         <td>Latitude</td>
         <td>Enabling this toggle records the latitude coordinates when geo-tagging is executed, providing location details of the registration process.</td>
      </tr>
      <tr>
         <td>Longitude</td>
         <td>Enabling this toggle records the longitude coordinates when geo-tagging is executed.</td>
      </tr>
      <tr>
         <td>Accuracy</td>
         <td>Enabling this toggle indicates the precision level of the captured latitude and longitude coordinates, showing how close the recorded location is to the actual position.</td>
      </tr>
      <tr>
         <td>Device Type</td>
         <td>Enabling this toggle includes device details in the response when geo-tagging is executed.</td>
      </tr>
      <tr>
      <td>Error Codes</td>
      <td>For error codes related to <strong>Selfie Registration</strong>, see <a href="/verify/docs/EV/WorkflowModules/ErrorCodes">here.</a></td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on the `SAVE` button.

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Selfie Registration** includes the following parameters:

| Name | Description |
|------|-------------|
| `selfie_registered` | This indicates whether selfie registration has been completed. The value will be "True" if the registration is successfully completed and "False" if it is not.|
| `photo_url` | URL to the photo used or captured during the selfie registration process.|
| `selfie_registered_at` | This provides a timestamp, in Unix epoch format, indicating the exact time when the selfie registration was completed. This allows you to know precisely when the registration event occurred. |
| `selfie_registration_latitude` | Latitude coordinate of the captured location during selfie registration. |
| `selfie_registration_longitude` | Longitude coordinate of the captured location during selfie registration. |
| `selfie_registration_device_type` | Type of device used for selfie registration, such as **mobile** or **web**. |
| `selfie_registration_accuracy` |  Accuracy level of the geo-tagging data. |

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Before proceeding, familiarize yourself with the **Do's** and **Don'ts.** Once you're aware, click on **Start** to begin.

![Start](https://images.tango.us/workflows/a84d0a1b-6ee3-44b4-a932-abb4d9a4602a/steps/0139685d-b9ec-4761-9901-eacd86d021f2/c3f2e42f-2f31-490a-aef9-424ffb82a70d.png)

*5* Once you have taken the pictures, preview your picture and then click on **Submit.**

![Capture](https://images.tango.us/workflows/a84d0a1b-6ee3-44b4-a932-abb4d9a4602a/steps/4e1966c8-77be-4ced-aba5-56afdfac7c16/859400d4-ddca-44da-bdcb-2206afd8ff01.png)

*6* The verification process will be completed only if the provided data is valid.

> **If "force onboarding" is enabled in the API request, all users, regardless of status, will be directed to the onboarding flow first, followed by the verification flow if configured.**

![Completed](https://images.tango.us/workflows/a84d0a1b-6ee3-44b4-a932-abb4d9a4602a/steps/59b1bd92-95eb-4474-b033-a580927d7d0b/c7656d00-f7d5-4f76-8bad-33ec28935f8b.png)

*7* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*8* Here is a sample response for **Selfie Registration:**

```json
{
   "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   "workflow": "verification",
   "user_id": "mark@gmail.com",
   // highlight-start
   "selfie_registration_latitude": "12.96867",
   "selfie_registration_longitude": "80.24826",
   "selfie_registration_device_type": "web",
   "selfie_registered_at": "1741779792",
   "selfie_registration_accuracy": "13.269",
   "photo_url": "https://abcdefghijk.eztovrfy.com/LggVqmTRjGcl3ef54Z0",
   "selfie_registered": "true/false"
   // highlight-end
}
```