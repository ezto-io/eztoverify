package org.keycloak.downloads;

import org.keycloak.models.ClientModel;
import org.keycloak.provider.Provider;

import java.util.List;

public interface ConfigDownloadProvider extends Provider {

    String getDownloadFile(ClientModel clientModel);

    String getFileName();
}
