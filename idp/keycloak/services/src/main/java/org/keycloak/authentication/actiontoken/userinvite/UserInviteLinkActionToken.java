package org.keycloak.authentication.actiontoken.userinvite;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

public class UserInviteLinkActionToken extends DefaultActionToken {

    public static final String TOKEN_TYPE = "user-invite-link";

    private static final String JSON_FIELD_EMAIL = "to";

    private static final String JSON_NEXT_AUTH = "nextauth";

    @JsonProperty(value = JSON_FIELD_EMAIL)
    private String email;

    @JsonProperty(value = JSON_NEXT_AUTH)
    private String nextAuthenticator;

    public UserInviteLinkActionToken(String userId, String emailId, int absoluteExpirationInSecs, String clientId) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null);
        this.issuedFor = clientId;
        this.email = emailId;
    }

    private UserInviteLinkActionToken() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNextAuthenticator() {
        return nextAuthenticator;
    }

    public void setNextAuthenticator(String nextAuthenticator) {
        this.nextAuthenticator = nextAuthenticator;
    }
}
