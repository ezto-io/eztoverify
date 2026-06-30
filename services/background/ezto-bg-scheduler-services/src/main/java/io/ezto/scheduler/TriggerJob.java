package io.ezto.scheduler;

import io.ezto.bg.core.IJobProvider;
import io.ezto.helpers.restPayload.RestPayload;
import io.ezto.helpers.JsonHelper;
import io.ezto.helpers.utility.HttpUtility;
import okhttp3.Response;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ezto.helpers.GeneralHelper.getValueFromConfig;
import static io.ezto.helpers.RetryStartegyHelper.*;

public class TriggerJob implements IJobProvider {


    private static final String PAYLOAD = "payload";
    private static final String TRIGGER_URL = "TRIGGER_URL";

    Logger log = new JobRunrDashboardLogger(LoggerFactory.getLogger(TriggerJob.class));
    @Override
    public String getProviderId() {
        return "trigger-job";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        HttpUtility httpUtility = new HttpUtility();
        Object payload = getValueFromConfig(taskRequest, PAYLOAD);
        RestPayload restPayload = JsonHelper.getInstance().getObjectMapper().convertValue(payload, RestPayload.class);

        try (Response response = httpUtility.callRequest(
                restPayload.getMediaType(),
                restPayload.getMethod(), 
                restPayload.getUrl(),
                restPayload.getBody() != null ? JsonHelper.getInstance().getStringified(restPayload.getBody()): null,
                restPayload.getAuth() != null ? restPayload.getAuth() : null,
                restPayload.getHeaders() != null ? restPayload.getHeaders() : null,
                DEFAULT_RETRIES,
                DEFAULT_WAIT_TIME_IN_MILLI)) {

            if (response != null && response.body() != null) {
                log.info("-------Rest Job response-------");
                log.info(response.toString());
                log.info("-------Rest Job response-------");
            }
        }
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        List<String> errors = new ArrayList<>();
        Object payload = getValueFromConfig(taskRequest, PAYLOAD);
        RestPayload restPayload = JsonHelper.getInstance().getObjectMapper().convertValue(payload, RestPayload.class);
        if (restPayload.getUrl() == null) {
            errors.add(TRIGGER_URL + " is mandatory");
            return errors;
        }

        return errors;
    }
}
