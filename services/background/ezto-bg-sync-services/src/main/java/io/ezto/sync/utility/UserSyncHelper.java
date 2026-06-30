package io.ezto.sync.utility;

import io.ezto.helpers.EnvironmentReader;
import io.ezto.helpers.JsonHelper;
import io.ezto.helpers.utility.HttpUtility;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UserSyncHelper {
    static HttpUtility httpUtility = new HttpUtility();
    private static final Logger LOG = LoggerFactory.getLogger(UserSyncHelper.class);
        public static void sendSyncResult(UserSyncResult syncResult, String actionToken){
        Response response = null;
        try{
            response = httpUtility.callRequest(
                    "application/json",
                    "POST",
                    EnvironmentReader.getValue("IDP_HOST", "http://localhost:8080")+"/auth/realms/api/notifications/sync/result",
                    JsonHelper.getInstance().getObjectMapper().writeValueAsString(syncResult),
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
