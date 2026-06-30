---
id: WebtoDevice
title: Web to Device
sidebar_label:  Web to Device
--- 

This process involves an **initial** verification step on the **web**, followed by a **second** verification step on a **mobile** device. This is accomplished using a **QR code** and **Push** integration.

> This flow supports both **Mobile** and **Desktop** applications.

### Prerequisites

* Ensure an additional verification template flow accompanies this **Web to Device** flow. This web-to-device flow serves as the initial part of verification, and the second template triggers another part of the verification that is configured.
* Have two different applications in the **Workspace**, each designated for a specific template.
* The second template must exclude the web-to-device template but can include any other verification templates.
* Download the **ezto verify mobile** app before proceeding with the flow.

### How to Setup

*1* Navigate to **Workspace > Workflows,** and click on the flow created for **Web to Device.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

> Ensure an additional workflow is present for the **Web to Device** to execute; for instance, **Verify Aadhaar** is used here.

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Web to Device** to add it.

![Web to Device](https://images.tango.us/workflows/488082c7-6c77-4011-8056-f228d4462bf2/steps/d66d2c82-7eee-4156-8ae7-1acd42fcd9bc/712eead1-1075-4c1b-b2e2-a8aadee9657f.png)

*3* Click on the newly added **Web to Device** workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/488082c7-6c77-4011-8056-f228d4462bf2/steps/62009436-0f8c-4355-ac2a-111971d93ec2/00e66bf8-ab95-4e07-8d1d-a14b3df33bde.png)

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

![Configs](https://images.tango.us/workflows/488082c7-6c77-4011-8056-f228d4462bf2/steps/fb98b909-e405-4c02-9b7c-8263c76454a0/b2f98c13-3f9a-4d11-9c53-c6ec104c7071.png)

> If no additional claims are added, the default claims will be used for the response.

### Manage Configs

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="251">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Consent Message</td>
         <td>This configuration determines the information included in the response generated. If no additional claims are added, the default claims will be taken.</td>
      </tr>
      <tr>
         <td>Mobile App Supported Application<span class="required-text">*</span></td>
         <td>Select the second application for the <strong>mobile verification</strong> (second part of verification) flow. Ensure that this application is configured with the <strong>Verification Way</strong> set as <strong>Mobile</strong> and <strong>Notification Way</strong> as <strong>Mobile SDK.</strong>  Note that the <strong>Workflows</strong> associated with this application can include any type of workflow except for the <strong>Web to Device</strong>. For instance, <strong>Verify Pan</strong> is used here.</td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on the `SAVE` button.

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

> The **Web to Device** also supports **Proximity Verification** To know how it works, see [here.](/verify/docs/EV/WorkflowModules/Device/ProximityVerification/)

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

> Make sure to configure the `"allow_new_device": true` in the body of the respective **API.**

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.
 
*4* Since **Verify** **Aadhaar** is configured as the initial verification, begin by providing the **Aadhaar Number,** and click on **Submit.**

![Aadhaar](https://images.tango.us/workflows/488082c7-6c77-4011-8056-f228d4462bf2/steps/16832b07-f8f3-4bf6-9226-bd39621e41e9/63877461-00a4-4110-89e2-bd1fc3606a9e.png)

*5* To initiate the second part of the verification process, you will need to complete the **Device Verification** step. During this step, you will be prompted to scan the provided **QR** code.

> This depends on what you have configured for the mobile notification way, i.e, **QR** code or **Push** integration. To know the difference between **QR** code or **Push,** refer [here.](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile/)

![QR](https://images.tango.us/workflows/488082c7-6c77-4011-8056-f228d4462bf2/steps/56d206b9-0f0d-41ff-8a51-495101155f84/a7595d4c-5b4d-4194-b731-4c070bdcd4b9.png)

*6* Once you scan the **QR code**, the verification will be in progress, to complete it, open your **ezto verify mobile app,** and click on **Scan Now** button.

> In both applications, the onboarding and verification **Workflows** will be triggered. If the second application does not have an onboarding workflow, the verification workflow will be triggered by default.

![QR CODE SCANNER](https://images.tango.us/workflows/488082c7-6c77-4011-8056-f228d4462bf2/steps/3fa1d092-1d25-41bf-94eb-3ecc39370117/23e6b426-fa65-43dd-a456-258858403396.png)

*7* Since **Verify Pan** is configured for the second part of verification. you will be asked to provide the **PAN** number and click on **Submit.**

![Pan Number](https://images.tango.us/workflows/488082c7-6c77-4011-8056-f228d4462bf2/steps/4e9436dc-1b9c-4443-a395-a4fb200bac18/ecd6ca64-f335-4e95-9716-0020873df6c4.png)

*8* The verification process will be completed if the entered data is valid.

![Verification Completed](https://images.tango.us/workflows/488082c7-6c77-4011-8056-f228d4462bf2/steps/4d46f308-cdd3-4c0a-a833-e944965152de/f8643a42-a1cb-4448-9533-118fba304473.png)

*9* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, [see here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*10* Here is a sample response for **Web to Device:**

```json
{
	"workflow": "verification",
    "user_id": "mark@gmail.com",
	"request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
	// highlight-start
	"skip_user_attrs_delete": "true/false",
	"web_to_device": "true/false",
	"allow_new_device": "true/false"
	// highlight-end
}
```