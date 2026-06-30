<!-- ---
id: OTPLess
title: OTPLess
sidebar_label: OTPLess
--- 

The **Verify Phone** process typically involves verifying a user's phone number through **OTPLess** verification.

### Prerequisites

* Download the **ezto verify mobile** app before proceeding with the flow.

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Verify Phone.** If you need understanding  in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Verify Phone** to add it.

![Verify Phone](https://images.tango.us/workflows/5e96a090-7c2e-48e1-ad01-52377f49414a/steps/16bb8aa5-3a67-40ba-a8e9-684ff4ef1c2d/49490b4b-b21f-4dcc-9b20-d63b8bddc226.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/5e96a090-7c2e-48e1-ad01-52377f49414a/steps/7f41b8e4-d4b8-4051-b05d-8f7101e929f1/5c0daa36-0829-4480-8de9-97c93611c793.png)

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

![Configs](https://images.tango.us/workflows/5e96a090-7c2e-48e1-ad01-52377f49414a/steps/e6998e93-7be2-4c8d-892c-6bc0e501ab06/c149a711-daec-41fa-83fa-a9a1e7bbf74f.png)

*5* Choose **OTP\_Less** for **Verification Type.**

![OTPless](https://images.tango.us/workflows/5e96a090-7c2e-48e1-ad01-52377f49414a/steps/27a34b24-01ea-4615-8df6-603a550016ba/9d4e68cd-c078-46bf-890c-10e8b061d0ff.png)

### Manage Configs

<table>
   <thead>
      <tr>
         <th width="186">Name</th>
         <th>Description </th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>OTP Less Fallback</td>
         <td><strong>OTPLess</strong> is supported exclusively in the <strong>India</strong> region. When <strong>OTPLess</strong> fails, if the fallback method is enabled, it will switch to OTP verification, and the provider supported for this fallback is <strong>Twilio.</strong></td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on the `SAVE` button.

*6* Proceed to attach the workflow to the relevant application for execution. Refer to instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **Verify Phone with OTPLess** includes the following parameters:

| Name | Description |
|------|-------------|
| `remote_addr` | This refers to the remote address from which the request originated. In this case, it is the IP address of the remote device. |
| `device_match` | It indicates whether there is a match with the device information provided.|
| `phone_verified` | This denotes whether the phone verification is completed. The value will be "True" if the verification is successfully completed and "False" if it is not.|
| `phone_verified_at` | This provides a timestamp, in Unix epoch format, indicating the exact time when the phone number verification was completed. This allows you to know precisely when the verification event occurred. | 
| `phone_number` | Refers to the phone number being validated or verified. |

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way (Mobile)** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Begin by scanning the **QR code** with the **ezto verify** mobile app**.**

> This depends on what you have configured for the mobile notification way, i.e, **QR** code or **Push** integration. To know the difference between **QR** code or **Push,** refer [here](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile/).

![QR](https://images.tango.us/workflows/5e96a090-7c2e-48e1-ad01-52377f49414a/steps/5acaad91-5f5a-4032-9b67-8c204e883d19/c11ec01d-0036-4f19-a578-d3419fcd8ea7.png)

*5* Once you scan the QR code, proceed by providing the phone number.

> **Ensure that you provide the number associated with your mobile device.**

![Phone](https://images.tango.us/workflows/5e96a090-7c2e-48e1-ad01-52377f49414a/steps/6dfba00c-ad90-4af1-b71d-31c0bfeb358e/939d410a-a064-40e2-ac41-dfeed256c070.png)

*6* If you're connected to an external network, you will be asked to switch to your mobile network to ensure the provided number matches the one on the device being used for verification.

![Deny](https://images.tango.us/workflows/5e96a090-7c2e-48e1-ad01-52377f49414a/steps/df45f9bc-28a8-4c40-b196-c36bc303773d/018c40fd-8842-4f5e-a8fc-4712e9124e41.png)

*7* The OTPLess verification process will take a few seconds to validate.

![OTPless Verification](https://images.tango.us/workflows/5e96a090-7c2e-48e1-ad01-52377f49414a/steps/bd45aac8-42f4-4fce-a4b1-c55ce36191d0/1bb2615d-08f7-49cc-9713-dad11cd059e1.png)

*8* If the provided number is successfully validated against the one present in the device, the verification process will be completed.

![Completed](https://images.tango.us/workflows/5e96a090-7c2e-48e1-ad01-52377f49414a/steps/f12fa9e4-5f62-42a3-8e8b-f72d16c9ae27/17928a27-b7c9-408e-9dbd-3f2bceab221a.png)

*9* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*10* Here is a sample response for **Verify Phone with OTPLess:**

```json
{
	"workflow": "verification",
	"user_id": "mark@gmail.com",
	"request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
	// highlight-start
	"remote_addr": "000.000.000.000",
	"device_match": "true/false",
	"phone_verified": "true/false",
	"phone_verified_at": "1719569072",
	"phone_number": "9183xxxxxxxx"
	// highlight-end
}

``` -->