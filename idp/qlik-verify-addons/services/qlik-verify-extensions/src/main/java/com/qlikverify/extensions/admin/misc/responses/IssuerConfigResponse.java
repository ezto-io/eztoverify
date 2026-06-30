package com.qlikverify.extensions.admin.misc.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IssuerConfigResponse {
    @JsonProperty("realm")
    private String realm;
    private String public_key;
    private String token_service;
    private String account_service;
    private String tokens_not_before;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getPublic_key() {
        return public_key;
    }

    public void setPublic_key(String public_key) {
        this.public_key = public_key;
    }

    public String getToken_service() {
        return token_service;
    }

    public void setToken_service(String token_service) {
        this.token_service = token_service;
    }

    public String getAccount_service() {
        return account_service;
    }

    public void setAccount_service(String account_service) {
        this.account_service = account_service;
    }

    public String getTokens_not_before() {
        return tokens_not_before;
    }

    public void setTokens_not_before(String tokens_not_before) {
        this.tokens_not_before = tokens_not_before;
    }
}
