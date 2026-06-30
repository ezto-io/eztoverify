package com.qlikverify.backoffice.liveness.analytics;

import com.segment.analytics.Analytics;
import com.segment.analytics.messages.GroupMessage;
import org.json.JSONObject;
import org.keycloak.EnvironmentReader;

import java.util.*;

public class TwilioSegmentHelper {
    private static final Analytics analytics = Analytics.builder(EnvironmentReader.getValue("EZTO_JAVA_SEGMENT_WRITE_KEY")).build();

    public static void queueEvent(String requestId, String userId, Map<String, Object> traits) {
        if(traits.toString().length() > 32768) {
            sendLargePayload(requestId, userId, traits);
        } else {
            GroupMessage.Builder e = GroupMessage
                    .builder(requestId)
                    .timestamp(new Date())
                    .userId(userId)
                    .traits(Collections.unmodifiableMap(traits));
            analytics.enqueue(e);
        }
    }

    public static void sendLargePayload(String requestId, String userId, Map<String, Object> traits) {
        String payload = convertToJson(traits);
        int batchSize = 30000;
        List<String> batches = splitIntoBatches(payload, batchSize);
        for (String batch : batches) {
            queueEvent(requestId, userId, Collections.singletonMap("batch", batch));
        }
    }

    private static List<String> splitIntoBatches(String payload, int batchSize) {
        int numBatches = (payload.length() + batchSize - 1) / batchSize;
        List<String> batches = new ArrayList<>();
        for (int i = 0; i < numBatches; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, payload.length());
            batches.add(payload.substring(start, end));
        }
        return batches;
    }

    private static String convertToJson(Map<String, Object> traits) {
        return new JSONObject(traits).toString();
    }
}
