package com.qlikverify.services.responses;

public class ControlObject {
    private String callback;
    private int intent;
    private int loa;
    private String lang;

    public ControlObject() {
        // NOP
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
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

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
