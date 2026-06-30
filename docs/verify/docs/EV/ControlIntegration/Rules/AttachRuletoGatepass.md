---
id: AttachRuletoGatepass
title: Attach Rule to Gatepass
sidebar_label: Attach Rule to Gatepass
--- 

To apply the conditions of a rule within an application, the rule must be integrated into that specific application's **Gatepass**. Once you configure the rule within the application's **Gatepass**, proceed with the execution process. 

This ensures that the rule is enforced at the **application-level,** preventing the initiation of the verification process if the **Gatepass** rule fails to meet the specified condition.

> Note that, if the selected rule successfully evaluates at the transaction level, the access will be granted to the user to initiate the verification flow.

*1* Navigate to **Workspace**, and click on **Applications**.

*2* Select the application for which you wanted to execute the rule.

*3* You will be taken to the applications' **General** page, click on **Gatepass**.

*4* Select the available rule from the dropdown and click on the `SAVE` button**.**

![Select Gatepass](https://images.tango.us/workflows/40887734-aa08-4451-8eb5-5075f56144bf/steps/edb2d201-66e8-4391-9c1b-35a8d8013f8e/24d46960-b7c1-4f9a-b655-f97b5c3e7390.png)

*5* Once the rule is set and saved, execute the application. To know how to, refer [here.](/verify/docs/EV/StartAnIntegration/SetupVerification/Web/)

*6* The flow execution will commence with the application's rule.

#### For further assistance or related topics, you may explore the following resources

- [Create Rule](/docs/EV/ControlIntegration/Rules/CreateRule)
- [Application Level](/docs/EV/ControlIntegration/ApplicationLevel)