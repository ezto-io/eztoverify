---
id: TermsandConditions
title: Terms and Conditions
sidebar_label: Terms and Conditions
--- 

import TabItem from "@theme/TabItem";
import Tabs from '@theme/Tabs';

**Terms and Conditions** means to review and confirm that you understand and agree to the terms and conditions associated with a particular service, product, or agreement.

You have the ability to upload PDF documents (which can be downloaded if needed), a series of images, and links.  Additionally, a text editor to allow you to provide your own content.

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Terms and Conditions.** If you need understanding  in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Terms and Conditions** to add it.

![Terms and Conditions](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/04a93478-ecfd-486d-b800-911ac645c4e8/f104f0f0-1752-480c-a15a-752199ecb3eb.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/1aba0b7b-5659-4434-a685-677eb420f584/a20f7709-1bc3-4898-ba32-9722177b9774.png)

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

![Configs](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/585a9a74-0a07-4148-b59d-a022943c9642/5498b748-6887-419a-abc2-67734baccb07.png?mark-x=808&mark-y=433&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz04MzAmaD0yNTcmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)

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
         <td>Terms and Conditions Version<span class="required-text">*</span></td>
         <td>Set the version of the <strong>Terms and Conditions</strong> to be displayed in HTML format (e.g., x.x.x). Note that each type configured must have a different version. Once created, the version and its type cannot be changed.</td>
      </tr>
      <tr>
         <td>Type<span class="required-text">*</span></td>
         <td>Set the <strong>Terms and Conditions</strong> type; such as <strong>Link, PDF, Image, Text.</strong></td>
      </tr>
   </tbody>
</table>

#### Each type has its own set of configurations and provide respective response

<Tabs>
<TabItem value="Link" label="Link">

When `Link` is configured as the type, you will be redirected to view the respective terms and conditions via the provided link.

![Link](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/0af84b9e-897d-4b92-8d24-8662f555aa7f/dff4184c-e88d-4a3a-b454-bfa5ba4aa781.png)

In the `Terms and Condition Link` field, provide the **URL** to be redirected to the terms and conditions.
</TabItem>

<TabItem value="PDF" label="PDF">

When `PDF` is configured as the type, a **PDF** containing the respective terms and conditions will be displayed.

![PDF](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/513c58ea-1cf7-4e8a-91ff-0d0d39a4149e/6a036f01-c217-4e92-a0e6-d1639906764e.png)

In the `Terms and Condition Link` field, upload the PDF to be displayed, with a maximum file size of 5 MB.
</TabItem>

<TabItem value="Image" label="Image">

When `Image` is configured as the type, an image containing the respective terms and conditions will be displayed as a PDF file.

![Image](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/ce3d344d-baf9-42eb-832e-a78011bbdb7a/c08e4c36-ff43-4619-a8c4-afedeb01995e.png)

In the `Terms and Condition Link` field, upload images with a maximum file size of 5MB. Note that SVG files are not supported.

</TabItem>

<TabItem value="Text" label="Text">

When `Text` is configured as the type, the terms and conditions will be displayed as text content.

![Text-1](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/944871fa-2f86-4607-9f2f-c805feabb966/83048e59-18b5-46b5-8d9d-1e710fc5dc06.png)

In the `Terms and Condition Link` field, click on the `ADD` button to open the text editor

![Text-2](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/b51bf02d-96b0-4701-acae-85551cc467c2/6844de68-09fc-4136-8cae-b9d228b04f75.png)

Provide the text with the necessary formatting, and then click on the `SAVE` button.


</TabItem>
</Tabs>

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* The verification page will vary based on the **Type** chosen for **Terms and Conditions.**

> On clicking **Decline,** the verification will be failed.


<Tabs>
<TabItem value="Link" label="Link">

If you have configured `Link` as the terms and conditions type, the following will be displayed.

![Link](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/921e4fac-5ea1-4f42-92ed-695ff2119099/f94b6bdc-c840-4645-963a-ab6d3a715824.png)

To view the content in a new tab, click on the link icon. Once done reviewing, check the consent box and click on the `Accept` button.
</TabItem>


<TabItem value="PDF" label="PDF">

If you have configured `PDF` as the terms and conditions type, the following will be displayed.

![PDF](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/6d02e820-004e-4cc3-8c22-f1cdf2945693/6da22be1-e14b-468b-aae6-fd72c06ee0b4.png)

To view the content in a new tab, click on the link icon. Once done reviewing, check the consent box and click on the `Accept` button.
</TabItem>

<TabItem value="Image" label="Image">

If you have configured `Image` as the terms and conditions type, the following will be displayed.

![Image](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/4dd86cee-79d4-443f-a39c-a38d04de94a3/f710a9b9-08c9-439e-8d73-9ace3cf340ab.png)

To view the content in a new tab, click on the link icon. Once done reviewing, check the consent box and click on the `Accept` button.

</TabItem>


<TabItem value="Text" label="Text">

If you have configured `Text` as the terms and conditions type, the following will be displayed.

![Text](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/fe3e1ed4-2b3a-4718-aacc-4039a7fa6eda/a625e3a4-614a-47f6-bf8d-6a0222b43d79.png)

Once done reviewing, check the consent box and click on the `Accept` button.

</TabItem>
</Tabs>

*5* The verification process will be completed.

> **If "force onboarding" is enabled in the API request, all users, regardless of status, will be directed to the onboarding flow first, followed by the verification flow if configured.**

![Verification complete](https://images.tango.us/workflows/a2c116cb-d61e-49e0-9c45-c89fdcb410eb/steps/1846a139-16b1-41dd-bdf1-5278933e0854/dbff7bb9-53b7-483b-9c0a-3d692329786f.png)

*6* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*7* Here is a sample response for **Terms and Condition:**

```json
{
    "workflow": "verification",
    "user_id": "mark@gmail.com",
    "request_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
    // highlight-start
	"terms_and_conditions_accepted_at": "1719569072",
	"terms_and_conditions_version": "x.x.x"
    // highlight-end
}
```