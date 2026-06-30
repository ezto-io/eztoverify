---
id: VueJS
title: Vue JS
sidebar_label: Vue JS
--- 

Integrating **ezto verify'**s Web SDK into your **Vue.js** project is a straightforward process. Follow these steps to include the necessary files and initiate the verification process within your **Vue** application.

## Implement the SDK in Component

Set up your **Vue.js** component to utilize the **ezto verify SDK:**

```html title="javascript"
<template>
  <button @click="handleRequest">Start verification</button>
</template>

<script>
// Import necessary functions and dependencies

export default {
  methods: {
    handleRequest() {
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

      const callback = (response) => {
        // Handle the response here
        console.log('Response:', response);
      };

      // Call eztoverify.request
      const ezVerifier = new window.eztoverify();
      ezVerifier.request(metadata, cnfg, callback);
    }
  }
}
</script>
```
