package com.qlikverify.backoffice.payment;

import com.qlikverify.helpers.GeneralHelper;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.jboss.logging.Logger;
import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;
import org.keycloak.common.Version;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultVerificationPolicy implements ClientRegistrationPolicy {

    private final KeycloakSession session;
    private final ComponentModel componentModel;
    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_URL);
    private static final String DEFAULT_ACCESS_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_ACCESS_KEY);
    private static final String DEFAULT_SECRET_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_SECRET_KEY);
    private static final Logger logger = Logger.getLogger(ResultVerificationPolicy.class);


    public ResultVerificationPolicy(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        // NOP
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel, Map<String, Object> attributes) {

        if (Version.OFFERING.equalsIgnoreCase("onpremise"))
            return;

        RealmModel realm = session.getContext().getRealm();
        ClientModel client = realm.getClientById(String.valueOf(attributes.get("clientid")));
        if (componentModel.getSubType().equalsIgnoreCase(RegistrationAuth.AUTHENTICATED.toString()) && Version.OFFERING.equalsIgnoreCase("cloud")) {
            TrxModel trxModel = realm.getTrx(String.valueOf(attributes.get("requestId")));
            boolean isSkip = Boolean.parseBoolean(trxModel.getAttributes().get("ik@skip_user_attrs_delete"));
            if(!isSkip) {

                removeTrxAttributes(realm,trxModel);

                ((List<RequiredActionProviderModel>) attributes.get("requiredActions")).stream().forEach(requiredActionProviderModel -> {
                    try {
                        removeObjectFromMinio(realm, client, trxModel, requiredActionProviderModel.getName());
                    } catch (Exception e) {
                        logger.error("Minio Operation Failed");
                    }
                });
            }
            realm.removeTrxAttribute(trxModel.getTrxId(),"ik@skip_user_attrs_delete");
        }

    }

    private void removeTrxAttributes(RealmModel realmModel, TrxModel trxModel) {
        realmModel.removeTrx(trxModel.getTrxId());
    }

    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
        // NOP
    }

    @Override
    public void afterUpdate(ClientRegistrationContext context, ClientModel clientModel) {
        // NOP
    }

    @Override
    public void beforeView(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
        // NOP
    }

    @Override
    public void beforeDelete(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
        // NOP
    }

    private static void removeObjectFromMinio(RealmModel realm, ClientModel clientModel, TrxModel trxModel, String providerId) throws UnsupportedOperationException {
        realm.removeTrxAttribute(trxModel.getTrxId(),"spm@" + providerId.replace("bo-", ""));
        List<String> providers = GeneralHelper.getRequiredActionProvidersForClient(realm, clientModel);
        if (providers.contains(providerId)) {
            MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_SERVER_URL).credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();
            try {
                minioClient.ignoreCertCheck();
            } catch (Exception e) {
                throw new UnsupportedOperationException("Issue in Minio Certificate. Contact admin");
            }

            String objectName = trxModel.getTrxId() + "/" + providerId + ".json";

            try {
                boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket((String.valueOf(realm.getName()))).build());
                if (bucketExists) {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(realm.getName()).object(objectName).build());
                }
            } catch (Exception e) {
                throw new UnsupportedOperationException("Minio operation failed");
            }
        }
    }
}
