#### DataDog Configuration Instructions

Send your logs to Datadog over HTTP by setting up a Datadog event stream.

#### In Datadog:
1. Head to the Datadog dashboard.

2. Navigate to Integrations -> APIs.

3. Expand the API Keys section on this page, and then copy the API Key that you would like to use.

#### In QlikVerify:
1. Paste the API Key you copied above into the Datadog settings form.

2. Choose the Region in which you are using the Datadog dashboard.

3. Click Save.

#### In Datadog:
1. Navigate to Logs > Livetail.

2. See QlikVerify logs by setting the source to {tenantname}.qlikverify.com.

#### Note

>**Logs will be streamed to Datadog with the source set to {tenantname}.qlikverify.com**
