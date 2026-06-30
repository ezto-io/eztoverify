---
id: Web
title: Web
sidebar_label: Web
--- 

import TabItem from "@theme/TabItem";
import Tabs from '@theme/Tabs';

Verification via the **Web** involves the process of verifying a user's identity using **web-based** methods and protocols. This typically involves users providing their credentials, such as usernames and passwords, through a web interface to gain access.

> Every application execution requires an **Onboarding** and **Verification** workflow. The onboarding workflow (can be any) will be used for a new user. You can configure it as **"None"** for a user who has already been onboarded. Please note that selecting the "None" option for the onboarding workflow will automatically redirect the user to the verification workflow, irrespective of the users availability.

### Access Application

*1* Navigate to **Workspace,** and click on **Applications.**

*2* Choose the application you wish to execute the flow with configuration as **Web.**

*3* Click on the **Configuration** tab, and select **Web** in the **Verification Way.**

![Web](https://images.tango.us/workflows/8bf43ac9-7285-4ff3-b151-93ce5c78f222/steps/8f74e4be-e500-4964-bab7-cc2157782516/364a4df2-c949-42fd-a5f8-d3efbfcefcff.png)

*4* You will be displayed with default settings, i.e, **Link Expiry** and **Notification Type**, you can select your preferred **Notification Type** from the drop-down and based on the chosen, the configurations will vary.

> There is currently five methods of **Notification Type** available for **"Web" Verification Way.**


<Tabs>
<TabItem value="Email" label="Email" default>

If this method is selected as the **Notification Type,** an email containing the **transaction link** will be sent to the provided email address.

![Email](https://images.tango.us/workflows/8bf43ac9-7285-4ff3-b151-93ce5c78f222/steps/4ff75fa4-ff3e-4a6e-81e0-c460fb332611/b8be24e9-316a-41a0-9de9-380a122a10ed.png)

### Email settings

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="158">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Email Sender</td>
         <td>The default email provider will be pre-configured. If you wish to use a custom provider, please refer <a href="/verify/docs/EV/GetStarted/Configurations/Email/Overview/">here</a>.</td>
      </tr>
      <tr>
         <td>Email Subject<span class="required-text">*</span></td>
         <td>Provide the subject of the email.</td>
      </tr>
      <tr>
         <td>Email Title<span class="required-text">*</span></td>
         <td>Title of the email.</td>
      </tr>
      <tr>
         <td>Email Body<span class="required-text">*</span></td>
         <td>Body of the email.</td>
      </tr>
   </tbody>
</table>

</TabItem>


<TabItem value="SMS" label="SMS">

If this method is selected as the **Notification Type,** a transaction link will be sent through text message (SMS) to the provided phone number.


![SMS](https://images.tango.us/workflows/8bf43ac9-7285-4ff3-b151-93ce5c78f222/steps/cee20aa7-b866-4afb-9efa-b923e76c58fc/02c5f60f-ca10-4c91-b553-2ba8e0a435fb.png)

### SMS settings

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="182">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Sms template<span class="required-text">*</span></td>
         <td>Set SMS template which will be formatted to notify user. Ensure add following placeholder in template <a href="/verify/docs/EV/GetStarted/Configurations/SMS/">here</a>.</td>
      </tr>
      <tr>
         <td>SMS Sender</td>
         <td>The default SMS provider will be pre-configured. If you wish to use a custom provider, please refer <a href="/verify/docs/EV/GetStarted/Configurations/SMS/">here</a>.</td>
      </tr>
   </tbody>
</table>


</TabItem>

<TabItem value="Whatsapp" label="Whatsapp">

If this method is selected as the **Notification Type,** the transaction link will be sent via the provided **WhatsApp** number**.**

![WhatsApp](https://images.tango.us/workflows/8bf43ac9-7285-4ff3-b151-93ce5c78f222/steps/d5172945-610c-4a32-8d74-91a463103951/5c03fa4c-6f9d-4caf-895e-73ca0e1e7d7e.png)

### Whatsapp settings

<table>
   <thead>
      <tr>
         <th width="187">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>WhatsApp Sender</td>
         <td>The default whatsapp provider will be pre-configured. If you wish to use a custom provider, please refer <a href="/verify/docs/EV/GetStarted/Configurations/Whatsapp/">here</a>.</td>
      </tr>
   </tbody>
</table>

</TabItem>

<TabItem value="Link" label="Link">

If this method is selected as the **Notification Type,** the transaction link will be provided in the **API.**

![Link](https://images.tango.us/workflows/8bf43ac9-7285-4ff3-b151-93ce5c78f222/steps/ab934fd1-1dfc-4084-b556-ea9b10692953/ab189405-74f8-4c74-b56d-a173bdc0b58d.png)

</TabItem>

<TabItem value="SDK" label="SDK">


This is used when **SDK** is set as **Hook** in the **Result Notification Way.**

![SDK](https://images.tango.us/workflows/8bf43ac9-7285-4ff3-b151-93ce5c78f222/steps/59b466de-360d-4432-b5bc-ee49bb75e8cb/35f04a4c-c017-44fd-8202-2334ed216b73.png)

### SDK settings

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="197">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Open Transaction In A New Tab</td>
         <td>Use the option to open transactions in a new tab to avoid issues with third-party blocking.  <br/>
         <strong>For <strong>Passkeys</strong>, when a transaction is opened in a new tab using the web SDK, it relies on the <a href="https://passkeys.dev/docs/advanced/related-origins/#:~:text=Related%20Origin%20Requests%20(ROR)%20works,a%20list%20of%20valid%20origins.">Related Origin Request (ROR)</a>. This feature is supported only in the latest versions of browsers and is not compatible with Firefox. </strong></td>
      </tr>
      <tr>
         <td>Whitelist Host URL<span class="required-text">*</span></td>
         <td>Add the host of your application where the below scripts will be added.</td>
      </tr>
      <tr>
         <td>Only HTTPS</td>
         <td>Enable the restriction of server certificate validity when accessed from the web. Disable for development mode; strongly recommended to enable in production environments.</td>
      </tr>
   </tbody>
</table>

</TabItem>
</Tabs>

*5* After configuring the required **Verification Way,** ensure to `SAVE` the configurations.

*6* Then, configure the **Result Notification Way.** This is necessary to proceed further in order to obtain the desired result or response. To know in detail, see [here.](/verify/docs/EV/StartAnIntegration/SetupResultNotification/Hook/)

### How it Works

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the  [**cURL commands**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands), or can simply test the flow using the test mode.

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* After completing the verification process, view the exact **response type** for the configured **Result Notification Type,** refer [here](/verify/docs/EV/StartAnIntegration/SetupResultNotification/Hook/).

> Note that, the results will be different based on the configured **Result Notification Type.**

*5* To view a sample response for **Web Verification Way,** refer to our [API Builder.](/verify/docs/DeveloperTools/API/BuildyourIntegration/TriggerRequest/v1/)
