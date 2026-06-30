package com.qlikverify.services.backgroundService.backgroundSyncService;

import com.qlikverify.services.backgroundService.BackgroundJobRequestBody;

public class BackgroundSyncRequestBody implements BackgroundJobRequestBody {
    public final String provider;
    public final String actionToken;
    public final String credentials;
    public final String realmId;
    public final String userId;
    public final String representation;
    public BackgroundSyncRequestBody(String provider, String credentials, String realmId, String representation, String userId, String actionToken){
        this.provider = provider;
        this.credentials = credentials;
        this.actionToken = actionToken;
        this.realmId = realmId;
        this.representation = representation;
        this.userId = userId;
    }

    public String getRepresentation() {
        return representation;
    }
    public String getRealmId() {
        return realmId;
    }
}
