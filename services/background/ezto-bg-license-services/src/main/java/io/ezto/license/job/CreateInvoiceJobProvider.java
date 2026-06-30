package io.ezto.license.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ezto.bg.core.IJobProvider;
import io.ezto.license.job.models.CreateInvoiceModel;
import io.ezto.helpers.EnvironmentReader;
import io.ezto.helpers.GeneralHelper;
import io.ezto.helpers.NotificationHelper;
import io.ezto.helpers.utility.HttpUtility;
import okhttp3.Response;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateInvoiceJobProvider implements IJobProvider {

    private static final String TENANT_ID = "tenant_id";
    private static final String DELETION_TIME = "deletion_time";
    private static final String CALLBACK_URL = "callback_url";
    private static final String PRODUCTS = "products";
    private static final String CURRENCY = "currency";
    private static final String CUSTOMER_ID = "customer_id";
    private static final Logger logger = LoggerFactory.getLogger(RemoveTenantJobProvider.class);

    @Override
    public String getProviderId() {
        return "create_invoice";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        logger.info("CreateInvoiceJobProvider :: doExecute");
        if(taskRequest.get(CALLBACK_URL) == null)
            return;
        if(taskRequest.get(TENANT_ID) == null)
            return;
        if(taskRequest.get(CUSTOMER_ID) == null)
            return;
        if(taskRequest.get(CURRENCY) == null)
            return;
        if(taskRequest.get(PRODUCTS) == null)
            return;

        String callbackUrl = taskRequest.get(CALLBACK_URL).toString();
        HttpUtility httpUtility = new HttpUtility();
        CreateInvoiceModel createInvoiceModel = new CreateInvoiceModel();
        createInvoiceModel.setTenantId(taskRequest.get(TENANT_ID).toString());
        createInvoiceModel.setCurrency(taskRequest.get(CURRENCY).toString());
        createInvoiceModel.setProducts(taskRequest.get(PRODUCTS).toString());
        createInvoiceModel.setCustomerId(taskRequest.get(CUSTOMER_ID).toString());

        try {
            Map<String, String> contentType = new HashMap<>();
            contentType.put("Content-Type", "application/json");
            Response response = httpUtility.callRequest(
                    "application/json",
                    "DELETE",
                    EnvironmentReader.getValue("LICENSE_URL") + callbackUrl,
                    new ObjectMapper().writeValueAsString(createInvoiceModel),
                    null,
                    contentType,
                    1,
                    10000
            );
            if(response.isSuccessful()){
                taskRequest.remove(CALLBACK_URL);
                taskRequest.remove(DELETION_TIME);
                NotificationHelper notificationHelper = new NotificationHelper();
                notificationHelper.tenantDeletedNotification(taskRequest);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        logger.info("CreateInvoiceJobProvider :: validateRequest");
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

        String callBackUrl = GeneralHelper.getValueFromConfig(taskRequest, CALLBACK_URL).toString();
        if(StringUtils.isNullOrEmpty(callBackUrl)){
            errors.add("Callback URL is missing");
            return errors;
        }

        return errors;
    }
}
