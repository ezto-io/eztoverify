package io.ezto.extensions.mail;

import com.sparkpost.Client;
import com.sparkpost.model.responses.Response;
import com.sparkpost.transport.IRestConnection;
import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.bg.core.models.GeneralResponse;
import io.ezto.extensions.helpers.FreemarkerTemplateProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SparkPostImpl implements IEmailProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SparkPostImpl.class);
    @Override
    public String getProviderId() {
        return "sparkpost";
    }

    @Override
    public GeneralResponse send(IEmailTemplate emailTemplate, Map<String, String> configMap) {
        try{
            String apiKey = configMap.get("SPARK_POST_API_KEY");
            String sparkPostRegion = configMap.get("SPARK_POST_REGION");
            String fromEmail = configMap.get("FROM");


            Client client = new Client(apiKey);
            if (sparkPostRegion != null && sparkPostRegion.equals("SparkPost-EU")) {
                client = new Client(apiKey, IRestConnection.SPC_EU_ENDPOINT);
            }

            String htmlMessage = emailTemplate.getPlaceHolders().get("htmlBody") != null && !emailTemplate.getPlaceHolders().get("htmlBody").equals("")
                    ? emailTemplate.getPlaceHolders().get("htmlBody")
                    : FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).processTemplate(emailTemplate.getTemplatePath(), emailTemplate.getPlaceHolders());
            ;
            Response response = client.sendMessage(fromEmail, emailTemplate.getToEmail(), emailTemplate.getSubject(),
                    null, htmlMessage);
            if (response.getResponseCode() == 200 || response.getResponseCode() == 201 || response.getResponseCode() == 202) {
                LOG.info("Success : " + response.getResponseBody());
                GeneralResponse successResponse = new GeneralResponse<>();
                successResponse.setStatus(200);
                return successResponse;
            } else {
                LOG.error(response.getResponseBody());

                LOG.info("Failed : " + response.getResponseBody());

                GeneralResponse failureResponse = new GeneralResponse<>();
                failureResponse.setStatus(400);
                failureResponse.setBody(response.getResponseBody());
                return failureResponse;
            }
        }
        catch (Exception ex){
            LOG.info("Failed : " + ex);
            GeneralResponse failureResponse = new GeneralResponse<>();
            failureResponse.setStatus(400);
            failureResponse.setBody(ex.getMessage());
            return failureResponse;
        }
    }
}
