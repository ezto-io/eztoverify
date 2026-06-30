package com.qlikverify.utils;

import org.keycloak.models.MgmtAccessModel;
import org.keycloak.models.RealmModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AccessKeyManager {

    public static Map<ExpirationStatus, List<MgmtAccessModel>> groupAccessKeysByExpirationStatus(RealmModel realm, List<MgmtAccessModel> accessKeys) {
        return accessKeys.stream()
                .filter(MgmtAccessModel::isEnabled)
                .collect(Collectors.groupingBy(AccessKeyManager::getExpirationStatus))
                .entrySet().stream()
                .filter(expirationStatusListEntry -> expirationStatusListEntry.getKey() != ExpirationStatus.VALID)
                .peek(entry -> {
                    if (entry.getKey() == ExpirationStatus.EXPIRED) {
                        entry.getValue().forEach(accessKeyModel -> {
                            accessKeyModel.setEnabled(false);
                            realm.updateMgmtAccess(accessKeyModel);
                        });
                    }
                 })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    private static ExpirationStatus getExpirationStatus(MgmtAccessModel accessKey) {
        long expiryEpochMilliSeconds = accessKey.getExpiryDate();
        long currentTime = System.currentTimeMillis();

        if (expiryEpochMilliSeconds <= currentTime) {
            return ExpirationStatus.EXPIRED;
        } else if (checkIfExpiresInOneWeek(expiryEpochMilliSeconds)) {
            return ExpirationStatus.EXPIRES_IN_ONE_WEEK;
        }
        return ExpirationStatus.VALID;
    }

    private static boolean checkIfExpiresInOneWeek(long expiryEpochMilliSeconds) {
        long currentTime = System.currentTimeMillis();
        long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000;

        return (expiryEpochMilliSeconds - currentTime) <= oneWeekInMillis;
    }

    public static List<Map<String, Object>> getSimplifiedExpiryDetails(List<MgmtAccessModel> accessModels) {
        return accessModels.stream()
                .map(model -> {
                    Map<String, Object> accessMap = new HashMap<>();
                    long expiryInDays = TimeUnit.MILLISECONDS.toDays(model.getExpiryDate() - System.currentTimeMillis());
                    accessMap.put("name", model.getName());
                    accessMap.put("expiry", expiryInDays < 0 ? -1 : expiryInDays);
                    return accessMap;
                })
                .collect(Collectors.toList());
    }

    public enum ExpirationStatus {
        EXPIRED,
        EXPIRES_IN_ONE_WEEK,

        VALID
    }
}
