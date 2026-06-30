package com.qlikverify.services.authenticators.onboarding.password;

import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import com.qlikverify.services.models.PassPolicy;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.PolicyError;
import org.keycloak.policy.password.PasswordPolicyManagerProvider;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.qlikverify.services.constants.Constants.*;

public class PasswordRegisterAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(PasswordRegisterAuthenticator.class);
    private final static String[] skippedPolicy = new String[]{PasswordPolicy.FORCE_EXPIRED_ID, PasswordPolicy.HASH_ITERATIONS_ID, PasswordPolicy.PASSWORD_HISTORY_ID,PasswordPolicy.HASH_ALGORITHM_ID};

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:PasswordRegisterAuthenticator:Authenticate");
        String nextAuth = context.getAuthenticationSession().getAuthNote("next_auth");
        if(nextAuth != null && !nextAuth.equalsIgnoreCase(PasswordRegisterAuthenticatorFactory.PROVIDER_ID)) {
            context.success();
            return;
        }
        context.getAuthenticationSession().removeAuthNote("next_auth");
        showView(context, null);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:PasswordRegisterAuthenticator:Action");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String password         = formData.getFirst(FORM_PASSWORD);
        String confirmPassword  = formData.getFirst(FORM_CONFIRM_PASSWORD);


        if (TextUtils.isEmpty(password) && TextUtils.isEmpty(confirmPassword)) {
            showView(context, ErrorMappings.PASSWORD_AND_CONFIRM_PASSWORD_REQUIRED);
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showView(context, ErrorMappings.PASSWORD_REQUIRED);
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            showView(context, ErrorMappings.CONFIRM_PASSWORD_REQUIRED);
            return;
        }
        if (!password.equals(confirmPassword)) {
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.PASSWORD_MISMATCH))
                    .setAttribute(ErrorMappings.ERROR_TYPE, ErrorMappings.PASSWORD_MISMATCH_ERROR)
                    .setAttribute(ATTRIBUTE_POLICY, getPolicy(context))
                    .createForm(PASSWORD_REGISTRATION_FTL);
            context.challenge(challenge);
            return;
        }

        //validates password policy
        PolicyError isPassPolicyErr = isValidPolicy(context, password);
        if(isPassPolicyErr != null){
            LoginFormsProvider formsProvider = context.form();
            if(isPassPolicyErr.getParameters().length > 0) {
                formsProvider.setAttribute("policyErrorParams", isPassPolicyErr.getParameters()[0]);
            }
            Response challenge = formsProvider
                    .setAttribute("policyError",isPassPolicyErr.getMessage())
                    .setAttribute(ATTRIBUTE_POLICY, getPolicy(context))
                    .createForm(PASSWORD_REGISTRATION_FTL);
            context.challenge(challenge);
        }
        if(context.getUser() != null){
            context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), UserCredentialModel.password(password));
        }

        context.success();
    }

    private PolicyError isValidPolicy(AuthenticationFlowContext context, String password) {
        String policyName = GeneralHelper.getConfigValue(context, PASS_POLICY, null);
        if(policyName == null || policyName.length() == 0)
            return null;

        RealmPolicyModel policyModel = context.getSession().policies().getPolicyByName(context.getRealm(), policyName);
        if(policyModel == null) {
            return null;
        }

        if(context.getUser() != null){
            return context.getSession().getProvider(PasswordPolicyManagerProvider.class)
                    .validate(policyModel.getId(), context.getUser(), password);
        }
        return null;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    private void showView(AuthenticationFlowContext context, String error) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.setAttribute(ERROR, error);
        }
        formsProvider.setAttribute(ATTRIBUTE_POLICY, getPolicy(context));
        Response challenge = formsProvider.createForm(PASSWORD_REGISTRATION_FTL);
        context.challenge(challenge);
    }

    private List<Map<String, String>> getPolicy(AuthenticationFlowContext context) {
        ArrayList arrayList = new ArrayList();
        String policyName = GeneralHelper.getConfigValue(context, PASS_POLICY, null);
        if(policyName == null || policyName.length() == 0)
            return arrayList;

        RealmPolicyModel policyModel = context.getSession().policies().getPolicyByName(context.getRealm(), policyName);
        if(policyModel == null) 
            return arrayList;

        PasswordPolicy passwordPolicy = PasswordPolicy.parse(context.getSession(), policyModel.getPolicyValue());
        if (passwordPolicy.getPolicies().size() == 0)
            return arrayList;

        passwordPolicy.getPolicies().stream().filter(x -> !Arrays.asList(skippedPolicy).contains(x)).forEach(
                x -> {
                    Object result = passwordPolicy.getPolicyConfig(x);
                    if (result instanceof Integer) {
                        int value = (Integer) result;
                        if (x.equals("length")) {
                            arrayList.add(new PassPolicy("Requires minimum " + x + " of " + value,value,"minimum"));
                        } else if (value == 1) {
                            arrayList.add(new PassPolicy("Requires atleast " + value + " " + x,value,x));
                        } else if (value > 1) {
                            arrayList.add(new PassPolicy("Requires " + value + " " + x,value,x));
                        }
                    }
                }
        );
        return arrayList;
    }
}

