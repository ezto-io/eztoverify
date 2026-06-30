---
id: SandboxLive
title: Sandbox / Live Mode
sidebar_label: Sandbox / Live Mode
--- 

This guide covers the two primary modes in our platform: **Sandbox mode** and **Live mode**. Sandbox mode allows for safe exploring and experimentation, while Live mode is used for actual operations. You'll learn how to transition between these modes and adapt your configurations for seamless operation.

## *1* What is Sandbox Mode? {#what-is-sand-box}

**Sandbox mode** is the default mode when a new workspace is created on [ezto verify.](https://ezto.io/verify) This mode allows you to experiment and explore all the features that **ezto verify** offers. It’s a secure space designed for testing various actions, configurations, and workflows, enabling you to fully understand and utilize what **ezto verify** can do.

In **Sandbox** mode, you have access to all features (except for updating your subscription), allowing you to try out verification flows and test different functionalities. Sandbox mode comes with a default balance in the wallet, which can be used for actions such as trying out verification workflow modules.

However, once the wallet’s balance is depleted, you’ll be unable to perform additional actions that require funds. At this point, if you need to continue exploring or wish to update your subscription, you’ll need to contact our [support](https://support.ezto.io/support/home) to transition your workspace to **Live** mode, where you can add money to your wallet and access the full range of services.

<br/>

## *2* What is Live Mode?

**Live mode** provides full access to all features of [ezto verify](https://ezto.io/verify), including the ability to update your subscription plan and add money to your wallet. In this mode, you can modify your subscription plan to increase usage limits and ensure you can continue running verification workflows and other services without interruption.

It is designed for users who are ready to fully utilize the platform's capabilities beyond experimentation and exploration, enabling you to operate and manage your account with complete access to all services.

<br/>

## *3* Transition from Sandbox to Live Mode

To switch your workspace from **Sandbox** to **Live** mode, simply reach out to our [support team](https://support.ezto.io/support/home) to request the transition. Once the request is processed and approved, your workspace will be moved to **Live** mode, enabling all functionalities and subscription updates.

<br/>

## *4* Adapt Applications from Sandbox to Live Mode {#adaptFromSandLive} 

You can move an application from **Sandbox** mode to **Live** mode. When this change occurs, the **app ID** and **app secret** will be updated. Therefore, the app ID and **app secret** in **Sandbox** mode will differ from those in **Live** mode.

To move an application from Sandbox to Live mode:

- Navigate to **Workspace**, and click on **Applications**.

- Click on the **Sandbox** tab, choose the application you wish to move from **Sandbox** to **Live** mode.

- You will be taken to the **General** page. Disable the toggle for `Sandbox Mode` and click on the **SAVE** button.

Applications created while the workspace is in **Live** mode will appear under the **Live** tab. If you need to convert an application to **Sandbox** mode, enable the toggle and save your changes.

<br/>

## *5* Adapt Access Keys from Sandbox to Live Mode {#adaptAccessKeys} 

Access keys created in **Sandbox** can be moved to **Live** mode. However, when an access key is converted to **Live** mode, its secret key will be rotated. This means that the secret key in **Sandbox** will differ from the secret key in **Live** mode.

To move an access key from Sandbox to Live mode:

- Navigate to **Workspace Settings**, and click on **Access Key.**

- Select the access key that you wish to convert from **Sandbox** to **Live** mode.

- Disable the toggle for `Sandbox Mode` and click on the **SAVE** button.

Access keys created in **Live** mode will automatically fall under **Live**. If you need to convert an access key to **Sandbox** mode, enable the toggle and save your changes.

#### For further assistance or related topics, you may explore the following resources:

- [Application](/docs/EV/StartAnIntegration/CreateApplication)
- [Workspace Activity](/docs/EV/AdvancedSettings/WorkspaceActivity)
- [Audit](/docs/EV/Audit/AdminLogs)