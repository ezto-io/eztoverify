---
id: CustomResponse
title: Custom Response
sidebar_label: Custom Response
--- 

The **Custom Response** allows you to include customized outputs alongside the default workflow response, offering flexibility to customize results based on specific needs while keeping the core response unchanged.

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Custom Response.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

> Make sure a preceding workflow is added and completed for the custom response to be fetched, as it can only be retrieved after the workflow is executed. For instance, **Verify Aadhaar** and **Verify PAN** are used here.
>
> Note that, the custom response or result is triggered only when the preceding workflow(s) are fully executed.

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Custom Response** to add it.

![Custom Response](https://images.tango.us/workflows/71c1ebf6-e814-475e-b0e2-01d44a14b6ae/steps/454867e9-af38-4a72-9683-5e30d577ffdc/6f09c4a0-5ae3-4d5d-a539-23e25f3f2296.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/71c1ebf6-e814-475e-b0e2-01d44a14b6ae/steps/4f97cf75-4c2f-4f30-b268-74b283fac3f5/c0c31f10-1ae9-4680-819e-ecd5b520c627.png)

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

![Configs](https://images.tango.us/workflows/71c1ebf6-e814-475e-b0e2-01d44a14b6ae/steps/41c8c85e-063b-410d-af9d-65d274aa0bf2/9e3b3bd7-6982-4b6c-aed6-feb4c19d6960.png)

### Manage Configs

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="297">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Custom Response Key<span class="required-text">*</span></td>
         <td>This is the key used to store the custom response data in the output. It serves as the field name in the response to identify your custom data (e.g., <code>Docs_Key</code>).</td>
      </tr>
      <tr>
         <td>Custom Response Value<span class="required-text">*</span></td>
         <td>Provide the <strong>Custom Response Value</strong>. if multiple values are provided, they should be formatted in JSON, with each claim enclosed in <code>"example": "{'${'}verify_aadhaar@state{'}'}"</code>. If only one value is being retrieved, it should be formatted as <code>{'${'}verify_aadhaar@state{'}'}</code>. This ensures the data is accurately displayed based on the workflow configuration.</td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.
 
*4* Since **Verify Aadhaar** and **Verify PAN** are configured, begin by providing the **Aadhaar number,** and click on **Submit.**

![Aadhaar](https://images.tango.us/workflows/71c1ebf6-e814-475e-b0e2-01d44a14b6ae/steps/d607ea3c-5a94-4942-8131-23c56b872fd6/c5c2e989-53dd-4490-867c-12098b524949.png)

*5* Continue with the **PAN** verification by providing the **PAN number** and click on **Submit.**

![PAN](https://images.tango.us/workflows/71c1ebf6-e814-475e-b0e2-01d44a14b6ae/steps/244c1c9d-935d-40db-8f4c-363a98d200d4/c5c3a645-d66c-491d-8735-dec53d02197b.png)

*6* The verification process will be completed only if the entered data is valid.

![Verification Completed](https://images.tango.us/workflows/71c1ebf6-e814-475e-b0e2-01d44a14b6ae/steps/51133a05-b9dc-4585-aedb-93b5e6167e6a/76a519c9-8803-4a4c-859f-1e1a86fcb205.png)

*7* Here is a sample response for **Custom Response:**

```json
{
   "requestId":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   "workflow": "verification",
   "userId": "mark@gmail.com",
   "pan_number": "Exxxxxxxxx",
   "dob": "2000-11-30",
   "aadhaar_number": "40xxxxxxxx",
   "pan_verified_at": "1733304403",
   "aadhaar_verified_at": "1733304391",
   "state": "Tamil Nadu",
   "pan_verified": "true",
   "aadhaar_verified": "true",
   // highlight-start
   "Docs_Key": "{\n\"aadhaar_no\":\"40xxxxxxxx\",\n\"state\":\"Tamil Nadu\",\n\"dob\":\"2000-12-30\"\n}"
   // highlight-end
}
```
