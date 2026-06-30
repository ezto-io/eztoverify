---
id: ApplicationLevel
title: Application Level
sidebar_label: Application Level
description: "Gatepass is a feature that allows you to define and enforce application level rules to control user access and transaction initiation"
--- 

> **Before proceeding, please ensure that you have created a rule. To know how to create, refer [here](/docs/EV/ControlIntegration/Rules/CreateRule).**

**Gatepass** is a feature that allows you to define and enforce **application-level** rules to control user access and transaction initiation. These rules are evaluated each time a transaction is initiated, ensuring that only users who meet the specified criteria can proceed. If a user fails to comply with any of the set rules, access is denied, and they cannot move forward with the verification flow.

By setting up **Gatepass** rules, you can customize the conditions under which transactions are allowed, adding an extra layer of control to your application. This ensures that only legitimate and authorized transactions are processed, protecting your application from potential fraud.

### Access Gatepass

*1* Navigate to **Workspace**, and click on **Application**.

*2* This will take you to the list of all applications available in your dashboard.

*3* Click on the application you wish to view and configure **Gatepass**. This will open the application **General** page.

![Applications](https://images.tango.us/workflows/1b3a991a-f2ce-4f16-a1ec-df633b182d10/steps/73012bf6-9b85-46c1-bec2-a0b6b9436a7b/3ee96cf1-ed41-4937-ad65-b5187f90f468.png)

*4* Within the application's General page, click on the **Gatepass** tab to configure the rules.

![Gatepass](https://images.tango.us/workflows/1b3a991a-f2ce-4f16-a1ec-df633b182d10/steps/b5133322-0b6a-4381-bed0-3b1b29d48ca5/fd8d5e9e-bfb2-4f1b-bc95-5c5a115a8fd2.png)

### Manage Gatepass

This configuration allow you to set rules that govern user access and transaction initiation.

<table><thead><tr><th width="177">Configuration</th><th>Description</th></tr></thead><tbody><tr><td><strong>Gatepass Rule</strong></td><td>The selected rule will be evaluated at the transaction level. If the rule evaluation is successful, access will be provided to the user. For detailed instructions on creating a <strong>Rule</strong> or understanding how a rule functions, refer <a href="/verify/docs/EV/ControlIntegration/WorkflowsLevel/">here</a>.</td></tr></tbody></table>

<br/>

### Attach Rule to Gatepass

To apply the conditions of a rule within an application, the rule must be integrated into that specific application's **Gatepass**. Once you configure the rule within the application's **Gatepass**, proceed with the execution process. 

This ensures that the rule is enforced at the **application-level,** preventing the initiation of the verification process if the **Gatepass** rule fails to meet the specified condition.

> Note that, if the selected rule successfully evaluates at the transaction level, the access will be granted to the user to initiate the verification flow.

*1* Navigate to **Workspace**, and click on **Applications**.

*2* Select the application for which you wanted to execute the rule.

*3* You will be taken to the applications' **General** page, click on **Gatepass**.

*4* Select the available rule from the dropdown and click on the `SAVE` button**.**

![Select Gatepass](https://images.tango.us/workflows/1b3a991a-f2ce-4f16-a1ec-df633b182d10/steps/17229175-8432-4e0c-8482-9feb732b2182/ea4df82c-0a99-47cb-9804-b198507515f5.png)

*5* Once the rule is set and saved, execute the application. To know how to, refer [here.](/verify/docs/EV/StartAnIntegration/SetupVerification/Web/)

*6* The flow execution will commence with the application's rule.

#### For further assistance or related topics, you may explore the following resources:

* [What are Rules?](/docs/EV/ControlIntegration/Rules/WhatareRules)
* [Create Rule](/docs/EV/ControlIntegration/Rules/CreateRule)
* [Setup Workflow](/docs/EV/StartAnIntegration/SetupWorkflow/)
