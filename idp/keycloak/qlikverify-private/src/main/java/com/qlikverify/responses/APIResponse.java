package com.qlikverify.responses;


import java.util.HashMap;
import java.util.Map;

public class APIResponse {
    private Map<Object, Object> response = new HashMap<>();

    public Map<Object, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<Object, Object> response) {
        this.response = response;
    }
}
