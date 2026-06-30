package io.ezto.license.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ezto.bg.core.IJobProvider;
import io.ezto.license.job.models.FreshDeskPayload;
import io.ezto.helpers.JsonHelper;
import okhttp3.*;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.context.JobContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ezto.helpers.GeneralHelper.getValueFromConfig;


public class FreshDeskJobProvider implements IJobProvider {
    private static final String JOB_ATTRIBUTES = "job_attributes";
    private static final String PAYLOAD = "payload";
    private static final String URL = "url";
    protected static final org.jboss.logging.Logger logger = Logger.getLogger(FreshDeskJobProvider.class);
    @Override
    public String getProviderId() {
        return "freshDesk";
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        List<String> errors = new ArrayList<>();
        Map<String, String> jobAttributes = (Map<String, String>) getValueFromConfig(taskRequest, JOB_ATTRIBUTES);
        if (jobAttributes == null || jobAttributes.size() == 0) {
            errors.add(JOB_ATTRIBUTES+" are mandatory");
            return errors;
        }

        if(!jobAttributes.containsKey(URL))
        {
            errors.add(URL+" is mandatory");
            return errors;
        }

        String freshDeskUrl = jobAttributes.get(URL);
        if(freshDeskUrl == null ||  freshDeskUrl.isEmpty())
        {
            errors.add(freshDeskUrl+" is mandatory");
            return errors;
        }

        Object payload = getValueFromConfig(taskRequest, PAYLOAD);
        if (payload == null) {
            errors.add(PAYLOAD+" are mandatory");
            return errors;
        }

        FreshDeskPayload freshDeskPayload = JsonHelper.getInstance().getObjectMapper().convertValue(payload, FreshDeskPayload.class);
        if (freshDeskPayload == null) {
            errors.add("invalid payload");
            return errors;
        }

        if( (freshDeskPayload.getAuth() == null || freshDeskPayload.getAuth().isEmpty()) || (freshDeskPayload.getMethod() == null || freshDeskPayload.getMethod().isEmpty()) || (freshDeskPayload.getMediatype() == null || freshDeskPayload.getMediatype().isEmpty()) ) {
            errors.add("Request Headers are mandatory");
            return errors;
        }

        return errors;
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        Map<String, String> providerAttributes = (Map<String, String>) getValueFromConfig(taskRequest, JOB_ATTRIBUTES);
        Object payload = getValueFromConfig(taskRequest, PAYLOAD);
        FreshDeskPayload freshDeskPayload = JsonHelper.getInstance().getObjectMapper().convertValue(payload, FreshDeskPayload.class);

        try {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, new ObjectMapper().writeValueAsString(freshDeskPayload.getBody()));
        Request request = new Request.Builder()
                .url(providerAttributes.get(URL))
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", freshDeskPayload.getAuth())
                .build();



            Response response = client.newCall(request).execute();
            int statusCode = response.code();

            if (statusCode == 201) {
                System.out.println("Notification sent successfully.");
            } else {
                System.out.println("Notification failed with status code: " + statusCode);
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
