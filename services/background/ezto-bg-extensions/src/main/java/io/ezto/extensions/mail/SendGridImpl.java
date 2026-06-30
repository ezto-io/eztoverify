package io.ezto.extensions.mail;


import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.extensions.helpers.FreemarkerTemplateProcessor;
import io.ezto.bg.core.models.GeneralResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class SendGridImpl implements IEmailProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SendGridImpl.class);

    @Override
    public String getProviderId() {
        return "sendgrid";
    }


    @Override
    public GeneralResponse send(IEmailTemplate emailTemplate, Map<String, String> configMap) {
        Email from = new Email(configMap.get("FROM"));
        Email to = new Email(emailTemplate.getToEmail());
        Content content = null;

        if(emailTemplate.getPlaceHolders().get("htmlBody") != null && !emailTemplate.getPlaceHolders().get("htmlBody").equals("") ){
            content = new Content("text/html", emailTemplate.getPlaceHolders().get("htmlBody"));
        }else{
            String htmlMessage = FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).processTemplate(emailTemplate.getTemplatePath(), emailTemplate.getPlaceHolders());
            if (htmlMessage != null) {
                content = new Content("text/html", htmlMessage);
            }
        }
        try{

        Mail mail = new Mail(from, FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).getSubject(emailTemplate.getSubject()).replace("QlikVerify", emailTemplate.getPlaceHolders().getOrDefault("variant", "QlikVerify")), to, content);

        SendGrid sg = new SendGrid(configMap.get("SEND_GRID_API_KEY"));
        Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() == 200 || response.getStatusCode() == 201 || response.getStatusCode() == 202) {
                GeneralResponse successResponse = new GeneralResponse<>();
                successResponse.setStatus(200);
                return successResponse;
            } else {
                GeneralResponse failureResponse = new GeneralResponse<>();
                failureResponse.setStatus(response.getStatusCode());
                failureResponse.setBody(response.getBody());
                return failureResponse;
            }
        } catch (IOException ex) {
            LOG.error(ex.toString());

            GeneralResponse failureResponse = new GeneralResponse<>();
            failureResponse.setStatus(400);
            failureResponse.setBody(ex.getMessage());
            return failureResponse;
        }
    }
}
