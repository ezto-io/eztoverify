package com.qlikverify.extensions.admin.misc.requests;

import java.util.List;

public class SendMailRequest {
    private String mailType;
    private String redirectUri;
    private String appName;
    private String subject;
    private String textBody;
    private String supportEmail;
    private String iosLink;
    private String androidLink;
    private String portalUrl;
    private String tenantName;
    private String userName;
    private String activationCode;
    private String base64Qr;
    private String tempPassword;
    private List<String> requiredActions;
    private String templateName;
    private String tokenHandlerType;


    public SendMailRequest() {
    }

    public SendMailRequest(String mailType, String redirectUri, String appName, String subject, String textBody, String supportEmail, String iosLink, String androidLink, String portalUrl, String tenantName, String userName, String templateName, String tokenHandlerType) {
        this.mailType = mailType;
        this.redirectUri = redirectUri;
        this.appName = appName;
        this.subject = subject;
        this.textBody = textBody;
        this.supportEmail = supportEmail;
        this.iosLink = iosLink;
        this.androidLink = androidLink;
        this.portalUrl = portalUrl;
        this.tenantName = tenantName;
        this.userName = userName;
        this.templateName = templateName;
        this.tokenHandlerType = tokenHandlerType;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public String getBase64Qr() {
        return base64Qr;
    }

    public void setBase64Qr(String base64Qr) {
        this.base64Qr = base64Qr;
    }

    public String getMailType() {
        return mailType;
    }

    public void setMailType(String mailType) {
        this.mailType = mailType;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(String textBody) {
        this.textBody = textBody;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getIosLink() {
        return iosLink;
    }

    public void setIosLink(String iosLink) {
        this.iosLink = iosLink;
    }

    public String getAndroidLink() {
        return androidLink;
    }

    public void setAndroidLink(String androidLink) {
        this.androidLink = androidLink;
    }

    public String getPortalUrl() {
        return portalUrl;
    }

    public void setPortalUrl(String portalUrl) {
        this.portalUrl = portalUrl;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTempPassword() {
        return tempPassword;
    }

    public void setTempPassword(String tempPassword) {
        this.tempPassword = tempPassword;
    }

    public List<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(List<String> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTokenHandlerType() {
        return tokenHandlerType;
    }

    public void setTokenHandlerType(String tokenHandlerType) {
        this.tokenHandlerType = tokenHandlerType;
    }
}
