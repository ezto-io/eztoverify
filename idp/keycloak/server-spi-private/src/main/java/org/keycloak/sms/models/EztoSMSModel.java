package org.keycloak.sms.models;


public class EztoSMSModel {
    private final boolean messageSent;
    private final String messageId;

    public EztoSMSModel(boolean messageSent, String messageId) {
        this.messageSent = messageSent;
        this.messageId = messageId;
    }

    public boolean isMessageSent() {
        return messageSent;
    }

    public String getMessageId() {
        return messageId;
    }
}
