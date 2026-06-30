package com.qlikverify.backoffice.authorizers.utility;

import com.qlikverify.backoffice.creds.device.TokenCredentialModel;
import com.qlikverify.backoffice.creds.device.TokenSecretData;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.CibaAuthConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.Constants.SECRET_KEY;
import static com.qlikverify.backoffice.authorizers.DesktopAuthorizer.OWN_DESKTOP_APP;
import static com.qlikverify.backoffice.authorizers.MobileAuthorizer.*;

public class TokenCredentialUtility {
    private static final Logger logger = Logger.getLogger(TokenCredentialUtility.class);
    private static TokenCredentialUtility instance;

    private TokenCredentialUtility() {
    }

    public static TokenCredentialUtility getInstance() {
        if (instance == null) {
            instance = new TokenCredentialUtility();
        }
        return instance;
    }

    public enum DeviceType {
        MOBILE,
        DESKTOP
    }

    public void updateLastUsedToken(KeycloakSession session, UserModel userModel, String token) {
        Optional<CredentialModel> lastUsedCredentialModel = session.userCredentialManager()
                .getStoredCredentialsByTypeStream(session.getContext().getRealm(), userModel, TokenCredentialModel.TYPE)
                .filter(CredentialModel::isEnabled)
                .filter(credentialModel -> {
                    try {
                        TokenSecretData tokenSecretData = JsonSerialization.readValue(credentialModel.getSecretData(), TokenSecretData.class);
                        return tokenSecretData.getToken().equals(token);
                    } catch (IOException e) {
                        logger.error("Failed to get last used device", e);
                        return false;
                    }
                }).findAny();
        if (lastUsedCredentialModel.isPresent()) {
            CredentialModel credentialModel = lastUsedCredentialModel.get();
            credentialModel.setLastUsed(Time.currentTimeMillis());
            session.userCredentialManager().updateCredential(session.getContext().getRealm(), userModel, credentialModel);
        }
    }

    public Optional<CredentialModel> getLastUsedDeviceCredentialModel(KeycloakSession session, UserModel userModel, DeviceType deviceType) {
        return session.userCredentialManager()
                .getStoredCredentialsByTypeStream(session.getContext().getRealm(), userModel, TokenCredentialModel.TYPE)
                .filter(credentialModel -> {
                    try {
                        TokenSecretData tokenSecretData = JsonSerialization.readValue(credentialModel.getSecretData(), TokenSecretData.class);
                        if (deviceType.equals(DeviceType.MOBILE)) {
                            return tokenSecretData.getDevicePlatform().equalsIgnoreCase("Android")
                                    || tokenSecretData.getDevicePlatform().equalsIgnoreCase("iOS");
                        } else if (deviceType.equals(DeviceType.DESKTOP)) {
                            return !tokenSecretData.getDevicePlatform().equalsIgnoreCase("Android")
                                    && !tokenSecretData.getDevicePlatform().equalsIgnoreCase("iOS");
                        }
                    } catch (IOException e) {
                        logger.error("Error occurred while retrieving last used push token: " + e.getMessage());
                    }
                    return false;
                })
                .filter(CredentialModel::isEnabled).min((cred1, cred2) -> {
                    Long lastUsed1 = cred1.getLastUsed() != null ? cred1.getLastUsed() : cred1.getCreatedDate();
                    Long lastUsed2 = cred2.getLastUsed() != null ? cred2.getLastUsed() : cred2.getCreatedDate();
                    return Long.compare(lastUsed2, lastUsed1);
                });
    }

    public Optional<String> getDeviceToken(CredentialModel credentialModel) {
        try {
            TokenSecretData tokenSecretData = JsonSerialization.readValue(credentialModel.getSecretData(), TokenSecretData.class);
            return Optional.of(tokenSecretData.getToken());
        } catch (IOException e) {
            logger.error("Error occurred while retrieving last used push token: " + e.getMessage());
            return Optional.empty();
        }
    }

    public String getSecret(KeycloakSession session, CibaAuthConfigModel cibaAuthConfigModel, DeviceType deviceType) {
        String secretKey = null;

        String intType = cibaAuthConfigModel.getConfig().get(INTEGRATION_TYPE);

        if (intType.equals(EZTO_VERIFY_APP)) {
            switch (deviceType) {
                case MOBILE:
                    return session.realms().getRealmByName(Config.getAdminRealm()).getAttribute(MOBILE_ENC_KEY);
                case DESKTOP:
                    return session.realms().getRealmByName(Config.getAdminRealm()).getAttribute(DESKTOP_ENC_KEY);
                default:
                    return null;
            }
        }

        String keyOpts = cibaAuthConfigModel.getConfig().get(KEY_OPTIONS);

        if (deviceType.equals(DeviceType.MOBILE) && keyOpts.equals(FROM_WORKSPACE)) {
            return session.getContext().getRealm().getAttribute(MOBILE_ENC_KEY);
        } else if (deviceType.equals(DeviceType.DESKTOP) && keyOpts.equals(FROM_WORKSPACE)) {
            return session.getContext().getRealm().getAttribute(DESKTOP_ENC_KEY);
        }

        if (keyOpts.equals(WILL_SET_APPLICATION_LEVEL)) {
            HashMap<String, Object> config = new HashMap<>();
            config.put(SECRET_KEY, cibaAuthConfigModel.getConfig().get(SECRET_KEY));
            ComponentRepresentation componentRepresentation = getEncryptionComponent(session, config);
            if (componentRepresentation != null) {
                secretKey = componentRepresentation.getConfig().get(SECRET).get(0);
            }
            return secretKey;
        }

        return null;
    }

    private ComponentRepresentation getEncryptionComponent(KeycloakSession session, HashMap<String, Object> config) {
        ComponentModel optionalComponentModel = session.getContext().getRealm().getComponent(String.valueOf(config.get(SECRET_KEY)));
        if (optionalComponentModel == null) {
            return null;
        }

        return ModelToRepresentation.toRepresentation(session, optionalComponentModel, true);
    }
}
