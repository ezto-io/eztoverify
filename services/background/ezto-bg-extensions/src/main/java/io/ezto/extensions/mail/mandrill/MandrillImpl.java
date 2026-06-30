package io.ezto.extensions.mail.mandrill;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ezto.extensions.helpers.FreemarkerTemplateProcessor;
import io.ezto.extensions.mail.mandrill.models.MailChimp;
import io.ezto.extensions.mail.mandrill.models.MailChimpResponse;
import io.ezto.extensions.mail.mandrill.models.Message;
import io.ezto.extensions.mail.mandrill.models.To;
import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.bg.core.models.GeneralResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MandrillImpl implements IEmailProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MandrillImpl.class);

    @Override
    public String getProviderId() {
        return "mandrill";
    }

    @Override
    public GeneralResponse send(IEmailTemplate emailTemplate, Map<String, String> config) {

        String apiKey = config.get("MANDRILL_API_KEY");
        String configFromEmail = config.get("FROM");
        Message message = new Message();
        message.setFromEmail(configFromEmail);
        message.setSubject(emailTemplate.getSubject());
        List<Object> tos = new ArrayList<>();
        To to = new To();
        to.setEmail(emailTemplate.getToEmail());
        to.setType("to");
        tos.add(to);
        message.setTo(tos);
        message.setHtml(FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).processTemplate(emailTemplate.getTemplatePath(), emailTemplate.getPlaceHolders()));

        message.setSubject(emailTemplate.getSubject());
        message.setImportant(true);

        MailChimp mailChimp = new MailChimp();
        mailChimp.setKey(apiKey);
        mailChimp.setMessage(message);

        OkHttpClient httpUtility = new OkHttpClient();

        String url = "https://mandrillapp.com/api/1.0/messages/send";
        MediaType mediaType = MediaType.parse("application/json" + "; charset=utf-8");

        try {
            ObjectMapper mapper = new ObjectMapper();
            RequestBody body = RequestBody.create(mapper.writeValueAsString(mailChimp), mediaType);

            Request request = new Request.Builder()
                    .url(url)
                    .method("POST", body)
                    .build();
            Response response = httpUtility.newCall(request).execute();
            // Handle the response as needed
            if(response == null) {
                LOG.info("MandrillImpl : Failed to send email");
            }

            String responseBody = response.body().string();

            if (response.isSuccessful()) {
                LOG.info("Response: " + responseBody);
            }

            MailChimpResponse[] mailChimpResponse = mapper.readValue(responseBody,MailChimpResponse[].class);
            if(mailChimpResponse.length == 0) {
                LOG.info("MandrillImpl : Invalid response");
            }

            if(mailChimpResponse.length > 0){
                MailChimpResponse mailChimpResponse1 = mailChimpResponse[0];
                if(!mailChimpResponse1.getStatus().equals("sent"))
                {
                    LOG.info("MandrillImpl : " + mailChimpResponse1.getRejectReason());

                    LOG.info("Success : " + mailChimpResponse1.getRejectReason());

                    GeneralResponse successResponse = new GeneralResponse<>();
                    successResponse.setStatus(200);
                    successResponse.setBody(mailChimpResponse1.getRejectReason());
                    return successResponse;
                }
            }
        } catch (IOException e) {
            LOG.error(e.toString());
            LOG.info("Failed : " + e);
            GeneralResponse failureResponse = new GeneralResponse<>();
            failureResponse.setStatus(400);
            failureResponse.setBody(e.getMessage());
            return failureResponse;
        }

        return null;
    }
}
