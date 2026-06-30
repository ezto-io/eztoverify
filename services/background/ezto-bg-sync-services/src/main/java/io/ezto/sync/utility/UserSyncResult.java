package io.ezto.sync.utility;

public class UserSyncResult {
    public final String syncResult;
    public final String realmId;
    public final String representation;
    public final String providerId;
    public final String userId;

    public UserSyncResult(String syncResult, String realmId, String representation, String providerId, String userId) {
        this.syncResult = syncResult;
        this.realmId = realmId;
        this.representation = representation;
        this.providerId = providerId;
        this.userId = userId;
    }
}
