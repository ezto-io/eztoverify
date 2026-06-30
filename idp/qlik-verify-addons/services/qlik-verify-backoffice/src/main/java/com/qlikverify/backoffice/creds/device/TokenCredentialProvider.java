/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package com.qlikverify.backoffice.creds.device;

import com.qlikverify.backoffice.creds.device.TokenCredentialProviderFactory;
import org.keycloak.common.util.Time;
import org.keycloak.credential.*;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenCredentialProvider implements CredentialProvider<TokenCredentialModel>, CredentialInputValidator {

    protected KeycloakSession session;
    protected EventBuilder eventBuilder;

    public TokenCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    public TokenCredentialProvider setEvent(EventBuilder event)
    {
        this.eventBuilder = event;
        return this;
    }

    private UserCredentialStore getCredentialStore() {
        return session.userCredentialManager();
    }


    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        return true;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getType().equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return false;
        return getCredentialStore().getStoredCredentialsByTypeStream(realm, user, credentialType).filter(x-> !x.isTemporary()).count() > 0;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, TokenCredentialModel credentialModel) {
        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }
        return getCredentialStore().createCredential(realm, user, credentialModel);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return getCredentialStore().removeStoredCredential(realm, user, credentialId);
    }

    @Override
    public TokenCredentialModel getCredentialFromModel(CredentialModel model) {
        try {
            return TokenCredentialModel.createFromCredentialModel(model,null,null,null,null, null, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.PASSWORDLESS)
                .displayName(TokenCredentialProviderFactory.PROVIDER_ID)
                .createAction(TokenCredentialProviderFactory.PROVIDER_ID)
                .removeable(false)
                .build(session);
    }

    @Override
    public String getType() {
        return TokenCredentialModel.TYPE;
    }
}
