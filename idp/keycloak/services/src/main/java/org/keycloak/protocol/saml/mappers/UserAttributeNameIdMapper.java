package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAttributeNameIdMapper extends AbstractSAMLProtocolMapper implements SAMLNameIdMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        NameIdMapperHelper.setConfigProperties(configProperties);
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
        property.setHelpText(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_HELP_TEXT);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "saml-user-attribute-nameid-mapper";

    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Attribute Mapper For NameID";
    }

    @Override
    public String getDisplayCategory() {
        return "NameID Mapper";
    }

    @Override
    public String getHelpText() {
        return "Map user attribute to SAML NameID value.";
    }

    @Override
    public String mapperNameId(String nameIdFormat, ProtocolMapperModel mappingModel, KeycloakSession session,
            UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        return userSession.getUser().getFirstAttribute(mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE));
    }

    public static ProtocolMapperModel createClaimMapper(String name,
            String... configKeyValue) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);

        Map<String, String> config = new HashMap<>();
        for (int i = 0; i < configKeyValue.length - 1; i += 2) {
            String key = configKeyValue[i];
            String value = configKeyValue[i + 1];
            config.put(key, value);
        }
        mapper.setConfig(config);
        return mapper;
    }

}
