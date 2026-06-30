package com.qlikverify.extensions.admin.controllers.actionToken;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.keycloak.common.util.Time;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;
import org.keycloak.representations.IDToken;

public class EztoActionToken extends IDToken {
    @JsonProperty("jti")
    protected String id;

    protected Long exp;
    protected Long iat;

    @JsonProperty("iss")
    protected String issuer;
    @JsonProperty("aud")
    @JsonSerialize(using = StringOrArraySerializer.class)
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    protected String[] audience;

    @JsonProperty("requestId")
    protected String requestId;

    public String getId() {
        return id;
    }

    public EztoActionToken id(String id) {
        this.id = id;
        return this;
    }

    public Long getExp() {
        return exp;
    }

    @Override
    public EztoActionToken expiration(int expiration) {
        return (EztoActionToken) super.expiration(expiration);
    }

    @JsonIgnore
    public boolean isExpired() {
        return exp != null && exp != 0 ? Time.currentTime() > exp : false;
    }
    @JsonIgnore
    public boolean isActive() {
        return isActive(0);
    }

    @JsonIgnore
    public boolean isActive(int allowedTimeSkew) {
        return !isExpired() && isNotBefore(allowedTimeSkew);
    }

    @JsonIgnore
    public boolean isIssuedBeforeSessionStart(long sessionStarted) {
        return getIat() + 1 < sessionStarted;
    }

    public Long getIat() {
        return iat;
    }

    @JsonIgnore
    public EztoActionToken issuedNow() {
        iat = Long.valueOf(Time.currentTime());
        return this;
    }

    public EztoActionToken iat(Long iat) {
        this.iat = iat;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public EztoActionToken issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    @JsonIgnore
    public String[] getAudience() {
        return audience;
    }

    public EztoActionToken audience(String... audience) {
        this.audience = audience;
        return this;
    }

    public String getType() {
        return type;
    }

    @Override
    public EztoActionToken type(String type) {
        return (EztoActionToken) super.type(type);
    }

    public String getIssuedFor() {
        return issuedFor;
    }

    public EztoActionToken issuedFor(String issuedFor) {
        this.issuedFor = issuedFor;
        return this;
    }
    @JsonIgnore
    public String getRequestId() {
        return requestId;
    }

    public EztoActionToken requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}

