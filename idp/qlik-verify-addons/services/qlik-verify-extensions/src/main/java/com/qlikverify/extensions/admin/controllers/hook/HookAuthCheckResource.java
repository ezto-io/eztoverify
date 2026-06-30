package com.qlikverify.extensions.admin.controllers.hook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.hook.spi.HookRepresentation;
import com.qlikverify.hook.spi.HookService;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SmsConfigModel;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class         HookAuthCheckResource extends BaseAuthCheckResource {

    public HookAuthCheckResource(KeycloakSession session) {
        super(session);
    }

    @POST
    @Path("wa/trigger")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSSOApps(@Context HttpRequest req) {

        MultivaluedMap<String, String> formData = req.getDecodedFormParameters();
        String secret = formData.getFirst("From");
        if (secret == null || secret.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST).build();

        RealmModel realmModel = session.realms().getRealmByName(Config.getAdminRealm());
        session.getContext().setRealm(realmModel);

        HookRepresentation hookRepresentation = session.getProvider(HookService.class).findHook(secret);

        if (hookRepresentation == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        realmModel = session.realms().getRealm(hookRepresentation.getRealmId());

        Optional<SmsConfigModel> hasWhatsAppProvider = realmModel.getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();
        SmsConfigModel smsConfigModel;

        // Whatsapp sender dependent on ```master``` realm
        if(!hasWhatsAppProvider.isPresent()){
            Optional<SmsConfigModel> masterWhatsappConfig =
                    session.getContext().getRealm().getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();
            if (!masterWhatsappConfig.isPresent()){
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            smsConfigModel = masterWhatsappConfig.get();
        } else {
            smsConfigModel = hasWhatsAppProvider.get();
        }

        if (smsConfigModel == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (!smsConfigModel.getConfig().get("accountsid").equalsIgnoreCase(formData.getFirst("AccountSid")))
            return Response.status(Response.Status.BAD_REQUEST).build();


        try {
            session.getProvider(HookService.class).updateHook(hookRepresentation.getChatCode(),JsonSerialization.writeValueAsString(formData));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, String> sendResultAttr = new HashMap<>();
        sendResultAttr.put("secret", secret);

        sendResult(hookRepresentation.getChatCode(), sendResultAttr);


        return Response.status(Response.Status.OK).build();
    }

    private void sendResult(String chatCode, Map<String, String> payload) {
        HttpUtility httpUtility = new HttpUtility();
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                okhttp3.Response response = null;
                try {
                    response = httpUtility.callRequest(MediaType.APPLICATION_JSON, "POST", EnvironmentReader.getValue("SOCKET_URL") + "/send?key=" + chatCode, JsonSerialization.writeValueAsString(payload), null);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                response.close();
            }
        });
    }
}
