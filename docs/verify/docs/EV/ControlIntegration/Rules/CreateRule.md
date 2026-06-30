---
id: CreateRule
title: Create Rule
sidebar_label: Create Rule
--- 

import DropBox from "../../../../src/components/docs/DropBox"
import TabItem from "@theme/TabItem";
import Tabs from '@theme/Tabs';

Creating and setting up rules helps maintain security over the transaction process. These rules allow you to define specific conditions and criteria that must be met for transactions to be considered executed. 

## Access Rules

*1* Navigate to **Workspace**, and click on **Rules.**

*2* Click on the `+ CREATE RULE` button.

*3* Provide a unique name and click on the `CREATE` button.

*4* You will be taken to the created rule, in order to manage your transaction rules, click on `ADD RULE`**,** the available list of rules will be displayed in the drop-down.

*5* You can add and manage rules based on the specific requirements.

![Create Rule](https://images.tango.us/workflows/3d2addf1-0c3a-4b85-aad1-9efb758baf33/steps/a1b8b5e6-0e84-4ad4-b3c0-8913060f521b/65b17be2-5ac3-44e2-b515-b130ba0e30f0.png)

## Manage Transaction Rules

Here are the available rules:

> Note that, if multiple workflow modules are configured, and if one rule fails,  It will proceed to the next workflow module.

<Tabs>

<TabItem value="METADATA" label="METADATA" default>

With the **METADATA** rule, you can configure various types of information with specified values based on the `user metadata` provided in the API request body. This information can relate to any aspect of user identity, such as; role, age, gender etc.

For example, if the rule specifies that a user must be above a certain age to initiate a transaction, and the provided age does not meet this requirement, the user might be denied or the transaction process might be skipped.

> Note that multiple **METADATA** properties can be included within a single rule.

![METADATA](https://images.tango.us/workflows/3d2addf1-0c3a-4b85-aad1-9efb758baf33/steps/8a43be1b-15f6-40da-aa5c-f2c182c0f8a4/cfbeac89-e95c-4bee-835e-fc64fa61c969.png)

<table>
   <thead>
      <tr>
         <th width="190">Name</th>
         <th>Functions</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>METADATA PROPERTY</td>
         <td>The metadata property serves as a key, and the value linked to this key, which is provided in the <code>user metadata</code> within the API request body, will be used for evaluation. For example, if the metadata key is "age" and the value provided is 25, this value will be considered when checking if the user meets the specified criteria for the verification process.</td>
      </tr>
      <tr>
         <td>PROPERTY TYPE</td>
         <td>There are <strong>four</strong> types,
         <ul>
                <li><code>string</code>: Used for textual data like usernames, email addresses, gender, and other identifiers. Should be enclosed in double quotes.</li>
                <li><code>int</code>: Used for numerical data that does not require decimals, such as user IDs, age, and counts.</li>
                <li><code>boolean</code>: Used for binary conditions like active/inactive status, success/failure, and conditions in control flow statements.</li>
                <li><code>double</code>: Used for numerical data that requires decimals, such as financial figures, measurements, and percentages.</li>
        </ul>
        </td>
      </tr>
      <tr>
         <td>CONDITION</td>
         <td>This condition determines how the expected value should be checked against the value provided by the user.  Following are the conditions,
         <ul>
                <li><code>equals</code>: The provided value should be equal to the expected value.</li>
                <li><code>not equals</code>: The provided value should not be equal to the expected value.</li>
                <li><code>greater than</code>: The provided value should be greater than the expected value.</li>
                <li><code>greater than equal</code>: The provided value should be greater than or equal to the expected value.</li>
                <li><code>lower than</code>: The provided value should be less than the expected value.</li>
                <li><code>lower than equal</code>: The provided value should be less than or equal to the expected value.</li>
                <li><code>contains</code>: The provided value should contain the expected value.</li>
                <li><code>startswith</code>: The provided value should start with the expected value.</li>
                <li><code>endswith</code>: The provided value should end with the expected value.</li>
        </ul>
        </td>
      </tr>
      <tr>
         <td>EXPECTED VALUE</td>
         <td>Provide the value that should be satisfied for the particular property. This expected value will be checked against the value provided by the user.</td>
      </tr>
   </tbody>
</table>

</TabItem>

<TabItem value="CHANGE IP HISTORY" label="CHANGE IP HISTORY">

This rule restricts a user's attempt to verify their identity based on their **IP address**. If the current **IP address** is different from the last few **IP addresses** used in previous verification attempts, the verification process will be blocked.

Specify the rule value with the number of previous verifications to check for <strong>IP changes</strong>.

![CHANGE IP HISTORY](https://images.tango.us/workflows/3d2addf1-0c3a-4b85-aad1-9efb758baf33/steps/375ff2fb-86a6-45ee-b4fb-97f95eb50dc6/5dedc150-e967-4c38-9ff9-6337f4ae1812.png) 

</TabItem>

<TabItem value="WEBHOOK" label="WEBHOOK" default>

This rule allows you to add a hook URL, to which the rule will send a `POST` request containing specific details such as the verification request details, user details and workspace details.

If the received response code is `200`, the rule evaluation will return `true`, allowing the transaction process to proceed. Any other response code will result in a `false` evaluation, causing the transaction process to be denied or skipped.

![WEBHOOK](https://images.tango.us/workflows/3d2addf1-0c3a-4b85-aad1-9efb758baf33/steps/953af2bd-a5fe-45d0-b683-139bfdc29ec7/7f94f7ab-aedd-4381-a158-7363206a0fb5.png)

</TabItem>

<TabItem value="NOT IN IP RANGE" label="NOT IN IP RANGE" default>

The **IP Range** allows the specification of either an <code>IPV4 range</code> or a single IPV4 address. The rule outcome will be affirmative if the requested **IP address** **is outside the defined IP range**.

Accepted rule value formats include: `192.0.0.0 to 192.0.0.10`, `192.0.0.0`, or a list like `192.0.0.1; 192.0.0.2`

![NOT IN IP RANGE](https://images.tango.us/workflows/3d2addf1-0c3a-4b85-aad1-9efb758baf33/steps/8a2d7a43-9b34-4b2f-933a-28902fabf49b/bfc620fb-ca55-4b9c-a30d-b0c35e84250d.png)

</TabItem>
<TabItem value="IP WHITELISTING" label="IP WHITELISTING" default>

This rule ensures that a user can only initiate the verifiaction process from an allowed IP address. 

You can update by adding whitelist IP addresses separated by semicolons (;).

![IP WHITELISTING](https://images.tango.us/workflows/3d2addf1-0c3a-4b85-aad1-9efb758baf33/steps/63cf12ca-f4eb-43e9-87fe-234f000fead4/8cfdb4bc-0a97-4373-9b0f-087fdb11b515.png)

</TabItem>
</Tabs>


## Rule Decision Strategies

Note that, when multiple rules are added, the **Rule Decision Strategies** option will be **enabled.**

![Rule Decision Strategies](https://images.tango.us/workflows/3d2addf1-0c3a-4b85-aad1-9efb758baf33/steps/a630020b-d247-4242-93cb-1441168b0894/9d769971-7f94-4a5d-a128-f0418cafaeba.png)

<table>
	<thead>
		<tr>
			<th width="147">Name</th>
			<th>Functions</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td><strong>Unanimous</strong></td>
			<td>This is the default strategy, whereas in the absence of any provided strategy, all rules must yield a positive evaluation for the final decision to be considered positive.</td>
		</tr>
		<tr>
			<td><strong>Affirmative</strong></td>
			<td>In this scenario, for the final decision to be considered positive, it is required that at least one rule evaluate to a positive decision.</td>
		</tr>
	</tbody>
</table>