package org.keycloak.authentication.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.grants.ciba.util.UserTransactionStatus;
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreferredLoginLocaleRequiredAction implements RequiredActionProvider, RequiredActionFactory {
    public static final String PREFERRED_LOCALE = "preferred_locale";
    public static final String ID = "preferred-login-locale";
    public static final String SUPPORTED_LOCALES = "supportedLocales";
    public static final String SELECTED_LOCALE = "selectedLocale";

    private static final Logger logger = Logger.getLogger(PreferredLoginLocaleRequiredAction.class);

    @Override
    public String getDisplayText() {
        return "Preferred-locale-RA";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return Collections.emptyList();
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public String[] supportedModes() {
        return new String[] { "" };
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        UserModel user = context.getUser();

        if (user == null) {
            logger.error("PreferredLoginLocaleRequiredAction:: No User set in the context");
            return;
        }
        if (user.getFirstAttribute(PREFERRED_LOCALE) != null) {
            user.removeRequiredAction(ID);
        }
    }


    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAttribute(SUPPORTED_LOCALES, new ArrayList<String>(Constants.SUPPORTED_LOCALES));
        context.challenge(loginFormsProvider.createForm("preferred-login-locale-form.ftl"));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        eventBuilder.user(context.getUser());
        eventBuilder.client(context.getAuthenticationSession().getClient());


        if(formData.containsKey("cancel") && Boolean.parseBoolean(formData.getFirst("cancel"))) {
            context.getRealm().updateTransactionStatus(context.getRequestId(), context.getUser().getUsername(), UserTransactionStatus.USER_CANCELLED.getDisplayName());
            eventBuilder.event(EventType.EZ_USER_PREFERRED_LOGIN_LOCALE_ERROR);
            eventBuilder.error("User cancelled the preferred login locale selection");
            context.failure();
            return;
        }

        if (formData.containsKey(SELECTED_LOCALE) && (formData.get(SELECTED_LOCALE) == null || formData.get(SELECTED_LOCALE).isEmpty())) {
            eventBuilder.event(EventType.EZ_USER_PREFERRED_LOGIN_LOCALE_ERROR);
            eventBuilder.error("Invalid locale selected.");
            context.failure();
            return;
        }

        if(formData.containsKey(SELECTED_LOCALE)
                && context.getRealm().getPreferredLoginLocalesStream().noneMatch(x -> x.equalsIgnoreCase(formData.getFirst(SELECTED_LOCALE)))) {
            eventBuilder.event(EventType.EZ_USER_PREFERRED_LOGIN_LOCALE_ERROR);
            eventBuilder.error("Locale not supported");
            context.failure();
            return;
        }

        context.getUser().setSingleAttribute(PREFERRED_LOCALE, formData.getFirst(SELECTED_LOCALE));

        eventBuilder.event(EventType.EZ_USER_PREFERRED_LOGIN_LOCALE);

        eventBuilder.success();
        context.success();
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOP
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return ID;
    }
    private void addAction(RequiredActionContext context, UserModel user) {
        if (context.getUser().getRequiredActionsStream().noneMatch(x -> x.equalsIgnoreCase(ID))) {
            logger.debug("User is required to select preferred login locale.");
            user.addRequiredAction(ID);
        }
    }
}
