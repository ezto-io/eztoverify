package org.keycloak.forms.sso.freemarker;

import org.keycloak.Config;
import org.keycloak.forms.account.AccountProvider;
import org.keycloak.forms.account.AccountProviderFactory;
import org.keycloak.forms.account.freemarker.FreeMarkerAccountProvider;
import org.keycloak.forms.sso.SSOProvider;
import org.keycloak.forms.sso.SSOProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;
import org.keycloak.theme.FreeMarkerUtil;

public class FreeMarkerSSOProviderFactory implements SSOProviderFactory {

    private FreeMarkerUtil freeMarker;

    @Override
    public SSOProvider create(KeycloakSession session) {
        return new FreeMarkerSSOProvider(session, freeMarker);
    }

    @Override
    public void init(Config.Scope config) {
        freeMarker = new FreeMarkerUtil();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }
    @Override
    public void close() {
        freeMarker = null;
    }

    @Override
    public String getId() {
        return "freemarker";
    }
}
