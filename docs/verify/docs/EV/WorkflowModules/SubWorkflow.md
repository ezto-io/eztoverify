---
id: SubWorkflow
title: Sub Workflow
sidebar_label: Sub Workflow
--- 

The **Sub Workflow** module enables you to utilize a predefined flow that incorporates multiple workflow modules, avoiding the repetition of the same workflows across various branches. By connecting to a central flow configuration, it helps maintain uniformity and simplifies the process of managing workflow modules.

### How to Setup

*1* Go to **Workspace > Workflows,** and click on the flow created for **Sub Workflow.** If you need understanding in creating a workflow, refer to the step-by-step guide [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/)

*2* Hover over the plus (+) symbol and select **Workflow**. From the list, click **Sub Workflow** to add it.

> You can include multiple **workflow modules** alongside the **Sub Workflow** module.

![Sub Workflow](https://images.tango.us/workflows/0870d99b-50e9-4897-af8f-1990708ae39f/steps/3bae3f81-fc02-423c-89a8-8825bd715ba0/028bb768-59c0-4f1a-8365-a2f9e41ae70a.png)

*3* Click on the newly added workflow, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/0870d99b-50e9-4897-af8f-1990708ae39f/steps/3826ff7c-cca1-48b9-ab6e-fb34c3f2ee7c/df5cfae1-c571-435d-a250-775bd00f4ed2.png)

### Manage General
(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="167">Name</th>
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
         <td>This determines the criteria for triggering the particular workflow module. When the selected rule is evaluated, and is satisfied, the workflow proceeds as provided. If not, the workflow will not be executed. <strong>Note: If no rules are set, the workflow defaults to its standard action without any rule. For more information about the rules, refer <a href="/verify/docs/EV/ControlIntegration/WorkflowsLevel/">here.</a></strong></td>
      </tr>
   </tbody>
</table>

*4* Then, click on the **Configs** tab to set the necessary configurations.

![Configs](https://images.tango.us/workflows/0870d99b-50e9-4897-af8f-1990708ae39f/steps/0cfd50dd-c1a8-4d0e-a84a-e3ab042c86f5/2002d2e1-e0bc-4803-b960-b3e118fbc094.png)

### Manage Configs

(<span class="required-text">*</span>) Indicates an action to be mandatory.

<table>
   <thead>
      <tr>
         <th width="297">Name</th>
         <th>Description </th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Sub Workflow<span class="required-text">*</span></td>
         <td>Choose the required sub workflow from the drop-down menu. To know how to create sub workflows, click <a href="/docs/EV/StartAnIntegration/SetupSubWorkflow">here</a>.</td>
      </tr>
   </tbody>
</table>

> Once you have added the required sub workflow, you can view the chosen sub workflow's flow by clicking on the highlighted **"click here"** link.

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to the instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow#attach-workflows)

#### For further assistance or related topics, you may explore the following resources:

* [Setup Workflow](/docs/EV/StartAnIntegration/SetupWorkflow)
* [Setup Sub Workflow](/docs/EV/StartAnIntegration/SetupSubWorkflow)
