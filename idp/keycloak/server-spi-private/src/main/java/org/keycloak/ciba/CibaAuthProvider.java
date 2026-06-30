package org.keycloak.ciba;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.provider.Provider;

import java.util.Map;

public interface CibaAuthProvider extends Provider {

    CibaAuthProvider setEventBuilder(EventBuilder eventBuilder);

    boolean onAuthorize(ClientModel clientModel,RealmModel realm, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, AuthenticationChannelRequest request, CibaStateModel cibaStateModel);

    void onAuthCompleted(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, String callbackUrl, CibaStateModel cibaState, EventBuilder event, Map<String,Object> response);

    void onAuthFailed(ClientModel clientModel, UserModel userModel, String callbackUrl, CibaStateModel cibaState, EventBuilder event, Map<String,Object> response);

    boolean validateUserDetails(UserModel user, RealmModel realm, CibaAuthConfigModel cibaAuthConfigModel, String requestId);

    default boolean isMobile(){
        return false;
    }

    default boolean isDesktop(){
        return false;
    }

    String getStep1(ClientModel clientModel, UserModel userModel,CibaAuthConfigModel cibaAuthConfigModel, String actionFlow);

    String getStep2(ClientModel clientModel, UserModel userModel,CibaAuthConfigModel cibaAuthConfigModel);
}
