package org.keycloak.ciba;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.Map;

public interface CibaTutorialProvider extends Provider {
    String getDisplayName();
    CibaTutorialProvider setUserModel(UserModel userModel);
    String getDescription();
    Map<String, Object> getCommand(ClientModel clientModel, String actionFlow);
    boolean onExecute(Map<String, String> attributes);
    boolean hasExecution();
    int order();
}
