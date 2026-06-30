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
package org.keycloak.mfa.recovery;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.credential.*;
import org.keycloak.mfa.recovery.credential.MFARecoveryCredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MFARecoveryCredentialProvider implements CredentialProvider<MFARecoveryCredentialModel>, CredentialInputValidator {
    private static final Logger logger = Logger.getLogger(MFARecoveryCredentialProvider.class);

    protected KeycloakSession session;

    public MFARecoveryCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    private UserCredentialStore getCredentialStore() {
        return session.userCredentialManager();
    }


    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel for CredentialInput");
            return false;
        }
        if (!input.getType().equals(getType())) {
            return false;
        }
        String challengeResponse = input.getChallengeResponse();
        if (challengeResponse == null) {
            return false;
        }
        CredentialModel credentialModel = null;
        if (input.getCredentialId() == null) {
            Optional<CredentialModel> optionalCredentialModel = getCredentialStore().getStoredCredentialsByTypeStream(realm, user, MFARecoveryCredentialModel.TYPE).findFirst();
            if (optionalCredentialModel.isPresent()) {
                credentialModel = optionalCredentialModel.get();
            } else {
                return false;
            }
        } else {
            credentialModel = getCredentialStore().getStoredCredentialById(realm, user, input.getCredentialId());
        }
        MFARecoveryCredentialModel sqcm = getCredentialFromModel(credentialModel);
        List<String> recoveryCodes = new ArrayList<>();
        Collections.addAll(recoveryCodes, sqcm.getMFARecoveryData().split("##"));
        if (recoveryCodes.contains(challengeResponse)) {
            recoveryCodes.removeIf(x-> x.equalsIgnoreCase(challengeResponse));
            credentialModel.setSecretData(String.join("##",recoveryCodes));
            getCredentialStore().updateCredential(realm, user, credentialModel);
            return true;
        }
        return false;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getType().equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return false;
        return getCredentialStore().getStoredCredentialsByTypeStream(realm, user, credentialType).count() > 0;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, MFARecoveryCredentialModel credentialModel) {

        getCredentialStore().getStoredCredentialsByTypeStream(realm, user, MFARecoveryCredentialModel.TYPE).forEach(x-> getCredentialStore().removeStoredCredential(realm,user, x.getId()));

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
    public MFARecoveryCredentialModel getCredentialFromModel(CredentialModel model) {
        return MFARecoveryCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName(MFARecoveryCredentialProviderFactory.PROVIDER_ID)
                .helpText("secret-question-text")
                .createAction(MFARecoveryCredentialProviderFactory.PROVIDER_ID)
                .removeable(false)
                .build(session);
    }

    @Override
    public String getType() {
        return MFARecoveryCredentialModel.TYPE;
    }
}
