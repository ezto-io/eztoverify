---
id: SMS
title: SMS
sidebar_label: SMS
--- 

Our default **SMS** provider meets the requirements of most applications providing significant advantages in terms of ease of integration, reliability, cost-effectiveness, security, scalability, simplified management, reduced maintenance, and consistent performance.

However, **ezto verify** offers you to configure your own **SMS** provider to help you manage, monitor, and troubleshoot your **SMS** communication. This allows for greater flexibility and control over your messaging.

To take advantage of this feature, ensure your subscription plan is updated to **"Own"** SMS providers**.** Note that only one **SMS** provider can be configured at a time, which will handle all your **SMS** communications.

Currently, we support [Twilio](https://www.twilio.com/) as a configurable **SMS** provider.

### Use case

The primary purpose of configuring a custom **SMS** provider is to deliver messages to recipients that are not supported by the default email options. Additionally, you can:

* Bulk messaging
* Change recipients
* Changing message payload

### Prerequisites {#sms-prerequisite}

* You need to update **"Own"** **SMS** provider in your subscription.
* You must have an active [Twilio](https://www.twilio.com/) account.

### Access your Twilio

* Log in to your [Twilio](https://www.twilio.com/) account.
* Setup a **SenderNumber** for the project you have created in **Twilio**.
* Once the **SenderNumber** is successfully configured, copy the **SenderNumber**, **Account SID**, and **Auth Token**.

### Configure Custom SMS Provider

*1* Navigate to **Workspace**, click on **Notification**, and choose **SMS**.

*2* Click on `+ADD PROVIDER`

*3* You will be directed to the **New SMS Provider** page, where all available providers will be displayed.

![Twilio](https://images.tango.us/workflows/83a785a5-b334-477c-bd00-f6687b7e954a/steps/a03c277a-b96d-402f-beec-705e36c2f425/35bc66b1-2706-4e11-854b-4c6f4a1e6720.jpeg)

*4* Choose the **Twilio**.

*5* You will be taken to the **Twilio** provider configs page. To obtain the necessary configs, log into **Twilio**. Refer to the [prerequisites](#sms-prerequisite) on how to do it.

![Configurations](https://images.tango.us/workflows/83a785a5-b334-477c-bd00-f6687b7e954a/steps/ea7ea4ed-cf80-4e83-8153-51c24e11f270/4e788eef-9b87-47d1-9d1c-ccc50b57408b.png)

*6* Navigate back to your **Workspace** and input the **SenderNumber, Account SID,** and **Auth Token**.

*7* After providing the necessary configurations.

*8* Click on `TEST CONFIGURATION` to ensure the configuration looks good.

![Test Configuration](https://images.tango.us/workflows/83a785a5-b334-477c-bd00-f6687b7e954a/steps/a899ee51-7275-4172-8321-d5bc5030c284/5f3fbe43-a09d-41ae-a90a-9cc1269d5144.png)

*9* Provide phone number and click on **SUBMIT**.

*10* If everything is set up correctly, a test success **SMS** will be sent to the phone number you provided earlier. Click on the **SAVE** button.

### Remove Custom SMS Provider

If you wish to switch to another provider, you must first create a new provider before removing the existing one. Alternatively, you can change your subscription to revert to the **ezto verify's "Default"** provider.

#### To remove your custom provider completely:

*1* Navigate to **Workspace**, click on **Notification**, and choose **SMS**.

*2* You will be presented with the existing (created) provider.

> If you want to modify the provider instead of deleting it, click on the edit icon next to the provider's name.

*3* Click on the name of the provider you want to delete. Scroll down to the bottom of the page to the **Danger Zone** section and click on the **DELETE** button.

![Remove Provider](https://images.tango.us/workflows/83a785a5-b334-477c-bd00-f6687b7e954a/steps/48f6b333-1e97-4677-9af3-44a72bce0b7b/e280d55d-91c2-41d4-8d80-e0ef4a51e776.png)

*4* A pop-up will appear, cautioning you that this action will permanently delete the provider. Confirm your intention by clicking on **DELETE**.

By following these steps, you can manage your **SMS** provider settings effectively and ensure that your **SMS** communications are handled according to your needs.
