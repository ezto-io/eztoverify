package io.ezto.license.job;

import io.ezto.bg.core.IJobProvider;
import io.ezto.helpers.EnvironmentReader;
import io.ezto.helpers.utility.HttpUtility;
import okhttp3.Response;
import org.jobrunr.jobs.context.JobContext;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IamHardSyncJobProvider implements IJobProvider {
    HttpUtility httpUtility = new HttpUtility();

    @Override
    public String getProviderId() {
        return "hard-sync";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        try (Response response = httpUtility.callRequest("application/json",
                "GET",
                EnvironmentReader.getValue("IDP_HOST", "http://localhost:8080") + "/auth/realms/api/qv_subscription/subscription/" + taskRequest.get("tenantId"),
                null,
                null)){
            System.out.println("hard sync api triggered");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        List<String> errors = new ArrayList<>();
        if(!taskRequest.containsKey("tenantId")){
            errors.add("tenant-id is mandatory");
            return errors;
        }
        if(!StringUtils.hasText(taskRequest.get("tenantId").toString())) {
            errors.add("tenant id cant be empty");
            return errors;
        }
        return new ArrayList<>();
    }
}
