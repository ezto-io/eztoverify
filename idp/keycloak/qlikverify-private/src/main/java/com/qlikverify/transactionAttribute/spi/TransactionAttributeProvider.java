package com.qlikverify.transactionAttribute.spi;

import org.keycloak.models.TrxModel;
import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;

public interface TransactionAttributeProvider extends Provider {
    TrxModel addAttribute(String trxId, String name, String value);
    void addAttributes(String trxId, Map<String, String> claims);
    void removeAttributes(String trxId, List<String> claimsToBeRemoved);

    void removeTrxAttribute(String trxId,String name);
    void removeTrx(String trxId);
    TrxModel getTrx(String trxId);
    String getTrxValue(String trxId, String name);
    boolean hasTrxValueAttribute(String trxId, String name);
    boolean hasTrxValueAttribute(String trxId, String name,String expectedValue);
}
