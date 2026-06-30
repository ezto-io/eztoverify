---
id: SelfieVerification
title: Selfie Verification
sidebar_label: Selfie Verification
--- 

The **Selfie Verification** process allows users to securely compare their stored facial template from the earlier **Selfie Registration** process, ensuring a smooth and secure verification experience.

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Selfie Verification.** If you need understanding  in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Selfie Verification** to add it.

![Selfie Verification](https://images.tango.us/workflows/ac710488-eaed-4f46-8218-d69a1f9ec9e0/steps/9e35d6b8-9895-4def-9f14-503d8040442e/ec036a24-9eb7-4844-8775-7e609760db19.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/ac710488-eaed-4f46-8218-d69a1f9ec9e0/steps/031dc6ea-ece6-4ef6-bcc7-2ec4e3735e43/17686bae-3a17-44da-9554-9599bd5f00ec.png)

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

![Configs](https://images.tango.us/workflows/ac710488-eaed-4f46-8218-d69a1f9ec9e0/steps/9df2e23d-5a1d-433f-89a5-62565daa7c60/75efa05f-3550-4fde-b7df-5ba1d92e233e.png)

### Manage Configs

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="302">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Threshold Selfie Verification Quality Score<span class="required-text">*</span></td>
         <td>Set the threshold quality score for Selfie Verification, with a maximum allowable value of 100. This score determines the minimum level of quality required in the selfie image for successful verification. Adjusting this threshold helps ensure accurate and reliable identification through facial recognition technology.</td>
      </tr>
      <tr>
         <td>Enable for Face Comparison</td>
         <td>Enable this to initiate the comparison process between the uploaded selfie and the reference image.</td>
      </tr>
      <tr>
         <td>Image to be Compared</td>
         <td>Use the image claim from previously executed workflows or provide a valid image from the request metadata as the reference for image comparison during the selfie verification process. This image serves as the basis for matching. The claim should be in the format "${'{workflowmodule_name@profile_image'}}"</td> 
      </tr>
      <tr>
         <td>Enable Geo Tagging for Selfie Verification</td>
         <td>Enabling this toggle displays the options and captures location details in the response.</td>
      </tr>
      <tr>
         <td>Latitude</td>
         <td>This records the latitude coordinates when geo-tagging is executed, providing location details of the verification process.</td>
      </tr>
      <tr>
         <td>Longitude</td>
         <td>This records the longitude coordinates when geo-tagging is executed.</td>
      </tr>
      <tr>
         <td>Accuracy</td>
         <td>It indicates the precision level of the captured latitude and longitude coordinates, showing how close the recorded location is to the actual position.</td>
      </tr>
      <tr>
         <td>Device Type</td>
         <td>Enabling this toggle includes device details in the response when geo-tagging is executed.</td>
      </tr>
      <tr> 
      <td>Error Codes</td>
      <td>For error codes related to <strong>Selfie Verification</strong>, see <a href="/verify/docs/EV/WorkflowModules/ErrorCodes">here.</a></td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on the `SAVE` button.

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Selfie Verification** includes the following parameters:

| Name | Description |
|------|-------------|
| `selfie_verified` | This indicates whether selfie verification has been completed. The value will be "True" if the verification is successfully completed and "False" if it is not.|
| `confidence` | Confidence level or score associated with the selfie verification process.|
| `selfie_verified_at` | This provides a timestamp, in Unix epoch format, indicating the exact time when the selfie verification was completed. This allows you to know precisely when the verification event occurred. |
| `selfie_verification_latitude` | Latitude coordinate of the captured location during selfie verification. | 
| `selfie_verification_longitude` | Longitude coordinate of the captured location during selfie verification.|
| `selfie_verification_accuracy` | Accuracy level of the geo-tagging data, typically measured in meters.| 
| `selfie_verification_device_type` | Type of device used for selfie verification, such as **mobile** or **web**.|


The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Before proceeding, familiarize yourself with the **Do's** and **Don'ts.** Once you're aware, click on **Start** to begin.

![Start](https://images.tango.us/workflows/ac710488-eaed-4f46-8218-d69a1f9ec9e0/steps/eb0db882-815d-434e-8217-c1118507a287/f2da1e05-5917-40e1-a276-c17fe9e22140.png)

*5* Once you have taken the pictures, preview your picture and then click on **Submit.**

![Capture](https://images.tango.us/workflows/ac710488-eaed-4f46-8218-d69a1f9ec9e0/steps/22da360f-da1d-40a4-8d77-1f7f7d345ef1/13991c4f-59eb-4c4b-b437-c9099e22b8fa.png)

*6* The verification process will be completed only if the provided data is valid.

> **If "force onboarding" is enabled in the API request, all users, regardless of status, will be directed to the onboarding flow first, followed by the verification flow if configured.**

![Completed](https://images.tango.us/workflows/ac710488-eaed-4f46-8218-d69a1f9ec9e0/steps/8e86185b-5420-4543-b415-d90662bdd4a0/ae97eecc-296e-4788-a9bd-1a1ce5600f04.png)

*7* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*8* Here is a sample response for **Selfie Verification:**

```json
{
"request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
"workflow": "verification",
"user_id": "mark@gmail.com",
// highlight-start
"selfie_verified": "true/false",
"confidence": "50.00",
"selfie_verified_at": "1719569072",
"selfie_verification_latitude": "12.96867",
"selfie_verification_longitude": "80.24826",
"selfie_verification_accuracy": "13.118",
"selfie_verification_device_type": "web"
// highlight-end
}
```