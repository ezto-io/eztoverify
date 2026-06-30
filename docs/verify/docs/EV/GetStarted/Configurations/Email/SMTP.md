---
id: SMTP
title: SMTP
sidebar_label: SMTP
--- 

**SMTP** facilitates the seamless exchange of emails by steering messages from a sender's email client or server towards the intended recipient's email server, ensuring precision in routing and timely delivery.

### Prerequisites

* You need to update own email provider in your subscription.
* Your **SMTP Server** must support:
  * TLS 1.0 or higher
  * LOGIN authentication

### Configure SMTP

*1* Navigate to **Workspace,** click on **Notification,** and choose **Email.**

*2* Click on `+ADD PROVIDER`

*3* You will be directed to the **New Email Provider** page, where all available providers will be displayed.

![SMTP](https://images.tango.us/workflows/27dff30f-aa65-4777-b39c-6523de64c182/steps/9abed88a-37dd-4a07-8ca1-3c03fc4f602e/4edbf776-ed70-425e-a53b-14bdd0d7c3e5.png)

*4* Choose the  **SMTP.**

*5* You will be taken to the **SMTP** provider configs page. To obtain the necessary configs, log into **SMTP.**

*6* Enter your **SMTP** configuration details into the respective fields.

![Configurations](https://images.tango.us/workflows/27dff30f-aa65-4777-b39c-6523de64c182/steps/47afa32b-8dfe-4473-ae29-8027f522ee60/932189d1-f597-4fdc-a8db-11deae2d999e.png)

### SMTP Settings

(\*) Indicates that the action is mandatory.



| Attributes            | Description                                                                                                                                                                                                                                                                                         |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| \*Host                | This field represents the **SMTP** server hostname utilized for email transmission.                                                                                                                                                                                                                 |
| Port                  | This indicates the **SMTP** server port.                                                                                                                                                                                                                                                            |
| From Display Name     | This feature enables the configuration of user-friendly email address aliases (optional). If left unset, the email clients will display the plain From email address.                                                                                                                               |
| \*From                | This field represents the address utilized for the **From SMTP-Header** in outgoing emails.                                                                                                                                                                                                         |
| Reply To Display Name | This feature allows the configuration of user-friendly email address aliases for replies (optional). If left unset, the plain Reply To email address will be displayed.                                                                                                                             |
| Reply To              | This field denotes the address used for the **Reply-To SMTP-Header** in outgoing emails (optional). If not set, the plain From email address will be used.                                                                                                                                          |
| Envelope From         | This field denotes the **Bounce Address** utilized for the **Return-Path SMTP-Header** in outgoing emails (optional).                                                                                                                                                                               |
| Enable SSL            | For security reasons, especially when dealing with emails for recovering usernames and passwords, it is advisable to use SSL. This is particularly important if the _SMTP_server is on an external network. In such cases, you will likely need to modify the Port (the default SSL port is 465).   |
| Enable Start TLS      | For security reasons, especially when dealing with emails for recovering usernames and passwords, it is recommended to use **TLS**. This is particularly important if the **SMTP** server is on an external network. You will likely need to modify the Port as well (the default TLS port is 465). |
| Enable Authentication | If your **SMTP** server mandates authentication, click on **Enable Authentication** and enter the username and password. The password field can reference a value from an external vault and will not be stored in our database.                                                                    |


*7* After providing the necessary configurations.

*8* Click on `TEST CONFIGURATION` to ensure the configuration looks good.

![Test Configurations](https://images.tango.us/workflows/27dff30f-aa65-4777-b39c-6523de64c182/steps/6bb17fbf-3813-40ea-a9aa-391d9f4cba8f/8d49bdf7-9b67-4d84-b108-aa3615f88aa1.png)

*9* Provide email address and click on **SUBMIT.**

*10* If everything is set up correctly, a test success email will be sent to the email address you provided earlier. Click on the **SAVE** button**.**
