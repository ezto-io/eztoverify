package com.qlikverify.services.requests;

public class PoiObject {
    private String reference;
    private  String context;

    public PoiObject() {
    }

    public PoiObject(String reference, String context) {
        this.reference = reference;
        this.context = context;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
