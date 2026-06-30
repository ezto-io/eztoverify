package com.qlikverify.userTransactionStatus.spi;

import org.keycloak.models.UserTransactionModel;
import org.keycloak.provider.Provider;

import java.util.List;

public interface UserTransactionStatusProvider extends Provider {
    UserTransactionModel addTransactionStatus(String realmId,String trxId, String userId, String status, Long expiresAt);
    UserTransactionModel getUserTransactionDetails(String trxId, String userId);
    void updateTransactionStatus(String realmId, String trxId, String userId, String status);
    List<UserTransactionModel> getUserTransactionStatus(String realmId, String userId);
    void removeUserTransactionStatus(String realmId,String userId);
}
