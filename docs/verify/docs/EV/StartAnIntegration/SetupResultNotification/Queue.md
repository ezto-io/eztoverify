---
id: Queue
title: Queue
sidebar_label: Queue
--- 

This is one of the methods specifying the means by which the **Result Notification** should be sent. Currently, we use **Kafka** specifically for **Queue**.

### Access Application

*1* Navigate to **Workspace,** and click on **Applications.**

*2* Choose the application you wish to execute the flow with configuration as **Queue.**

*3* Click on the **Configuration** tab, and select **Queue** in the **Result Notification Way.**

![Queue](https://images.tango.us/workflows/d2afae7c-4c47-4f6e-acc8-c45888122e88/steps/3ae36ed8-2eec-4395-a40b-871c8230598b/bffe4f37-1587-4a43-8a7c-13c10523e234.png)

*4* You will be displayed with the **Queue** settings.

![Queue Settings](https://images.tango.us/workflows/d2afae7c-4c47-4f6e-acc8-c45888122e88/steps/053709c4-0649-43cb-b2b1-ab0f9faab986/21afd2b3-8531-49dc-a122-77ccb14445e1.png)

### **Queue** settings

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="235">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Queue</td>
         <td>Select <strong>Kafka</strong> as the method.</td>
      </tr>
      <tr>
         <td>Kafka Bootstrap Server<span class="required-text">*</span></td>
         <td>Set the <strong>Kafka</strong> bootstrap server. The format should be <code>127.0.0.1:9092</code>. For clusters, separate node addresses with commas.</td>
      </tr>
      <tr>
         <td>Kafka Topic<span class="required-text">*</span></td>
         <td>Specify the <strong>Kafka</strong> topic where messages will be published.</td>
      </tr>
      <tr>
         <td>Authentication</td>
         <td>Enable this option to add your <strong>Kafka</strong> authentication details. It supports SASL/PLAIN authentication.</td>
      </tr>
      <tr>
         <td>Username or API key<span class="required-text">*</span></td>
         <td>Set the <strong>API</strong> key or username of your <strong>Kafka</strong> server.</td>
      </tr>
      <tr>
         <td>Password or API secret<span class="required-text">*</span></td>
         <td>Set the <strong>API</strong> secret or password of your <strong>Kafka</strong> server.</td>
      </tr>
   </tbody>
</table>

*5* After configuring the required **Result Notification Way,** be sure to click on `TEST CONFIGURATION`, the`SAVE` button will be enabled enabled after a successful test.

![Test Configurations](https://images.tango.us/workflows/d2afae7c-4c47-4f6e-acc8-c45888122e88/steps/a4256d52-e124-403b-94f1-4673da95c55f/e566dc3d-e707-4ded-b8ae-cec5df5a114a.png)

*6* Ensure to `SAVE` upon the successful configurations.

![SAVE](https://images.tango.us/workflows/d2afae7c-4c47-4f6e-acc8-c45888122e88/steps/f514a9e5-e918-4727-ad13-9483d354cef9/08cebee9-7e30-4c5b-9ff8-222d96b43ab6.png)

### How it Works

> * Make sure your required workflow is set before starting with the execution process in order to get the expected result.
> * To initiate the execution process, go to the **How to use?** tab within the **Application**. You'll see the steps laid out for you to follow using the  [**cURL commands**](/docs/EV/StartAnIntegration/CreateApplication/#curl-commands), or can simply test the flow using the test mode.

*1* Launch **Postman** or your preferred API tool and execute the flow by configuring the suitable **Method,** **URL,** and **Body settings**. If you're seeking guidance on how to perform these actions, see [here.](/docs/DeveloperTools/API/Transaction/TriggerRequest/v1/)

*2* Upon initiating the API call, a verification request will be sent to your configured **Verification Way** settings.

*3* Based on the request, execute the flow in an incognito browser and follow the prompts to complete the verification process.

*4* After completing the verification process, check the response in your **Kafka** account and view it within your configured **Kafka Topic.**

**Below is the sample response type:**

![Sample response](https://images.tango.us/workflows/d2afae7c-4c47-4f6e-acc8-c45888122e88/steps/9e4e0900-10c3-4ab1-ac47-4e62241a8cab/cfc3d820-398a-4a00-8226-26ba12b8a08d.png)