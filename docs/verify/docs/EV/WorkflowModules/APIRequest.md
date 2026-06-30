---
id: APIRequest
title: API Request
sidebar_label: API Request
---

import GifPlayer from "@site/src/components/changelogs/GifPlayer";

The **API Request Module** is a configurable component within a workflow that enables **ezto verify** to interact with external APIs. Similar to Postman or other API testing tools, this module integrates directly into your onboarding/verification flow, allowing you to send requests and process responses from external services.

It can send API requests to external services, facilitating interaction with various systems or applications. With fields for request headers, methods, body types, and content types, the API Request Module is a comprehensive tool for making outbound API requests.

### Key Features

**Flexible Request Methods:** Supports multiple HTTP methods, including GET, POST, PUT, PATCH, and DELETE, offering versatility in how data is fetched or modified.

**Content and Body Customization:** Choose from a variety of content types such as application/json, application/xml, or text/plain for request bodies, and define specific formats that suit your service's requirements.

**Authentication Control:** Specify whether authentication is required for the API call, ensuring secure integration with third-party services.

**Dynamic Integration:** Acts as an intermediary between the verification process and external services, making real-time API calls to trigger actions or retrieve necessary data for validation purposes.

By configuring the **API Request Module,** you enable efficiendt and automated interaction with external services, reducing the need for manual intervention.

<GifPlayer name="8.2.0/Api-request"/>

<br/>

### How to Setup

*1* Navigate to **Workspace > Workflows,** and click on the flow created for **API Request.** 

*2* Hover over the plus (+) symbol and select **API Request** to add it.

![API Request](https://images.tango.us/workflows/c6aa699f-dc1d-4142-ac57-b7561fe09dcb/steps/a5d74a2a-0f29-4300-a833-e0b93d7d5774/07f5020b-6df9-4ca4-a197-c550ac5ab1e0.png)

*3* Click on the newly added API Request, then select the **General** tab to customize your preferences.

![General](https://images.tango.us/workflows/c6aa699f-dc1d-4142-ac57-b7561fe09dcb/steps/ae9a9050-f6aa-4a84-8520-4d3b3f194697/a96b76a1-e1a6-4574-9896-c148f21c04cc.png)


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
         <td>This determines the criteria for triggering the particular workflow module. When the selected rule is evaluated, and is satisfied, the workflow proceeds as provided. If not, the workflow will not be executed. <strong>Note: If no rules are set, the workflow defaults to its standard action without any rule. For more information about the rules, refer <a href="/verify/docs/EV/ControlIntegration/WorkflowsLevel/">here.</a></strong></td>
      </tr>
   </tbody>
</table>

*4* Then, click on the **Configs** tab to set the necessary configurations.

![Configs](https://images.tango.us/workflows/c6aa699f-dc1d-4142-ac57-b7561fe09dcb/steps/a17d2489-9189-4ca3-8dc8-1d96c5efaa51/be46be38-69c0-41ea-8831-364b4def80b6.png)

### Manage Configs

(<span class="required-text">*</span>) Indicates an action to be mandatory.

The **API Request Module** functions through a series of configurations that specify how the external request should be executed:

- **API Request Name<span class="required-text">*</span>**: This is the identifier for the API request you're setting up. It should clearly describe the purpose of the request, making it easy to reference within the workflow.  
**Example**: "User Details", "Verify PAN".

- **API Request Title<span class="required-text">*</span>**: A human-readable title that summarizes the API request. This will be visible to users interacting with the request in the workflow, providing context about the request's function.  
**Example**: "Documentation Verification", "Send Payment Request".

- **API Request Description<span class="required-text">*</span>**: A detailed explanation of the request's purpose. This can include information about the type of data being retrieved or sent, and how it integrates with the verification process.  

- **API Request Method<span class="required-text">*</span>**: Specifies the HTTP method for the API request, defining the action the API will perform. The available methods are:
   - **GET**: Retrieve data from the server.
   - **POST**: Send new data to the server.
   - **PUT**: Update an existing resource on the server.
   - **PATCH**: Partially update a resource.
   - **DELETE**: Remove a resource from the server.  
**Example**: Use "GET" to fetch user details, or "POST" to create a new user.

- **API Request URL<span class="required-text">*</span>**: The endpoint URL where the API request is sent. This defines the destination for the API call and indicates where the data is retrieved from or sent to. It must be a fully qualified URL.

-  **API Request Content Type<span class="required-text">*</span>**: Specifies the format of the data being sent to the server so that the server can properly process it. Options include:
   - **application/json**: JSON-formatted data.
   - **application/x-www-form-urlencoded**: Form submission data.
   - **application/xml**: XML-formatted data.
   - **text/plain**: Plain text data.
   - **text/javascript**: JavaScript content.
   - **text/html**: HTML content.  
**Example**: Use `application/json` for sending JSON data.

- **API Request Body Type<span class="required-text">*</span>**: Indicates whether a request body is needed for the API request. This is typically required for methods like POST or PUT. Set to **"None"** if no body is needed or **"Required"** if a body must be provided.

- **API Request Body Form**: If "Required" is selected, the following fields will be displayed for configuring the API Request Body: 

Note that, keys and values can only be provided if the **API Request Content Type** is set to `application/x-www-form-urlencoded` 

![Request Body](https://images.tango.us/workflows/c6aa699f-dc1d-4142-ac57-b7561fe09dcb/steps/5eb1fec2-e11c-4326-978e-21fae6aa14c8/6958791c-ec49-4ff6-9bdd-f5fcde001a61.png)

For other content types, you can specify additional data to be included in the request body. You can provide the information in various formats such as JSON, XML, or form data, depending on the API requirements

![Request Body](https://images.tango.us/workflows/c6aa699f-dc1d-4142-ac57-b7561fe09dcb/steps/63deb2f0-25ea-4d22-9fc9-851bd666ff15/779b5ee7-2f22-4b14-a424-67d20d8a9413.png)

- **API Request Headers**: This field will appear once an **API Request Method** is selected

![Request Header](https://images.tango.us/workflows/c6aa699f-dc1d-4142-ac57-b7561fe09dcb/steps/fb63a040-462a-4e07-92e4-caa6bb34bdeb/15f6bce9-9a70-44f5-9994-b103d6924990.png)


<table>
   <thead>
      <tr>
         <th width="147">Name</th>
         <th>Description</th>
      </tr>
   </thead>
   <tbody>
      <tr>
         <td>Key</td>
         <td>This refers to the name of the header you want to send with your API request. Headers are used to pass metadata with the API request. The key defines what kind of metadata you're sending.</td>
      </tr>
      <tr>
         <td>Value</td>
         <td>The value for the header key can either be static (a predefined value) or dynamic (using environment variables). You can add environment variables in the format "Value": "${'{env.authorization' }}". The value complements the key by providing the specific information the server needs to process the request correctly.
         </td>
      </tr>
      <tr>
         <td>Actions</td>
         <td>This usually includes options to <strong>add</strong> or <strong>delete</strong> header entries. It allows you to manage multiple headers for a single API request. You can add new headers, modify the existing key's value, or remove them entirely if not needed.</td>
      </tr>
   </tbody>
</table>


- **Auth Required**: A toggle that determines if the API request needs authentication. When enabled, you must provide authentication credentials, such as a bearer token (the only supported method), to ensure secure access to the API. Enable this option when working with APIs that require secure communication.

- **API Request Bearer Token**: The authentication token required for accessing secured APIs. This token will be included in the request headers as part of the authentication process. Ensure the token is up-to-date and has the necessary permissions to interact with the external service. The format for adding the token is `Bearer A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6`

> For error codes related to **API Request**, see [here.](/verify/docs/EV/WorkflowModules/ErrorCodes)

*5* After you've configured everything needed, click on **SAVE.**

*6* Proceed to attach the workflow to the relevant application for execution. Refer to instructions on how to attach a workflow to an application [here.](/verify/docs/EV/StartAnIntegration/SetupWorkflow/#attach-workflows)