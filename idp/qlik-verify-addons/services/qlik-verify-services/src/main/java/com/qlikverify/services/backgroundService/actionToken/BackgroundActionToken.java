package com.qlikverify.services.backgroundService.actionToken;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.keycloak.common.util.Time;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;
import org.keycloak.representations.IDToken;

public class BackgroundActionToken extends IDToken {

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

    @JsonProperty("userEmail")
    protected String userEmail;

    public String getId() {
        return id;
    }

    public BackgroundActionToken id(String id) {
        this.id = id;
        return this;
    }

    public Long getExp() {
        return exp;
    }

    @Override
    public BackgroundActionToken expiration(int expiration) {
        return (BackgroundActionToken) super.expiration(expiration);
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
    public BackgroundActionToken issuedNow() {
        iat = Long.valueOf(Time.currentTime());
        return this;
    }

    public BackgroundActionToken iat(Long iat) {
        this.iat = iat;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public BackgroundActionToken issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    @JsonIgnore
    public String[] getAudience() {
        return audience;
    }

    public BackgroundActionToken audience(String... audience) {
        this.audience = audience;
        return this;
    }

    public String getType() {
        return type;
    }

    @Override
    public BackgroundActionToken type(String type) {
        return (BackgroundActionToken) super.type(type);
    }

    public String getIssuedFor() {
        return issuedFor;
    }

    public BackgroundActionToken issuedFor(String issuedFor) {
        this.issuedFor = issuedFor;
        return this;
    }
    @JsonIgnore
    public String getUserEmail() {
        return userEmail;
    }

    public BackgroundActionToken userEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

}
