#### Splunk Configuration Instructions

Learn about Splunk Event Audit Streams and export your events in near real-time to Splunk.

#### In Splunk:
This tutorial follows the steps for Splunk Cloud. In case this is the first HEC (HTTP Event Collector) you will configure for your account, make sure that the Event Collector is enabled. You can find details on how to do this [here](https://docs.splunk.com/Documentation/Splunk/latest/Data/UsetheHTTPEventCollector). For information on Splunk Enterprise set-up, please click [here](https://docs.splunk.com/Documentation/Splunk/8.0.4/Data/UsetheHTTPEventCollector#Configure_HTTP_Event_Collector_on_Splunk_Enterprise).

In order to configure a new HTTP Event Collector for Auth0 logs and acquire the Domain, Token and Port information, follow the next steps:

#### Retrieving the Domain, Token, and Port
1. Navigate to your Splunk instance. Copy the domain part of the URL, this is your Splunk **Domain**. (You may have received this information via email upon Splunk signup as well).
2. From the system menu select `Settings > Data Inputs`. Select the `Add New`link under `Local Inputs > HTTP Event Collector`.
3. A wizard, that will configure a new token for receiving data over HTTP, is displayed. Set a name for this new token and click Next. We recommend naming it `qlikverify`.
4. Select a Source type and an Index. We will create a new Source type, named `qlikverify`, and use main as our Index. Click Review.
5. Review the information displayed and click Submit.
6. Your new token should be created successfully. Copy the value, this is your **Token**.
7. The default **port** is **8088**.

#### View Logs in Splunk
1. Log into your Splunk instance
2. In the menu bar go to `App: Cloud Monitoring....`
3. Click `Search & Reporting` in the sub-menu.
4. In search bar, input wildcard * and adjust time drop down to desired window.

#### General Configuration

`Send Admin Events` : Enable to send admin events as well to Splunk

`Events Types` : Select the event types that needs to be forwarded to Splunk, if none selected no events will be pushed.

#### Note
>**1.If you checked the `Enable indexer acknowledgment` in Splunk's token settings, please uncheck the option to save the configuration successfully.<br />
   2.If the Splunk is not reachable or out of order automatically the streamer will be disabled.**

