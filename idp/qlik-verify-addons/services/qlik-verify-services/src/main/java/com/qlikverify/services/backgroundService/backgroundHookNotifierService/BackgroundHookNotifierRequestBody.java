package com.qlikverify.services.backgroundService.backgroundHookNotifierService;

import com.qlikverify.services.backgroundService.BackgroundJobRequestBody;

public class BackgroundHookNotifierRequestBody implements BackgroundJobRequestBody {
    public final String mode;
    public final String notification_endpoint;
    public final String notification_endpoint_auth;
    public final String notification_body;

    public  final String fallback_details;
    private String triggeredFor;


    public BackgroundHookNotifierRequestBody(String mode, String notificationEndpoint,
                                             String notificationEndpointAuth, String notificationBody,
                                             String fallbackDetails) {
        this.mode = mode;
        notification_endpoint = notificationEndpoint;
        notification_endpoint_auth = notificationEndpointAuth;
        notification_body = notificationBody;
        fallback_details = fallbackDetails;
    }

    public String getTriggeredFor() {
        return triggeredFor;
    }

    public void setTriggeredFor(String triggeredFor) {
        this.triggeredFor = triggeredFor;
    }
}
