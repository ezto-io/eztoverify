package com.qlikverify.services.requests;

public class CaseRequest {
    private PoiObject poi;
    private ControlObject control;

    public CaseRequest(PoiObject poi, ControlObject control) {
        this.poi = poi;
        this.control = control;
    }

    public PoiObject getPoi() {
        return poi;
    }

    public void setPoi(PoiObject poi) {
        this.poi = poi;
    }

    public ControlObject getControl() {
        return control;
    }

    public void setControl(ControlObject control) {
        this.control = control;
    }
}
