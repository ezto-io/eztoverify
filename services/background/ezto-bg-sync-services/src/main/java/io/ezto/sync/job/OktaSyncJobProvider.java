package io.ezto.sync.job;

import io.ezto.bg.core.IJobProvider;
import io.ezto.helpers.JsonHelper;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.ezto.sync.syncRunner.OktaWsSyncRunner;
import io.ezto.sync.utility.UserSyncHelper;
import io.ezto.sync.utility.UserSyncResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OktaSyncJobProvider implements IJobProvider {
    Logger log = new JobRunrDashboardLogger(LoggerFactory.getLogger(OktaSyncJobProvider.class));
    @Override
    public String getProviderId() {
        return "okta-ws-sync";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {

        try{
            Map credentials = JsonHelper.getInstance().getObjectMapper().readValue(taskRequest.get("credentials").toString(), Map.class);
            String domainName = (String) credentials.get("domainName");
            String apiToken = (String) credentials.get("apiToken");
            UserSyncResult groups = OktaWsSyncRunner.syncFromOktaWS(domainName, apiToken, (String) taskRequest.get("realmId"), (String) taskRequest.get("representation"), getProviderId(), (String) taskRequest.get("userId"));
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
