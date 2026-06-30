---
id: OTP
title: Mobile Number Verification - OTP
sidebar_label: OTP
--- 

The **Verify Phone** process typically involves verifying a user's phone number through the use of a One-Time Password (OTP).

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Verify Phone.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Verify Phone** to add it.

![Verify Phone](https://images.tango.us/workflows/bcfcb103-bc37-4e7e-93cc-06b620ffe5db/steps/c50455bb-77e9-4a39-87a9-da90e74b2203/3d86d331-b38c-41c5-8bcb-3e0d593c2195.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/bcfcb103-bc37-4e7e-93cc-06b620ffe5db/steps/06e7253e-9467-4416-b35f-235e74aaff29/88dc6047-fefb-4327-8040-2cf12d9f917e.png)

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

*4* Then, click on the **Configs** tab to set the necessary configurations. When configuring the **Verification Type,** by default **OTP** is set, but there are two types available: **OTP** and **OTP\_LESS.**

![Configs](https://images.tango.us/workflows/bcfcb103-bc37-4e7e-93cc-06b620ffe5db/steps/9f49a897-237d-4be2-9c58-cbfb7115ce58/31a021d8-376c-4560-a6ec-5d2546bb34e4.png)

*5* Choose **OTP** for **Verification Type.**

![OTP](https://images.tango.us/workflows/bcfcb103-bc37-4e7e-93cc-06b620ffe5db/steps/08655ed7-048f-4a4b-ba65-997e9283ef3d/c2a4ec8f-a6a9-4f13-965c-a7915be522f5.png)

### Manage Configs

<table>
   <thead>
      <tr>
         <th width="325">Name</th>
         <th>Description </th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Phone Verification Expiry Duration</td>
         <td>This setting allows you to specify the duration for which the phone verification link or OTP (One-Time Password) remains valid. After this period, the verification process expires and users will need to request a new verification.</td>
      </tr>
      <tr>
         <td>Email Verification OTP Length</td>
         <td>You can configure the length of the OTP (One-Time Password) used for email verification. The length can be set within the range of 4 to 8 digits, with a default value of 6 digits.</td>
      </tr>
      <tr>
         <td>Template of Text to Send to the User</td>
         <td>Add phone message template, include code attribute in your text to map the generated OTP. <strong>Eg. Your OTP is code</strong></td>
      </tr>
      <tr>
         <td>SMS Sender</td>
         <td>By default, all <strong>SMS</strong> are sent through the default <strong>SMS</strong> <strong>Provider</strong> configured for the system. If you prefer to use your own sms provider, instructions on how to configure this can be found <a href="/verify/docs/EV/GetStarted/Configurations/SMS/">here.</a></td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on the `SAVE` button.

*6* Proceed to attach the workflow to the relevant application for execution. Refer to instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Verify Phone with OTP** includes the following parameters:

| Name | Description |
|------|-------------|
| `phone_verified` | This denotes whether the phone verification is completed. The value will be "True" if the verification is successfully completed and "False" if it is not.|
| `phone` | Refers to the phone number being validated or verified. |
| `phone_verified_at` | This provides a timestamp, in Unix epoch format, indicating the exact time when the phone number verification was completed. This allows you to know precisely when the verification event occurred. | 
| `phone_region` | Refers to the country code associated with the phone number.|

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Begin by providing the **Phone Number** and click on **Verify.**

![Phone](https://images.tango.us/workflows/bcfcb103-bc37-4e7e-93cc-06b620ffe5db/steps/183246f4-b507-43c6-a578-05ad79a4518d/517c33a8-c742-4ce1-b6b0-2749865584d6.png)

*5* An **OTP** will be sent to the phone number provided above. Please input the **OTP** and click on **Submit** to proceed.

![OTP](https://images.tango.us/workflows/bcfcb103-bc37-4e7e-93cc-06b620ffe5db/steps/3e17618a-25b9-4d45-bc1c-0103efcb86c5/e7d95426-b63d-43a7-a529-fb54e9d48c39.png)

*6* The verification process will be completed only if the entered data is valid.

> **If "force onboarding" is enabled in the API request, all users, regardless of status, will be directed to the onboarding flow first, followed by the verification flow if configured.**

![Completed](https://images.tango.us/workflows/bcfcb103-bc37-4e7e-93cc-06b620ffe5db/steps/cd0d4835-5c9e-49cf-84c1-d7e811b8d51c/27a23240-029b-4700-9545-9e27c7ebc2d0.png)

*7* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*8* Here is a sample response for **Verify Phone with OTP:**

```json
{
   "workflow": "verification",
   "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   "user_id": "mark@gmail.com",
   // highlight-start
   "phone_verified": "true/false",
   "phone": "98xxxxxxxx",
   "phone_verified_at": "1719569072",
   "phone_region": "in"
   // highlight-end
}
```