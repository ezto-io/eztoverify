package io.ezto.extensions.mail;

import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.bg.core.models.GeneralResponse;
import io.ezto.extensions.helpers.FreemarkerTemplateProcessor;
import net.sargue.mailgun.Configuration;
import net.sargue.mailgun.Mail;
import net.sargue.mailgun.MailBuilder;
import net.sargue.mailgun.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MailGunImpl implements IEmailProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MailGunImpl.class);

    @Override
    public String getProviderId() {
        return "mailgun";
    }

    @Override
    public GeneralResponse send(IEmailTemplate emailTemplate, Map<String, String> config) {
        String domain = config.get("MAIL_GUN_DOMAIN");
        String apiKey = config.get("MAIL_GUN_API_KEY");
        String configFromEmail = config.get("FROM");

        Configuration mailgunConfig = new Configuration()
                .domain(domain)
                .apiKey(apiKey)
                .from(configFromEmail);

        MailBuilder mailBuilder = Mail.using(mailgunConfig)
                .to(emailTemplate.getToEmail());

        String htmlMessage = emailTemplate.getPlaceHolders().get("htmlBody") != null && !emailTemplate.getPlaceHolders().get("htmlBody").equals("")
                ? emailTemplate.getPlaceHolders().get("htmlBody")
                : FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).processTemplate(emailTemplate.getTemplatePath(), emailTemplate.getPlaceHolders());
        mailBuilder.html(htmlMessage);

        try {
            Response resp = mailBuilder.build().send();
            if (!resp.isOk()) {
                LOG.error("Failed : " + resp.responseMessage());

                GeneralResponse failureResponse = new GeneralResponse<>();
                failureResponse.setStatus(resp.responseCode());
                failureResponse.setBody(resp.responseMessage());
                return failureResponse;
            }
            LOG.info("Success : " + resp.responseMessage());
            GeneralResponse successResponse = new GeneralResponse<>();
            successResponse.setStatus(200);
            return successResponse;
        } catch (Exception e) {
            LOG.error("Failed : " + e);
            e.printStackTrace();

            GeneralResponse failureResponse = new GeneralResponse<>();
            failureResponse.setStatus(400);
            failureResponse.setBody(e.getMessage());
            return failureResponse;

        }
    }
}
