---
id: WhatsApp
title: WhatsApp
sidebar_label: WhatsApp
--- 

Our default **WhatsApp** provider is accessed through our official provider, [Twilio](https://www.twilio.com/). If you have a **WhatsApp** provider from a different service, **ezto verify** allows you to configure your own **WhatsApp** provider. This offers greater flexibility and control over your messaging.

To take advantage of this feature, ensure your subscription plan is updated to **"Own"** **WhatsApp** provider.

### Use case

The primary purpose of configuring a custom **WhatsApp** provider is to deliver messages to recipients that are not supported by the default options. Additionally, you can:

* Perform high engagement rates
* Change recipients
* Modify message payloads

### Prerequisites {#whatsapp-prerequisite}

* You need to update **"Own"** **WhatsApp** provider in your subscription.
* To configure **WhatsApp** with **ezto verify,** you must have an active [Twilio](https://www.twilio.com/) account.

### Access your Twilio

* Log in to your [Twilio](https://www.twilio.com/) account.
* Setup a **SenderNumber** for the project you have created in **Twilio**.
* Once the **SenderNumber** is successfully configured, copy the **SenderNumber**, **Account SID**, and **Auth Token**.

### Configure Custom WhatsApp Provider

> When you're using a custom WhatsApp provider, make sure to enter both Template SIDs. You can do this by navigating to **Workspace Settings > Workspace Attributes >** [Twilio SID.](/docs/EV/GetStarted/Branding/WorkspaceAttributes/TwilioSID)

*1* Navigate to **Workspace**, click on **Notification**, and choose **SMS**.

*2* Click on `+ADD PROVIDER`

*3* You will be directed to the **New SMS Provider** page, where all available providers will be displayed. Click on the **WhatsApp** tab.

![WhatsApp](https://images.tango.us/workflows/ef98af6e-ed77-4814-8695-0fde53e16733/steps/e901c2ca-99dd-4f80-a0a9-a14cb56d4e73/22bd5281-368c-4f66-a55f-de236736865a.png)

*4* Click on **WhatsApp**.

*5* You will be taken to the **WhatsApp** provider configs page. To obtain the necessary configs, log into [Twilio](https://www.twilio.com/). Refer to the [prerequisites](#whatsapp-prerequisite) on how to do it.

![Configurations](https://images.tango.us/workflows/ef98af6e-ed77-4814-8695-0fde53e16733/steps/caa91f82-1b97-49a7-a1ec-eaf2dea58e1c/60de66b5-3a27-4b6f-91cb-065b28d57453.png)

*6* Navigate back to your **Workspace** and input the **SenderNumber, Account SID,** and **Auth Token**.

> Make sure both Template SIDs are added under **Workspace Settings > Workspace Attributes >** [Twilio SID.](/docs/EV/GetStarted/Branding/WorkspaceAttributes/TwilioSID)

*7* After providing the necessary configurations.

*8* Click on `TEST CONFIGURATION` to ensure the configuration looks good.

![Test Configuration](https://images.tango.us/workflows/ef98af6e-ed77-4814-8695-0fde53e16733/steps/21e61cee-843d-4564-bb45-81a771954e6c/aabed70e-467d-4685-8b85-d5a6a76e60e5.png)

*9* Provide phone number and click on **SUBMIT**.

*10* If everything is set up correctly, a test success **SMS** will be sent to the phone number you provided earlier.

*11* Click on the **SAVE** button.

### Remove Custom SMS Provider

If you wish to switch to another provider or remove a provider, you must first create a new provider before removing the existing one. Alternatively, you can change your subscription to revert to the **ezto verify's "Default"** provider.

#### To remove your custom provider completely:

*1* Navigate to **Workspace**, click on **Notification**, and choose **SMS**.

*2* You will be presented with a list of existing (created) providers. Click on the **WhatsApp** tab, choose the provider.

> If you want to modify the provider instead of deleting it, click on the edit icon next to the provider's name.

*3* Click on the name of the provider you want to delete. Scroll down to the bottom of the page to the **Danger Zone** section and click on the **DELETE** button.

![Remove Provider](https://images.tango.us/workflows/ef98af6e-ed77-4814-8695-0fde53e16733/steps/3d9feea8-a7b6-4dd9-8bc2-32e4f9c0f9df/1f5d87c3-a3ba-417e-ad6e-3d3818a07039.png)

*4* A pop-up will appear, cautioning you that this action will permanently delete the provider. Confirm your intention by clicking on **DELETE**.

By following these steps, you can manage your provider settings effectively and ensure that your **WhatsApp** communications are handled according to your needs.