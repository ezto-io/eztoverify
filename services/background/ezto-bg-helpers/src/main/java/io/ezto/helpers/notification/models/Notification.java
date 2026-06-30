package io.ezto.helpers.notification.models;

import io.ezto.helpers.notification.enums.NotificationType;

public class Notification {
    public final String message;
    public final int statusCode;
    public final String realmId;
    public final String email;

    public final NotificationType notificationType;
    public final String isAdminEvent;
    public final String clientName;


    public Notification(int statusCode,String message,String realmId,String email, NotificationType notificationType, String isAdminEvent, String clientName) {
        this.message = message;
        this.statusCode = statusCode;
        this.notificationType = notificationType;
        this.realmId = realmId;
        this.email = email;
        this.isAdminEvent = isAdminEvent;
        this.clientName = clientName;
    }
}
