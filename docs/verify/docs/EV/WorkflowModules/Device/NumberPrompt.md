---
id: NumberPrompt
title: Number Prompt 
sidebar_label: Number Prompt
--- 

This method enhances convenience by enabling verifications through a simple number prompt system, making the process faster and more user-friendly.

> This flow supports only **Mobile** applications.

### Prerequisites

* Download the **ezto verify mobile** app before proceeding with the flow.
* Ensure an additional verification workflow accompanies the **Number Prompt** workflow. In this case **Web to Device** flow serves as the initial part of verification, and the second workflow triggers another part of the verification that is configured.
* Have two different applications in the **Workspace**, each designated for a specific workflow.
* The second workflow must include the **Number Prompt** along with any other verification workflows if required. Ensure that the **Web to Device** has "Number Prompt" enabled in the workflow's configs.

### How to Setup

*1* Navigate to **Workspace > Workflows,** and click on the flow created for **Number Prompt.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Number Prompt** to add it.

> This workflow only works with **Web to Device.** So, make sure to add an application consisting of **Web to Device** in order for the **Number Prompt** to execute.

![Number Prompt](https://images.tango.us/workflows/65ca07f4-1a7a-4f7c-ae39-f8ebec14d527/steps/64326ee5-8a19-4566-9768-b5139247f601/ed13ec57-5b59-42d8-a99d-21fae020c9ca.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/65ca07f4-1a7a-4f7c-ae39-f8ebec14d527/steps/5d0b9a06-9ba4-48a3-977d-701c138d9b49/1415ba6b-32a0-47ae-a88a-3c21ad3d7101.png)

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

*4* After you've configured everything needed, click on the `SAVE` button.

*5* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Number Prompt** includes the following parameters:

| Name | Description |
|------|-------------|
| `web_to_device` | Status indicating the outcome of communication between the web platform and the device. |
| `allow_new_device` | Indicates whether a user is permitted to access the service or application from a new or unrecognized device. If set to true, the user can use a new device; if false, the user may be restricted to previously authorized devices.|
| `prompt_success` | Indicates whether the number prompt process was successfully completed.|
| `prompt_completed_at` |Timestamp marking when the number prompt process was completed. |

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

> Make sure to configure the `"allow_new_device": true` in the body of the respective **API.**

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

> This is the initial verification process configured; as an example, **Verify Aadhaar** is used here.

*4* Begin by providing the **Aadhaar Number** and click **Submit.**

![Aadhaar](https://images.tango.us/workflows/65ca07f4-1a7a-4f7c-ae39-f8ebec14d527/steps/5a96cdb0-a88e-4859-ab0b-c5bf430b0fe4/2042a518-9b16-4cd1-9a10-4e97acd8841e.png)

*5* To initiate **Number Prompt** flow, you will be prompted to scan the QR code with the **ezto verify mobile app.**

> This depends on what you have configured for the mobile notification way, i.e, **QR** code or **Push** integration. To know the difference between **QR** code or **Push,** refer [here.](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile/)

![QR](https://images.tango.us/workflows/65ca07f4-1a7a-4f7c-ae39-f8ebec14d527/steps/d030c979-d2bc-4232-b867-18f3f38d812c/235d9a42-fbcf-4d11-9aae-f665246a6b5d.png)

*6* Once you've scanned the code, a number will appear on the web interface. Simultaneously, a set of numbers will be displayed in the **ezto verify mobile app.**

![Numbers](https://images.tango.us/workflows/65ca07f4-1a7a-4f7c-ae39-f8ebec14d527/steps/331cfe16-a551-479c-b5f4-2cb716fcd48c/92501b57-56c2-42f2-9151-5ab299b94e22.png)

*7* Select the number in the mobile app that matches the one shown on the web interface to proceed. 

![Choose](https://images.tango.us/workflows/65ca07f4-1a7a-4f7c-ae39-f8ebec14d527/steps/3b381201-80fa-4d10-abe2-5a9811a14d36/027cf8fa-bdb4-43a6-bfda-a19bba3ceecc.png)

*8* The verification process will be completed successfully.

![Completed](https://images.tango.us/workflows/65ca07f4-1a7a-4f7c-ae39-f8ebec14d527/steps/14678828-20b1-4e55-831f-db4675e735c1/c78953e7-5d6b-40a5-8994-b5b892aa7dd9.png)

*9* Here is a sample response for **Number Prompt:**

```json
{
   "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",  
   "workflow": "verification",
   "user_id": "mark@gmail.com",
   "aadhaar_number": "334108903696",
   "aadhaar_verified": "true",
   "aadhaar_verified_at": "1733729102",
   // highlight-start
   "web_to_device": "true/false",
   "allow_new_device": "true/false",
   "prompt_success": "true/false",
   "prompt_completed_at": "1733729174"
   // highlight-end
}
```