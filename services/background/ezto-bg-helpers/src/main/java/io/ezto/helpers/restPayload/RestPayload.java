package io.ezto.helpers.restPayload;

import lombok.Data;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestPayload {
    private String url;
    private String mediaType;
    private String method;
    private HashMap<String, Object> body;
    private String auth;
    private HashMap<String, String> headers;
    private HashMap<String, Long> retry;
}
