package com.qlikverify.services.locale;

import com.qlikverify.services.events.models.response.User;
import org.keycloak.Config;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.locale.LocaleSelectorProviderFactory;
import org.keycloak.locale.LocaleType;
import org.keycloak.models.*;
import org.keycloak.theme.beans.LocaleBean;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import java.util.Locale;

public class QlikVerifyLocaleSelectorProvider implements LocaleSelectorProvider, LocaleSelectorProviderFactory {

    KeycloakSession session;

    private final String PREFERRED_LOCALE = "preferred_locale";

    @Override
    public Locale resolveLocale(RealmModel realm, UserModel user) {
        return Locale.ENGLISH;
    }

    @Override
    public Locale resolveLocale(RealmModel realm, UserModel userModel, LocaleType localeType) {
        Locale locale = null;
        HttpHeaders requestHeaders = session.getContext().getRequestHeaders();
        if (requestHeaders == null) {
            locale =  getUserPreferredLocale(userModel);
            if (userModel != null) {
                String currentUserLocale = userModel.getFirstAttribute("preferred_locale");
                if(!locale.getLanguage().equalsIgnoreCase(currentUserLocale))
                    userModel.setSingleAttribute(PREFERRED_LOCALE, locale.getLanguage());
            }
            return locale;
        }

        Cookie localeCookie = requestHeaders.getCookies().get(LOCALE_COOKIE);
        if (localeCookie == null) {
            locale =  getUserPreferredLocale(userModel);
        } else {
            locale =  new Locale.Builder().setLanguage(localeCookie.getValue()).build();
        }

        if (userModel != null) {
            String currentUserLocale = userModel.getFirstAttribute("preferred_locale");
            if(!locale.getLanguage().equalsIgnoreCase(currentUserLocale))
                userModel.setSingleAttribute(PREFERRED_LOCALE, locale.getLanguage());
        }
        return locale;
    }

    private Locale getUserPreferredLocale(UserModel userModel) {
        if (userModel != null && userModel.getFirstAttribute(PREFERRED_LOCALE) != null && !userModel.getFirstAttribute(PREFERRED_LOCALE).isEmpty()) {
            return new Locale.Builder().setLanguage(userModel.getFirstAttribute(PREFERRED_LOCALE)).build();
        }

        return Locale.ENGLISH;
    }
    @Override
    public LocaleSelectorProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return "qlikverify_locale";
    }

    @Override
    public int order() {
        return 1;
    }
}
