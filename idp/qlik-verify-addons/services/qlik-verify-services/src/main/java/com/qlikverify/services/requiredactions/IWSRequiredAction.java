package com.qlikverify.services.requiredactions;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.vault.DefaultVaultRawSecret;
import org.keycloak.vault.VaultProvider;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class IWSRequiredAction implements RequiredActionProvider, RequiredActionFactory {
    public static final String IWS_CREDTYPE = "iws.credtype";
    public static final String IWS_ADMIN_SET_PASS = "iws_admin_set_pass";
    public static final String IWS_USER_SET_PASS = "iws_user_set_pass";
    public static final String IWS_USERNAME = "iws_username";
    public static final String IWS_PASSWORD = "iws_password";
    public static final String IWS_CRED_TYPE = "iws_cred_type";
    public static final String APP_NAME = "app_name";
    KeycloakSession session;

    @Override
    public String getDisplayText() {
        return "iws-action-ra";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.IWS;
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
        return new String[] {"wiam"};
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        KeycloakSession kcSession = context.getSession();
        if (kcSession != null && kcSession.getContext() != null) {
            VaultProvider vaultProvider = getVaultProvider(kcSession, context.getUser());
            boolean hasCred = vaultProvider.hasSecret(false);
            String result = context.getUser().getFirstAttribute("hidden_ua_iwa_" + kcSession.getContext().getClient().getClientId());
            if (hasCred && (result == null || result.equals("false"))) {
                context.getUser().removeRequiredAction(getId());
            }
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        showView(context, null);
    }

    private void showView(RequiredActionContext context, String error) {
        ClientModel clientModel = context.getSession().getContext().getClient();
        String iswCredType = clientModel.getAttribute(IWS_CREDTYPE);
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }
        if (iswCredType.equals(IWS_ADMIN_SET_PASS)) {

            Response response = formsProvider
                    .setAttribute(IWS_CRED_TYPE, iswCredType)
                    .setAttribute(APP_NAME, clientModel.getName())
                    .createForm("iws-admin-info-form.ftl");

            context.challenge(response);
            return;
        }

        if (iswCredType.equals(IWS_USER_SET_PASS)) {
            Response response = formsProvider
                    .setAttribute(IWS_CRED_TYPE, iswCredType)
                    .setAttribute(APP_NAME, clientModel.getName())
                    .createForm("iws-cred-form.ftl");
            context.challenge(response);
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        ClientModel clientModel = context.getSession().getContext().getClient();

        String iwsCredType = params.getFirst(IWS_CRED_TYPE);

        String clCredType = clientModel.getAttribute(IWS_CREDTYPE);

        if(!iwsCredType.equals(clCredType))
        {
            context.failure();
            return;
        }

        if (iwsCredType.equals(IWS_ADMIN_SET_PASS)) {
            context.failure();
            return;
        }

        String username = params.getFirst(IWS_USERNAME);
        String password = params.getFirst(IWS_PASSWORD);

        VaultProvider vaultProvider = getVaultProvider(context.getSession(), context.getUser());
        boolean isCreated = vaultProvider.writeUserSecret(username, DefaultVaultRawSecret.forBuffer(Optional.of(ByteBuffer.wrap(String.valueOf(password).getBytes(StandardCharsets.UTF_8)))),false);
        if (!isCreated) {
            showView(context, "failed-to-update-credentials");
            return;
        }
        context.getUser().removeAttribute("hidden_ua_iwa_" + session.getContext().getClient().getClientId());
        context.success();
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
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

    private VaultProvider getVaultProvider(KeycloakSession kcSession, UserModel user) {

        ClientModel clientModel = kcSession.getContext().getClient();
        String iswCredType = clientModel.getAttribute(IWS_CREDTYPE);
        VaultProvider vaultProvider = kcSession.getProvider(VaultProvider.class, "winlogon-vault");
        if (iswCredType.equals(IWS_USER_SET_PASS)) {
            vaultProvider.setUserModel(user).setClientModel(clientModel);
        } else if (iswCredType.equals(IWS_ADMIN_SET_PASS)) {
            vaultProvider.setClientModel(clientModel);
        }

        return vaultProvider;

    }

    @Override
    public String getId() {
        return "iws-action";
    }
}
