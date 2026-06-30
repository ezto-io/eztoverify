---
id: Mobile
title: Mobile
sidebar_label: Mobile
--- 

import TabItem from "@theme/TabItem";
import Tabs from '@theme/Tabs';
import CodeHighlighter from "@site/src/components/api/CodeHighlighter";

Integrating mobile applications with the **ezto verify's** `Mobile SDK`, a service used for identity verification and fraud prevention, involves several key steps to ensure a seamless and secure user experience.

### Prerequisites

* Ensure you have the necessary **SDK** installed.
* Make sure all frameworks are updated to their latest versions.
* You must have a workflow and application already set up.

### *1* Setup Workflow

Make sure to have a workflow configured in your Workspace to be executed. This guide uses **Biometric** as an example workflow.

*1* Navigate to **Workspace > Workflows,** and create a new workflow for **Biometric**. If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/docs/EV/StartAnIntegration/SetupWorkflow)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Biometric** to add it.

![Biometric](https://images.tango.us/workflows/7207d6c4-0414-4901-b93a-dfb6225a766b/steps/a5eb3c7a-246a-4628-aa90-1c14e4a8716b/150f7d93-4b42-4187-aa6d-7f3af421f8b8.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

*4* Then, click on the **Configs** tab to set the necessary configurations.

*5* After you've configured everything needed, click on the `SAVE` button.

![Configured](https://images.tango.us/workflows/7207d6c4-0414-4901-b93a-dfb6225a766b/steps/69fe6fa7-4b12-42cd-ae5a-83b31bf139b5/af412759-a265-4a7f-9f53-3a32873a7632.png)

*3* Proceed to attach the workflow to the relevant application for execution.

*4* Go to **Applications** and choose the application that is configured for this integration. Please refer to the below [application requirements](#id-setup-application) for **Mobile** integration.

*5* Proceed to attach the **Biometric Workflow.** To know how, refer [here](/verify/docs/EV/WorkflowModules/Device/Biometric/).

### *2* Configure Application {#id-setup-application}

Please ensure that your application is configured with `Mobile` as the **Verification Way**, using `SDK` for integration type, and `Mobile SDK` for the **Result Notification Way**.

<Tabs>
<TabItem value="VerificationWay" label="Verification Way" default>

*1* Navigate to **Workspace**, and click on **Applications.**

*2* Select the application you want to set up for this integration.

*3* Click on application's **Configurations** tab and choose `Mobile` as the **Verification Way.**

![Mobile](https://images.tango.us/workflows/7207d6c4-0414-4901-b93a-dfb6225a766b/steps/c91fa5ea-e3f3-4660-89b8-4e6be463e7cd/95de12ae-3eb2-4166-8dcd-c0cc3acab1ec.png)

*4* Once the settings are displayed, make the necessary configurations, and then click on the `SAVE` button. To know more about **Mobile**, refer [here](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile/).

</TabItem>

<TabItem value="Result Notification Way" label="Result Notification Way" default>

*1* Navigate to **Workspace**, and click on **Applications.**

*2* Select the application you want to set up for this integration.

*3* Click on application's **Configurations** tab and choose `Hook` as the **Result Notification Way,** the settings will open.

*4* For **Hook Notification Way**, choose `Mobile SDK` from the dropdown.

![Result Notification Way](https://images.tango.us/workflows/7207d6c4-0414-4901-b93a-dfb6225a766b/steps/456df4d0-5251-49eb-aacb-42ded934a219/0a27868b-2f1b-4d08-904a-16446a2fe314.png)

*5* Configure the necessary and click on the `SAVE` button. To know in detail about **Hook**, refer [here](/verify/docs/EV/StartAnIntegration/SetupResultNotification/Hook/).

</TabItem>
</Tabs>

Once the application is set up with the appropriate configurations, proceed with the following steps.

### *3* Execution Process

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application.** You'll see the steps laid out for you to follow using the [**cURL commands.**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands)

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

> Make sure to configure the `allow_new_device`: true in the body of the respective API. This is only required for when using [Web to Device.](/docs/EV/WorkflowModules/Device/WebtoDevice)

*2* Upon initiating the API **(Send Request)** call, a verification request will be sent to your configured Verification Way settings. In this flow, it is a **base64 QR** in the APIs response body.

*3* Upon receiving the request, copy the **base64 QR** code and paste it into an incognito browser to convert it into a QR code image.

###  *4* QR function call

*1* Use the `launchQrScanner` method from the SDK to scan the QR code:

<Tabs>
    <TabItem value="flutter" label="Flutter" default>
       <CodeHighlighter path="./mdx/Mobile/QR/flutter.mdx"/>
    </TabItem>
    <TabItem value="android" label="Android" default>
       <CodeHighlighter path="./mdx/Mobile/QR/android.mdx"/>
    </TabItem>
    <TabItem value="ios" label="IOS" default>
       <CodeHighlighter path="./mdx/Mobile/QR/ios.mdx"/>
    </TabItem>
    <TabItem value="react-native" label="React Native" default>
       <CodeHighlighter path="./mdx/Mobile/QR/react-native.mdx"/>
    </TabItem>
</Tabs>


*2* This should open the **Biometric** flow in the SDK's webview. Complete the flow as instructed.

*3* Once the flow is completed, the SDK will receive the `auth_req_id` via the `onResult` method:

<Tabs>
    <TabItem value="flutter" label="Flutter" default>
       <CodeHighlighter path="./mdx/Mobile/result/flutter.mdx"/>
    </TabItem>
    <TabItem value="android" label="Android" default>
       <CodeHighlighter path="./mdx/Mobile/result/android.mdx"/>
    </TabItem>
    <TabItem value="ios" label="IOS" default>
       <CodeHighlighter path="./mdx/Mobile/result/ios.mdx"/>
    </TabItem>
    <TabItem value="react-native" label="React Native" default>
       <CodeHighlighter path="./mdx/Mobile/result/react-native.mdx"/>
    </TabItem>
</Tabs>

*4* Use the `auth_req_id` to check the result using the [Get Result](/docs/DeveloperTools/API/Transaction/GetResult/v1/) API.