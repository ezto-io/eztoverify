package io.ezto.email.templates;

import io.ezto.bg.core.IEmailTemplate;

import java.util.Map;

public class AccountCreatorReqTemplate extends IEmailTemplate {

    private String toEmail;
    private String subject;
    private Map<String, String> placeHolders;
    @Override
    public String getTemplatePath() {
        return "account-creator-req.ftl";
    }

    @Override
    public String getSubject() {
        return "accountCreatorReqSubject";
    }

    @Override
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String getId() {
        return "account-creator-req";
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
