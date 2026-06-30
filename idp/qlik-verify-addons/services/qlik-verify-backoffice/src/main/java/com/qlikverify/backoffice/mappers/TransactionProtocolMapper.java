package com.qlikverify.backoffice.mappers;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.apache.kafka.common.protocol.types.Field;
import org.jboss.logging.Logger;
import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {
    private static final Logger logger = Logger.getLogger(TransactionProtocolMapper.class);
    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_URL);
    private static final String DEFAULT_ACCESS_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_ACCESS_KEY);
    private static final String DEFAULT_SECRET_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_SECRET_KEY);

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        return token;
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSession) {
        String requestId = userSession.getNote("requestId");

        if (requestId == null)
            return token;

        List<String> tempAttrs = new ArrayList<>();
        token.setOtherClaims("requestId", requestId);
        Map<String, String> trxAttrs = userSession.getRealm().getTrx(requestId).getAttributes();
        trxAttrs.keySet().stream().filter(x -> x.startsWith("sp@")).forEach(x -> {
            tempAttrs.add(x);
            token.setOtherClaims(x.split("sp@")[1], trxAttrs.get(x) == null || trxAttrs.get(x).isEmpty() ? "No Value" : trxAttrs.get(x));
        });

        ClientModel clientModel = clientSession.getClientSession().getClient();

//        if (clientModel != null && clientModel.getAuthenticationFlowBindingOverrides().containsKey("action")) {
//            RealmModel realm = session.getContext().getRealm();
//            realm.getRequiredActionProvidersStream(clientModel.getAuthenticationFlowBindingOverrides().get("action")).forEach(x -> getAndSetClaims(token, realm.getName(), requestId, x.getName()));
//        }


        return token;
    }

    private void getAndSetClaims(IDToken backofficeTokenResponse, String realmName, String requestId, String verificationId) {

        String objectName = requestId + "/" + verificationId + ".json";


        MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_SERVER_URL).credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();
        try {
            minioClient.ignoreCertCheck();
            InputStream object = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(realmName).object(objectName).build());
            String minioData = new String(object.readAllBytes(), StandardCharsets.UTF_8);
            if (StringUtil.isBlank(minioData))
                return;
            HashMap<String, Object> retrievedData = JsonSerialization.readValue(minioData, HashMap.class);
            retrievedData.forEach(backofficeTokenResponse::setOtherClaims);
        } catch (io.minio.errors.ErrorResponseException e) {
            logger.error("object not found in Minio");
            return;
        } catch (ServerException | IOException | InsufficientDataException | NoSuchAlgorithmException |
                InvalidKeyException | InvalidResponseException | XmlParserException | InternalException |
                KeyManagementException e) {
            logger.error("object not found in Minio");
            throw new IllegalStateException(e);
        }
    }

    @Override
    public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        return token;
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "secureplus-mapper-title";
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return new ArrayList<>();
    }

    @Override
    public String getId() {
        return "ciba_protocol-mapper";
    }

    public static ProtocolMapperModel createMapper(String name) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper("ciba_protocol-mapper");
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        return mapper;
    }
}
