package org.keycloak.forms.account.freemarker.model;

import org.keycloak.credential.CredentialModel;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.MFAProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MFABean {

    private Collection<MFACredentialBean> mfas;
    private final KeycloakSession session;
    private final UserModel user;
    private final RealmModel realmModel;

    public MFABean(KeycloakSession session, RealmModel realmModel, UserModel userModel) {
        this.session = session;
        this.user = userModel;
        this.realmModel = realmModel;
    }

    public Collection<MFACredentialBean> getMfas() {

        mfas = new ArrayList<>();
        List<CredentialModel> credentialModels = session.userCredentialManager().getStoredCredentialsStream(realmModel, user)
                .filter(credentialModel -> !(credentialModel.getProviderId() == null || credentialModel.getType().equalsIgnoreCase("password")))
                .collect(Collectors.toList());

        if (credentialModels.isEmpty()) {
            return mfas;
        }

        credentialModels.forEach(x -> {
            MFAProviderFactory provider = (MFAProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(MFAProvider.class, x.getProviderId());
            if (provider != null) {
                mfas.add(new MFACredentialBean(provider.getDisplayName(), provider.getLogo(), provider.getId(),x.getId()));
            }
        });

        return mfas;
    }


    public class MFACredentialBean {

        private String displayName;
        private String logo;
        private String providerId;
        private String credentialId;

        public MFACredentialBean(String displayName, String logo, String providerId,String credentialId) {
            this.displayName = displayName;
            this.logo = logo;
            this.providerId = providerId;
            this.credentialId = credentialId;
        }

        public String getProviderId() {
            return providerId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getLogo() {
            return logo;
        }

        public String getCredentialId() {
            return credentialId;
        }
    }
}
