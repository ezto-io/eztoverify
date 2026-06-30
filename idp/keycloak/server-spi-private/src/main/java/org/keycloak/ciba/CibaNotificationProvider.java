package org.keycloak.ciba;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.provider.Provider;

import java.util.Map;

public interface CibaNotificationProvider extends Provider {

    CibaNotificationProvider setEventBuilder(EventBuilder eventBuilder);

    void sendNotification(KeycloakSession session, RealmModel realm, ClientModel client, CibaConfig cibaConfig, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig, HttpRequest httpRequest, Map<String, String> attrs);

    void sendError(KeycloakSession session, RealmModel realm, ClientModel client, CibaConfig cibaConfig, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig, HttpRequest httpRequest, Map<String, String> attrs);

    Map<String, Object> getResult(ClientModel client, CibaNotificationConfigModel notificationConfig, Map<String, Object> attrs);

    Object getStep3(ClientModel clientModel, UserModel userModel, CibaNotificationConfigModel notificationConfig);
}
