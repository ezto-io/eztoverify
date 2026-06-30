package com.qlikverify.extensions.admin.controllers.hook.models;

public class HookRequestModel {

    private String chatCode;
    private String secret;

    public String getChatCode() {
        return chatCode;
    }

    public void setChatCode(String chatCode) {
        this.chatCode = chatCode;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
