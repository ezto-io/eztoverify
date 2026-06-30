---
id: Hook
title: Hook
sidebar_label: Hook
--- 

import TabItem from "@theme/TabItem";
import Tabs from '@theme/Tabs';

The **Hook** result notification method allows you to define how the result should be shared. Hooks are used to intercept events and perform additional actions or modifications before or after the event occurs.

### Access Application

*1* Navigate to **Workspace,** and click on **Applications.**

*2* Choose the application you wish to execute the flow with configuration as **Hook.**

*3* Click on the **Configuration** tab, and select  **Hook** in the **Result Notification Way.**

![Hook](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/321c990c-1123-484e-a2f2-846a378de4c1/e1b7c863-6ad4-49c1-b9d1-0759d3d07854.png)

*4* You will be displayed with the **Hook** settings. There are six **Hook** notification way available:

![Hook Settings](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/23915094-7edd-4843-93b7-9ef4fce820e1/4cccd252-740b-4078-902a-38c0645bf1e4.png)


<Tabs>
<TabItem value="Poll" label="Poll" default>


**Poll** involves regularly checking endpoint for updates or results. This method requires sending requests at specified intervals to retrieve the latest information or determine if any updates have occurred since the last check.

#### How it Works

*1* Set up the **Result Notification Way** as `Hook` and select **Poll** as the method for hook notifications.

![Poll](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/fa06f3a5-3389-433c-ae0c-a3c334ca8d7e/cf80a34e-06fa-4e44-8f1c-15bceb1754ee.png)

#### Manage Poll

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
         <td>Hook Notification Way</td>
         <td>When using the <strong>Poll</strong> method for hook notifications, configure the following settings.</td>
      </tr>
      <tr>
         <td>Poll Interval<span class="required-text">*</span></td>
         <td>Set the interval in seconds between each poll request to check for updates.</td>
      </tr>
      <tr>
         <td>Notification Response Type</td>
         <td>Choose the type of notification response required from the available options, i.e, Plaintext, Encrypted and Token.</td>
      </tr>
   </tbody>
</table>

*2* After configuring, ensure to `SAVE` the configurations.

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the [**cURL commands**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands), or can simply test the flow using the test mode.

*3* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*4* Upon initiating the API **(Send Request)** call, (copy the `auth_req_id` from the API response body to the clipboard) a verification request will be sent to your configured **Verification Way** settings.

*5* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*6* After completing the verification process, view the exact **response** type with **Get Result cURL** command, and initiate the API**(Get Result)** call with the copied `auth_req_id`

> Note that, each results will be different based on the configured **Hook** type for **Result Notification Type.**

*7* Below is a sample response:

```json
{
  "requestId":"ef80c15a-d16e-40d1-b60d-bcb0034c0a97",
  "phone_verified":"true",
  "phone_verified at": "1692885766",
  "phone_region": "in",
  "userId":"8f923852-d621-43b1-8268-f4e8c2792119",
}
```

*8* To view a detailed response for `Poll` **Hook type,** refer to our [API Builder.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

</TabItem>

<TabItem value="Ping" label="Ping">

**Ping** is employed to verify the accessibility and response duration of a network-connected device or server. In simpler terms, it will alert you once the authentication process has been finalized.

#### How it Works

*1* Set up the **Result Notification Way** as `Hook` and select **Ping** as the method for hook notifications.

![Ping](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/5af6380a-7ad5-4f60-b445-935add895795/8e0e748e-017f-4dc3-8dbe-5471b583153a.png)

#### Manage Ping

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="250">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Hook Notification Way</td>
         <td>When using the <strong>Ping</strong> method for hook notifications, configure the following settings.</td>
      </tr>
      <tr>
         <td>Hook URL<span class="required-text">*</span></td>
         <td>Specify the URL (endpoint, such as a <strong>webhook AP</strong>I) where the result will be sent using the hook notification. This endpoint should support the <code>POST</code> method.</td>
      </tr>
      <tr>
         <td>Auth Token</td>
         <td>If the specified URL requires an authentication token for access, provide the token here.</td>
      </tr>
      <tr>
         <td>Notification Response Type</td>
         <td>Choose the type of notification response required from the available options.</td>
      </tr>
   </tbody>
</table>

*2* After configuring, ensure to `SAVE` the configurations. '

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the  [**cURL commands**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands), or can simply test the flow using the test mode.

*3* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*4* Upon initiating the API **(Send Request)** call, (copy the `token_endpoint` from the API response body to the clipboard) a verification request will be sent to your configured **Verification Way** settings.

*5* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*6* After completing the verification process, review the endpoint configured above for the `auth_req_id`

*7* After obtaining the `auth_req_id,` view the exact **response** type with **Get Result cURL** command, initiate the API**(Get Result)** call with the copied `auth_req_id`

> Note that, each results will be different based on the configured **Hook** type for **Result Notification Type.**

*8* Below is a sample response:

```json
{
  "requestId":"ef80c15a-d16e-40d1-b60d-bcb0034c0a97",
  "phone_verified":"true",
  "phone_verified at":"1692885766",
  "phone_region": "in",
  "userId":"8f923852-d621-43b1-8268-f4e8c2792119",
}
```

*9* To view a detailed response for `Ping` **Hook type,** refer to our [API Builder.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)


</TabItem>


<TabItem value="Push" label="Push">

Once a task or process has finished, you can review its outcomes by making a request to the designated endpoint where the results are available. This endpoint typically holds the data generated from the completed process, allowing you to retrieve and analyze the relevant information.

#### How it Works

*1* Set up the **Result Notification Way** as `Hook` and select **Push** as the method for hook notifications.

![Push](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/b88b63d3-4c30-41f7-9def-de9d10403a7b/c4edf39b-c419-4e15-989c-aeb64f80fbdc.png)

#### Manage Push

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
         <td>Hook Notification Way</td>
         <td>When using the <strong>Push</strong> method for hook notifications, configure the following settings.</td>
      </tr>
      <tr>
         <td>Hook URL<span class="required-text">*</span></td>
         <td>Specify the URL (endpoint, such as a <strong>webhook AP</strong>I) where the result will be sent using the hook notification. This endpoint should support the <code>POST</code> method.</td>
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

*2* After configuring, ensure to `SAVE` the configurations. 

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the  [**cURL commands**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands), or can simply test the flow using the test mode.

*3* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*4* Upon initiating the API **(Send Request)** call, a verification request will be sent to your configured **Verification Way** settings.

*5* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*6* After completing the verification process, review the endpoint configured above for the **response.**

![Push response](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/2bae8acc-81c4-40a0-9830-8b8a5dd3dca6/84e03555-f89b-43ee-8a02-6a9d9c1c37c0.png)

> **Push** result can be checked only through the configured **Hook endpoint.**

*7* To view a detailed response for `Push` **Hook type,** refer to our [API Builder.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)


</TabItem>

<TabItem value="Mobile SDK" label="Mobile SDK">

The **Mobile SDK** supports verification only when "**Mobile"** is configured as the **Verification Way.**

#### Prerequisites

* Download the **ezto verify mobile** app before proceeding with the flow.

#### How it Works

*1* Set up the **Result Notification Way** as `Hook` and select **Mobile SDK** as the method for hook notifications.

![Mobile SDK](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/7ed2d971-95da-44dc-a463-1063a298f524/7e767edf-0ee8-4713-b586-7163fa3f936f.png)

#### Manage Mobile SDK

<table>
   <thead>
      <tr>
         <th width="258">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Hook Notification Way</td>
         <td>When the <strong>Mobile SDK</strong> method for hook notifications is selected, the following configuration will be displayed.</td>
      </tr>
      <tr>
         <td>Notification response type</td>
         <td>Notification will always be the <code>auth_request_id</code> for security reasons.</td>
      </tr>
   </tbody>
</table>

> **QR** is configured  as the **Verification Way** in this flow. To know how `Push` for **Mobile** works, refer [here.](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile/)

*2* After configuring, ensure to `SAVE` the configurations. 

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the  [**cURL commands**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands).

*3* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*4* Upon initiating the API **(Send Request)** call, a verification request will be sent to your configured **Verification Way** settings. In this flow, it is a **QR code** link in the APIs response body.

*5* Based on the request, copy the **QR code** execute the flow in an incognito browser nd proceed to scan the **QR** in the **ezto verify mobile app.**

![QR](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/ac90eff2-514e-40f4-8b1e-71fa2a8a3362/3d72de77-e6e8-4f23-bb16-b0a01f688e60.png)

*6* The flow will be executed in the mobile app and the verification will be completed.

![Verification Completed](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/527db2ff-0984-47ee-80b2-190c756a9178/02a25a89-0695-4a91-9112-8c3084914f13.png)

*7* The `auth_req_id` will be copied to the clipboard.

*8* After obtaining the `auth_req_id,` view the exact **response** type with **Get Result cURL** command, initiate the API**(Get Result)** call with the copied `auth_req_id`

*9* Below is a sample response:

```json
{
  "request_id": "4366b1f5-ef05-44e0-9a28-138e975d6f8f",
  "email_verified": "true",
  "user_id": "020f44c9-6c97-49b4-8b7c-5ed6755f5b35",
  "workflow": "verification",
  "email_verified_at": "1708758780",
  "email": "mark@gmail.com"
}
```

*10* To view a detailed response for `Mobile SDK` **Hook type,** refer to our [API Builder.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

</TabItem>

<TabItem value="Desktop App" label="Desktop App">

This verification will take place in the **ezto verify desktop** application.

#### Prerequisites

* Download the **ezto verify desktop** app before proceeding with the flow.

#### How it Works

*1* Set up the **Result Notification Way** as `Hook` and select **Desktop app** as the method for hook notifications.

![Desktop](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/49695d33-f64e-4af9-b7c6-261d30fb4f78/437f12ef-23df-4576-a622-0d664bd1824c.png)

#### Manage Desktop

<table>
   <thead>
      <tr>
         <th width="258">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Hook Notification Way</td>
         <td>When the <strong>Desktop app</strong> method for hook notifications is selected, the following configuration will be displayed.</td>
      </tr>
      <tr>
         <td>Notification response type</td>
         <td>Notification will always be the <code>auth_request_id</code> for security reasons.</td>
      </tr>
   </tbody>
</table>

*2* After configuring, ensure to `SAVE` the configurations.

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the  [**cURL commands**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands), or can simply test the flow using the test mode.

*3* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*4* Upon initiating the API **(Send Request)** call, a verification request will be sent to your configured **Verification Way** settings. In this flow, it is a `transaction_link` or a `deep_link` in the APIs response body.

> * `transaction_link:` Opening this in the browser will prompt you to open the ezto verify desktop app.(this takes place, only if the app is present in the desktop.)
> * `deep_link:`Opening this in the browser will open the desktop app if available in the system, if not you will be directed to the app store for download.

*5* After doing either of the above two, the flow will be executed in the desktop app and the verification will be completed.

*6* The `auth_req_id`  will automatically copied to the clipboard.

*7* After obtaining the `auth_req_id,` view the exact **response** type with **Get Result cURL** command, initiate the API**(Get Result)** call with the copied `auth_req_id`

*8* Below is a sample response:

```json
{
  "request_id": "4366b1f5-ef05-44e0-9a28-138e975d6f8f",
  "email_verified": "true",
  "user_id": "020f44c9-6c97-49b4-8b7c-5ed6755f5b35",
  "workflow": "verification",
  "email_verified_at": "1708758780",
  "email": "mark@gmail.com"
}
```

    
</TabItem>

<TabItem value="Web SDK" label="Web SDK">

This only works when **SDK** is configured in **Verification Way.** The notification process can take place on any host server that is configured in the **Whitelist Host URL**.

#### How it Works

*1* Set up the **Result Notification Way** as `Hook` and select Web **SDK** as the method for hook notifications.

![Web SDK](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/4b9e01bf-3df8-4e72-8042-8f26f477a6e0/fc70ea81-4dfa-4635-ae6b-a12ac4a0d965.png)

#### Manage Web SDK

<table>
   <thead>
      <tr>
         <th width="258">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Hook Notification Way</td>
         <td>When the <strong>Web SDK</strong> method for hook notifications is selected, the following configuration will be displayed.</td>
      </tr>
      <tr>
         <td>Notification response type</td>
         <td>Notification will always be the <code>auth_request_id</code> for security reasons.</td>
      </tr>
   </tbody>
</table>

*2* After configuring, ensure to `SAVE` the configurations. 

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the  [**cURL commands**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands), or can simply test the flow using the test mode.

*3* Paste your **Whitelist Host URL** in an incognito browser that you have configured in the **Verification Way,** and execute the flow.

*4* You will be directed to the configured verification flow. Below is a sample:

> The UI is an example of how the transaction flow starts, you can always customize as per your need in your `.html` file.

![Sample flow](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/f1ab3a1d-5c12-4146-b236-e907e9ddc0cc/7973ff50-98d7-4182-9ddc-4237091f22cb.png)

*5* Provide the necessary details and click on **Submit.** Once the flow is completed, the page will close by itself, and The `auth_req_id` will be copied to the clipboard.

![Verification Completed](https://images.tango.us/workflows/f840d8d5-8c5e-4c7d-b65f-f5386a3d1c02/steps/a56c89f3-c4c1-4892-a856-efa22058c0db/c3a13c28-4476-4f47-8014-2a66afd87099.png)

*6* After obtaining the `auth_req_id,` view the exact **response** type with **Get Result cURL** command, initiate the API**(Get Result)** call with the copied `auth_req_id`

*7* Below is a sample response:

``` json
{
  "request_id": "4366b1f5-ef05-44e0-9a28-138e975d6f8f",
  "email_verified": "true",
  "user_id": "020f44c9-6c97-49b4-8b7c-5ed6755f5b35",
  "workflow": "verification",
  "email_verified_at": "1708758780",
  "email": "mark@gmail.com"
}
```
    
</TabItem>
</Tabs>
