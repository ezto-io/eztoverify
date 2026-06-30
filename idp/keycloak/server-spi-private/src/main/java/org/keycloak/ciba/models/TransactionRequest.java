package org.keycloak.ciba.models;

import org.keycloak.models.RealmModel;

import java.util.HashMap;

public class TransactionRequest {
    private BindingMessage message;
    private UserData user;
    private String requested_verification;
    private Object metadata;

    public BindingMessage getMessage() {
        return message;
    }

    public void setMessage(BindingMessage message) {
        this.message = message;
    }

    public UserData getUser() {
        return user;
    }

    public void setUser(UserData user) {
        this.user = user;
    }

    public String getRequested_verification() {
        return requested_verification;
    }

    public void setRequested_verification(String requested_verification) {
        this.requested_verification = requested_verification;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    public static TransactionRequest newMessage(RealmModel realmModel){
        TransactionRequest request = new TransactionRequest();
        BindingMessage bindingMessage = new BindingMessage();
        Consent data1 = new Consent();
        data1.setTitle("Request from "+realmModel.getDisplayName());
        data1.setType("string");
        data1.setMessage(realmModel.getDisplayName()+" has request to update details. If its not intended for you, please cancel the request.");
        bindingMessage.setConsent(data1);
        request.setMessage(bindingMessage);
        request.setUser(new UserData());
        request.setRequested_verification("enter_verifier");
        request.setMetadata(new HashMap<>());
        return request;
    }
}
