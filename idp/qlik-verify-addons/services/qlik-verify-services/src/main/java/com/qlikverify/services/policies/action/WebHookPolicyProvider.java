package com.qlikverify.services.policies.action;

import okhttp3.Response;
import org.keycloak.Config;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.validation.Validation;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class WebHookPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {

    public static final String ID = "webhookPolicy";

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        URL policyConfig = policy.getPolicyConfig(ID);
        if (policyConfig == null)
            return false;

        ClientConnection clientConnection = session.getContext().getConnection();
        Map<String, Object> map = new HashMap<>();
        map.put("user", ModelToRepresentation.toBriefRepresentation(user));
        Map<String, Object> clientConnectionAttr = new HashMap<>();
        clientConnectionAttr.put("ip", clientConnection.getRemoteAddr());
        clientConnectionAttr.put("browser", clientConnection.getClientBrowser());
        clientConnectionAttr.put("os", clientConnection.getClientOs());
        clientConnectionAttr.put("browserRequest", clientConnection.isBrowserRequest());
        map.put("request", clientConnectionAttr);
        map.put("realm", ModelToRepresentation.toBriefRepresentation(session.getContext().getRealm(), false, session));
        map.put("userAttributes", santizeUserAttributes(user));
        HttpUtility httpUtility = new HttpUtility();
        Response response = null;
        boolean check = false;
        try {
            response = httpUtility.callRequest("application/json", "POST", policyConfig.toString(),
                    JsonSerialization.writeValueAsString(map), null);
            if ((response == null || !response.isSuccessful()) && !isHookFailureNotificationSent(session)) {
                sendHookFailedEmail(session, policy, policyConfig);
                session.getContext().getAuthenticationSession().setAuthNote("hook_failure_notification_sent", "true");
            }
            check = response.isSuccessful();
        } catch (IOException e) {
            sendHookFailedEmail(session, policy, policyConfig);
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return check;
    }

    private static void sendHookFailedEmail(KeycloakSession session, ActionPolicy policy, URL policyConfig) {
        final RealmModel realm = session.getContext().getRealm();
        Map<String, Object> attrs = new HashMap<>();
        final Optional<RealmPolicyModel> currentPolicy =
                session.policies().getAllPoliciesStream(session.getContext().getRealm(), -1, -1)
                        .filter(p -> p.getPolicyValue().equalsIgnoreCase(policy.toBuilder().build(session).toString())).findAny();
        attrs.put("policyName", currentPolicy.isPresent() ? currentPolicy.get().getPolicyName() : "");
        attrs.put("hookUrl", policyConfig.toString());
        attrs.put("isPolicy", true);
        attrs.put("workspaceName", realm.getName());

        AdminEventBuilder adEvt = new AdminEventBuilder(realm,
                new AdminAuth(realm,
                        null, null, realm.getClientByClientId("dashboard")), session,
                session.getContext().getConnection());
        adEvt.realm(realm);
        adEvt.operation(OperationType.HOOK_FAILURE_NOTIFICATION)
                .resource(ResourceType.NOTIFICATION)
                .resourcePath(session.getContext().getUri(),
                        session.clients().getClientByClientId(realm, "dashboard").getId())
                .representation(attrs).success();
    }

    @Override
    public URL parseConfig(String value) {
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getDisplayName() {
        return "webhook";
    }

    @Override
    public String getConfigType() {
        return ActionPolicyProvider.STRING_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return "";
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public ActionPolicyProvider create(KeycloakSession session) {
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
        return ID;
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"wiam", "ciam", "bo"};
    }

    private Map<String, List<String>> santizeUserAttributes(UserModel userModel) {
        String[] blacklisted = new String[]{UserModel.USERNAME, UserModel.FIRST_NAME, UserModel.LAST_NAME,
                UserModel.EMAIL, UserModel.MOBILE_NUMBER, UserModel.MOBILE_NUMBER_REGION};
        Map<String, List<String>> sanitized = new HashMap<>();
        Map<String, List<String>> attrs = userModel.getAttributes();
        if (attrs.size() == 0)
            return sanitized;

        attrs.forEach((k, v) -> {
            if (k.startsWith("hidden"))
                return;

            if (Arrays.stream(blacklisted).filter(x -> x.equalsIgnoreCase(k)).findFirst().isPresent())
                return;

            sanitized.put(k, v);
        });
        return sanitized;
    }

    private boolean isHookFailureNotificationSent(KeycloakSession session) {
        String hookFailureNotificationSent = session.getContext().getAuthenticationSession().getAuthNote("hook_failure_notification_sent");
        if (Validation.isBlank(hookFailureNotificationSent)) {
            return false;
        }
        return Boolean.parseBoolean(hookFailureNotificationSent);
    }
}
