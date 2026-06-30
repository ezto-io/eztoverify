package com.qlikverify.extensions.admin.controllers.workflows.rest;

import org.keycloak.Config;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.models.*;
import org.keycloak.models.utils.DefaultClientScopes;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.HardcodedAttributeMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.util.HashMap;
import java.util.Map;

import static org.keycloak.models.Constants.SAML;

public class WorkflowsResourceProviderFactory implements RealmResourceProviderFactory {

    //~ Static fields/initializers =========================================================================================================

    public static final String API_PROVIDER_ID = "workflows";

    //~ Methods ============================================================================================================================

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new WorkflowsResource(session);
    }

    //~ ====================================================================================================================================

    @Override
    public void init(Config.Scope config) {
        // No implementation required
    }

    //~ ====================================================================================================================================

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register((ProviderEvent event) -> {
                if(event instanceof RealmModel.RealmPostCreateEvent) {
                    RealmModel.RealmPostCreateEvent createRealmEvent = (RealmModel.RealmPostCreateEvent) event;
                    if (!createRealmEvent.getCreatedRealm().getMode().equals("bo"))
                        createStreamsManagementClient(factory.create(), createRealmEvent.getCreatedRealm());
                }
                if (event instanceof PostMigrationEvent)
                    KeycloakModelUtils.runJobInTransaction(factory, this::createStreamsManagementClient);
        });
    }

    //~ ====================================================================================================================================

    private void createStreamsManagementClient(KeycloakSession session) {
        session.realms()
                .getRealmsStream()
                .filter(realm -> !realm.getMode().equals("bo"))
                .forEach(realm -> createStreamsManagementClient(session, realm));
    }

    //~ ====================================================================================================================================

    private void createStreamsManagementClient(KeycloakSession session, RealmModel realm) {
        ClientModel streamClient = realm.getClientByClientId(realm.getName());
        if (streamClient == null) {
            streamClient = KeycloakModelUtils.createClient(realm, realm.getName());
            streamClient.setName(realm.getName());
            streamClient.setProtocol(SAML);
            streamClient.setClientId(realm.getName());
            streamClient.setEnabled(true);
            streamClient.setPublicClient(true);
            streamClient.setStandardFlowEnabled(true);
            streamClient.setFrontchannelLogout(true);
            streamClient.addRedirectUri("*");
            streamClient.addWebOrigin("*");
            streamClient.setAttribute("aud", realm.getName());
            streamClient.setAttribute("isInBuilt", "true");
            streamClient.setAttribute("clientType", API_PROVIDER_ID);
            streamClient.setAttribute(SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME, realm.getName());

            SamlClient samlClient = new SamlClient(streamClient);
            samlClient.setIncludeAuthnStatement(true);
            samlClient.setRequiresRealmSignature(true);
            samlClient.setRequiresAssertionSignature(true);
            samlClient.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
            samlClient.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
            samlClient.setNameIDFormat("username");
            samlClient.setForcePostBinding(true);

            streamClient.addProtocolMapper(setupEmailMapper());
            streamClient.addProtocolMapper(setupRealmMapper(realm));

            if (!streamClient.getClientScopes(true).containsKey("role_list"))
                DefaultClientScopes.createDefaultClientScopes(session, realm, false);
        }
    }

    //~ ====================================================================================================================================

    private ProtocolMapperModel setupDefaultMapper(String name, String protocolMapperId, Map<String, String> config) {
        ProtocolMapperModel streamsMapperModel = new ProtocolMapperModel();
        streamsMapperModel.setName(name);
        streamsMapperModel.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        streamsMapperModel.setProtocolMapper(protocolMapperId);
        streamsMapperModel.setConfig(config);
        return streamsMapperModel;
    }

    //~ ====================================================================================================================================

    private ProtocolMapperModel setupEmailMapper() {
        String emailMapperName = "email";
        Map<String, String> config = new HashMap<>();
        config.put(UserAttributeMapper.ATTRIBUTE_NAME, emailMapperName);
        config.put(AttributeStatementHelper.FRIENDLY_NAME, emailMapperName);
        config.put(UserAttributeMapper.USER_ATTRIBUTE, emailMapperName);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC);
        return setupDefaultMapper(emailMapperName, UserPropertyAttributeStatementMapper.PROVIDER_ID, config);
    }

    //~ ====================================================================================================================================

    private ProtocolMapperModel setupRealmMapper(RealmModel realm) {
        Map<String, String> config = new HashMap<>();
        config.put(HardcodedAttributeMapper.ATTRIBUTE_VALUE, realm.getName());
        config.put(AttributeStatementHelper.FRIENDLY_NAME, realm.getName());
        config.put(UserAttributeMapper.ATTRIBUTE_NAME, realm.getName());
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC);
        return setupDefaultMapper("workspace", HardcodedAttributeMapper.PROVIDER_ID, config);
    }

    //~ ====================================================================================================================================

    @Override
    public void close() {
        // No implementation required
    }

    //~ ====================================================================================================================================

    @Override
    public String getId() {
        return API_PROVIDER_ID;
    }
}
