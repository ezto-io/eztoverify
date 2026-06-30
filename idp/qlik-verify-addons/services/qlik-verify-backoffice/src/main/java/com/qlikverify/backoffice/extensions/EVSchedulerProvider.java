package com.qlikverify.backoffice.extensions;

import com.qlikverify.helpers.ExchangeRatesHelper;
import com.qlikverify.utils.AccessKeyManager;
import org.jboss.logging.Logger;
import org.keycloak.events.email.EmailProducer;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.MgmtAccessModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EVSchedulerProvider implements EztoPublicRealmResourceProvider {

    Logger logger = Logger.getLogger(EVSchedulerProvider.class);
    private final KeycloakSession session;

    public EVSchedulerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
        //NOP
    }

    @GET
    @Path("/validate-management-keys")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateAllKeys() {
        session.realms().getRealmsStream().forEach(realm -> {
            Map<AccessKeyManager.ExpirationStatus, List<MgmtAccessModel>> accessKeysBasedOnExpirationStatus =
                    AccessKeyManager.groupAccessKeysByExpirationStatus(realm, realm.getMgmtAccessKeys().collect(Collectors.toList()));

            Optional<UserModel> workspaceOwner = session.users()
                    .getUsersStream(realm, false)
                    .filter(UserModel::isAccountCreator)
                    .findFirst();
            if (workspaceOwner.isEmpty()) {
                logger.error("Workspace owner not found for workspace:: " + realm.getName());
                return;
            }

            accessKeysBasedOnExpirationStatus.forEach((expirationStatus, mgmtAccessModels) -> {
                Map<String, Object> representation = new HashMap<>();
                representation.put("workspace", realm.getName());
                representation.put("keysCount", (long) mgmtAccessModels.size());
                representation.put("keys", AccessKeyManager.getSimplifiedExpiryDetails(mgmtAccessModels));
                representation.put("toEmail", workspaceOwner.get().getEmail());
                representation.put("user_name", workspaceOwner.get().getUsername());

                EmailProducer.sendWarningEmailForAccessKeys(session, realm, representation, expirationStatus);
            });
        });

        return Response.status(Response.Status.OK).entity("").build();
    }


    @GET
    @Path("/update-exchange-rates")
    public Response updateExchangeRates() {
        ExchangeRatesHelper.getUpdatedExchangeRates(session);
        return Response.status(Response.Status.OK).entity("").build();
    }
}
