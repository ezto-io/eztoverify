---
id: DynamicForm
title: Dynamic Form
sidebar_label: Dynamic Form
--- 

import DropBox from "../../../src/components/docs/DropBox"
import TabItem from "@theme/TabItem";
import Tabs from '@theme/Tabs';

With **Dynamic Form,** you have the flexibility to customize the form or template according to your specific requirements using a customized **JSON** structure.

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Dynamic Form.** If you need understanding xin creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Dynamic Form** to add it.

![Dynamic Form](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/5e8f2b95-755a-4d49-81d8-810cfb7ced79/19b61e52-4fec-4f3e-9342-1972a6b68772.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/190ad25d-f8ca-4993-9b91-82e427a97a40/b41b025f-346d-4cf9-9b71-4b7538ecc9ae.png)

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

![Configs](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/bcda9a0c-1d73-4f13-9df1-cb1716a641ee/019d9b79-c67d-4545-aaf4-40b292f4d6c3.png)

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
         <td>Form Title<span class="required-text">*</span></td>
         <td>Specify the title for the form. Only alphanumeric characters and spaces <code>"a-zA-Z0-9"</code> are allowed.</td>
      </tr>
      <tr>
         <td>Input JSON<span class="required-text">*</span></td>
         <td>Provide a valid <strong>JSON</strong> format specifying the fields you want to display on the verification page. This customization allows you to fetch additional user verification data, the supported field types you can include in the script are: text, checkbox, date, file, radio, button, capture and option. Ensure the script is enclosed within an array <code>[]</code></td>
      </tr>
   </tbody>
</table>

<DropBox title="Here's how you can build your script">

To customize the UI for collecting additional information through the **Dynamic Form** workflow, you need to modify the **JSON** script accordingly.

For example, if a bank wants to verify your identity with extra details such as your **mother's name, nationality, birth month,** and the **date** when you opened your account, you must adjust the **JSON** script to display these fields in the UI.

To achieve this, you need to build a **JSON** script that includes the required attributes. Here is a set of available attributes to create the **JSON** script:

* `name:` The name attribute is used to uniquely identify the input field within the form. It acts as an internal reference or key for the field, which can be used to retrieve the submitted data. This attribute is mandatory and must be unique for each field in the form. Supported characters for the `name` attribute are alphanumeric values and underscores `"a-zA-Z0-9_"`
* `label:`The label attribute specifies the text that will appear next to the input field. It serves as a description or prompt, indicating what information is required.
* `type:` The type attribute defines the type of input field. It determines the kind of data the field will accept. Supported types include **checkbox**, **text**, **date**, **option**, **file**, **radio**, **button**, **phone number**, and **capture**.

<Tabs>
<TabItem value="Checkbox" label="Checkbox" default>

A **checkbox** allows you to select or deselect one or more options. It is typically used for choices to be selected.

Note that you can customize this according to your requirements, ensure it is valid and appropriate.

When you provide this type, make sure to give choice of values in the "options" attributes.

**Here's how it should be in the script:**

```json
[
  { 
    "name": "ethnicity",
    "label": "American",
    "type": "checkbox",
    "options": "yes,no",
    "isRequired": true
  } 
]
```

**The following will appear in the UI:**

![Checkbox](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/c5567aa9-87c0-4e19-8afa-1eb3799b6f5a/ac2ca262-4115-4170-89ba-61de3ad62474.png)

The script ensures that you can select either "yes" or "no" for the "American" checkbox.
</TabItem>

<TabItem value="Text" label="Text">

A **text** field allows you to input a text. It can be used for various types of input, such as names, addresses, or any other short text.

Note that you can customize this according to your requirements, ensure it is valid and appropriate.

**Here's how it should be in the script:**

```json
[
  {
    "name": "mother_name",
    "label": "Mother Name",
    "type": "text",
    "placeHolder": "Enter your mother's name",
    "isRequired": true,
    "regex": "^[A-Za-z\\s]+$"
  }
]  
```

**The following will appear in the UI:**

![Text](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/c4b24bf6-59e6-44f8-a713-2d0b50ab6017/7dfcac5f-e3bb-41c2-bc77-ae86c688d57f.png)

The script ensures that you can input your mother's name in a text field, with the placeholder guiding on what to enter.

</TabItem>

<TabItem value="Date" label="Date">

A **date** field allows you to select a date from a date picker. It is used for inputs like birthdates, transaction dates, or any other date-related information.

Note that you can customize this according to your requirements, ensure it is valid and appropriate.

**Here's how it should be in the script:**

```json
[
  {
    "name": "opening_date",
    "label": "Enter Account Opening Date",
    "type": "date",
    "isRequired": true
  } 
]
```

**The following will appear in the UI:**

![Date](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/90fdde95-a1ed-4eea-8933-796d80709edc/f52f4420-5c35-4b8f-91e9-850a27c93802.png)

The script ensures that you can select the date using a date picker, with the placeholder guiding them on what to do.

</TabItem>

<TabItem value="Option" label="Option">

An **option** field, often represented as a dropdown menu, which allows you to select one value from a predefined set of options. If you want to choose multiple values, simply add the "multiple" attribute.

Note that you can customize this according to your requirements, ensure it is valid and appropriate.

**Here's how it should be in the script:**

```json
[
  {
    "name": "transaction_months",
    "label": "Choose Transaction Months",
    "type": "option",
    "options": "January,February,March,April,May,June,July,August,September,October,November,December",
    "helperText": "Select the months you want to get the transaction details on.",
    "isRequired": false,
    "multiple": true
  }
]
```

**The following will appear in the UI:**

![Options](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/5ada8ccf-d923-47bc-be39-abe5440262fb/a82cad4c-601e-4cac-bdef-b90adee61746.png)

The script ensures that you can select the month from a dropdown menu with the provided options.
</TabItem>

<TabItem value="File" label="File">

A **file** field represents a file upload input, allowing you to select one or more files from your device. You can specify the types of files that are allowed and whether multiple files can be selected.

**Supported file types:** `jpeg`, `jpg`, `gif`, `svg`, `png`, `pdf`, `doc`, `docx`, `xls`, `xlsx`, `txt`, `mp4`, `mov`, `ppt`, `pptx`, `mp3`

**File size limit:** Each file and the combined size of all selected files must not exceed 20 MB.

**Here's how it should be in the script:**

```json
[
  {
    "name": "id_documents",
    "label": "Upload ID Documents",
    "type": "file",
    "multiple": true,
    "onlyAccept": "pdf,jpeg,jpg,png,txt",
    "helperText": "Make sure you upload the appropriate IDs to be checked and verified.",
    "isRequired": false
  }
]
```

**The following will appear in the UI:**

![File](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/3678122c-b313-4fbe-828f-e59e7d287502/617621c9-51c6-41c7-87a2-f527dbec3ea6.png)

This script ensures that you can upload one or more files from the device, with restrictions on file types if specified.

</TabItem>

<TabItem value="Radio" label="Radio">

A **radio** field allows you to select one option from a predefined set of choices. Radio buttons are used when only one selection is allowed from a group of options.

**Here's how it should be in the script:**

```json
[
  {
    "name": "gender",
    "label": "Select Gender",
    "type": "radio",
    "options": "Male,Female,Other",
    "isRequired": false
  }
]
```

**The following will appear in the UI:**

![Radio](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/bc30639d-b9dc-444f-b7e7-45f1e507e691/ff6b2bb4-4ac4-470c-9505-e0180ae41dd7.png)

This script will display a group of radio buttons allowing you to choose one option from the provided set.

</TabItem>

<TabItem value="Capture" label="Capture">

A **capture** field is used to collect media directly from a device, such as taking photos with a camera. This is useful in scenarios where you need real-time input from the user, like verifying identity or capturing evidence. You can add the `includeMetdata` key type as **true** or **false**, if given true, the image detail will be fetched along with the image data.

The `facingMode` key can be specified to select either the front or back camera. By default, mobile devices will use the back camera, while web applications will default to the front camera.

**Here's how it should be in the script:**

```json
[
 {
        "name": "camera",
        "label": "Camera Capture",
        "type": "capture",
        "includeMetadata": false,
        "facingMode": "front",
        "isRequired": false
  }
]
```

**The following will appear in the UI:**

![Capture](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/9c225246-59bd-44d4-a0ff-e2ae1bace197/f344adc9-de55-4a2d-8ff4-59aceaec6d8f.png)

This script ensures that the user can take a photo with their device, with the front and back facing mode.

</TabItem>

<TabItem value="Phone Number" label="Phone Number" default>

A **phone number** field is used to collect phone numbers, which can be used in workflows requiring phone verification. If a **Verify Phone** workflow follows the dynamic form, you can prefill the phone number field using the `name` attribute, such as `"name": "phone_number_prefill"`. This makes the phone number available for verification.

You can customize the field to be required or use the default values if needed.

**Here's how it should be in the script:**

```json
[
 {
        "name": "phone_number_prefill",
        "label": "Enter Phone Number",
        "type": "phonenumber",
        "isRequired": true
 }
]
```

**The following will appear in the UI:**

![Phone Number](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/37b6c13f-b4cf-4d73-bbe3-c52950995b10/0d931bd5-9c38-441f-93b3-c7fe851bd896.png)

This script ensures users can enter or verify their phone numbers with proper validation. 

</TabItem>

<TabItem value="Text Area" label="Text Area">

A **text area** field allows users to enter multiple lines of text, making it useful for capturing detailed input like addresses, descriptions, or comments. 

**Here's how it should be in the script:**

```json
[
  {
    
    "name": "address",
    "label": "Enter Address",
    "type": "textarea",
    "placeHolder": "Enter your full address",
    "isRequired": false
  }
]
```

**The following will appear in the UI:**

![Text Area](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/6a180fac-0c37-462f-abd3-0a29cab476d0/d9af957d-10a7-438c-9394-74606d7f8f03.png)

This script ensures that users can enter long-form text in a multi-line input box, making it ideal for capturing detailed information.

</TabItem>

<TabItem value="Button" label="Button" default>

A **button** is an interactive element that performs an action when clicked. It is typically used to trigger form submission or other events within the UI. 

You can customize the appearance by adding the button color and text color and behavior of the button based on your requirements.

**Here's how it should be in the script:**

```json
[
 {
        "name": "reject",
        "label": "Cancel",
        "type": "button",
        "buttonColor": "grey",
        "textColor": "black"
  },
  {
        "name": "confirm",
        "label": "Proceed",
        "type": "button",
        "buttonColor": "blue",
        "textColor": "white"
  }
]
```

**The following will appear in the UI:**

![Button](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/34207c76-29b2-4c3b-89ca-7790b2a49164/8263ac9e-246e-4e6c-8613-9268acd0b04c.png)

In this scenario, the script creates two buttons labeled "Proceed" and "Cancel." When either custom button is clicked, it will return a value of true for the selected button.

</TabItem>

</Tabs>

* `options:` The options attribute provides a set of predefined choices. This is applicable only to certain input `types` like **checkbox**, **option** and **radio**. It defines the values that you can select from.
* `placeHolder:` The placeholder attribute displays a hint or example in the input field to guide you on what to enter. It disappears once you start typing. This is only applicable and optional if the `type` is **text**.
* `helperText:` The helper text attribute provides additional guidance or instructions for the input field. It is displayed below the input field to help you understand what is required.
* `isRequired:` This attribute indicates whether the input field is mandatory. If set to true, you must fill in this field before submitting the form.
* `regex:` This attribute specifies a regular expression that the input must match to be considered valid. It is used for input validation. This is only applicable and optional if the `type` is **text**.
* `regexError:` This attribute specifies the error message displayed when the input does not match the defined `regex` pattern. It helps users understand what is wrong with their input and how to correct it. This is only applicable and optional if the `type` is **text** and `regex` is defined. If no custom message is provided, a default error message will be used.
* `multiple:` This attribute is used with input types like **file** or **option** to allow the selection of multiple items. When set to true, select more than one file or option at a time. 
* `onlyAccept:` This attribute specifies the types of files or values that are allowed. It is applicable if the input `type` is **file**.
* `dependsOn:` This attribute specifies the conditions under which the field will be shown. It is an array that includes the **field** and its **value**. The field will only be displayed when the condition is met.
* `validationTypeForDependsOn:` This attribute determines how multiple conditions in `dependsOn` should be evaluated. Use **"all_true"** if all conditions must be true, or **"any_true"** if at least one condition can be true.
* `defaultValue:` This attribute pre-fills an input field, commonly used for **text** fields. It can also automatically display values from previous workflows. For example, if a **Verify PAN** workflow has been completed, you can display the **PAN** number in the form by setting:

```json
"defaultValue": "${pan_no}"
```
* `editable:` This attribute defines whether the field is editable or not. If set to `false`, the field will be displayed as read-only. Only can be used when `defaultValue:` is used.
* `includeMetadata:` This attribute determines whether metadata like the image format should be included when capturing photo.
* `facingMode:` This attribute specifies which camera to use when capturing media. The value can be either `"front"` (for selfie mode) or `"back"` (for the rear camera).
* `buttonColor:` This attribute allows you to define the color of the button. It is optional and used to customize the button's appearance. It is applicable if the input `type` is **button**.
* `textColor:` This attribute allows you to specify the text color for the button, adding further customization options. It is applicable if the input `type` is **button**.

<DropBox title="Here's a sample JSON script with all attributes filled out">

```json
[
  {
    "name": "ethnicity",
    "label": "American",
    "type": "checkbox",
    "options": "yes,no",
    "isRequired": true
  },
  {
    "name": "mother_name",
    "label": "Mother Name",
    "type": "text",
    "placeHolder": "Enter your mother's name",
    "isRequired": true,
    "regex": "^[A-Za-z\\s]+$",
    "regexError": "Only letters and spaces are allowed."
  },
  {
    "name": "age",
    "label": "Enter Your Age",
    "type": "text",
    "placeHolder": "Enter your correct age",
    "isRequired": true,
    "defaultValue": "18",
    "editable": true
  },
  {
    "name": "opening_date",
    "label": "Enter Account Opening Date",
    "type": "date",
    "isRequired": true
  },
  {
    "name": "transaction_months",
    "label": "Choose Transaction Months",
    "type": "option",
    "options": "January,February,March,April,May,June,July,August,September,October,November,December",
    "helperText": "Select the months you want to get the transaction details on.",
    "isRequired": false,
    "multiple": true
  },
  {
    "name": "id_documents",
    "label": "Upload ID Documents",
    "type": "file",
    "multiple": true,
    "onlyAccept": "pdf,jpeg,jpg,png,txt",
    "helperText": "Make sure you upload the appropriate IDs to be checked and verified.",
    "isRequired": false
  },
  {
    "name": "gender",
    "label": "Select Gender",
    "type": "radio",
    "options": "Male,Female,Other",
    "isRequired": false
  },
  {
    "name": "occupation",
    "label": "Select Employment",
    "type": "radio",
    "options": "Salaried, Self Employed",
    "isRequired": false
  },
  {
    "name": "phone_number_prefill",
    "label": "Enter Phone Number",
    "type": "phonenumber",
    "isRequired": true
  },
  {
    "name": "camera",
    "label": "Camera Capture",
    "type": "capture",
    "includeMetadata": false,
    "facingMode": "front",
    "isRequired": false
  },
  {
    "name": "address",
    "label": "Enter Address",
    "type": "textarea",
    "placeHolder": "Enter your full address",
    "isRequired": false
  },
  {
    "name": "cancel",
    "label": "Cancel",
    "type": "button",
    "buttonColor": "grey",
    "textColor": "black"
  },
  {
    "name": "confirm",
    "label": "Proceed",
    "type": "button",
    "buttonColor": "#blue",
    "textColor": "white",
    "dependsOn": [
      {
        "field": "gender",
        "value": "Female"
      },
      {
        "field": "ethnicity",
        "value": "yes"
      }
    ]
  }
]
```
</DropBox>

</DropBox>

<br/>

> **Dynamic Form** to collect inputs such as aadhaar number, pan number, email, and phone number, following it with respective workflows like Verify Aadhaar, [Verify Phone,](/docs/EV/WorkflowModules/Mobile%20Number/Verification/OTP) Verify PAN, or [Verify Email,](/docs/EV/WorkflowModules/Email/OTP) the information entered in the **Dynamic Form** does not need to be re-entered during the verification process. Instead, it can be automatically prefilled in the next workflow step using specific keys configured in the Dynamic Form itself.
>
>**To enable this prefill functionality, use the following prefill keys:**
>
- Verify PAN - `pan_number_prefill` 
- Verify Aadhaar and Verify Aadhaar with OTP - `aadhaar_number_prefill` 
- [Verify Phone Number](/docs/EV/WorkflowModules/Mobile%20Number/Verification/OTP) - `phone_number_prefill` 
- [Verify Email](/docs/EV/WorkflowModules/Email/OTP) - `email_prefill` 
>
>By setting up these keys, the details from the Dynamic Form are automatically populated when the process moves to the next workflow, reducing manual input.
>
>Note that, for **Verify Email** and **Verify Phone** workflows triggered via API tool(Postman), if email or phone details are included in the user metadata of the request body, the system will validate those details at the **API level first.** If no values are present in the metadata, the inputs collected from the **Dynamic Form** will be used for verification.

<br/>

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

### How it Works

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* Begin by providing the necessary details, click on **Proceed.**

> Please note that these buttons are customizable.

![Verification](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/53cdd486-5dec-40b9-8fd1-1d2433740116/a20686ba-91bb-4705-bfcc-d41b66ad574d.png)

*5* The verification process will be completed.

> **If "force onboarding" is enabled in the API request, all users, regardless of status, will be directed to the onboarding flow first, followed by the verification flow if configured.**

![Completed](https://images.tango.us/workflows/b5ec45bb-117f-449f-8ec7-a471069e903a/steps/dd5331f7-be5e-4ad7-93a7-a635fa41e26a/62c17186-532e-4612-ba18-cdd7637b5e6e.png)

*6* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*7* Here is a sample response for **Dynamic Form:**

```json
{
  "request_id": "bec4127f-eac8-4245-9bb1-63f9f50a41b5"
  "user_id": "mark@gmail.com",
  "workflow": "verification",
  // highlight-start
  "transaction_months": "August, October",
  "confirm": "true",
  "occupation": "Salaried",
  "gender": "Female",
  "ethnicity": "yes",
  "details_from_documentation_verification": "true",
  "mother_name": "Jane Doe",
  "age": "18",
  "phone_number_prefill_dialcode": "+91",
  "phone_number_prefill": "73xxxxxxxxx",
  "opening_date": "04/08/2000"
  // highlight-end
}
```