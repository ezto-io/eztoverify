package com.qlikverify.services.backgroundService.backgroundSyncService;

import com.qlikverify.services.backgroundService.BackgroundJobRequestBody;

public class BackgroundSyncCancelRequestBody implements BackgroundJobRequestBody {
    public final String realmId;
    public final String actionToken;
    public final String connectionId;
    public final String jobId;

    public BackgroundSyncCancelRequestBody(String realmId, String connectionId, String jobId, String actionToken) {
        this.realmId = realmId;
        this.actionToken = actionToken;
        this.connectionId = connectionId;
        this.jobId = jobId;
    }
}
