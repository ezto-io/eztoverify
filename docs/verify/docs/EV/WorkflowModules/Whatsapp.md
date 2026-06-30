---
id: Whatsapp
title: Whatsapp
sidebar_label: Whatsapp
--- 

**Verify WhatsApp** typically involves confirming the legitimacy and authenticity of a WhatsApp account using a **One-Time Password (OTP).**

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Verify WhatsApp.** If you need understanding  in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Verify WhatsApp** to add it.

![Verify WhatsApp](https://images.tango.us/workflows/be0ee86f-1d3f-4474-ac01-21ff955adc13/steps/3dcc24c0-b9e4-4e5c-a4d8-a81fb1096c02/1eec6828-864f-4414-b947-d0f5f3ef4793.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/be0ee86f-1d3f-4474-ac01-21ff955adc13/steps/57810bb2-75ea-4280-a9e7-e90786e30b8e/093305bc-0083-45b7-89f6-29adcce68605.png)

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

![Configs](https://images.tango.us/workflows/be0ee86f-1d3f-4474-ac01-21ff955adc13/steps/c7c4680d-7151-4c57-8502-1c785cbc48ad/d906232c-1bc0-4cb4-b0ca-7cdd2de23116.png)

### Manage Configs

<table>
   <thead>
      <tr>
         <th width="297">Name</th>
         <th>Description </th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>WhatsApp Verification Expiry Duration</td>
         <td>This setting allows you to specify the duration for which the <strong>WhatsApp</strong> verification link or OTP (One-Time Password) remains valid. After this period, the verification process expires and users will need to request a new verification.</td>
      </tr>
      <tr>
         <td>WhatsApp Verification OTP Length</td>
         <td>You can configure the length of the OTP (One-Time Password) used for email verification. The length can be set within the range of 4 to 8 digits, with a default value of 6 digits.</td>
      </tr>
      <tr>
         <td>WhatsApp Sender</td>
         <td>By default, all sms are sent through the default <strong>WhatsApp</strong> <strong>Provider</strong> configured for the system. If you prefer to use your own <strong>WhatsApp</strong> <strong>Provider</strong>, instructions on how to configure this can be found <a href="/verify/docs/EV/GetStarted/Configurations/Whatsapp/">here.</a></td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on the `SAVE` button.

*6* Proceed to attach the workflow to the relevant application for execution. Refer to instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **WhatsApp** includes the following parameters:

| Name | Description |
|------|-------------|
| `whatsapp_number_region` | The country code associated with the <strong>WhatsApp</strong> number.|
| `pin` | The PIN used in the verification process. |
| `whatsapp_number_verified_at` | Timestamp (in Unix epoch format) indicating when the <strong>WhatsApp</strong> number verification was done. | 
| `whatsapp_number_verified` | This denotes whether the WhatsApp number has been successfully verified. The value will be "True" if the verification is successfully completed and "False" if it is not. |
| `whatsapp_number` | The actual <strong>WhatsApp</strong> number being verified.|

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Begin by providing the **WhatsApp Phone Number** and click on **Submit.**

![WhatsApp Phone Number](https://images.tango.us/workflows/be0ee86f-1d3f-4474-ac01-21ff955adc13/steps/ac48c03e-7b06-478d-8f70-8288682e348b/b11cb8fc-7250-4be3-b3c9-ebe5ccfc7368.png)

*5* An email containing an **OTP** will be sent to the WhatsApp number provided above. Please input the **OTP** and click on **Submit** to proceed.

![OTP](https://images.tango.us/workflows/be0ee86f-1d3f-4474-ac01-21ff955adc13/steps/a7e0428e-64e6-4a7f-bc0f-b9665dd94619/6245c4aa-0303-4b61-a3c0-6f2d4c3170bf.png)

*6* The verification process will be completed only if the entered data is valid.

> **If "force onboarding" is enabled in the API request, all users, regardless of status, will be directed to the onboarding flow first, followed by the verification flow if configured.**

![Completed](https://images.tango.us/workflows/be0ee86f-1d3f-4474-ac01-21ff955adc13/steps/87fe3d66-d812-4b0d-bb61-d05451aa4b48/3ee65422-7517-4c78-96a4-35ee56f029df.png)

*7* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*8* Here is a sample response for **Verify WhatApp:**

```json
{
   "request_id":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   "workflow": "verification",
   "user_id": "mark@gmail.com",
   // highlight-start
   "whatsapp_number_region" : "in",
   "whatsapp_number_verified_at": "1719569072",
   "pin": "xxxx",
   "whatsapp_number_verified": "true/false",
   "whatsapp_number" : "83xxxxxxxx"
   // highlight-end
}
```