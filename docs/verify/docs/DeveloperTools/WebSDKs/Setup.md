---
id: Setup
title: Setup
sidebar_label: Setup
--- 

Integrating **ezto verify's Web SDK** into your project is simple and doesn't require any build steps. Follow the steps below to get started by including the production-ready CSS and JavaScript via CDN.

> For **Passkeys**, when a transaction is opened in a new tab using the web SDK, it relies on the [Related Origin Request (ROR)](https://passkeys.dev/docs/advanced/related-origins/#:~:text=Related%20Origin%20Requests%20(ROR)%20works,a%20list%20of%20valid%20origins). This feature is supported only in the latest versions of browsers and is not compatible with Firefox. 

<br />

## *1* Add CDN to Project
 
- Add the `ezto verify` and `socket.js` bundle using CDN, by including the following script tags in your HTML to load the necessary JavaScript files:
 
```html
<script src="https://cdn.socket.io/4.7.2/socket.io.min.js" type="text/javascript"></script>
<script src="https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/eztoverify.min.js" type="text/javascript"></script>
```

<br />

## *2* Add ezto verify CSS 
 
Include the following link tag in your HTML to load the necessary CSS file:

```html
<link href="https://cdn.jsdelivr.net/gh/ezto-io/web-push@latest/eztoverify.min.css" rel="stylesheet" />
```
 
<br />

## *3* Call Request Function

To use the SDK, call the `request` method provided by the ezto verify script included via CDN. Refer [here](/verify/docs/DeveloperTools/WebSDKs/Integration/VanilaJS/) for the integration.

``` javascript

    const ez = new eztoverify();
    
    ez.request({
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
    }, {
        api: "{API}"
    }, function(res) {
        //Handle the result here;
    });

```

:::note
For additional information on configuring the **Web SDK**, please see [here.](/verify/docs/EV/StartAnIntegration/SetupVerification/Web/)
:::