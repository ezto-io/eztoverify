package org.keycloak.authentication.requiredactions;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.locale.LocaleUpdaterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class UpdateUserLocaleAction implements RequiredActionProvider, RequiredActionFactory {

    @Override
    public String getDisplayText() {
        return "Update User Locale";
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        String userRequestedLocale = context.getAuthenticationSession().getAuthNote(LocaleSelectorProvider.USER_REQUEST_LOCALE);
        if (userRequestedLocale != null) {
            LocaleUpdaterProvider updater = context.getSession().getProvider(LocaleUpdaterProvider.class);
            updater.updateUsersLocale(context.getUser(), userRequestedLocale);
        } else {
            String userLocale = context.getUser().getFirstAttribute(UserModel.LOCALE);

            if (userLocale != null) {
                LocaleUpdaterProvider updater = context.getSession().getProvider(LocaleUpdaterProvider.class);
                updater.updateLocaleCookie(userLocale);
            } else {
                LocaleUpdaterProvider updater = context.getSession().getProvider(LocaleUpdaterProvider.class);
                updater.expireLocaleCookie();
            }
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
    }

    @Override
    public void processAction(RequiredActionContext context) {
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "update_user_locale";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public String[] supportedModes() {
        return new String[] {"ciam","wiam"};
    }

}
