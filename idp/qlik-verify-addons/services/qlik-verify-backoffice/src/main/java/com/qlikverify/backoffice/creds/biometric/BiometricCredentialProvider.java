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
package com.qlikverify.backoffice.creds.biometric;

import com.qlikverify.backoffice.models.deviceDetails.DeviceDetails;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.credential.*;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BiometricCredentialProvider implements CredentialProvider<BiometricCredentialModel>, CredentialInputValidator {
    private static final Logger logger = Logger.getLogger(BiometricCredentialProvider.class);

    protected KeycloakSession session;

    protected EventBuilder eventBuilder;

    public BiometricCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    private UserCredentialStore getCredentialStore() {
        return session.userCredentialManager();
    }

    public BiometricCredentialProvider setEvent(EventBuilder event)
    {
        this.eventBuilder = event;
        return this;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof BiometricCredentialInput)) {
            logger.debug("Expected instance of BiometricCredentialInput for CredentialInput");
            return false;
        }
        if (!input.getType().equals(getType())) {
            eventBuilder.error("Invalid input type");
            return false;
        }
        String challengeResponse = input.getChallengeResponse();
        if (challengeResponse == null) {
            eventBuilder.error("Challenge is missing");
            return false;
        }

        String encryptedChallenge = ((BiometricCredentialInput) input).getEncryptedChallenge();
        CredentialModel credentialModel = getCredentialStore().getStoredCredentialById(realm, user, input.getCredentialId());

        if(credentialModel == null) {
            eventBuilder.error("No credential model is available");
            return false;
        }

        BiometricCredentialModel sqcm = getCredentialFromModel(credentialModel);
        if(((BiometricCredentialInput) input).getDeviceId() == null) {
            eventBuilder.error("No device details available");
            return false;
        }

        if(!sqcm.getBiometricSecretData().getDeviceId().equals(((BiometricCredentialInput) input).getDeviceId())) {
            eventBuilder.error("Invalid device");
            return false;
        }

        if(!sqcm.getBiometricSecretData().getDeviceHash().equals(((BiometricCredentialInput)input).getDeviceHash()))
        {
            eventBuilder.error("Invalid device hash. User might have update the os version. User need to re-register the device. Ask user to re-register or trigger biometric transaction again");
            return false;
        }
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(sqcm.getBiometricSecretData().getPrivateKey());

            // Create a PKCS8EncodedKeySpec from the decoded bytes
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);

            // Initialize the RSA KeyFactory and generate the private key
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey rsaPrivateKey = keyFactory.generatePrivate(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
            byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedChallenge));
            if(!(new String(Base64.getEncoder().encode(decryptedData))).equals(input.getChallengeResponse()))
            {
                eventBuilder.error("Challenge match failed");
                return false;
            }
            return true;
        } catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | InvalidKeySpecException |
                 IllegalBlockSizeException | NoSuchPaddingException e) {
            eventBuilder.error(e.getMessage());
            e.printStackTrace();
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

        getCredentialStore().getStoredCredentialsByTypeStream(realm, user, credentialType).filter(x-> x.isTemporary()).filter(x->x.getCreatedDate() < Time.currentTimeMillis()).forEach(x-> getCredentialStore().removeStoredCredential(realm,user,x.getId()));

        return getCredentialStore().getStoredCredentialsByTypeStream(realm, user, credentialType).filter(x-> !x.isTemporary()).count() > 0;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, BiometricCredentialModel credentialModel) {
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
    public BiometricCredentialModel getCredentialFromModel(CredentialModel model) {
        return BiometricCredentialModel.createFromCredentialModel(model,new DeviceDetails(),null);
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.PASSWORDLESS)
                .displayName(BiometricCredentialProviderFactory.PROVIDER_ID)
                .helpText("biometric-text")
                .createAction(BiometricCredentialProviderFactory.PROVIDER_ID)
                .removeable(false)
                .build(session);
    }

    @Override
    public String getType() {
        return BiometricCredentialModel.TYPE;
    }
}
