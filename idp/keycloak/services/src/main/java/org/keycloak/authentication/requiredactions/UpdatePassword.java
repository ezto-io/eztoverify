/* * Copyright 2016 Red Hat, Inc. and/or its affiliates * and other contributors as indicated by the @author tags. * * Licensed under the Apache License, Version 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of the License at * * http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */
package org.keycloak.authentication.requiredactions;

import com.qlikverify.helpers.constants.ErrorMappings;
import com.qlikverify.helpers.models.PassPolicy;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.PolicyError;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.policy.ciba.CibaPolicyProvider;
import org.keycloak.policy.password.PasswordPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;

import static com.qlikverify.helpers.constants.Constants.*;
import static org.keycloak.models.Constants.*;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;
import static com.qlikverify.helpers.constants.Constants.ERROR;
import static com.qlikverify.helpers.constants.Constants.ATTRIBUTE_POLICY;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a> * @version $Revision: 1 $
 */
public class UpdatePassword implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(UpdatePassword.class);
    private final static String[] skippedPolicy = new String[]{PasswordPolicy.FORCE_EXPIRED_ID, PasswordPolicy.HASH_ITERATIONS_ID, PasswordPolicy.PASSWORD_HISTORY_ID, PasswordPolicy.HASH_ALGORITHM_ID};

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getRealm().getMode().equalsIgnoreCase("bo") && !context.getRealm().getName().equals(Config.getAdminRealm())) {
            context.getUser().removeRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
            return;
        }

        if (context.getPolicyId() != null) {
            boolean result;
            String policyType = context.getPolicyType();
            if (policyType.equals("action")) {
                result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
            } else {
                result = context.getSession().getProvider(CibaPolicyProvider.class).validate(context.getSession(), context.getAuthenticationSession().getAuthNote(BINDING_MESSAGE), context.getPolicyId());
            }
            if (result) {
                addAction(context);
            } else {
                context.getUser().removeRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
            }
        }
    }

    private void addAction(RequiredActionContext context) {
        if (!context.getUser().getRequiredActionsStream().filter(x -> x.equalsIgnoreCase(UserModel.RequiredAction.UPDATE_PASSWORD.name())).findFirst().isPresent()) {
            context.getUser().addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form().setAttribute("username", context.getAuthenticationSession().getAuthenticatedUser().getUsername()).setAttribute("reqFromWebsite", checkReqFrom(context)).setAttribute("policy", getPolicy(context)).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        EventBuilder event = context.getEvent();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        KeycloakSession session = context.getSession();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        event.event(EventType.UPDATE_PASSWORD);
        String passwordNew = formData.getFirst("password");
        String passwordConfirm = formData.getFirst("confirmPassword");
        EventBuilder errorEvent = event.clone().event(EventType.UPDATE_PASSWORD_ERROR).client(authSession.getClient().getClientId()).user(authSession.getAuthenticatedUser());
        if (TextUtils.isEmpty(passwordNew) && TextUtils.isEmpty(passwordConfirm)) {
            showView(context, ErrorMappings.PASSWORD_AND_CONFIRM_PASSWORD_REQUIRED);
            errorEvent.error(Errors.PASSWORD_MISSING);
            return;
        }
        if (TextUtils.isEmpty(passwordNew)) {
            showView(context, ErrorMappings.PASSWORD_REQUIRED);
            errorEvent.error(Errors.PASSWORD_CONFIRM_ERROR);
            return;
        }
        if (TextUtils.isEmpty(passwordConfirm)) {
            showView(context, ErrorMappings.CONFIRM_PASSWORD_REQUIRED);
            return;
        }
        if (!passwordNew.equals(passwordConfirm)) {
            Response challenge = context.form().addError(new FormMessage(ErrorMappings.PASSWORD_MISMATCH)).setAttribute(ErrorMappings.ERROR_TYPE, ErrorMappings.PASSWORD_MISMATCH_ERROR).setAttribute(ATTRIBUTE_POLICY, getPolicy(context)).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
            context.challenge(challenge);
            return;
        }
        PolicyError isPassPolicyErr = isValidPolicy(context, passwordNew);
        if (isPassPolicyErr != null) {
            LoginFormsProvider formsProvider = context.form();
            if (isPassPolicyErr.getParameters().length > 0) {
                formsProvider.setAttribute("policyErrorParams", isPassPolicyErr.getParameters()[0]);
            }
            Response challenge = formsProvider.setAttribute("policyError", isPassPolicyErr.getMessage()).setAttribute("username", authSession.getAuthenticatedUser().getUsername()).setAttribute(ATTRIBUTE_POLICY, getPolicy(context)).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
            context.challenge(challenge);
            return;
        }
        try {
            session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password(passwordNew, false));
            // Marking email as verified.
            context.getUser().setEmailVerified(true);
            session.sessions().removeUserSessions(realm);
            context.success();
            return;
        } catch (ModelException me) {
            errorEvent.detail(Details.REASON, me.getMessage()).error(Errors.PASSWORD_REJECTED);
            Response challenge = context.form().setAttribute("username", authSession.getAuthenticatedUser().getUsername()).setError(me.getMessage(), me.getParameters()).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
            context.challenge(challenge);
            return;
        } catch (Exception ape) {
            errorEvent.detail(Details.REASON, ape.getMessage()).error(Errors.PASSWORD_REJECTED);
            Response challenge = context.form().setAttribute("username", authSession.getAuthenticatedUser().getUsername()).setError(ape.getMessage()).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
            context.challenge(challenge);
            return;
        }
    }

    private void showView(RequiredActionContext context, String error) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.setAttribute(ERROR, error);
        }
        Response challenge = formsProvider.setAttribute("username", context.getAuthenticationSession().getAuthenticatedUser().getUsername()).setAttribute("reqFromWebsite", checkReqFrom(context)).setAttribute("policy", getPolicy(context)).setAttribute("username", context.getUser().getUsername()).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
        context.challenge(challenge);
    }

    @Override
    public void close() {
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleUpdatePassword.SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getDisplayText() {
        return "update-password";
    }

    @Override
    public String getId() {
        return UserModel.RequiredAction.UPDATE_PASSWORD.name();
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();


    static {

        ProviderConfigProperty configProperty;
        configProperty = new ProviderConfigProperty();
        configProperty.setName(PASS_POLICY);
        configProperty.setLabel("password-policy-label");
        configProperty.setType(ProviderConfigProperty.POLICY_LIST_TYPE);
        configProperty.setDefaultValue("default_password_policy");
        configProperty.setHelpText("password-policy-hlp-txt");
        configProperty.setRequired(false);
        configProperties.add(configProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public String[] supportedModes() {
        return new String[] {"ciam","wiam"};
    }

    private boolean checkReqFrom(RequiredActionContext context) {
        String authNote = context.getAuthenticationSession().getAuthNote("requestFromWebsite");
        if (authNote == null) return false;
        return Boolean.parseBoolean(authNote);
    }

    private List<Map<String, String>> getPolicy(RequiredActionContext context) {
        ArrayList arrayList = new ArrayList();
        RealmPolicyModel policyModel = context.getSession().policies().getPolicyByName(context.getRealm(), getPolicyName(context));
        if (policyModel == null) return arrayList;
        PasswordPolicy passwordPolicy = PasswordPolicy.parse(context.getSession(), policyModel.getPolicyValue());
        if (passwordPolicy.getPolicies().size() == 0) return arrayList;
        passwordPolicy.getPolicies().stream().filter(x -> !Arrays.asList(skippedPolicy).contains(x)).forEach(x -> {
            Object result = passwordPolicy.getPolicyConfig(x);
            if (result instanceof Integer) {
                int value = (Integer) result;
                if (x.equals("length")) {
                    arrayList.add(new PassPolicy("Requires minimum " + x + " of " + value, value, "minimum"));
                } else if (value == 1) {
                    arrayList.add(new PassPolicy("Requires atleast " + value + " " + x, value, x));
                } else if (value > 1) {
                    arrayList.add(new PassPolicy("Requires " + value + " " + x, value, x));
                }
            }
        });
        return arrayList;
    }

    private PolicyError isValidPolicy(RequiredActionContext context, String password) {
        RealmPolicyModel policyModel = context.getSession().policies().getPolicyByName(context.getRealm(), getPolicyName(context));
        if (policyModel == null) {
            return null;
        }
        return context.getSession().getProvider(PasswordPolicyManagerProvider.class).validate(policyModel.getId(), context.getUser(), password);
    }

    private String getPolicyName(RequiredActionContext context){
        RealmModel realm = context.getRealm();

        String policyId = null;
        String actionsId = context.getAuthenticationSession().getAuthNote(CURRENT_ACTION_ID);

        Optional<RequiredActionProviderModel> requiredActionProviderModel = realm.getRequiredActionProvidersStream(actionsId).filter(x -> x.getAlias().equals(getId())).findFirst();

        if (requiredActionProviderModel.isPresent())
            policyId = requiredActionProviderModel.get().getPasswordPolicy();
        if(context.getAuthenticationSession().getAuthNote("recovery_password_policy_id") != null){
            policyId = context.getAuthenticationSession().getAuthNote("recovery_password_policy_id");
        }

        return policyId == null ?
                context.getSession().policies().getPolicyByName(realm,DEFAULT_PASSWORD_POLICY).getPolicyName() :
                context.getSession().policies().getPolicyById(realm,policyId).getPolicyName();
    }
}