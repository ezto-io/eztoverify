package com.qlikverify.extensions.admin.misc.responses;

public class CreateTenantAdminResponse {
    private String tenantName;
    private String emailId;
    private String tenantUrl;
    private String appId;
    private String appSecret;

    public CreateTenantAdminResponse() {
    }

    public CreateTenantAdminResponse(String tenantName, String emailId, String tenantUrl, String clientId, String clientSecret) {
        this.tenantName = tenantName;
        this.emailId = emailId;
        this.tenantUrl = tenantUrl;
        this.appId = clientId;
        this.appSecret = clientSecret;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getTenantUrl() {
        return tenantUrl;
    }

    public void setTenantUrl(String tenantUrl) {
        this.tenantUrl = tenantUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }
}
