---
id: VanillaJS
title: Vanilla JS
sidebar_label: Vanilla JS
--- 

This setup allows for a simple and effective way to integrate the **ezto verify's** Web SDK into your project using **Vanilla JS**, enabling you to start the verification process with minimal configuration.

## Include ezto verify SDK and CSS

Set up your **Vanilla JS** component to utilize the **ezto verify SDK:**

```html title="html"
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/ezto-io/dev-web-push@latest/eztoverify.min.css" />
  <title>Vanilla JS App</title>
</head>
<body>
  <button id="startVerify">Start verification</button>
  <script src="https://cdn.socket.io/4.7.2/socket.io.min.js" type="text/javascript"></script>
  <script src="https://cdn.jsdelivr.net/gh/ezto-io/dev-web-push@latest/eztoverify.min.js" type="text/javascript"></script>
  <script>
    var btn = document.getElementById('startVerify');
    btn.onclick = function () {
        let ez = new eztoverify();
        ez.request({
          request: JSON.stringify({
              "user": {
                  "metadata": {
                      "workflow": "" // onboarding or verification
                  },
                  "email": "example@gmail.com"
              }
            }
          ), // Build the request body using the API builder, and input it here.
        }, 
        {
          api: "{{base_url}}/auth/realms/api/ezto_web_push/{{appId}}/{{workspaceName}}/register",
          apiVersion:"1"
        }, 
        function (res) {
          console.log(res);
          // Handle the result here
        });
    }
  </script>
</body>
</html>

```

## Implement the SDK Request

Set up the request functionality to use the **ezto verify SDK.** The script included in your HTML handles the request when the button is clicked:

```javascript
var btn = document.getElementById('startVerify');

btn.onclick = function() {
    let ez = new eztoverify();
    
    ez.request({
      request: JSON.stringify({
          "user": {
              "metadata": {
                  "workflow": "" // onboarding or verification
              },
              "email": "example@gmail.com"
          }
        }
      ), // Build the request body using the API builder, and input it here.
    }, {
        api: "{{base_url}}/auth/realms/api/ezto_web_push/{{appId}}/{{workspaceName}}/register"
    }, 
    function(res) {
        console.log(res);
        // Handle the result here
    });
}
```