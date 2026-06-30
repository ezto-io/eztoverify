---
id: SetupWorkflow
title: Setup Workflow
sidebar_label: Setup Workflow
--- 

import DropBox from "../../../src/components/docs/DropBox"

Imagine having the ability to design and personalize your own **verification Workflow modules**, ensuring seamless user access to your application. This is where **ezto verify** comes in, offering pre-defined, ready-to-use workflows. These are highly secure, advanced verification methods that securely execute your **transactions.**

**ezto verify's Workflow modules** are designed to enhance your verification process, making it effortless to set up secure and user-friendly access to your application.

> **Based on the country chosen, the list of available workflow modules will vary.**

### **Create Workflow**

*1* Navigate to **Workspace**, and click on **Workflows**.

*2* To create a workflow, click the `+ CREATE WORKFLOW` button.

![CREATE WORKFLOW](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/0a1eb60d-3beb-4a7f-88ba-3ab159839bcf/2a3dd316-7db2-4581-abb2-9e34bc0643db.png)

*3* Provide a descriptive name and click on the `CREATE` button.

![Create Workflow](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/709b32ea-726a-47c7-84d9-1913698250d1/d261e260-d2ce-40e0-895f-c333e3a61e49.png)

<DropBox title="Importing Workflows">

The **Import Workflow** option makes setting up workflows easier by letting you upload an existing workflow configuration. This is helpful when you need to reuse a workflow without starting from scratch. You can import the file, review the settings, and make any needed changes quickly, saving time and keeping workflows consistent. To know how to export a file, see [here.](#export-workflow)

**If you have an exported workflow file, you can import it by following these steps:**

- Enable the **Import Workflow** toggle to display import options.

![Import Workflow](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/7fac4937-0516-4bdf-8b6b-81d98cbc4684/fb637e13-8f43-4024-9d36-e3cbf96fe1a1.png)

- Choose to either upload the workflow file from your device or paste the workflow’s **JSON** from the downloaded file into the provided field.

- After importing, the workflow will display all saved configurations, allowing you to review and modify as needed.

</DropBox>

*4* To incorporate the available **verification module(s)** for execution, hover over the plus (+) symbol.

![Hover](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/0fcf60cf-d344-49cd-8e9f-071c3d682674/b79ec842-fc8b-4af9-8906-d7b79a107cef.png)

*5* You will be presented with options to select, such as **Workflow** or **Validator**.

![Options](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/26c0dd16-0b32-48e0-8673-83ba44427d7f/c73488db-2151-479b-85fa-2df42dea575d.png)

*6* Click on **Workflow** and choose the required.

> **You can include a suitable number of workflows, ensure it is compatible and effective with the verification process.**

![Add Workflow Module](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/9fdfac91-b8c4-4aba-aa80-0fde5e9a6b9b/60fba79e-1f23-4407-8af2-8ccf7f5b4bb8.png)


<p id="export-workflow">
    <em>7</em> Once you add the needed workflow(s), you can see the estimated cost of the workflow transaction by hovering over the <code>Estimated Cost</code> label.
</p>

![Estimated Cost](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/693af49d-6767-4f91-8e5f-9f2c868192ff/9fd3ccb9-87d5-4434-bd98-2f1fa035cc1c.png)

*8* To export a workflow, click the export icon. Enter a name for the exported file, then click **Create**. The workflow will be saved as a **JSON** file on your device, which can later be imported when creating a new workflow.

![Export](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/eaa7be72-703b-461d-83d6-9685b633df29/99201e6e-65c8-4b3d-8ce9-f1c68fad7d0b.png?mark-x=822&mark-y=436&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz05MDUmaD01ODgmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)

*9* Add predefined variables to dynamically adjust configurations of the [environment variables](/docs/EV/WorkflowModules/HelperFunctions), which only apply to **API Request, Validator, Custom Response, and Dynamic Form workflow modules**.  

![Variables](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/c892314a-4834-4488-b2ca-1ff9ce4038f1/34f90eb6-36cf-4987-a0c0-bac9738b3531.png)

*10* You can also view a visual representation of the selected workflow module(s) to understand how it works by clicking the **Visualize** button (the eye icon).

![Visualize](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/45029367-c2a5-420a-87dc-a03db0d0cebb/e5c908c7-a40b-4c28-9a82-ae86c141ca1b.png)

*11* Now, give a click to the newly added workflow in order to set up your preferences. The workflow's **Info** will be displayed, giving you a clear and detailed overview.

![INFO](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/38b04e4e-ecfc-4389-aa23-3ae4f259b25c/f731a8de-06bc-4441-98a9-f30428669b55.png)

*12* Then, click on the **General** tab to display the settings and provide the required configurations.

![General](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/8a185293-316c-48da-a99d-6b7e6e8dc651/e89a250a-abb6-461e-a0f3-983d1b9912f8.png)

*13* Click on the **Configs** tab to proceed with filling in the necessary configurations.

> **When you pick any of the workflow modules, you'll see the corresponding settings config panel for you to customize and configure.**

![Configs](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/e5fe7e90-ab71-416f-ab4c-031670005c83/b39a45ea-867e-4982-8c6f-10bb2bcb195e.png)

*14* Once all the settings are configured, click on the `SAVE` button.

#### For further assistance or related topics, you may explore the following resources {#attach-workflows}

<DropBox title="Attach Workflows to an Application">

To observe the **workflows** in action, simply select the workflow you've established within your application and execute the process.

*1* Navigate to **Workspace**, and click on **Applications**.

*2* Choose the application to which you want to attach the workflow. To know how to create an application, refer [here](/verify/docs/EV/StartAnIntegration/CreateApplication/).

*3* You will be directed to its **General** tab. From this section, click on the **Workflows** tab.

![General Tab](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/a2f718f1-2e3a-41f6-9558-c744edc078e7/c495ab1b-6d8c-4f18-9e09-f00281cdb8d4.png)

*4* Select the **Workflows**(i.e **Onboarding** and **Verification),** from the dropdown and click on the `SAVE.`

Choose which **onboarding workflow** you want to execute for a **newly onboarding user**, you can configure it as **"None"**. Please note that selecting the **"None"** option for the **onboarding workflow** will automatically redirect the user to the **verification workflow,** irrespective of the users availability.

![Workflow Tab](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/e65c9e01-afca-46b2-b3e0-00208dd6cf51/34499587-30d9-49ef-867e-8863e5635642.png)

*5* Once the workflow is set and saved, execute the application.

*6* The execution will commence with the application's configured workflows.

</DropBox>

<DropBox title="Removing Workflow" optional={true}>

It is easy to delete the **verification workflows** which are created, allowing you to fine-tune your setup for optimal performance. Utilize the delete option to effortlessly remove verification methods that are no longer required.

*1* Navigate to **Workspace**, and click on **Workflows**.

*2* You will be presented with a list of existing(created) workflows. Click on the workflow you wish to delete from the list

**The edit icon is located next to the name of the template, if you would like to modify the flow rather than delete it.**

![Workflows](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/5633ea43-e413-4152-a331-ba7e67442d98/366f96f3-f8d0-44d0-94d7-3d46a9e195f9.png)

*3* Click on the name of the workflow you want to delete, make sure it contains the workflow you wish to remove.

*4* You will be directed to the configuration page.

*5* The **Delete** option will be displayed in the top right corner, click on the `DELETE` button.

![Delete Workflow](https://images.tango.us/workflows/424b6e2b-c667-4290-9fe1-ee5fefa51ff0/steps/c14be363-5149-4e8a-a6f1-a747a351519c/bee26c02-e68b-4c7d-9255-af036a1d51b0.png)

*6* You will be prompted with a confirmation pop-up, click on `DELETE` to remove.

</DropBox>