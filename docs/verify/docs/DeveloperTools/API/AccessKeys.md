---
id:  AccessKeys
title: Access Keys
sidebar_label: Access Keys
--- 

# Access Keys

Access keys can be generated to meet specific requirements, enabling API-level access. By default, each **Workspace** is assigned a default access key. Additionally, custom access keys can be created within a **Workspace**.

These secret keys are utilized as the **Authorization** header for **Device Management** APIs and are authenticated based on their assigned permissions, allowing interaction with corresponding API functionalities.

Access keys have a configurable expiration date, typically set to one year by default. An email notification is sent to the creator one week before a token expires.

The permissions specified for the access key will be enforced, while any other permissions will not be applied when the API call is made.

Only **Workspace creator** has the capability to access, view, or create access keys. Co-admins do not possess this privilege.

### To access, view, or create,

*1* Navigate to **Workspace** **Settings**, and click on **Access Key.**

*2* On this page, you can view the default access key or create a new one.

![Access Key](https://images.tango.us/workflows/e295c0ef-ebce-4ce3-940d-90e464d1e715/steps/73443ec8-cd93-4d68-9360-bf01e8586c89/72537797-c946-4c1b-9632-35d204d76e81.png)

*3* To create a new access key, click  on the `+ ADD ACCESS KEY` button.

*4* Provide a name for the access key, and click on  `CREATE`

![Name](https://images.tango.us/workflows/e295c0ef-ebce-4ce3-940d-90e464d1e715/steps/d6511f62-902b-4609-a244-5a3f68826874/9617efc0-42db-423a-a3e2-7dbd4be51a3e.png)

*5* The new access key will be created and displayed with its configurations.

![Configurations](https://images.tango.us/workflows/e295c0ef-ebce-4ce3-940d-90e464d1e715/steps/7f59444a-24ce-4ff7-96d5-7326d1d8aa98/8023e339-ebbd-4f74-b855-8d2a2dac1642.png)

> Ensure to toggle the enable button for the access key to function.

#### Manage Access Key

<table><thead><tr><th width="157">Name</th><th>Description</th></tr></thead><tbody><tr><td><strong>ID</strong></td><td>This is an auto-generated unique identifier for each access key.</td></tr><tr><td><strong>Name</strong></td><td>The name given to your access key during creation. This can be edited.</td></tr><tr><td><strong>Secret</strong></td><td>The secret token is unique to the access key and can be rotated to generate a new one. These secret keys serve as the <strong>Authorization</strong> header for <strong>Device Management</strong> APIs.</td></tr><tr><td><strong>Enable</strong></td><td>The access key will only function if this is toggled on.</td></tr><tr><td><strong>Expiry Time</strong></td><td>The expiry time of the access key is determined by a configurable expiry date, which is set to one year by default.</td></tr><tr><td><strong>Requested By</strong></td><td>Details of the user who requested or created the access key.</td></tr><tr><td><strong>Created At</strong></td><td>Timestamp of when the access key was created.</td></tr><tr><td><strong>Last Updated</strong></td><td>Timestamp of the last modification made to the access key.</td></tr><tr><td><strong>Last Used</strong></td><td>Timestamp of the last time the access key was used.</td></tr><tr><td><strong>Permissions</strong></td><td>The list of permissions granted to this access key. Here are the details for each permission granted to this access key:
<br/>
<ul>
<li><code>Devices@list</code>: Grants the ability to retrieve and display the list of all available devices.</li>
<li><code>Devices@remove</code>: Allows the access key to remove a specific device for the user.</li>
<li><code>Devices@disable</code>: Permits the disabling of a specific device for the user.</li>
<li><code>Devices@enable</code>: Allows the access key to enable a specific device for the user</li>
<li><code>Devices@mark-as-last-used</code>: Allows the access key to mark devices as last used, for all subsequent API requests to be directed to this device.</li>
</ul>


</td></tr></tbody></table>

*6* After making any changes, ensure to click on the `SAVE` button to update the configurations.

> Note that by default, while your workspace is in **Sandbox** mode, the access key(s) will be created in **Sandbox**. When you transition to **Live** mode, you can move the access key(s) to **Live** by disabling the **Sandbox Mode** toggle. To learn how to do this, refer [here.](/verify/docs/EV/GetStarted/SandboxLive/#what-is-sand-box)

#### For further assistance or related topics, you may explore the following resources:

- [Workspace Activity](/docs/EV/AdvancedSettings/WorkspaceActivity)
- [Audit](/docs/EV/Audit/AdminLogs)