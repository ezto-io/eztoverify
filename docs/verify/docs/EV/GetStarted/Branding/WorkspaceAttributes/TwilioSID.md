---
id: TwilioSID
title: Twilio SID
sidebar_label: Twilio SID
--- 
  
The **Twilio SID** configuration is displayed when the custom [WhatsApp](/docs/EV/GetStarted/Configurations/WhatsApp) provider is enabled. It allows you to set up your own **Twilio** account as the messaging provider by specifying template SIDs for OTPs and transaction notifications.

## Configuring Twilio SID

*1* Navigate to **Workspace** and click on **Settings.**

*2* Click on the **Workspace Attributes** tab.

*3* Locate **Twilio SID** and click on it.

*4* You will be redirected to the **Twilio SID Configuration** page.

![Twilio SID](https://images.tango.us/workflows/f45de70d-1457-4535-9c0f-89e3f12ea93f/steps/1f9c4c24-c637-4828-ac62-10a4ed0dbc71/56ce281a-d5d9-4ca5-a1db-5b6a91ad79d1.png)

*5* The Twilio SID configuration options will be displayed.

*6* Provide the **Twilio OTP Template SID**, and **Twilio Transaction Template SID**.

![Configurations](https://images.tango.us/workflows/f45de70d-1457-4535-9c0f-89e3f12ea93f/steps/5320f19d-dc06-4f22-b52b-48926c78920c/849225fb-430d-4415-af31-496aa27948dd.png)

## Manage FIDO Configs

| Name | Description |
|------|-------------|
| Twilio OTP Template SID | Provide the **Template SID** for OTP messages from Twilio. This SID is required to send OTPs via Twilio's messaging service. To know how to get this SID, see [here](https://www.twilio.com/docs/content/manage-your-templates-with-the-content-template-builder). Note that the template associated with this SID must contain a **single variable "{{1}}"** in its content. |
| Twilio Transaction Template SID |  Provide the **Template SID** for transaction messages from Twilio. This SID is used for sending transactional notifications. To know how to get this SID, see [here](https://www.twilio.com/docs/content/manage-your-templates-with-the-content-template-builder). Note that the template associated with this SID must contain **two variables "{{1}}", "{{2}}"** in its content. |

*7* Once all configurations are completed, click on **SAVE**.

#### For further assistance or related topics, you may explore the following resource:

- [WhatsApp](/docs/EV/GetStarted/Configurations/WhatsApp)