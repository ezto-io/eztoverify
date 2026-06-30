/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.protocol.oidc.grants.ciba.resolvers;

import org.keycloak.OAuthErrorException;
import org.keycloak.ciba.models.UserData;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class DefaultCIBALoginUserResolver implements CIBALoginUserResolver {

    private final KeycloakSession session;
    private EventBuilder eventBuilder;
    private String requestId;
    public static final String WORKFLOW = "workflow";
    public static final String SP_WORKFLOW = "sp@workflow";
    private static final String ONBOARDING = "onboarding";
    private static final String VERIFICATION = "verification";

    public DefaultCIBALoginUserResolver(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public UserModel getUserFromLoginHint(UserData loginHint) {

        loginHint = validateUserDataFromRequest(loginHint);
        if (loginHint == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST,
                    "Invalid details. Check Audit > logs for more details.", Response.Status.BAD_REQUEST);
        }
        UserModel userModel = null;
        if (loginHint.getUserId() != null) {
            userModel = session.users().getUserByUsername(session.getContext().getRealm(), loginHint.getUserId());
        }

        if (userModel == null) {
            String username = null;
            if (username == null)
                username = loginHint.getEmail();

            if (username == null || username.isEmpty())
                username = loginHint.getPhone();

            if (username != null && !username.isEmpty()) {
                userModel = KeycloakModelUtils.findUserByNameOrEmail(session, session.getContext().getRealm(), username);
            }
            if (userModel == null && StringUtil.isNotBlank(loginHint.getPhone()) && StringUtil.isNotBlank(loginHint.getPhoneRegion())) {
                userModel = KeycloakModelUtils.findUserByMobile(session, session.getContext().getRealm(), loginHint.getPhone(), loginHint.getPhoneRegion().toLowerCase());
            }
            if (userModel != null && !validateUsersEmailAndMobile(userModel, loginHint))
                return null;
        } else {
            UserModel userByMobile = null;
            UserModel userByEmail = null;
            if (StringUtil.isNotBlank(loginHint.getEmail())) {
                userByEmail = session.users().getUserByEmail(session.getContext().getRealm(), loginHint.getEmail());
            }
            if (StringUtil.isNotBlank(loginHint.getPhone()) && StringUtil.isNotBlank(loginHint.getPhoneRegion())) {
                userByMobile = session.users().getUserByMobile(loginHint.getPhoneRegion() + loginHint.getPhone(), session.getContext().getRealm());
            }
            if (userByEmail != null && !userByEmail.getUsername().equals(loginHint.getUserId())) {
                eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).event(EventType.EZ_TRANSACTION_ERROR).error("Provided email is not associated with this user.");
                return null;
            }
            if (userByMobile != null && !userByMobile.getUsername().equals(loginHint.getUserId())) {
                eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).event(EventType.EZ_TRANSACTION_ERROR).error("Provided mobile number is not associated with this user.");
                return null;
            }
        }

        try {
            if (userModel == null) {
                userModel = session.users().addUser(session.getContext().getRealm(), (loginHint.getUserId() != null && !loginHint.getUserId().isEmpty()) ? loginHint.getUserId() : UUID.randomUUID().toString());
                userModel.setEnabled(true);
                // here we create random username in case user is not provided in request
                if (userModel != null) {
                    resolveActionFlow(loginHint, true);
                    eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).user(userModel).event(EventType.EZ_TRANSACTION_USER_ADD).success();
                } else {
                    eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).user(userModel).event(EventType.EZ_TRANSACTION_USER_ADD).error("User cannot be created. Check with admin");
                }
            } else {
                resolveActionFlow(loginHint, false);
                eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).user(userModel).event(EventType.EZ_TRANSACTION_USER_IDENTIFIED).detail(WORKFLOW, VERIFICATION).success();
            }
        } catch (Exception e){
            System.out.println("user model la exception airuku doiiiiiiiiii");
        }

        assert userModel != null;
        
        if (loginHint.getEmail() != null && !loginHint.getEmail().isEmpty()) userModel.setEmail(loginHint.getEmail());
        if (loginHint.getPhone() != null && !loginHint.getPhone().isEmpty())
            userModel.setSingleAttribute(UserModel.MOBILE_NUMBER, loginHint.getPhone());
        if (loginHint.getPhoneRegion() != null && !loginHint.getPhoneRegion().isEmpty())
            userModel.setSingleAttribute(UserModel.MOBILE_NUMBER_REGION, loginHint.getPhoneRegion());

        UserModel finalUserModel = userModel;

        if (loginHint.getMetadata() != null && !loginHint.getMetadata().isEmpty()) {
            String workflow = loginHint.getMetadata().get(WORKFLOW);
            loginHint.getMetadata().forEach((k, v) -> {
                if (!k.equals(WORKFLOW)) {
                    if (k.equalsIgnoreCase("sp@request_id") || k.equalsIgnoreCase("sp@user_id") || k.equalsIgnoreCase("sp@transaction_status")) {
                        return;
                    }
                    if(k.startsWith("sp@")) {
                        if(workflow.equalsIgnoreCase(ONBOARDING))
                            session.getContext().getRealm().addAttribute(requestId, k, v);
                    }
                    else
                        session.getContext().getRealm().addAttribute(requestId, k, v); // removed sp@
                }

            });
        }

        return finalUserModel;
    }

    public static boolean isValidUserId(String userId) {
        String regex = "^(?!.*([@#*.-])\\1)(?!\\.)(?!.*[^\\w\\s@#*.-])(?!.*\\.{2})[a-zA-Z0-9@#*._-]*[a-zA-Z0-9@#*._-](?:\\s?[a-zA-Z0-9@#*._-]+)*$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(userId);
        return matcher.matches();
    }

    public static boolean containsOnlyAlphanumeric(String userId) {
        String regex = "[a-zA-Z0-9]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(userId);
        return matcher.matches();
    }

    private boolean validateUsersEmailAndMobile(UserModel user, UserData loginHint) {
        boolean userHasEmailValue = StringUtil.isNotBlank(user.getEmail());
        boolean loginHintHasEmailValue = StringUtil.isNotBlank(loginHint.getEmail());
        boolean userHasMobileValue = StringUtil.isNotBlank(user.getMobileNumber());
        boolean loginHintHasMobileValue = StringUtil.isNotBlank(loginHint.getPhone());
        if (userHasEmailValue && loginHintHasEmailValue && !user.getEmail().equals(loginHint.getEmail())) {
            if (userHasMobileValue && loginHintHasMobileValue && user.getMobileNumber().equals(loginHint.getPhone())) {
                eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).event(EventType.EZ_TRANSACTION_ERROR).error("Provided mobile number is associated with another user.");
                return false;
            }
        }
        return true;
    }

    private UserData validateUserDataFromRequest(UserData userData) {
        String phoneNumber = userData.getPhone();
        String email = userData.getEmail();
        String phoneRegion = userData.getPhoneRegion();
        String userName = userData.getUserId();

        if (userName != null && StringUtil.isBlank(userName)) {
            userName = userName.trim();
            userData.setUserId(userName);
        }

        if (userName != null && StringUtil.isNotBlank(userName.trim())) {
            userName = userName.trim();
            if (userName.length() == 1 && !containsOnlyAlphanumeric(userName)) {
                eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).event(EventType.EZ_TRANSACTION_ERROR).error("Invalid userId format.");
                return null;
            }
            if (!isValidUserId(userName)) {
                eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).event(EventType.EZ_TRANSACTION_ERROR).error("Invalid userId format.");
                return null;
            }
            userData.setUserId(userName);
        }

        if (StringUtil.isNotBlank(email) && validateStringWithRegex("^[A-Za-z0-9+_.-]+@(.+)$", email)) {
            eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).event(EventType.EZ_TRANSACTION_ERROR).error("Invalid email.");
            return null;
        }

        if (StringUtil.isNotBlank(phoneRegion) && validateStringWithRegex("^[a-zA-Z]+$", phoneRegion)) {
            eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).event(EventType.EZ_TRANSACTION_ERROR).error("Invalid phone number region.");
            return null;
        }

        if (StringUtil.isNotBlank(phoneNumber) && (validateStringWithRegex("^(\\+\\d{1,3}[- ]?)?\\d{10}$", phoneNumber) || StringUtil.isBlank(phoneRegion))) {
            eventBuilder.realm(session.getContext().getRealm()).client(session.getContext().getClient().getClientId()).event(EventType.EZ_TRANSACTION_ERROR).error("Invalid phone number or region.");
            return null;
        }

        return userData;
    }

    private boolean validateStringWithRegex(String regex, String StringToBeValidated) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(StringToBeValidated);
        return !matcher.matches();
    }

    @Override
    public UserModel getUserFromInfoUsedByAuthentication(String info) {
        return KeycloakModelUtils.findUserByNameOrEmail(session, session.getContext().getRealm(), info);
    }

    @Override
    public CIBALoginUserResolver setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    @Override
    public CIBALoginUserResolver setEvent(EventBuilder event) {
        this.eventBuilder = event;
        return this;
    }

    @Override
    public void close() {
    }

    private void resolveActionFlow(UserData loginHint, boolean forceOnboarding) {
        if (loginHint != null && loginHint.getMetadata() != null
                && loginHint.getMetadata().get(WORKFLOW) != null
                && !loginHint.getMetadata().get(WORKFLOW).isEmpty()) {
            if (!loginHint.getMetadata().get(WORKFLOW).equals(ONBOARDING)
                    && !loginHint.getMetadata().get(WORKFLOW).equals(VERIFICATION)) {
                eventBuilder
                        .realm(session.getContext().getRealm())
                        .client(session.getContext().getClient().getClientId())
                        .event(EventType.EZ_TRANSACTION_START_ERROR)
                        .error("Invalid metadata value for 'workflow'." +
                                "It should be either 'onboarding' or 'verification'");

                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST,
                        "Invalid metadata value for 'workflow'." +
                                " It should be either 'onboarding' or 'verification'",
                        Response.Status.BAD_REQUEST);
            }
            session.getContext()
                    .getRealm()
                    .addAttribute(requestId, SP_WORKFLOW, loginHint.getMetadata().get(WORKFLOW));
        } else {
            session.getContext().getRealm().addAttribute(requestId, SP_WORKFLOW, forceOnboarding ? ONBOARDING: VERIFICATION);
        }

        String action = session.getContext().getRealm().getTrxValue(requestId, SP_WORKFLOW);
        Map<String, String> availableApplicationFlowOverrides = session.getContext().getClient().getAuthenticationFlowBindingOverrides();
        if (action.equals(ONBOARDING)) {
            String onboardingAction = availableApplicationFlowOverrides.getOrDefault(AuthenticationFlowBindings.OB_ACTION_BINDING, null);
            if (onboardingAction == null) {
                session.getContext().getRealm().addAttribute(requestId, SP_WORKFLOW,VERIFICATION);
                setApplicationOverride(session.getContext().getClient(), availableApplicationFlowOverrides.get(AuthenticationFlowBindings.VER_ACTION_BINDING));
            } else {
                setApplicationOverride(session.getContext().getClient(), availableApplicationFlowOverrides.get(AuthenticationFlowBindings.OB_ACTION_BINDING));
            }
        } else {
            setApplicationOverride(session.getContext().getClient(), availableApplicationFlowOverrides.get(AuthenticationFlowBindings.VER_ACTION_BINDING));
        }
    }

    private void setApplicationOverride(ClientModel clientModel, String action) {
        session.getContext().getClient().setAuthenticationFlowBindingOverride(AuthenticationFlowBindings.ACTION_BINDING, action);
    }

}
