---
id: LivenessVerification
title: Liveness Verification
sidebar_label: Liveness Verification
--- 

import ExpandableBox from "@site/src/components/docs/ExpandableBox";

This process is used to verify both the image and the liveness of the user. It ensures that the user is physically present and not using a photo or video during the verification process, enhancing security by confirming the user's live presence.

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Liveness Verification.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Liveness Verification** to add it.

![Liveness Verification](https://images.tango.us/workflows/279ffee8-1c06-4555-9d65-89dbfd7fe208/steps/1003b087-b87f-49c0-963b-f2f0cb6ff911/e947385e-c1b5-41c6-a04e-eeb00b33ccd6.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/279ffee8-1c06-4555-9d65-89dbfd7fe208/steps/4c59955b-ccbd-42d6-912c-b90fc6fc9445/7b463857-822c-49b6-9e74-c3b5f9352e75.png)

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

![Configs](https://images.tango.us/workflows/279ffee8-1c06-4555-9d65-89dbfd7fe208/steps/35b0f1ce-1eba-4613-b7e4-08d53629307d/a4c1fc62-7391-430b-bd41-978d2eee16f4.png)

> Note that, if you configure only **Face comparison** the flow will not work, for the face comparison to work, you need to set up the below additional verification, such as **Blink verification**, for liveness verification to function properly.

### Manage Configs

<table>
   <thead>
      <tr>
         <th width="246">Configuration</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Face Comparison</td>
         <td>This feature compares the user's image and video during the verification process to confirm their identity.</td>
      </tr>
      <tr>
         <td>Blink Verification</td>
         <td>Users are prompted to perform specific blinking actions for verification purposes.</td>
      </tr>
      <tr>
         <td>Movement Verification</td>
         <td>This involves assessing the user's movements to confirm their identity.</td>
      </tr>
      <tr>
         <td>Fingers Count Verification</td>
         <td>Users are required to display their fingers to the camera as part of the verification process.</td>
      </tr>
      <tr>
         <td>Speech Verification</td>
         <td>Verification necessitates users to recite a specific word in a video as a means of confirmation.</td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on the `SAVE` button.

*6* Proceed to attach the workflow to the relevant application for execution. Refer to instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Liveness Verification** includes the following parameters:

| Name | Description |
|------|-------------|
| `liveness_completed` | This indicates whether the liveness verification has been completed. The value will be "True" if the verification is successfully completed and "False" if it is not.|
| `liveness-fingercount-verified` | Checks whether the finger count during the liveness verification process has been verified ("True" if completed, "False" if not). |
| `liveness-speech-verified` |Indicates whether the speech during the liveness verification process has been verified ("True" if completed, "False" if not). |
| `liveness_video` | URL to the video used for the liveness verification process.|
| `liveness-blink-verified` | Checks whether the blinking during the liveness verification process has been verified ("True" if completed, "False" if not). |
| `photo_url` | URL to the photo used or captured during the liveness verification process. |
| `liveness-movement-verified` | Validates the movement during the liveness verification ("True" if completed, "False" if not).|

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Before proceeding, familiarize yourself with the **Do's** and **Don'ts.** Once you're aware, click on **Start** to begin.

![Liveness](https://images.tango.us/workflows/279ffee8-1c06-4555-9d65-89dbfd7fe208/steps/989b3369-41c7-4997-98a6-5f4b1c289a30/628d8733-25dc-43a2-b8e0-51b95f893b2f.png)

*5* Once you have taken the pictures, click on **Submit.**

*6* The verification process will be completed only if the provided data is valid.

> **If "force onboarding" is enabled in the API request, all users, regardless of status, will be directed to the onboarding flow first, followed by the verification flow if configured.**

![Completed](https://images.tango.us/workflows/279ffee8-1c06-4555-9d65-89dbfd7fe208/steps/b03f9d94-fbc9-4cb5-acee-70c8c24e91fc/dd7f726c-35d2-4163-a0a5-f7de2fb4d1e0.png)

*7* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*8* Here is a sample response for **Liveness Verification:**

```json
{
   "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   "workflow": "verification",
   "user_id": "mark@gmail.com",
   // highlight-start
   "liveness_completed": "true/false",
   "liveness-fingercount-verified": "true/false",
   "liveness-speech-verified": "true/false",
   "liveness_video": "https://minio-api.eztovrfy.com/xx-xx-xx",
   "liveness-blink-verified": "true/false",
   "photo_url": "https://minio-api.eztovrfy.com/xx-xx-xx",  	
   "liveness-movement-verified": "true/false"
   // highlight-end
}
```