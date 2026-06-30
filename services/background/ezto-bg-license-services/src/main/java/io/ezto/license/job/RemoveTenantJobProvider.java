package io.ezto.license.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ezto.license.job.models.RemoveLicenseRequestModel;
import io.ezto.bg.core.IJobProvider;
import io.ezto.helpers.EnvironmentReader;
import io.ezto.helpers.GeneralHelper;
import io.ezto.helpers.NotificationHelper;
import io.ezto.helpers.notification.enums.NotificationType;
import io.ezto.helpers.notification.models.Notification;
import io.ezto.helpers.utility.HttpUtility;
import okhttp3.Response;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoveTenantJobProvider implements IJobProvider {
    private static final String TENANT_ID = "tenant_id";
    private static final String DELETION_TIME = "deletion_time";
    private static final String CALLBACK_URL = "callback_url";
    private static final String ACTION_TOKEN = "actionToken";
    private static final Logger logger = LoggerFactory.getLogger(RemoveTenantJobProvider.class);
    private static NotificationHelper notificationHelper = new NotificationHelper();

    @Override
    public String getProviderId() {
        return "remove_tenant";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        logger.info("RemoveTenantJobProvider :: doExecute");
        if(taskRequest.get(CALLBACK_URL) == null)
            return;
        if(taskRequest.get(TENANT_ID) == null)
            return;
        if(taskRequest.get(ACTION_TOKEN) == null)
            return;

        String actionToken = taskRequest.get(ACTION_TOKEN).toString();

        String callbackUrl = taskRequest.get(CALLBACK_URL).toString();
        HttpUtility httpUtility = new HttpUtility();
        RemoveLicenseRequestModel removeLicenseRequestModel = new RemoveLicenseRequestModel();
        removeLicenseRequestModel.setTenantId(taskRequest.get(TENANT_ID).toString());
        removeLicenseRequestModel.setMarkedToDelete(true);
        removeLicenseRequestModel.setHardDelete(true);
        removeLicenseRequestModel.setActionToken(actionToken);

        try {
            Map<String, String> contentType = new HashMap<>();
            contentType.put("Content-Type", "application/json");
            Response response = httpUtility.callRequest(
                    "application/json",
                    "DELETE",
                    EnvironmentReader.getValue("LICENSE_URL") + callbackUrl,
                    new ObjectMapper().writeValueAsString(removeLicenseRequestModel),
                    null,
                    contentType,
                    1,
                    10000
            );
            if(response.isSuccessful()){
                notificationHelper.sendNotification(new Notification(200, "", taskRequest.get(TENANT_ID).toString(), "", NotificationType.SOFT_DELETE_SUCCESS, "true", ""), actionToken);
                taskRequest.remove(CALLBACK_URL);
                taskRequest.remove(DELETION_TIME);
                NotificationHelper notificationHelper = new NotificationHelper();
                notificationHelper.tenantDeletedNotification(taskRequest);
            }
            notificationHelper.sendNotification(new Notification(response.code(), response.body().string(), taskRequest.get(TENANT_ID).toString(), "", NotificationType.SOFT_DELETE_SUCCESS, "true", ""), actionToken);
        } catch (JsonProcessingException e) {
            notificationHelper.sendNotification(new Notification(400, "Failed to process JSON", taskRequest.get(TENANT_ID).toString(), "", NotificationType.SOFT_DELETE_SUCCESS, "true", ""), actionToken);
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        logger.info("RemoveTenantJobProvider :: validateRequest");
        // validate the request sent from license manager
        List<String> errors = new ArrayList<>();

        if(taskRequest.isEmpty()){
            errors.add("Request Body missing");
            return errors;
        }

        String tenantId = GeneralHelper.getValueFromConfig(taskRequest, TENANT_ID).toString();
        if(StringUtils.isNullOrEmpty(tenantId)){
            errors.add("Tenant ID can't be null");
            return errors;
        }

        String deletionTime = GeneralHelper.getValueFromConfig(taskRequest, DELETION_TIME).toString();
        if(StringUtils.isNullOrEmpty(deletionTime)){
            errors.add("Deletion time is required to schedule deletion.");
            return errors;
        }

        String callBackUrl = GeneralHelper.getValueFromConfig(taskRequest, CALLBACK_URL).toString();
        if(StringUtils.isNullOrEmpty(callBackUrl)){
            errors.add("Callback URL is missing");
            return errors;
        }

        String actionToken = GeneralHelper.getValueFromConfig(taskRequest, ACTION_TOKEN).toString();
        if(StringUtils.isNullOrEmpty(actionToken)){
            errors.add("Action Token missing");
            return errors;
        }

        return errors;
    }
}
