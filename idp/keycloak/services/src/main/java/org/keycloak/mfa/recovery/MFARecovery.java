package org.keycloak.mfa.recovery;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.mfa.recovery.credential.MFARecoveryCredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.MFAConfigModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.utils.GeneralHelper;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MFARecovery {

    private final int RECOVERY_CODE_COUNT = 5;

    protected List<String> getRecovery(RequiredActionContext context) {
        List<String> recoveryCodes = new ArrayList<>();
        for (int x = 0; x < RECOVERY_CODE_COUNT; x++) {
            String code = GeneralHelper.getRandomValue(10);
            recoveryCodes.add(code);
        }
        context.getAuthenticationSession().setAuthNote("recovery_code", String.join("##", recoveryCodes));
        return recoveryCodes;
    }

    protected boolean createRecovery(RequiredActionContext context) {
        String rcCodes = context.getAuthenticationSession().getAuthNote("recovery_code");
        MFARecoveryCredentialProvider sqcp = (MFARecoveryCredentialProvider) context.getSession().getProvider(CredentialProvider.class, MFARecoveryCredentialProviderFactory.PROVIDER_ID);

        CredentialModel credentialModel = sqcp.createCredential(context.getRealm(), context.getUser(), MFARecoveryCredentialModel.createSecretQuestion(Arrays.asList(rcCodes.split("##"))));
        return credentialModel != null;
    }

    protected void showRecovery(RequiredActionContext context, String mfaProviderId, String erro) {
        LoginFormsProvider loginFormsProvider = context.form();
        if (erro != null) {
            loginFormsProvider.addError(new FormMessage(erro));
        }
        Response response = loginFormsProvider
                .setAttribute("mfaProviderId", mfaProviderId)
                .createForm("mfa-troubleshoot.ftl");
        context.challenge(response);
    }

    protected boolean isValid(RequiredActionContext context, String code) {
        UserCredentialModel input = new UserCredentialModel(null, MFARecoveryCredentialModel.TYPE, code);
        return getCredentialProvider(context.getSession()).isValid(context.getRealm(), context.getUser(), input);
    }

    protected boolean isMFARecoveryEnabled(RequiredActionContext context, MFAConfigModel mfaConfigModel) {
        return Boolean.parseBoolean(GeneralHelper.getConfigValue(mfaConfigModel, "enable_mfa_recovery")) && isMFARecoveryConfigured(context);
    }

    protected boolean isMFARecoveryConfigured(RequiredActionContext context) {
        return getCredentialProvider(context.getSession()).isConfiguredFor(context.getRealm(), context.getUser(), MFARecoveryCredentialModel.TYPE);
    }


    private MFARecoveryCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (MFARecoveryCredentialProvider) session.getProvider(CredentialProvider.class, MFARecoveryCredentialProviderFactory.PROVIDER_ID);
    }
}
