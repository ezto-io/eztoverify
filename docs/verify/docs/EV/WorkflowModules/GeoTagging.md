---
id: GeoTagging
title: Geo Tagging
sidebar_label: Geo Tagging
--- 

import TabItem from "@theme/TabItem";
import Tabs from '@theme/Tabs';

The **Geo Tagging** captures and returns geographic coordinates when executed. It includes latitude and longitude details, along with accuracy measurements, to provide precise location data. This module is useful for verifying locations, tracking activities, or enhancing location based workflows.

> This flow supports both **Mobile** and **Web** applications.

### Prerequisites

* If [Mobile](/docs/EV/StartAnIntegration/SetupVerification/Mobile) application is configured, make sure to download the **ezto verify mobile** app before proceeding with the flow, 
### How to Setup

*1* Navigate to **Workspace > Workflows,** and click on the flow created for **Geo Tagging.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Geo Tagging** to add it.

![Geo Tagging](https://images.tango.us/workflows/08c75e49-84de-4a68-8e81-35764698890d/steps/ac99ce67-dfe7-40bb-93be-05bf47638870/3d93a66e-4614-4d8c-b5e5-264027e8afb6.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/08c75e49-84de-4a68-8e81-35764698890d/steps/523a88de-caf7-4279-b69f-d2a7c7eab142/2f4320b5-86e2-462c-ad9d-f5d367a02115.png)

### Manage General

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="147">Name</th>
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

![Configs](https://images.tango.us/workflows/08c75e49-84de-4a68-8e81-35764698890d/steps/bb412f34-50f0-4688-85f6-b83e48b78320/e5ce33f7-d8a6-476f-8581-847e3f1ceab7.png)

### Manage Configs

<table>
   <thead>
      <tr>
         <th width="249">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
      <td>Retry Count</td>
      <td>Specify the number of allowed retries (1 to 3). The maximum limit is 3. If left empty, it defaults to 1.</td>
      </tr>
       <tr>
      <td>Latitude</td>
      <td>Enabling this toggle records the latitude coordinates when geo-tagging is executed, providing location details of the verification process.
</td>
      </tr>
       <tr>
      <td>Longitude</td>
      <td>Enabling this toggle records the longitude coordinates when geo-tagging is executed.
</td>
      </tr>
       <tr>
      <td>Accuracy</td>
      <td>Enabling this toggle indicates the precision level of the captured latitude and longitude coordinates, showing how close the recorded location is to the actual position.</td>
      </tr>
       <tr>
      <td>Device Type</td>
      <td>Enabling this will includes the device details in the response when geo-tagging is executed.</td>
      </tr>
      <tr>
      <td>Error Codes</td>
      <td>For error codes related to <strong>Geo Tagging</strong>, see <a href="/verify/docs/EV/WorkflowModules/ErrorCodes">here.</a></td>
      </tr>
   </tbody>
</table>

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)


#### Expected Result

Based on the provided configuration, the information that will be fetched for **Geo Tagging** includes the following parameters:

| Name | Description |
|------|-------------|
| `geo_tagging_verified_at` | Timestamp (in Unix epoch format) indicating when the **Geo Tagging** details were verified. |
| `geo_tagging_verified` | This indicates whether geo tagging verification has been completed. The value will be "True" if the registration is successfully completed and "False" if it is not.
| `geo_tagging_accuracy` | Accuracy level of the geo-tagging data, typically measured in meters. |
| `geo_tagging_longitude` | Longitude coordinate of the captured location. |
| `geo_tagging_latitude` | Latitude coordinate of the captured location. |
| `geo_tagging_device_type` | Type of device used during geo-tagging, such as a **mobile** or **web**. |

The response received will include the accurate information mentioned above. Note that, the response values can vary depending on the specific workflow modules.

### How it Works

:::note
**The Geo Tagging Verification has two types of verification methods,** 

- **Web Application:** Verification happens through a web-based process. Any web-supported [Verification Way](/verify/docs/EV/StartAnIntegration/SetupVerification/Web) and [Result Notification Way](/verify/docs/EV/StartAnIntegration/SetupResultNotification/Hook) can be used. 

- **Mobile Application:** Verification is done via the ezto verify mobile app. The **Verification Way** must be configured as [Mobile,](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile) and any applicable **Result Notification Way** can be used.
:::

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings.** If you're seeking guidance on how to perform these actions, see [here.](/verify/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

<Tabs>

<TabItem value="Web" label="Web">

> To ensure **Geo Tagging** works correctly, location access must be enabled for your browser. 

- If the location is not enabled, follow the prompted steps to enable location access. The steps provided may differ based on the browser. For instance, Google Chrome is used here.

![Steps](https://images.tango.us/workflows/08c75e49-84de-4a68-8e81-35764698890d/steps/49cfce5b-cba4-4386-ab7f-d2db37a99a13/01e7e733-b74d-4718-989d-7cad6fb841bd.png)

- Once enabled, click the on **Retry** button.  

![Retry](https://images.tango.us/workflows/08c75e49-84de-4a68-8e81-35764698890d/steps/d7ba635c-c7d4-4f15-921b-ba128e00dd7a/3dc48037-e0dc-43c4-9d01-19ac1889f188.png)

- **Geo Tagging** will begin retrieving location details. 

![Loading](https://images.tango.us/workflows/08c75e49-84de-4a68-8e81-35764698890d/steps/7afc11a5-7121-40b4-8e40-11b96b011c69/0d828d36-5532-4f0f-90c8-d513a32d09f6.png)

- The verification process will then be completed.

![Completed](https://images.tango.us/workflows/08c75e49-84de-4a68-8e81-35764698890d/steps/89f1205e-0670-44d7-bce9-d065c0388bd9/15d1d610-d827-4aef-9e70-959ba1985206.png)

</TabItem>

<TabItem value="Mobile" label="Mobile">

> To ensure **Geo Tagging** works correctly, location access must be enabled for the ezto verify mobile app. 

- Begin by scanning the **QR code** with the **ezto verify** mobile app.

![QR](https://images.tango.us/workflows/08c75e49-84de-4a68-8e81-35764698890d/steps/011ba4ae-ee58-493a-9772-f87cc5231898/a7427250-82fa-47db-b317-b619acd592c3.png)

> This depends on what you have configured for the mobile notification way, i.e, **QR** code or **Push** integration. To know the difference between **QR** code or **Push,** refer [here](/verify/docs/EV/StartAnIntegration/SetupVerification/Mobile/).

- Ensure location access is enabled for the **ezto verify** mobile app. If not, you will be prompted to enable it for the Geo Tagging process. 

![Prompt](https://images.tango.us/workflows/08c75e49-84de-4a68-8e81-35764698890d/steps/d215d2f2-739c-46a5-bf6c-1cb3782f5dfa/f24c147f-84b4-4bf4-a2a8-7c6c61d4d35a.png?mark-x=2240&mark-y=529&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz00NjgmaD0yOTMmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)

- Once the location access is provided, **Geo Tagging** will begin retrieving location details. 

- The verification process will then be completed.

![Completed](https://images.tango.us/workflows/08c75e49-84de-4a68-8e81-35764698890d/steps/6c4feb95-27ee-4ec3-8c08-61fc0be63124/2f90a82c-642d-4871-b922-4f49508f19b6.png)

</TabItem>

</Tabs>

*4* The response can differ depending on the chosen **Verification Way** and **Notification Way** configurations. To know more, see [here.](/verify/docs/DeveloperTools/API/BuildyourIntegration/GetResult/v1/)

*5* Here is a sample response for **Geo Tagging:**

```json
{
  "workflow": "verification",
  "user_id": "mark@gmail.com",
  "request_id": "e892b4c7-b3e2-4f5c-a328-54d1370fa77f",
   // highlight-start
  "geo_tagging_accuracy": "13.713",
  "geo_tagging_longitude": "80.24825",
  "geo_tagging_latitude": "12.96868",
  "geo_tagging_verified_at": "1740995695",
  "geo_tagging_verified": "true/false",
  "geo_tagging_device_type": "web/mobile"
  // highlight-end
}
```