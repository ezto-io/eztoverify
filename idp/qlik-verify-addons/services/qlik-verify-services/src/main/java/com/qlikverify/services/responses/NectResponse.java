package com.qlikverify.services.responses;

import java.util.HashMap;

public class NectResponse {
    private String id;
    private ResponsePoiObject poi;
    private HashMap<String, String> control;
    private StatusObject status;

    public NectResponse() {
        // NOP
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ResponsePoiObject getPoi() {
        return poi;
    }

    public void setPoi(ResponsePoiObject poi) {
        this.poi = poi;
    }

    public HashMap<String, String> getControl() {
        return control;
    }

    public void setControl(HashMap<String, String> control) {
        this.control = control;
    }

    public StatusObject getStatus() {
        return status;
    }

    public void setStatus(StatusObject status) {
        this.status = status;
    }
}
