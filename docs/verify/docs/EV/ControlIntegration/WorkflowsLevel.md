---
id: WorkflowsLevel
title: Workflows Level
sidebar_label: Workflows Level
description: "Rules allow you to define and enforce workflow-level conditions to control transaction initiation, ensuring that users meet the necessary criteria to proceed with transactions."
--- 

> **Before proceeding, please ensure that you have created a rule. To know how to create, refer [here](/docs/EV/ControlIntegration/Rules/CreateRule).**

**Rules** allow you to define and enforce **workflow-level** conditions to control transaction initiation, ensuring that users meet the necessary criteria to proceed with transactions.

By setting up rules, you can customize the conditions under which transactions are allowed, adding an extra layer of control to your workflows.

These **Rules** are configured in workflows-level, making sure the particular **Workflows** satisfies the configured/set rules in order to complete the flow.

### Attach Rule to Workflow

For a rule's condition to work in the workflow-level, you need to integrate it into that specific workflow module. After selecting the rule, you can proceed with the execution process. This ensures that the rule is checked during the verification process, and if the rule's conditions are met, the user can continue with the transaction flow. If not met, the transaction process will be skipped.

## Access Workflows

*1* Navigate to **Workspace**, and click on **Workflows**.

*2* Select the workflow for which you wanted to execute the rule.

*3* Upon clicking the workflow, select the **General** tab. In this section, find the configuration labeled as **Rule**.

*4* Select the available rule from the dropdown and click on the `SAVE` button.

![Select Rule](https://images.tango.us/workflows/9cc06460-2f8a-493b-8640-1a3d65b2ca87/steps/3323540f-5ddf-48df-9379-b0d97007eb11/1f3ebe97-c096-40a8-803b-a2649399a86b.png?mark-x=882&mark-y=731&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz04MzImaD0xODMmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)

*5* Once the rule is set and saved, execute the application. To know how to, refer [here.](/verify/docs/EV/StartAnIntegration/SetupVerification/Web/)

*6* The flow execution will commence with the configured workflow rule.

#### For further assistance or related topics, you may explore the following resources:

* [What are Rules?](/docs/EV/ControlIntegration/Rules/WhatareRules)
* [Create Rule](/docs/EV/ControlIntegration/Rules/CreateRule)
* [Setup Workflow](/docs/EV/StartAnIntegration/SetupWorkflow/)