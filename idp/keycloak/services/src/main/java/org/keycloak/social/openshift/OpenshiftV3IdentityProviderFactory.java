package org.keycloak.social.openshift;

import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public class OpenshiftV3IdentityProviderFactory extends AbstractIdentityProviderFactory<OpenshiftV3IdentityProvider>
        implements SocialIdentityProviderFactory<OpenshiftV3IdentityProvider> {

    public static final String PROVIDER_ID = "openshift-v3";
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public String getName() {
        return "Openshift v3";
    }

    @Override
    public String getProviderClass() {
        return (IdentityProviderFactory.class).getSimpleName();
    }

    @Override
    public String getSetupGuideLink() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/Social-Connections/Openshift-v3.md";
    }

    @Override
    public OpenshiftV3IdentityProvider create(KeycloakSession keycloakSession, IdentityProviderModel identityProviderModel) {
        return new OpenshiftV3IdentityProvider(keycloakSession, new OpenshiftV3IdentityProviderConfig(identityProviderModel));
    }

    @Override
    public OpenshiftV3IdentityProviderConfig createConfig() {
        return new OpenshiftV3IdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
