package org.keycloak.forms.account.freemarker.model;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import java.util.*;
import org.keycloak.representations.idm.license.CurrentSubscriptionRepresentation;

public class SubscriptionBean {
    private final KeycloakSession session;
    private final RealmModel realm;
    private static final Logger logger = Logger.getLogger(SubscriptionBean.class);

    public SubscriptionBean(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }

    public Map<String, String> getCustomerPortalDetails(RealmModel realm, String returnUrl) {
        return new HashMap<>();
    }

    public CurrentSubscriptionResponse getCurrentSubscription(RealmModel realm) {
        CurrentSubscriptionResponse subResponse = new CurrentSubscriptionResponse();
        subResponse.setSubscriptionActive(true);
        subResponse.setMessage("Active");
        subResponse.setSubs(new ArrayList<>());
        return subResponse;
    }

    public static class CurrentSubscriptionResponse {
        private boolean subscriptionActive;
        private String message;
        private List<CurrentSubscriptionRepresentation> subs = new ArrayList<>();

        public boolean isSubscriptionActive() {
            return subscriptionActive;
        }

        public void setSubscriptionActive(boolean subscriptionActive) {
            this.subscriptionActive = subscriptionActive;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<CurrentSubscriptionRepresentation> getSubs() {
            return subs;
        }

        public void setSubs(List<CurrentSubscriptionRepresentation> subs) {
            this.subs = subs;
        }
    }
}