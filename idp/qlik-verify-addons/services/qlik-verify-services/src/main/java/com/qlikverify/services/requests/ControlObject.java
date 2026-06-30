package com.qlikverify.services.requests;

public class ControlObject {
    private int intent;
    private int loa;
    private String jumpResponseURI;
    private String lang;
    private String callback;
    private boolean enableMock;
    private int mockType;

    public ControlObject() {
        // NOP
    }

    public int getIntent() {
        return intent;
    }

    public void setIntent(int intent) {
        this.intent = intent;
    }

    public int getLoa() {
        return loa;
    }

    public void setLoa(int loa) {
        this.loa = loa;
    }

    public String getJumpResponseURI() {
        return jumpResponseURI;
    }

    public void setJumpResponseURI(String jumpResponseURI) {
        this.jumpResponseURI = jumpResponseURI;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public boolean isEnableMock() {
        return enableMock;
    }

    public void setEnableMock(boolean enableMock) {
        this.enableMock = enableMock;
    }

    public int getMockType() {
        return mockType;
    }

    public void setMockType(int mockType) {
        this.mockType = mockType;
    }
}
