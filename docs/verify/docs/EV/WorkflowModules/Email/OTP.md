---
id: OTP
title: Email OTP - Overview
sidebar_label: OTP
--- 

The **Verify Email** process is used to confirm the validity of a user's email address, it typically involves sending a verification email to the user's provided address, which contains a unique verification code.

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Verify Email.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Verify Email** to add it.

![Verify Email](https://images.tango.us/workflows/0d4bd3df-d442-43c4-8915-1274a85538e7/steps/aef1fa6b-e1c3-4e37-977b-39c42d683d36/de0a0518-a8f8-4a4d-b784-658b3c2a02d8.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/0d4bd3df-d442-43c4-8915-1274a85538e7/steps/9e3d5e3a-f650-417d-a6e5-0d3ee81736f2/a0bf985b-bdaf-4aad-9cb9-acc82b8b5d3a.png)

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

![Configs](https://images.tango.us/workflows/0d4bd3df-d442-43c4-8915-1274a85538e7/steps/e1267db8-8fd4-4fe0-a9e1-9727ea8996b3/8196a991-d052-419c-bfdf-77d3f13ee1be.png)

### Manage Configs

<table>
   <thead>
      <tr>
         <th width="286">Name</th>
         <th>Description </th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Email Verification Expiry Duration</td>
         <td>This setting allows you to specify the duration for which the email verification link or OTP (One-Time Password) remains valid. After this period, the verification process expires and users will need to request a new verification.</td>
      </tr>
      <tr>
         <td>Email Verification OTP Length</td>
         <td>You can configure the length of the OTP (One-Time Password) used for email verification. The length can be set within the range of 4 to 8 digits, with a default value of 6 digits.</td>
      </tr>
      <tr>
         <td>Allowed Email Domains</td>
         <td>Specify the email domains that are allowed to be verified. Only email addresses from these specified domains will be considered valid for verification purposes.</td>
      </tr>
      <tr>
         <td>Email Sender</td>
         <td>By default, all emails are sent through the default <strong>Email</strong> <strong>Provider</strong> configured for the system. If you prefer to use your own email provider, instructions on how to configure this can be found <a href="/verify/docs/EV/GetStarted/Configurations/Email/Overview/">here.</a></td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Verify Email** includes the following parameters:

| Name | Description |
|------|-------------|
| `email_verified` | This denotes whether the email verification is completed. The value will be "True" if the verification is successfully completed and "False" if it is not.|
| `email` | Refers to the email address being validated or verified. |
| `email_verified_at` | This provides a timestamp, in Unix epoch format, indicating the exact time when the email verification was completed. This allows you to know precisely when the verification event occurred. | 

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Begin by providing the **Email Address** and click on **Submit.**

![Email](https://images.tango.us/workflows/0d4bd3df-d442-43c4-8915-1274a85538e7/steps/79aaf7af-267e-42b2-a775-b30d8ba2fbfe/bef27e02-5a6b-448e-a866-71e669933ec8.png)

*5* An email containing an **OTP** will be sent to the email address provided above. Please input the **OTP** and click on **Submit** to proceed.

![OTP](https://images.tango.us/workflows/0d4bd3df-d442-43c4-8915-1274a85538e7/steps/8549689f-81bd-42c1-92e4-648d759201df/084006c5-1998-4b4c-94ee-d120df41ea1f.png)

*6* The verification process will be completed only if the entered data is valid.

> **If "force onboarding" is enabled in the API request, all users, regardless of status, will be directed to the onboarding flow first, followed by the verification flow if configured.**

![Completed](https://images.tango.us/workflows/0d4bd3df-d442-43c4-8915-1274a85538e7/steps/8cebecec-7b5d-400e-a0ee-97f6fc48554c/70c445b9-c9a8-4f14-b5b3-0d464a1147e6.png)

*7* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*8* Here is a sample response for **Verify Email:**

```json
{
   "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   "user_id": "mark@gmail.com",
   "workflow": "verification",
   // highlight-start
   "email": "mark@gmail.com",
   "email_verified": "true/false",
   "email_verified_at": "1719569072"
   // highlight-end
}
```