---
id: Register
title: Register
sidebar_label: Register
--- 

**FIDO Register** allows users to securely register both their unique fingerprint patterns and a confidential passkey.

### How to Setup

*1* Navigate to **Workspace > Workflows,** and click on the flow created for **FIDO Register.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **FIDO Register** to add it.

![FIDO Register](https://images.tango.us/workflows/f41c3c2b-6ad1-4185-a6b6-5c91ea535e1c/steps/abeba093-c5a0-435b-9ee6-059218d21ae2/e252d94b-4ced-47c0-911f-81cde203b925.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/f41c3c2b-6ad1-4185-a6b6-5c91ea535e1c/steps/1d68ffa5-9255-48dc-9c1a-bfd8b9893499/c3502c32-9d50-44cf-82a9-cb989e3243f6.png)

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
         <td>Rule</td>
         <td>This determines the criteria for triggering the particular workflow module. When the selected rule is evaluated, and is satisfied, the workflow proceeds as provided. If not, the workflow will not be executed. <strong>Note: If no rules are set, the workflow defaults to its standard action without any rule. For more information about the rules, refer <a href="/verify/docs/EV/ControlIntegration/WorkflowsLevel/">here</a>.</strong></td>
      </tr>
   </tbody>
</table>

*4* Then, click on the **Configs** tab to set the necessary configurations.

![Configs](https://images.tango.us/workflows/f41c3c2b-6ad1-4185-a6b6-5c91ea535e1c/steps/b53cd662-8392-4054-a237-a5fc8f03c4e8/a5ba5834-a7db-4349-b915-45d9867ff7ac.png)


### Manage Configs

<table>
   <thead>
      <tr>
         <th width="269">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Relying Party Entity Name</td>
         <td>Use a human-readable server name as the FIDO Relying Party. It helps users identify the server or service.</td>
      </tr>
      <tr>
         <td>Signature Algorithms</td>
         <td>Assign specific algorithms used for cryptographic signing to ensure secure communication between the client and the server.</td>
      </tr>
      <tr>
         <td>Relying Party ID</td>
         <td>The <strong>Relying Party ID (RP ID)</strong> serves as a unique identifier for the <strong>FIDO</strong> <strong>Relying Party</strong> and should correspond to the effective domain of the origin. This ID helps in identifying the entity that is requesting the verification.</td>
      </tr>
      <tr>
         <td>Attestation Conveyance Preference</td>
         <td>Attestation statements are used to verify the authenticity of the verification device and can be configured to provide varying levels of detail and privacy. This informs the system about the preferred method for generating an attestation statement. </td>
      </tr>
      <tr>
         <td>Authenticator Attachment</td>
         <td>This conveys a permissible attachment pattern.</td>
      </tr>
      <tr>
         <td>Require Resident Key</td>
         <td>A <strong>Resident Key</strong> is stored on the verification device itself, allowing for credential storage and management directly on the device rather than on the server. It determines whether a public key credential should be created as a <strong>Resident Key.</strong> </td>
      </tr>
      <tr>
         <td>User Verification Requirement</td>
         <td>Instructs to confirm the verification of a user.</td>
      </tr>
      <tr>
         <td>Timeout</td>
         <td>Sets the maximum time, in seconds, allowed for generating a user's public key credential. This ensures the process completes within a reasonable timeframe. If set to 0, there is no timeout applied, and the process will wait indefinitely.</td>
      </tr>
      <tr>
         <td>Avoid Same Authenticator Registration</td>
         <td>Prevents the registration of a workflow that is already in the system.</td>
      </tr>
      <tr>
         <td>Acceptable AAGUIDs</td>
         <td>Lists the <strong>Authenticator Attestation GUIDs (AAGUIDs)</strong> that are acceptable for registration. This setting ensures that only recognized and trusted authenticators can be registered, enhancing security by avoiding the use of unverified or duplicate authenticators.</td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on the `SAVE` button.

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### Expected Result

Based on the provided configuration, the information that will be fetched for **FIDO Register** includes the following parameters:

| Name | Description |
|------|-------------|
| `fido_registered` | This indicates whether **FIDO** registration has been completed. The value will be "True" if the registration is successfully completed and "False" if it is not.|
| `allow_new_device` | Indicates whether a user is permitted to access the service or application from a new or unrecognized device. If set to true, the user can use a new device; if false, the user may be restricted to previously authorized devices.|
| `fido_registered_at` | This provides a timestamp, in Unix epoch format, indicating the exact time when the **FIDO** registration was completed. This allows you to know precisely when the registration event occurred. |

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Begin by granting permission to your device and proceed to execute using **Fingerprint** or **Password.**

![FIDO Setup](https://images.tango.us/workflows/f41c3c2b-6ad1-4185-a6b6-5c91ea535e1c/steps/d724430f-7b9c-46d9-a7db-efd4699d4d81/b42ce521-8c1c-46a9-9a81-b272c11fae1a.png)

*5* The verification process will be completed if the provided data is valid.

![Completed](https://images.tango.us/workflows/f41c3c2b-6ad1-4185-a6b6-5c91ea535e1c/steps/c2c06e51-c5ed-4df7-b0e2-8531e162be7e/b3b1652c-6975-4dc0-8742-3bec4369c625.jpeg)

*6* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)


*7* Here is a sample response for **FIDO Register:**

```json
{
   "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
   "user_id": "mark@gmail.com",
   "workflow": "verification",
   // highlight-start
   "fido_registered": "true/false",
   "fido_registered_at": "1719569072",
   "allow_new_device": "true/false"
   // highlight-end
}
```