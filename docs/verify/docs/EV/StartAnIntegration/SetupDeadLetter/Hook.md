---
id: Hook
title: Hook
sidebar_label: Hook
--- 

The **Hook** dead letter method ensures that when a flow is incomplete, the result is still sent to the designated WebHook, where users can retrieve the result of a discontinued flow, allowing to continue from where they left off. This function ensures that users are aware they have discontinued the flow.

> Note that for onboarding flows, the flow will pick up from where it stopped. For verification flows, the process will begin again from the start.

### Access Application

*1* Navigate to **Workspace,** and click on **Applications.** 

*2* Choose the application you wish to execute the flow with configuration as **Hook.**

*3* Click on the **Configuration** tab, and select **Hook** in the **Dead Letter.**

![Hook](https://images.tango.us/workflows/74b4405c-eb58-428b-b44f-09c3b54eb3d2/steps/5866e952-607d-44c7-a019-5a9a3324bbba/fd47747b-3d19-4b99-896b-9eb2c519a4b2.png)

*4* You will be displayed with the **Hook** settings. Enable the toggle to configure the notifications. 

![Hook Settings](https://images.tango.us/workflows/74b4405c-eb58-428b-b44f-09c3b54eb3d2/steps/d21b4694-4311-4920-91b4-537104ed8f10/385136fe-dac7-4e76-9b8e-3da1deb8a996.png)

### Hook settings

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="252">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Enable</td>
         <td>Enable the toggle to configure the <strong>Hook</strong> settings.</td>
      </tr>
      <tr>
         <td>Hook URL<span class="required-text">*</span></td>
         <td>Specify the URL (endpoint, such as a <strong>webhook API</strong>) where the result will be sent using the hook notification. This endpoint should support the <code>POST</code> method.</td>
      </tr>
      <tr>
         <td>Auth Token</td>
         <td>If the specified URL requires an authentication token for access, provide the token here.</td>
      </tr>
      <tr>
         <td>Notification response type</td>
         <td>Choose the type of notification response required from the available options.</td>
      </tr>
      <tr>
         <td>Hook failure threshold</td>
         <td>Admin users will receive an email notification if the hook fails to deliver the result within the specified threshold.</td>
      </tr>
   </tbody>
</table>

*5* After configuring, ensure to `SAVE` the configurations.

### How it Works

When a triggered transaction cannot be completed, the **Hook** method ensures that the current status of the flow is sent to the configured **WebHook**. This allows admins to retrieve the result and determine whether to proceed with the flow using the partial data.

**Here’s an example of a response:**

```json
{
  "sp@aadhaar_verified_at": "1729172831",
  "sp@workflow": "verification",
  "sp@request_id": "xxxxxxxxx-xxxx-xxx-xxx-xxxxxxxxx",
  "sp@user_id": "mark@gmail.com",
  // highlight-start
  "sp@aadhaar_verified": "true",
  "sp@transaction_status": "pending",
  "sp@aadhaar_number": "xxxxx46819"
  // highlight-end
}
```

To proceed with a discontinued flow, the result from the **WebHook** must be added to the transaction API’s request body in the `user metadata` field. After updating the metadata with this result, the transaction can be triggered again, allowing the flow to continue from where it stopped.

![User Metadata](https://images.tango.us/workflows/74b4405c-eb58-428b-b44f-09c3b54eb3d2/steps/bb5ace00-a7aa-4150-b915-0a429ec43dae/fcd7fe92-a59a-4667-84b5-12c41da228bc.png?mark-x=928&mark-y=336&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz04MDcmaD01NDQmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)