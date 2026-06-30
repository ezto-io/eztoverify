#### API & Webhook Configuration Instructions

Webhooks provide a way for events to be delivered to an external web server. QlikVerify offers several out-of-the-box integrations that automatically push events to third-party log management and analysis systems. If however your third-party system isn't supported with an out-of-the-box integration, you can easily set up a custom webhook integration.

1. Type in the following information to set up a custom webhook:

    `Webhook URL`: Target URL where the HTTP POST request should push the payload.

    `Authorization Token`: This is an optional field and will be set in the Authorizationheader of the request to your webhook if provided. `eg: Bearer {token}`


2. Click `Test Configuration`, a test event will be send to the webhook to ensure the configuration looks good.


3. Click `Save`. After webhook creation, you will see live near the Webhook Service .

#### General Configuration

`Send Admin Events` : Enable to send admin events as well to Splunk

`Events Types` : Select the event types that needs to be forwarded to Splunk, if none selected no events will be pushed.


#### Note
>**The payload will be delivered in JSON format. Please keep this in mind while consuming the logs in your webhook.**

>**If the webhook url is not reachable or out of order automatically the streamer will be disabled**
