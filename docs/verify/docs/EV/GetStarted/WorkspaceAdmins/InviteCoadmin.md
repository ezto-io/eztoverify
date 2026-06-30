---
id: InviteCoadmin
title: Invite Co-Admin
sidebar_label: Invite Co-Admin
--- 

Co-admins play a role in managing your workspace alongside the account creator (owner) and other admins. They share administrative responsibilities within the workspace, having the authority to perform various tasks such as user management, configuring settings, and overseeing the workspace's operations. This guide will walk you through the step-by-step process of inviting a new co-admin, granting them access to the workspace, and managing their permissions.

### To begin with,

*1* Navigate to **Workspace** and click on **Settings**.

*2* Select **Workspace Admins** from the menu.

In the **Workspace Admins** tab, you'll find a list of current admins, including the account creator (owner), and the option to add new admins.

![Workspace Admins](https://images.tango.us/workflows/56571f94-f480-4ab2-be42-7a883aac7f4c/steps/7ebc5847-de17-4165-87de-3e914f6820bd/3cd0575c-a0f7-45a2-8191-25eca37fd168.png)

### Adding a Co-admin

*1* Click on the **ADD ADMIN** button.

*2*  Enter the email address of the user you wish to invite as a co-admin.

> By default, the **User Enabled** feature is enabled, allowing users to log in to the workspace. You can toggle this feature as per your preference.

![Add Admin](https://images.tango.us/workflows/56571f94-f480-4ab2-be42-7a883aac7f4c/steps/9961e9f0-e3b6-4770-8ea6-2945a9fdf8e4/746ed8d3-9f44-435a-b89b-146389112378.png)


*3* Click on **SAVE** to send an invitation to the provided email address. The invited user will receive an email containing a link to accept the invitation.

### Accepting the Invite

Upon receiving the invite email, the invited user can follow these steps to accept the invite:

*1* Click on the invite link received in the email.

*2*  Enter the email address.

![Email Address](https://images.tango.us/workflows/56571f94-f480-4ab2-be42-7a883aac7f4c/steps/b323e8a7-8843-4910-8520-e6e1b885af65/e86118e6-e70e-4e09-8e1b-43450399be06.png)

*3* Enter the OTP (One-Time Password) received via email.

![OTP](https://images.tango.us/workflows/56571f94-f480-4ab2-be42-7a883aac7f4c/steps/2c51f142-a578-4965-9626-190f77bdc966/1761b105-a5f2-4690-a84b-78b4a6ce82df.png)

*4* Set up **Multi-Factor Authentication (MFA)** by scanning the QR code and entering the OTP value.

![MFA](https://images.tango.us/workflows/56571f94-f480-4ab2-be42-7a883aac7f4c/steps/53d85d70-911d-488d-b399-40d609292552/7926f9de-2119-4cf7-927d-66f847e21483.png)

*5* Copy and securely store the provided MFA Recovery codes for future use.

![Recovery Codes](https://images.tango.us/workflows/56571f94-f480-4ab2-be42-7a883aac7f4c/steps/ae77f125-6dfa-4db7-9698-416f04a301f1/3f4c3c49-c596-4512-92c8-be1faa53ae45.png)

### Granting Access

After accepting the invitation, the co-admin won't gain access to the **Workspace** immediately. The admin (owner) needs to grant access by following these steps:

*1* Navigate to **Workspace > Settings > Workspace Admins**.

*2*  Choose the co-admin to whom you wish to provide access.

*3* Toggle the switch to grant access.

*4* Scroll down and click on **SAVE** to finalize the changes.

![Approval](https://images.tango.us/workflows/56571f94-f480-4ab2-be42-7a883aac7f4c/steps/f0075684-9c73-4ae4-b6cb-1fbdb9ac8ec6/877a7206-e8ac-475c-bdfd-4ef3e0d536fe.png?mark-x=497&mark-y=496&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNEQjMwMzAmdz0xMDUmaD04NCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)

### Co-admins Permissions

Co-admins have specific permissions within the workspace. While they can perform various administrative tasks, there are actions reserved exclusively for the account creator (owner).

Some of the permissions are mentioned below:

| Permissions                    | Workspace Owner (Creator) | Co-admins                         |
| ------------------------------ | ------------------------- | --------------------------------- |
| Change Workspace Owner         |                       ✔️  |                         **X**     |
| Add Co-admins                  |                       ✔️  |                         ✔️        |
| Delete Workspace (Hard Delete) |                       ✔️  |                         **X**     |
| Delete Workspace (Soft Delete) |                       ✔️  |                        ✔️         |
| Manage Access Keys             |                       ✔️  |                        **X**      |
| Manage Encryp & Decryp Keys    |                       ✔️  |                        **X**      |
| Update Subscription Plan       |                       ✔️  |                       **X**       |
| Demote Co-admins               |                       ✔️  |                       **X**       |
| Delete Co-admins               |                       ✔️  |                       ✔️          |