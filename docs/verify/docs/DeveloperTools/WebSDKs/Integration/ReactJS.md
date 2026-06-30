---
id: ReactJS
title: React JS
sidebar_label: React JS
--- 

Integrating **ezto verify's** Web SDK into your **React.js** project is a streamlined process. Follow these steps to add the necessary files and initiate the verification process within your **React** application.

## Implement the SDK in Component

Set up your **React.js** component to utilize the **ezto verify SDK:**

```jsx title="jsx"
import { useEffect } from 'react';

const YourComponent = () => {

    useEffect(() => {
       // code to be executed when component mount
    }, []); 

    const eztoverify = new window.eztoverify();
    
    const webSDKRequest = () => {
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
        eztoverify.request(metadata, cnfg, callback); 
    }

    const callback = (response) => {
        console.log('Response:', response);
    };

    return (
        <div>
        {/* JSX content here */}
        <button onClick={webSDKRequest}>Start verification</button>
        </div>
    );
};

export default YourComponent;
```