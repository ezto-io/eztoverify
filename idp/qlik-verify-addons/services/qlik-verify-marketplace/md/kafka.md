#### Apache Kafka Configuration Instructions

Apache Kafka is a community distributed event streaming platform capable of handling trillions of events a day

1. Type in the following information to set up Apache Kafka:

   `Kafka Bootstrap Server`: Set kafka bootstrap server. Expected format 127.0.0.1:9092 and for cluster just node with ','.

   `Kafka Topic`: This is an required field. Set a kafka topic to forward events to the topic

   `Kafka Admin Event Topic`: This is an optional field. Set to route admin events to be forwarded to the topic. If topic is not provided and admin events are enabled, it will be routed to `Kafka topic` 

   `Send Admin Events` : Enable to send admin events as well to Apache Kafka

   `Events Types` : Select the event types that needs to be forwarded to Splunk, if none selected no events will be pushed.


2. Click `Test Configuration`, a test event will be send to the kafka to ensure the configuration looks good.


3. Click `Save`. After kafka creation, you will see live near the Apache Kafka Service .


#### Note

>**If the Apache Kafka Bootstrap servers are not reachable or out of order automatically the streamer will be disabled**
