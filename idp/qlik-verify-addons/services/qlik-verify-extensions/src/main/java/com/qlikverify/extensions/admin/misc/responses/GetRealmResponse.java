package com.qlikverify.extensions.admin.misc.responses;

public class GetRealmResponse {
    private String name;
    private boolean enabled;
    private boolean rememberMe;
    private boolean resetPassword;
    private boolean registrationAllowed;

    public GetRealmResponse(String name, boolean enabled, boolean rememberMe, boolean resetPassword, boolean registrationAllowed) {
        this.name = name;
        this.enabled = enabled;
        this.rememberMe = rememberMe;
        this.resetPassword = resetPassword;
        this.registrationAllowed = registrationAllowed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public boolean isResetPassword() {
        return resetPassword;
    }

    public void setResetPassword(boolean resetPassword) {
        this.resetPassword = resetPassword;
    }

    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }
}
