package org.keycloak.social.openshift;

import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

/**
 * OpenShift 4 Identity Provider factory class.
 *
 * @author David Festal and Sebastian Łaskawiec
 */
public class OpenshiftV4IdentityProviderFactory extends AbstractIdentityProviderFactory<OpenshiftV4IdentityProvider>
        implements SocialIdentityProviderFactory<OpenshiftV4IdentityProvider> {

    public static final String PROVIDER_ID = "openshift-v4";
    public static final String NAME = "Openshift v4";
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProviderClass() {
        return (IdentityProviderFactory.class).getSimpleName();
    }

    @Override
    public String getSetupGuideLink() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/Social-Connections/Openshift-v4.md";
    }

    @Override
    public OpenshiftV4IdentityProvider create(KeycloakSession keycloakSession, IdentityProviderModel identityProviderModel) {
        return new OpenshiftV4IdentityProvider(keycloakSession, new OpenshiftV4IdentityProviderConfig(identityProviderModel));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public OpenshiftV4IdentityProviderConfig createConfig() {
        return new OpenshiftV4IdentityProviderConfig();
    }
}
