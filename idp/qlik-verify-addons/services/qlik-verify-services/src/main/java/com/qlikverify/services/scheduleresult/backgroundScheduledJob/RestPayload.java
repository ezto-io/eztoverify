package com.qlikverify.services.scheduleresult.backgroundScheduledJob;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;

@Data
@Builder
public class RestPayload {
    private String url;
    private String mediaType;
    private String method;
    private HashMap<String, Object> body;
    private String auth;
    private HashMap<String, String> headers;
}
