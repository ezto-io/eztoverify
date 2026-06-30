---
id: NameSimilarityCheck
title: Name Similarity Check
sidebar_label: Name Similarity Check
--- 

This module compares names from two documents and gives a score showing how similar they are. It helps check for any differences between the names, making it easier to confirm whether the documents belong to the same person.

### How to Setup

*1* Navigate to **Workspace > Workflows,** and click on the flow created for **Name Similarity Check.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

> Ensure that preceding workflows are added, only then can the **Name Similarity Check** workflow module compare the names with the claims from previously completed workflows. For instance, **Verify Aadhaar** and **Verify PAN** are used here. 

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Name Similarity Check** to add it.

![Name Similarity Check](https://images.tango.us/workflows/cce72227-f13e-47d3-a801-e0552cc2a84e/steps/6dfa7c1d-1a55-4ac1-8173-e7bae0cc2ff6/4701e03a-ebc0-4972-8f2e-090afb5c3c1d.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/cce72227-f13e-47d3-a801-e0552cc2a84e/steps/8fb880be-6ea9-4495-a612-327c9508a01a/546a4a9f-d382-4f9c-bbd4-6ef50dd736ca.png)

### Manage General

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="159">Name</th>
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

![Configs](https://images.tango.us/workflows/cce72227-f13e-47d3-a801-e0552cc2a84e/steps/888fbc23-61a5-4a33-8728-12545d960804/8cd1df97-e982-4e2c-9158-f9f548c3a4f3.png)

### Manage Configs

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="197">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Title<span class="required-text">*</span></td>
         <td>Provide an appropriate title to be displayed in the verification page.</td>
      </tr>
      <tr>
         <td>Description</td>
         <td>Provide the description to be displayed to the user during the verification process.</td>
      </tr>
      <tr>
         <td>First Name to be Compared</td>
         <td>Enter the first name to be compared for a match.</td>
      </tr>
      <tr>
         <td>Second Name to be Compared</td>
         <td>Enter the second name to be compared for a match.</td>
      </tr>
      <tr>
      <td>Error Codes</td>
      <td>For error codes related to <strong>Name Similarity Check</strong>, see <a href="/verify/docs/EV/WorkflowModules/ErrorCodes">here.</a></td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Name Similarity Check** includes the following parameters:

| Name | Description |
|------|-------------|
| `first_name_to_be_scored` | First name used for similarity comparison.|
| `second_name_to_be_scored` |  Second name used for similarity comparison.|
| `score` | Numeric value representing the similarity between the two names.|

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Since **Verify PAN** and **Verify Aadhaar with OTP** are set as preceding workflows, start by entering the required details and click **Submit.**  

*5* After completing both workflows, the name comparison process will begin.

![Name Check](https://images.tango.us/workflows/cce72227-f13e-47d3-a801-e0552cc2a84e/steps/97d80935-aa13-4668-902b-4d48260615d1/5623f83b-b243-489a-a433-965365038b9f.png)

*6* The verification process will be completed.

> **If "force onboarding" is enabled in the API request, all users, regardless of status, will be directed to the onboarding flow first, followed by the verification flow if configured.**

![Completed](https://images.tango.us/workflows/cce72227-f13e-47d3-a801-e0552cc2a84e/steps/986a3281-3421-4280-82b4-de31898572c5/f02b06d1-7b3a-4167-83b1-33d75ceccb42.png)

*7* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*8* Here is a sample response for **Name Similarity Check:**

```json
{
   "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   "workflow": "verification",
   "user_id": "mark@gmail.com",
   "lastName": "ROBERT",
   "pan_verified_at": "1738909975",
   "aadhaar_number_otp": "857xxxxxxx",
   "panStatus": "VALID",
   "pan_number": "IDxxxxxxx",
   "full_name": "Mark R",
   "aadhaar_otp_verified": "true/false",
   "aadhaar_otp_verified_at": "1738910033",
   "name": "ROBERT MARK",
   "middleName": "",
   "pan_verified": "true/false",
   // highlight-start
   "documentation_check_first_name_to_be_scored": "ROBERT MARK",
   "docs_check_second_name_to_be_scored": "Mark R",
   "docs_check_score": "57.95"
   // highlight-end
}
```