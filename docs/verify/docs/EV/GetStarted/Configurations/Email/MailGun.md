---
id: MailGun
title: MailGun
sidebar_label: MailGun
--- 

**MailGun** serves as a reputable email automation and delivery service crafted to aid businesses in effectively sending transactional and marketing emails. It provides an intuitive platform for crafting and overseeing email campaigns, guaranteeing the dependable delivery of messages to recipients' inboxes.

### Prerequisites {#mailgun-prerequisite}

* You need to update own email provider in your subscription.
* You must have an active [MailGun](https://www.mailgun.com/) account.

### Access your MailGun

* Log in to your [MailGun](https://www.mailgun.com/) account.
* Begin by obtaining a new **API** as your first step.
* Go to **API Security** tab, copy the **Private API Key** to the clipboard.

![API](https://images.tango.us/workflows/8ce9d27c-0509-4aa9-85e1-88b66446a652/steps/675d6002-4ee7-49f0-8ba3-e526e17b8145/9d340b82-c002-4f22-b3b0-53f0b1c2605b.png)

* Expand the **Sending** option from side menu, and then surf to **Domain** section. Copy the **Sandbox** url from this section to the clipboard.

![Domain](https://images.tango.us/workflows/8ce9d27c-0509-4aa9-85e1-88b66446a652/steps/c7ddb164-7134-45b8-bb9c-daed7d123fa6/bf51fa3f-9fb4-4079-a9ac-4a60b67c99da.png)

> If you are utilizing a **MailGun Free Sandbox** account, it's necessary to register your email addresses with the mailbox sandbox account in order to receive emails from **MailGun.**

* Go to **Sending** section, click on **Domain setting,** and copy the **From** email address under **SMTP Credentials** section. If it's a premium account, use the registered from email address.

![SMTP](https://images.tango.us/workflows/8ce9d27c-0509-4aa9-85e1-88b66446a652/steps/95ca30c2-e51c-4f53-b0e8-090ac99a422f/7806593c-ccc3-4308-820f-8e160392e8db.png)

### Configure MailGun

*1* Navigate to **Workspace,** click on **Notification,** and choose **Email.**

*2* Click on `+ADD PROVIDER`

*3* You will be directed to the **New Email Provider** page, where all available providers will be displayed.

![Mailgun](https://images.tango.us/workflows/8ce9d27c-0509-4aa9-85e1-88b66446a652/steps/4f23ed0c-ed6b-47ea-8215-cf63f6450175/3fda0c6c-bae1-4884-9c48-74c903663cb3.png)

*4* Choose the  **MailGun.**

*5* You will be taken to the **MailGun** provider configs page. To obtain the necessary configs, log into **MailGun.** Refer to the [prerequisites](#mailgun-prerequisite) on how to do it.

![Configurations](https://images.tango.us/workflows/8ce9d27c-0509-4aa9-85e1-88b66446a652/steps/e50ee6dc-8807-40ce-980c-0f5b35e08505/388c6b13-9c34-4a37-993d-4091770a0ae1.png)

*6* Navigate back to your **Workspace** and input the **API, Sandbox url, and From email.**

*7* After providing the necessary configurations.

*8* Click on `TEST CONFIGURATION` to ensure the configuration looks good.

![Test Configuration](https://images.tango.us/workflows/8ce9d27c-0509-4aa9-85e1-88b66446a652/steps/5e31ba16-940a-4abe-8ffa-1bb181debc52/ad971af8-7b86-415a-9133-c5fc57741a12.png)

*9* Provide email address and click on **SUBMIT.**

*10* If everything is set up correctly, a test success email will be sent to the email address you provided earlier. Click on the **SAVE** button**.**