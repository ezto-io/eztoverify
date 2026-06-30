package io.ezto.hook;

import io.ezto.bg.core.IJobProvider;
import io.ezto.helpers.JsonHelper;
import io.ezto.helpers.NotificationHelper;
import io.ezto.helpers.notification.models.HookResponseModel;
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


public class HookJobProvider implements IJobProvider {

    private static final String MODE = "mode";
    private static final String NOTIFICATION_ENDPOINT = "notification_endpoint";
    private static final String NOTIFICATION_ENDPOINT_AUTH = "notification_endpoint_auth";
    private static final String NOTIFICATION_BODY = "notification_body";
    private static final String FALLBACK_DETAILS = "fallback_details";
    private static final List<String> DELIVERY_MODES = new ArrayList();
    private static final String ACTION_TOKEN = "actionToken";

    static{
        DELIVERY_MODES.add("push");
        DELIVERY_MODES.add("ping");
    }
    Logger log = new JobRunrDashboardLogger(LoggerFactory.getLogger(HookJobProvider.class));

    @Override
    public String getProviderId() {
        return "hook-notifier";
    }
    private final NotificationHelper notificationHelper = new NotificationHelper();

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        HttpUtility httpUtility = new HttpUtility();

        String auth = (String) getValueFromConfig(taskRequest, NOTIFICATION_ENDPOINT_AUTH);
        String url = (String) getValueFromConfig(taskRequest, NOTIFICATION_ENDPOINT);
        String fallbackDetails = (String) getValueFromConfig(taskRequest,FALLBACK_DETAILS);

        final Map<String, Object> data = JsonHelper.getInstance().getData(fallbackDetails);
        Response response = null;
        HookResponseModel hookResponseModel = null;
        final Object clientId = JsonHelper.getInstance().getData(fallbackDetails).get("clientId");
        final Object tenantId = JsonHelper.getInstance().getData(fallbackDetails).get("tenantId");
        final Object actionToken = JsonHelper.getInstance().getData(fallbackDetails).get("actionToken");

        try{
            String body = (String) getValueFromConfig(taskRequest, NOTIFICATION_BODY);
//            String sanitizedBody = JsonHelper.getInstance().getObjectMapper().convertValue(body, JSONObject.class).toString();
            response = httpUtility.callRequest("application/json", "POST", url, body,
                    auth == null ? null :
                            auth, null, 1, 1000);
            if (response != null && response.body() != null) {
                log.info("-------Hook Notifier Job response-------");
                log.info(response.toString());
                log.info("-------Hook Notifier Job response-------");
                hookResponseModel = HookResponseModel.builder().
                        code(response.code())
                        .success(response.code() == 200)
                        .message(response.message())
                        .clientId(clientId == null ? "" : clientId.toString())
                        .realmId(tenantId.toString())
                        .build();
            } else {
                hookResponseModel =
                        hookResponseModel.builder().success(false).code(0)
                                .message("Unknown Error").clientId(clientId == null ?
                                        "" :
                                clientId.toString())
                                .realmId(tenantId.toString()).build();
            }
        } catch (Exception e) {
            hookResponseModel =
                    hookResponseModel.builder().success(false)
                            .code(500)
                            .message(e.getMessage())
                            .clientId(clientId == null ? "" :
                            clientId.toString())
                    .realmId(tenantId.toString()).build();
        } finally {
            HookResponseModel hookResponse = null;
            if(taskRequest.containsKey("triggeredFor") && taskRequest.get("triggeredFor").equals("deadLetter")){
                hookResponse = HookResponseModel.builder()
                        .success(hookResponseModel.isSuccess())
                        .code(hookResponseModel.getCode())
                        .message("deadLetterHookFailure")
                        .clientId(hookResponseModel.getClientId())
                        .realmId(hookResponseModel.getRealmId())
                        .build();
            }
            notificationHelper.sendHookFallBackNotification(hookResponse == null ? hookResponseModel : hookResponse, actionToken.toString());
            if (response != null && response.isSuccessful()) response.close();
        }

    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        List<String> errors = new ArrayList<>();
        String mode = (String) getValueFromConfig(taskRequest, MODE);
        if (mode == null || mode.isEmpty()){
            errors.add("Delivery mode is mandatory");
        }
        if (!DELIVERY_MODES.contains(mode)){
            errors.add("Unsupported delivery mode");
        }

        String notficationEndpoint = (String) getValueFromConfig(taskRequest, NOTIFICATION_ENDPOINT);
        if (notficationEndpoint == null || notficationEndpoint.isEmpty()){
            errors.add("Notification endpoint is mandatory");
        }

        String notficationBody = (String) getValueFromConfig(taskRequest, NOTIFICATION_BODY);
        if (notficationBody == null){
            errors.add("Notification body is mandatory");
        }

        return errors;
    }
}
