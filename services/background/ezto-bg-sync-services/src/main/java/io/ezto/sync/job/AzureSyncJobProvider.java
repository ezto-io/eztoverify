package io.ezto.sync.job;

import io.ezto.bg.core.IJobProvider;
import io.ezto.helpers.JsonHelper;
import io.ezto.sync.utility.UserSyncResult;
import org.jobrunr.jobs.context.JobContext;
import io.ezto.sync.syncRunner.AzureSyncRunner;
import io.ezto.sync.utility.UserSyncHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AzureSyncJobProvider implements IJobProvider {
    @Override
    public String getProviderId() {
        return "azure-sync";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        try{
            Map credentials = JsonHelper.getInstance().getObjectMapper().readValue(taskRequest.get("credentials").toString(), Map.class);
            String accessToken = (String) credentials.get("access_token");
            UserSyncResult groups = AzureSyncRunner.syncFromAzureWS(accessToken, (String) taskRequest.get("realmId"), (String) taskRequest.get("representation"), getProviderId(), (String) taskRequest.get("userId"));
            UserSyncHelper.sendSyncResult(groups,
                    (String) taskRequest.get("actionToken")
            );
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        List<String> errors = new ArrayList<>();
        if(taskRequest.get("provider") == null){
            errors.add("Provider Id is mandatory");
            return errors;
        }

        if(taskRequest.get("credentials") == null){
            errors.add("Credentials is mandatory");
            return errors;
        }

        if(taskRequest.get("realmId") == null){
            errors.add("Realm Id is missing");
            return errors;
        }

        if(taskRequest.get("representation") == null){
            errors.add("Representation is missing");
            return errors;
        }
        return errors;
    }
}
