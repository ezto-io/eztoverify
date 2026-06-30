package com.qlikverify.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;
/*
* {
    "account_sid": "AC8602200135969093015148517be0e928",
    "api_version": "2010-04-01",
    "body": "Your Verification Code is: 587562",
    "date_created": "Fri, 17 Jan 2025 10:57:30 +0000",
    "date_sent": "Fri, 17 Jan 2025 10:57:31 +0000",
    "date_updated": "Fri, 17 Jan 2025 10:57:35 +0000",
    "direction": "outbound-api",
    "error_code": null,
    "error_message": null,
    "from": "+18508088612",
    "messaging_service_sid": null,
    "num_media": "0",
    "num_segments": "1",
    "price": "-0.08320",
    "price_unit": "USD",
    "sid": "SMf5d510f8c6cecada8a0181a2a16fe4df",
    "status": "delivered",
    "subresource_uris": {
        "feedback": "/2010-04-01/Accounts/AC8602200135969093015148517be0e928/Messages/SMf5d510f8c6cecada8a0181a2a16fe4df/Feedback.json",
        "media": "/2010-04-01/Accounts/AC8602200135969093015148517be0e928/Messages/SMf5d510f8c6cecada8a0181a2a16fe4df/Media.json"
    },
    "to": "+919677546596",
    "uri": "/2010-04-01/Accounts/AC8602200135969093015148517be0e928/Messages/SMf5d510f8c6cecada8a0181a2a16fe4df.json"
}*/

@Data
public class TwilioMessageModel {
    @JsonProperty("account_sid")
    private String accountSid;
    @JsonProperty("api_version")
    private String apiVersion;
    private String body;
    @JsonProperty("date_created")
    private String dateCreated;
    @JsonProperty("date_sent")
    private String dateSent;
    @JsonProperty("date_updated")
    private String dateUpdated;
    private String direction;
    @JsonProperty("error_code")
    private String errorCode;
    @JsonProperty("error_message")
    private String errorMessage;
    private String from;
    @JsonProperty("messaging_service_sid")
    private String messagingServiceSid;
    @JsonProperty("num_media")
    private String numMedia;
    @JsonProperty("num_segments")
    private String numSegments;
    private String price;
    @JsonProperty("price_unit")
    private String priceUnit;
    private String sid;
    private String status;
    @JsonProperty("subresource_uris")
    private Map<String, String> subresourceUris;
    private String to;
    private String uri;
}