---
id: Svelte
title: Svelte
sidebar_label: Svelte
--- 

Integrating **ezto verify's** Web SDK into your **Svelte** project is simple and efficient. Follow these steps to include the necessary files and initiate the verification process within your **Svelte** application.

## Implement the SDK in Component

Set up your **Svelte** component to utilize the **ezto verify SDK:**

```html title="javascript"

<div>
	<button on:click={webSDKRequest}>Start verification</button>
</div>

<script>
  const webSDKRequest = () => {
    const ez = new eztoverify();
    const metadata = {
        request: JSON.stringify(
            {
                "user": {
                    "metadata": {
                        "workflow": "" // onboarding or verification
                    },
                    "email": "example@gmail.com"
                }
            }
        ) // Build the request body using the API builder, and input it here.
    }; 
    const cnfg = {
        api: "{{base_url}}/auth/realms/api/ezto_web_push/{{appId}}/{{workspaceName}}/register",
        apiVersion:"1"
    }; 
    ez.request(metadata, cnfg, callback); 
}

const callback = (response) => {
    console.log('Response:', response);
};
</script>

```