package com.qlikverify.services.actiontoken;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.keycloak.common.util.Time;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;
import org.keycloak.representations.IDToken;

public class OfflineActionToken extends IDToken {

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
    @JsonProperty("sub")
    protected String subject;
    @JsonProperty("typ")
    protected String type;
    @JsonProperty("azp")
    public String issuedFor;

    public String getId() {
        return id;
    }

    public OfflineActionToken id(String id) {
        this.id = id;
        return this;
    }
    public Long getExp() {
        return exp;
    }

    @Deprecated
    @JsonIgnore
    public int getExpiration() {
        return exp != null ? exp.intValue() : 0;
    }

    public OfflineActionToken exp(Long exp) {
        this.exp = exp;
        return this;
    }

    @Override
    public OfflineActionToken expiration(int expiration) {
        return (OfflineActionToken) super.expiration(expiration);
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

    @Deprecated
    @JsonIgnore
    public int getIssuedAt() {
        return iat != null ? iat.intValue() : 0;
    }

    @JsonIgnore
    public OfflineActionToken issuedNow() {
        iat = Long.valueOf(Time.currentTime());
        return this;
    }

    public OfflineActionToken iat(Long iat) {
        this.iat = iat;
        return this;
    }

    @Deprecated
    @JsonIgnore
    public OfflineActionToken issuedAt(int issuedAt) {
        this.iat = Long.valueOf(issuedAt);
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public OfflineActionToken issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    @JsonIgnore
    public String[] getAudience() {
        return audience;
    }

    public OfflineActionToken audience(String... audience) {
        this.audience = audience;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public OfflineActionToken subject(String subject) {
        this.subject = subject;
        return this;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getType() {
        return type;
    }

    @Override
    public OfflineActionToken type(String type) {
        return (OfflineActionToken) super.type(type);
    }

    public String getIssuedFor() {
        return issuedFor;
    }

    public OfflineActionToken issuedFor(String issuedFor) {
        this.issuedFor = issuedFor;
        return this;
    }
}
