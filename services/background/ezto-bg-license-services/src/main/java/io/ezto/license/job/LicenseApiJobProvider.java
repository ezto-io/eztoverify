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
import java.util.concurrent.locks.ReentrantLock;

public class LicenseApiJobProvider  implements IJobProvider {
    HttpUtility httpUtility = new HttpUtility();
    private final ReentrantLock updateLock = new ReentrantLock(true);
    @Override
    public String getProviderId() {
        return "license-api";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        updateLock.lock();
        try {
            System.out.println("------started");
            try (Response response = httpUtility.callRequest("application/json",
                    taskRequest.get("method").toString(),
                    EnvironmentReader.getValue("LICENSE_URL", "http://localhost:8090") + "/" + taskRequest.get("url"),
                    taskRequest.get("body").toString(),
                    null)) {
//            System.out.println("hard sync api triggered");
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("ended------------");
        }finally {
            updateLock.unlock();
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
        if(!taskRequest.containsKey("method")){
            errors.add("api method mandatory");
            return errors;
        }
        if(!taskRequest.containsKey("body")){
            errors.add("body is mandatory");
            return errors;
        }
        if(!taskRequest.containsKey("url")){
            errors.add("url is mandatory");
            return errors;
        }
        return new ArrayList<>();
    }
}
