/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authentication.requiredactions;


import jakarta.ws.rs.WebApplicationException;
import org.keycloak.authentication.*;
import org.keycloak.authentication.authenticators.util.AcrStore;
import org.keycloak.authentication.requiredactions.util.CredentialDeleteHelper;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DeleteCredentialAction implements RequiredActionProvider, CredentialAction {

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }


    @Override
    public void evaluateTriggers(RequiredActionContext context) {

    }

    @Override
    public String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSession) {
        String credentialId = authenticationSession.getClientNote(Constants.KC_ACTION_PARAMETER);
        if (credentialId == null) {
            return null;
        }

        UserModel user = authenticationSession.getAuthenticatedUser();
        if (user == null) {
            return null;
        }

        CredentialModel credential = session.userCredentialManager().getStoredCredentialById(authenticationSession.getRealm(), user, credentialId);
        if (credential == null) {
            if (credentialId.endsWith("-id")) {
                return credentialId.substring(0, credentialId.length() - 3);
            } else {
                return null;
            }
        } else {
            return credential.getType();
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        String credentialId = context.getAuthenticationSession().getClientNote(Constants.KC_ACTION_PARAMETER);
        UserModel user = context.getUser();
        if (credentialId == null) {
            context.getEvent()
                    .error(Errors.MISSING_CREDENTIAL_ID);
            context.ignore();
            return;
        }

        String credentialLabel;
        CredentialModel credential = context.getSession().userCredentialManager().getStoredCredentialById(context.getRealm(), user, credentialId);
        if (credential == null) {
            // Backwards compatibility with account console 1 - When stored credential is not found, it may be federated credential.
            // In this case, it's ID needs to be something like "otp-id", which is returned by account REST GET endpoint as a placeholder
            // for federated credentials (See CredentialHelper.createUserStorageCredentialRepresentation )
            if (credentialId.endsWith("-id")) {
                credentialLabel = credentialId.substring(0, credentialId.length() - 3);
            } else {
                context.getEvent()
                        .detail(Details.CREDENTIAL_ID, credentialId)
                        .error(Errors.CREDENTIAL_NOT_FOUND);
                context.ignore();
                return;
            }
        } else {
            credentialLabel = StringUtil.isNotBlank(credential.getUserLabel()) ? credential.getUserLabel() : credential.getType();
        }

        Response challenge = context.form()
                .setAttribute("credentialLabel", credentialLabel)
                .createForm("delete-credential.ftl");
        context.challenge(challenge);
    }

    private void setupEvent(CredentialModel credential, EventBuilder event) {
        if (credential != null) {
            if (OTPCredentialModel.TYPE.equals(credential.getType())) {
                event.event(EventType.REMOVE_TOTP);
            }
            event.detail(Details.CREDENTIAL_TYPE, credential.getType())
                    .detail(Details.CREDENTIAL_ID, credential.getId());
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        EventBuilder event = context.getEvent();
        String credentialId = context.getAuthenticationSession().getClientNote(Constants.KC_ACTION_PARAMETER);

        CredentialModel credential = context.getSession().userCredentialManager().getStoredCredentialById(context.getRealm(), context.getUser(), credentialId);
        setupEvent(credential, event);

        try {
            CredentialDeleteHelper.removeCredential(context.getSession(), context.getUser(), context.getRealm(), credentialId, () -> getCurrentLoa(context.getSession(), context.getAuthenticationSession()));
            context.success();

        } catch (WebApplicationException wae) {
            javax.ws.rs.core.Response response = context.getSession().getProvider(LoginFormsProvider.class)
                    .setAuthenticationSession(context.getAuthenticationSession())
                    .setUser(context.getUser())
                    .setError(wae.getMessage())
                    .createErrorPage(javax.ws.rs.core.Response.Status.BAD_REQUEST);
            event.detail(Details.REASON, wae.getMessage())
                    .error(Errors.DELETE_CREDENTIAL_FAILED);
            context.challenge(response);
        }
    }

    private int getCurrentLoa(KeycloakSession session, AuthenticationSessionModel authSession) {
        return new AcrStore(session, authSession).getLevelOfAuthenticationFromCurrentAuthentication();
    }

    @Override
    public void close() {

    }
}