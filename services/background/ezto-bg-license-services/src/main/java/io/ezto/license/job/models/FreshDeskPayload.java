package io.ezto.license.job.models;

import lombok.Data;

import java.util.Map;

@Data
public class FreshDeskPayload {
    private String mediatype;
    private String method;
    private Map<String,Object> body;
    private String auth;
    private Map<String,String> headers;
    private Map<String,Object> retry;
}
