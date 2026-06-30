#### AWS EventBridge Configuration Instructions

Amazon EventBridge is a serverless event bus that makes it easier to build event-driven applications at scale using events generated from your applications, integrated Software-as-a-Service (SaaS) applications, and AWS services.
You shall find more details [here](https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-what-is.html).

#### How to setup AWS Eventbridge for QlikVerify Audit

You shall go through a detailed walkthrough [here](https://www.canva.com/design/DAEiOhKN0lY/watch)

**Event Pattern**

`
{
"source": [
"{tenantname}.qlikverify.com"
],
"detail-type": [
"test_event",
"log_event",
"admin_event"
]
}
`

#### How to setup configuration

1. Type in the following information to set up Apache Kafka:

   `AWS Access Key Id`: Aws Access key id , which you can find under iam credentials in aws console.

   `AWS Secret Access Key`: Aws Secret access key , which you will get download when you create iam credentials in aws.

   `Event Bus Name`: This is an required field, where you enter the event bus name

   `AWS Region`: This is a required field, select the AWS region where the Eventbridge is enabled.
   
   `Send Admin Events` : Enable to send admin events as well to AWS Eventbridge

   `Events Types` : Select the event types that needs to be forwarded to AWS Eventbridge, if none selected no events will be pushed.


2. Click `Test Configuration`, a test event will be send to the AWS Eventbridge to ensure the configuration looks good.


3. Click `Save`. After AWS Eventbridge creation, you will see live near the AWS Eventbridge Service .


#### Note

>**If the AWS Eventbridge are not reachable or out of order automatically the streamer will be disabled**
