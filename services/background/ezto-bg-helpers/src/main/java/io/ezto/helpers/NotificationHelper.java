package io.ezto.helpers;

import io.ezto.helpers.notification.models.HookResponseModel;
import io.ezto.helpers.notification.models.Notification;
import io.ezto.helpers.utility.HttpUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class NotificationHelper {

    HttpUtility httpUtility = new HttpUtility();

    private static final Logger LOG = LoggerFactory.getLogger(NotificationHelper.class);
    public void sendNotification(Notification body, String actionToken){
        Response response = null;
        try{
            response = httpUtility.callRequest(
                    "application/json",
                    "POST",
                    EnvironmentReader.getValue("IDP_HOST", "http://localhost:8080")+"/auth/realms/api/notifications/add",
                    new ObjectMapper().writeValueAsString(body),
                    "Bearer "+ actionToken,
                    null,
                    1,
                    10000
            );
        } catch ( Exception ex){
            LOG.error(ex.toString());
        } finally {
            if (response != null){
                response.close();
            }
        }
    }

    public void tenantDeletedNotification(Map<String, Object> request){
        Response response = null;
        try{
            response = httpUtility.callRequest(
                    "application/json",
                    "POST",
                    EnvironmentReader.getValue("IDP_HOST", "http://localhost:8080")+"/auth/realms/api/notifications/delete-tenant",
                    new ObjectMapper().writeValueAsString(request),
                    null,
                    null,
                    1,
                    10000
            );
        } catch ( Exception ex){
            LOG.error(ex.toString());
        } finally {
            if (response != null){
                response.close();
            }
        }
    }

    public void sendHookFallBackNotification(HookResponseModel body, String actionToken){
        Response response = null;
        try{
            response = httpUtility.callRequest(
                    "application/json",
                    "POST",
                    EnvironmentReader.getValue("IDP_HOST", "http://localhost:8080")+"/auth/realms/api/hook-fallback" +
                            "-notification/result",
                    new ObjectMapper().writeValueAsString(body),
                    "Bearer "+ actionToken,
                    null,
                    1,
                    10000
            );
        } catch ( Exception ex){
            LOG.error(ex.toString());
        } finally {
            if (response != null){
                response.close();
            }
        }
    }
}
