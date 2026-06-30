package org.keycloak.authentication.actiontoken.verifyemaillink;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

public class VerifyEmailLinkActionToken extends DefaultActionToken {
    public static final String TOKEN_TYPE = "verify-email-link";

    private static final String JSON_FIELD_EMAIL = "to";
    private static final String JSON_FIELD_COMP_NAME="comp-name";

    @JsonProperty(value = JSON_FIELD_EMAIL)
    private String email;
    @JsonProperty(value = JSON_FIELD_COMP_NAME)
    private String comp_name;

    public VerifyEmailLinkActionToken(String email, String comp_name, int absoluteExpirationInSecs, String clientId) {
        super(email, TOKEN_TYPE, absoluteExpirationInSecs, null);
        this.email = email;
        this.comp_name = comp_name;
        this.issuedFor = clientId;
    }

    private VerifyEmailLinkActionToken() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getComp_name() {
        return comp_name;
    }

    public void setComp_name(String comp_name) {
        this.comp_name = comp_name;
    }
}
