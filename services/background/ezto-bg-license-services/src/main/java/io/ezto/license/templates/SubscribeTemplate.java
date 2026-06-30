package io.ezto.license.templates;

import io.ezto.bg.core.IEmailTemplate;

import java.util.Map;

public class SubscribeTemplate extends IEmailTemplate {

    private String toEmail;
    private Map<String, String> placeHolders;
    @Override
    public String getTemplatePath() {
        return "subscribe.ftl";
    }

    @Override
    public String getSubject() {
        return "subscribe_subject";
    }

    @Override
    public void setSubject(String subject) {
        // NOP
    }

    @Override
    public String getId() {
        return "subscribe";
    }

    @Override
    public String getToEmail() {
        return toEmail;
    }

    @Override
    public IEmailTemplate setToEmail(String toEmail) {
        this.toEmail = toEmail;
        return this;
    }

    @Override
    public IEmailTemplate setPlaceHolders(Map<String, String> params) {
        this.placeHolders = params;
        return this;
    }

    @Override
    public Map<String, String> getPlaceHolders() {
        return this.placeHolders;
    }
}
