---
id: AzureB2C
title: AzureB2C
sidebar_label: AzureB2C
--- 

# Azure B2C

**Azure B2C** is a cloud identity management service that enables you to customize and control how customers sign up, sign in, and manage their profiles when using your applications.

> Every application execution requires an **Onboarding** and **Verification** workflow. The onboarding workflow (can be any) will be used for a new user. You can configure it as **"None"** for a user who has already been onboarded. Please note that selecting the "None" option for the onboarding workflow will automatically redirect the user to the verification workflow, irrespective of the users availability.

### Access Application

*1* Navigate to **Workspace,** and click on **Applications.**

*2* Choose the application you wish to execute the flow with configuration as **Azure B2C.**

*3* Click on the **Configuration** tab, and select  **Azure B2C** in the **Verification Way.**

![Azure B2C](https://images.tango.us/workflows/c8bded05-d06d-4db8-960f-5b3a8aa993d6/steps/221a9e6d-2402-4b63-ba53-7ca75fb72ee9/54a9845d-bafe-44be-a04b-7eaa55a08b92.png)

*4* You will be displayed with the **Azure B2C** settings.

![Azure B2C Settings](https://images.tango.us/workflows/c8bded05-d06d-4db8-960f-5b3a8aa993d6/steps/dde16fb2-d8dc-458e-b751-eac2f31e6fef/75b3da1c-a0f0-4894-96e3-ca57d0e44a95.png)

### Azure B2C settings

(<span class="required-text">*</span>) Indicates an action to be mandatory.&#x20;

<table><thead><tr><th width="253">Name</th><th>Description</th></tr></thead><tbody><tr><td>Link Expiry</td><td>Set the duration after which the link will expire: 2 days (2d), 2 hours (2h), 2 minutes (2m), or 2 seconds (2s). The default is currently set to 120 seconds.</td></tr><tr><td>Whitelist Host URL</td><td>Ensure that the effective URL matches the whitelist URL to prevent unauthorized access.</td></tr><tr><td>Only Https</td><td>Enable or disable server certificate validity restriction when accessed from the web. This is recommended for production environments to ensure secure communication.</td></tr><tr><td>*Azure B2C Tenant ID</td><td>The <strong>Azure B2C tenant ID</strong>, which follows the format <code>yourtenant.onmicrosoft.com</code></td></tr><tr><td>Azure B2C Tenant ID Object</td><td>The unique identifier for your <strong>Azure B2C</strong> tenant object.</td></tr></tbody></table>

*5* Download the `B2C_1A_TRUSTFRAMEWORKBASE.xml` and `B2C_1A_SIGNIN.xml` files, and click on the `SAVE` button.

![Downloads](https://images.tango.us/workflows/c8bded05-d06d-4db8-960f-5b3a8aa993d6/steps/97509f0d-f01a-464b-b296-1b957d7da82f/e133601f-9678-4402-a881-9f3d1f90045b.png)

> For **Result Notification Way**, select **Hook**, and configure the **Hook Notification way** as `Web_sdk`

### Setup Azure B2C

*1* Login to your **Azure** account, go to **Identity Experience Framework** and click on **Upload custom policy.**

*2* Upload the downloaded XML files.

![Upload](https://images.tango.us/workflows/c8bded05-d06d-4db8-960f-5b3a8aa993d6/steps/272654a4-a56a-41f2-9f5d-da46dee1ddff/a035a773-9b28-4106-a008-d2786d804fd7.png)

*3* In the same page, under **Manage,** click on **Policy keys.**

![Public Keys](https://images.tango.us/workflows/c8bded05-d06d-4db8-960f-5b3a8aa993d6/steps/69bad4bc-f1a5-4809-bccf-050d9b76bba6/dc2042f8-6039-4a31-9483-d134c4c5a62f.png)

*4* Click on the `+Add` button to generate a signature key named `TokenSigningKeyContainer` with key type as `RSA` and key usage as `Signature`

![Create Key](https://images.tango.us/workflows/c8bded05-d06d-4db8-960f-5b3a8aa993d6/steps/b607467d-9af1-4f4f-9ee7-318795bb5995/015fd2ad-a6bf-4e81-9180-fa7d901e3eb8.png)

*5* The keys will be generated.

> The names of the generated keys may differ from those downloaded.

![Generated](https://images.tango.us/workflows/c8bded05-d06d-4db8-960f-5b3a8aa993d6/steps/659edc90-ef43-4f15-b727-8194d5ffe96c/9ccf5b03-7999-4e83-b968-28bf9859363a.png)

*6* To proceed with the sign-in, click on the created key an  copy the login endpoint from there.

![Endpoint](https://images.tango.us/workflows/c8bded05-d06d-4db8-960f-5b3a8aa993d6/steps/87be7a2c-b872-490f-8612-1aa3cd553db8/9310127d-b913-45ed-ba3b-5ae7b1ddecfe.png)

*7* Proceed with login using the copied endpoint.

*8* The verification flow will be triggered based on the configured workflow.&#x20;
