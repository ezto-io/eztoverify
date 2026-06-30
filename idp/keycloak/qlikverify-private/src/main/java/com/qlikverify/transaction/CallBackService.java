package com.qlikverify.transaction;

import org.keycloak.events.EventBuilder;
import org.keycloak.provider.Provider;

import java.util.Map;

public interface CallBackService extends Provider {
    CallBackService setEventBuilder(EventBuilder eventBuilder);
    void verifyResult(String token, TransactionStatus status, Map<String, String> params);
}
