package io.ezto.license.job;

import io.ezto.helpers.restPayload.RestPayload;
import io.ezto.bg.core.IJobProvider;
import io.ezto.helpers.GeneralHelper;
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


public class RestJobProvider implements IJobProvider {
    private static final String JOB_ATTRIBUTES = "job_attributes";
    private static final String PAYLOAD = "payload";

    Logger log = new JobRunrDashboardLogger(LoggerFactory.getLogger(RestJobProvider.class));

    @Override
    public String getProviderId() {
        return "rest";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        HttpUtility httpUtility = new HttpUtility();
        Map<String, String> jobAttributes = (Map<String, String>) GeneralHelper.getValueFromConfig(taskRequest, JOB_ATTRIBUTES);
        Object payload = getValueFromConfig(taskRequest, PAYLOAD);
        RestPayload licenseUsagePayload = JsonHelper.getInstance().getObjectMapper().convertValue(payload, RestPayload.class);

        int retry = licenseUsagePayload.getRetry().containsKey("count") ? (int) licenseUsagePayload.getRetry().get("count").longValue() : DEFAULT_RETRIES;
        long delay = licenseUsagePayload.getRetry().containsKey("delay") ? licenseUsagePayload.getRetry().get("delay").longValue() : DEFAULT_WAIT_TIME_IN_MILLI;

        Response response = httpUtility.callRequest(licenseUsagePayload.getMediaType(), licenseUsagePayload.getMethod(), jobAttributes.get("url"), licenseUsagePayload.getBody() != null ? JsonHelper.getInstance().getStringified(licenseUsagePayload.getBody()) : null, licenseUsagePayload.getAuth() != null ? licenseUsagePayload.getAuth() : null, licenseUsagePayload.getHeaders() != null ? licenseUsagePayload.getHeaders() : null, retry, delay);

        if (response != null && response.body() != null) {
            log.info("-------Rest Job response-------");
            log.info(response.toString());
            log.info("-------Rest Job response-------");
        }
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        List<String> errors = new ArrayList<>();
        Map<String, String> jobAttributes = (Map<String, String>) GeneralHelper.getValueFromConfig(taskRequest, JOB_ATTRIBUTES);
        if (jobAttributes == null || jobAttributes.size() == 0) {
            errors.add(JOB_ATTRIBUTES + " are mandatory");
            return errors;
        }
        Object payload = getValueFromConfig(taskRequest, PAYLOAD);
        if (payload == null) {
            errors.add(PAYLOAD + " are mandatory");
            return errors;
        }

        RestPayload licenseUsagePayload = JsonHelper.getInstance().getObjectMapper().convertValue(payload, RestPayload.class);
        if (licenseUsagePayload == null) {
            errors.add("invalid payload");
            return errors;
        }

        return errors;
    }
}
