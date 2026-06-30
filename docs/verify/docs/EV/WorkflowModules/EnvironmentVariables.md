---
id: EnvironmentVariables
title: Environment Variables
sidebar_label: Environment Variables
--- 

Environment variables enhance the flexibility of workflow modules, such as [API Request](/docs/EV/WorkflowModules/APIRequest), [Dynamic Form](/docs/EV/WorkflowModules/DynamicForm), [Validator](/docs/EV/WorkflowModules/Validator), and [Custom Response](/docs/EV/WorkflowModules/CustomResponse). They allow you to avoid hardcoding static values like keys, URLs, or tokens, making workflows more adaptable and easier to manage.  

Instead of embedding these values directly into workflows, you can define them as environment variables for the specific flow and reference them dynamically. 

### Set Environment Variables

*1* Navigate to the workflow module where you want to use environment variables.

*2* Click the **Edit** icon to open the environment variables configuration panel.

![Edit Icon](https://images.tango.us/workflows/5e260ce5-61c8-4ad6-b119-a2aab9b99a40/steps/75b97871-c010-45f2-8543-728763d2c3a1/dc00bf6c-e7e3-4672-abac-14b8e8dc4205.png)

*3* In the panel, define the required environment variables using the JSON format. For example:

![JSON](https://images.tango.us/workflows/5e260ce5-61c8-4ad6-b119-a2aab9b99a40/steps/8367d7ca-1354-4f8f-bfe8-4e71453837dd/4bcb597b-6f82-4b02-88aa-c2b2c2d22c8b.png)

```json
   {
       "Authorization": "hg345667oguixxxxxxxxx"
   }
```

In this example, `"Authorization"` is the key, and `"hg345667oguixxxxxxxxx"` holds the token value.

<br/>

### Use Environment Variables in Workflows

Once defined, you can reference these variables in your workflow modules. Use the syntax `${env.<VariableName>}` to use the variable dynamically. For example:

- **In API Request Headers:**  
  To include the authorization token in the headers, specify the value as:  
 ![API Request](https://images.tango.us/workflows/5e260ce5-61c8-4ad6-b119-a2aab9b99a40/steps/a23d9364-a3e6-44a3-b118-4243ecdc0946/4f34522e-f1b1-4526-810f-d248d19645e3.png)

- **In Workflow Claims:**  
  If the workflow is required to use the environment claims, use the variable in the definition, such as:  
![Workflow](https://images.tango.us/workflows/5e260ce5-61c8-4ad6-b119-a2aab9b99a40/steps/c42024f2-d7ab-4c15-88df-814b09f02a4c/acf068ff-843a-4480-8af0-d500c15d4b74.png)
 
By integrating environment variables, workflows become dynamic, adaptable, and easier to configure, especially for scenarios that require frequent changes or multiple configurations. 