package com.qlikverify.extensions.admin.misc.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenIdConfigResponse {
    @JsonProperty("issuer")
    private String issuer;
    @JsonProperty("authorization_endpoint")
    private String authorization_endpoint;
    @JsonProperty("token_endpoint")
    private String token_endpoint;
    @JsonProperty("token_introspection_endpoint")
    private String token_introspection_endpoint;
    @JsonProperty("userinfo_endpoint")
    private String userinfo_endpoint;
    @JsonProperty("end_session_endpoint")
    private String end_session_endpoint;
    @JsonProperty("jwks_uri")
    private String jwks_uri;
    @JsonProperty("check_session_iframe")
    private String check_session_iframe;
    @JsonProperty("grant_types_supported")
    private List<String> grant_types_supported = null;
    @JsonProperty("response_types_supported")
    private List<String> response_types_supported = null;
    @JsonProperty("subject_types_supported")
    private List<String> subject_types_supported = null;
    @JsonProperty("id_token_signing_alg_values_supported")
    private List<String> id_token_signing_alg_values_supported = null;
    @JsonProperty("id_token_encryption_enc_values_supported")
    private List<String> id_token_encryption_enc_values_supported;
    @JsonProperty("id_token_encryption_alg_values_supported")
    private List<String> id_token_encryption_alg_values_supported = null;
    @JsonProperty("userinfo_signing_alg_values_supported")
    private List<String> userinfo_signing_alg_values_supported = null;
    @JsonProperty("request_object_signing_alg_values_supported")
    private List<String> request_object_signing_alg_values_supported = null;
    @JsonProperty("response_modes_supported")
    private List<String> response_modes_supported = null;
    @JsonProperty("registration_endpoint")
    private String registration_endpoint;
    @JsonProperty("token_endpoint_auth_methods_supported")
    private List<String> token_endpoint_auth_methods_supported = null;
    @JsonProperty("token_endpoint_auth_signing_alg_values_supported")
    private List<String> token_endpoint_auth_signing_alg_values_supported = null;
    @JsonProperty("claims_supported")
    private List<String> claims_supported = null;
    @JsonProperty("claim_types_supported")
    private List<String> claim_types_supported = null;
    @JsonProperty("claims_parameter_supported")
    private Boolean claims_parameter_supported;
    @JsonProperty("scopes_supported")
    private List<String> scopes_supported = null;
    @JsonProperty("request_parameter_supported")
    private Boolean request_parameter_supported;
    @JsonProperty("request_uri_parameter_supported")
    private Boolean request_uri_parameter_supported;
    @JsonProperty("code_challenge_methods_supported")
    private List<String> code_challenge_methods_supported = null;
    @JsonProperty("tls_client_certificate_bound_access_tokens")
    private Boolean tls_client_certificate_bound_access_tokens;
    @JsonProperty("introspection_endpoint")
    private String introspection_endpoint;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAuthorization_endpoint() {
        return authorization_endpoint;
    }

    public void setAuthorization_endpoint(String authorization_endpoint) {
        this.authorization_endpoint = authorization_endpoint;
    }

    public List<String> getId_token_encryption_enc_values_supported() {
        return id_token_encryption_enc_values_supported;
    }

    public void setId_token_encryption_enc_values_supported(List<String> id_token_encryption_enc_values_supported) {
        this.id_token_encryption_enc_values_supported = id_token_encryption_enc_values_supported;
    }

    public List<String> getId_token_encryption_alg_values_supported() {
        return id_token_encryption_alg_values_supported;
    }

    public void setId_token_encryption_alg_values_supported(List<String> id_token_encryption_alg_values_supported) {
        this.id_token_encryption_alg_values_supported = id_token_encryption_alg_values_supported;
    }

    public String getToken_endpoint() {
        return token_endpoint;
    }

    public void setToken_endpoint(String token_endpoint) {
        this.token_endpoint = token_endpoint;
    }

    public String getToken_introspection_endpoint() {
        return token_introspection_endpoint;
    }

    public void setToken_introspection_endpoint(String token_introspection_endpoint) {
        this.token_introspection_endpoint = token_introspection_endpoint;
    }

    public String getUserinfo_endpoint() {
        return userinfo_endpoint;
    }

    public void setUserinfo_endpoint(String userinfo_endpoint) {
        this.userinfo_endpoint = userinfo_endpoint;
    }

    public String getEnd_session_endpoint() {
        return end_session_endpoint;
    }

    public void setEnd_session_endpoint(String end_session_endpoint) {
        this.end_session_endpoint = end_session_endpoint;
    }

    public String getJwks_uri() {
        return jwks_uri;
    }

    public void setJwks_uri(String jwks_uri) {
        this.jwks_uri = jwks_uri;
    }

    public String getCheck_session_iframe() {
        return check_session_iframe;
    }

    public void setCheck_session_iframe(String check_session_iframe) {
        this.check_session_iframe = check_session_iframe;
    }

    public List<String> getGrant_types_supported() {
        return grant_types_supported;
    }

    public void setGrant_types_supported(List<String> grant_types_supported) {
        this.grant_types_supported = grant_types_supported;
    }

    public List<String> getResponse_types_supported() {
        return response_types_supported;
    }

    public void setResponse_types_supported(List<String> response_types_supported) {
        this.response_types_supported = response_types_supported;
    }

    public List<String> getSubject_types_supported() {
        return subject_types_supported;
    }

    public void setSubject_types_supported(List<String> subject_types_supported) {
        this.subject_types_supported = subject_types_supported;
    }

    public List<String> getId_token_signing_alg_values_supported() {
        return id_token_signing_alg_values_supported;
    }

    public void setId_token_signing_alg_values_supported(List<String> id_token_signing_alg_values_supported) {
        this.id_token_signing_alg_values_supported = id_token_signing_alg_values_supported;
    }

    public List<String> getUserinfo_signing_alg_values_supported() {
        return userinfo_signing_alg_values_supported;
    }

    public void setUserinfo_signing_alg_values_supported(List<String> userinfo_signing_alg_values_supported) {
        this.userinfo_signing_alg_values_supported = userinfo_signing_alg_values_supported;
    }

    public List<String> getRequest_object_signing_alg_values_supported() {
        return request_object_signing_alg_values_supported;
    }

    public void setRequest_object_signing_alg_values_supported(List<String> request_object_signing_alg_values_supported) {
        this.request_object_signing_alg_values_supported = request_object_signing_alg_values_supported;
    }

    public List<String> getResponse_modes_supported() {
        return response_modes_supported;
    }

    public void setResponse_modes_supported(List<String> response_modes_supported) {
        this.response_modes_supported = response_modes_supported;
    }

    public String getRegistration_endpoint() {
        return registration_endpoint;
    }

    public void setRegistration_endpoint(String registration_endpoint) {
        this.registration_endpoint = registration_endpoint;
    }

    public List<String> getToken_endpoint_auth_methods_supported() {
        return token_endpoint_auth_methods_supported;
    }

    public void setToken_endpoint_auth_methods_supported(List<String> token_endpoint_auth_methods_supported) {
        this.token_endpoint_auth_methods_supported = token_endpoint_auth_methods_supported;
    }

    public List<String> getToken_endpoint_auth_signing_alg_values_supported() {
        return token_endpoint_auth_signing_alg_values_supported;
    }

    public void setToken_endpoint_auth_signing_alg_values_supported(List<String> token_endpoint_auth_signing_alg_values_supported) {
        this.token_endpoint_auth_signing_alg_values_supported = token_endpoint_auth_signing_alg_values_supported;
    }

    public List<String> getClaims_supported() {
        return claims_supported;
    }

    public void setClaims_supported(List<String> claims_supported) {
        this.claims_supported = claims_supported;
    }

    public List<String> getClaim_types_supported() {
        return claim_types_supported;
    }

    public void setClaim_types_supported(List<String> claim_types_supported) {
        this.claim_types_supported = claim_types_supported;
    }

    public Boolean getClaims_parameter_supported() {
        return claims_parameter_supported;
    }

    public void setClaims_parameter_supported(Boolean claims_parameter_supported) {
        this.claims_parameter_supported = claims_parameter_supported;
    }

    public List<String> getScopes_supported() {
        return scopes_supported;
    }

    public void setScopes_supported(List<String> scopes_supported) {
        this.scopes_supported = scopes_supported;
    }

    public Boolean getRequest_parameter_supported() {
        return request_parameter_supported;
    }

    public void setRequest_parameter_supported(Boolean request_parameter_supported) {
        this.request_parameter_supported = request_parameter_supported;
    }

    public Boolean getRequest_uri_parameter_supported() {
        return request_uri_parameter_supported;
    }

    public void setRequest_uri_parameter_supported(Boolean request_uri_parameter_supported) {
        this.request_uri_parameter_supported = request_uri_parameter_supported;
    }

    public List<String> getCode_challenge_methods_supported() {
        return code_challenge_methods_supported;
    }

    public void setCode_challenge_methods_supported(List<String> code_challenge_methods_supported) {
        this.code_challenge_methods_supported = code_challenge_methods_supported;
    }

    public Boolean getTls_client_certificate_bound_access_tokens() {
        return tls_client_certificate_bound_access_tokens;
    }

    public void setTls_client_certificate_bound_access_tokens(Boolean tls_client_certificate_bound_access_tokens) {
        this.tls_client_certificate_bound_access_tokens = tls_client_certificate_bound_access_tokens;
    }

    public String getIntrospection_endpoint() {
        return introspection_endpoint;
    }

    public void setIntrospection_endpoint(String introspection_endpoint) {
        this.introspection_endpoint = introspection_endpoint;
    }
}
