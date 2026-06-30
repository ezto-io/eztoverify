---
id: NextJS
title: Next JS
sidebar_label: Next JS
--- 

Integrating **ezto verify's** Web SDK into your **Next.js** project is efficient and requires minimal setup. Follow these steps to include the necessary resources and initiate verification within your **Next.js** application.

## Implement the SDK in Component

Set up your **Next.js** component to use the **ezto verify SDK:**

```js title="javascript"

import { useEffect } from 'react';

const YourComponent = () => {

  useEffect(() => {
    // Optionally, add initialization code here
  }, []);

  const handleClick = async () => {
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
      console.log('Response:', response);
      // Handle the response here
    };
    
    const ezVerifier = new window.eztoverify();
    ezVerifier.request(metadata, cnfg, callback);
  };

  return (
    <button onClick={handleClick}>Start verification</button>
  );
};

export default YourComponent;
```