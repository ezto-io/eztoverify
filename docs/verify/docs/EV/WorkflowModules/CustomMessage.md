---
id: CustomMessage
title: Custom Message
sidebar_label: Custom Message
--- 

**Custom Message** allows personalizing and configuring the **end message** according to preference. The title, description, and logo can be customized, ensuring the message aligns perfectly with needs and intentions.

Additionally, when you add multiple validators in a flow, you can customize the end message for each path according to the specific use case of that particular validator. This allows for more targeted and relevant messaging based on the outcome of the validation process.

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Custom Message.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

> Make sure a preceding workflow is added and completed for the custom message to appear, as it will display after the workflow is executed. For instance, **Verify Aadhaar** is used here.
>
>Note that, the custom message is triggered when the preceding workflow is fully executed and reaches the custom message step. For example, if there are five workflow modules configured before the custom message, and the verification process ends after the third module, the message set at the **application level** will be displayed.

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Custom Message** to add it.

![Custom Message](https://images.tango.us/workflows/92cb498e-a896-4664-9889-1ec4199d7d32/steps/4d6a8a55-97ce-475a-970e-8d331eb2b517/a214a6a3-c03c-4b63-9c80-a5aa79e56549.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/92cb498e-a896-4664-9889-1ec4199d7d32/steps/57506f38-0312-417f-b254-a5575ddffca6/65b9cd4e-841b-43c6-b930-d1294029ccf7.png)

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

![Configs](https://images.tango.us/workflows/92cb498e-a896-4664-9889-1ec4199d7d32/steps/cab6ecfb-a893-4d8d-bbcf-ac182ce06c57/92234077-e248-4b44-a7b7-c7697f95fe2a.png)

### Manage Configs

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="297">Name</th>
         <th>Description </th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Logo<span class="required-text">*</span></td>
         <td>Upload a logo that will be displayed alongside the custom message once the workflow is completed.</td>
      </tr>
      <tr>
         <td>Title<span class="required-text">*</span></td>
         <td>Specify the title for the custom message that will appear at the end of the workflow execution.</td>
      </tr>
      <tr>
         <td>Description<span class="required-text">*</span></td>
         <td>Provide the description for the custom message that will be shown once the workflow has been executed.</td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.
 
*4* Since **Verify Aadhaar** is configured, begin by providing the **Aadhaar Number,** and click on **Submit.**

![Aadhaar](https://images.tango.us/workflows/92cb498e-a896-4664-9889-1ec4199d7d32/steps/b9e047c2-a844-4d3a-90c2-c49dc8520fe0/731ab775-55d8-428a-b734-a02298e0eaa7.png)

*5* The verification process will be completed if the entered data is valid, and the custom message you configured will then appear.

![Verification Completed](https://images.tango.us/workflows/92cb498e-a896-4664-9889-1ec4199d7d32/steps/cdabb2dc-a035-4b3f-b2fc-f1febcf4e6ad/579bf391-3ac4-420d-a562-3c5ea92941a3.png)