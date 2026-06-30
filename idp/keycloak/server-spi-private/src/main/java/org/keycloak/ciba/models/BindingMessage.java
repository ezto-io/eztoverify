
package org.keycloak.ciba.models;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.AESHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class BindingMessage {

    private Map<String, String> metadata;
    private Consent consent;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Consent getConsent() {
        return consent;
    }

    public void setConsent(Consent data) {
        this.consent = data;
    }

    public static BindingMessage decode(String rawData, ClientModel clientModel) throws Exception {
        return JsonSerialization.readValue(AESHelper.decrypt(new String(Base64.getDecoder().decode(rawData.getBytes(StandardCharsets.UTF_8))),clientModel.getSecret().substring(0,32)), BindingMessage.class);
    }

    public static String encode(BindingMessage message, ClientModel clientModel) throws Exception {
        return Base64.getEncoder().encodeToString(AESHelper.encrypt(JsonSerialization.writeValueAsString(message),clientModel.getSecret().substring(0,32)).getBytes(StandardCharsets.UTF_8));
    }

    public static BindingMessage newMessage(RealmModel realmModel){
        BindingMessage bindingMessage = new BindingMessage();
        Consent data1 = new Consent();
        data1.setTitle("Request from "+realmModel.getDisplayName());
        data1.setType("string");
        data1.setMessage(realmModel.getDisplayName()+" has request to update details. If its not intended for you, please cancel the request.");
        bindingMessage.setConsent(data1);
        return bindingMessage;
    }

}
