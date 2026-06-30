package io.ezto.extensions.mail;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.bg.core.models.GeneralResponse;
import io.ezto.extensions.helpers.FreemarkerTemplateProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AmazonSESImpl implements IEmailProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AmazonSESImpl.class);

    @Override
    public String getProviderId() {
        return "amazon-ses";
    }

    @Override
    public GeneralResponse send(IEmailTemplate emailTemplate, Map<String, String> config) {
        try {
            String configFromEmail = config.get("FROM");
            AmazonSimpleEmailService client =
                    AmazonSimpleEmailServiceClientBuilder.standard()
                            .withCredentials(new AWSCredentialsProvider() {
                                @Override
                                public AWSCredentials getCredentials() {
                                    return new AWSCredentials() {
                                        @Override
                                        public String getAWSAccessKeyId() {
                                            return config.get("AWS_ACCESS_ID");
                                        }

                                        @Override
                                        public String getAWSSecretKey() {
                                            return config.get("AWS_SECRET_ACCESS_KEY");
                                        }
                                    };
                                }

                                @Override
                                public void refresh() {
                                    // NOP
                                }
                            }).withRegion(config.get("AWS_REGION")).build();
            String htmlMessage = emailTemplate.getPlaceHolders().get("htmlBody") != null && !emailTemplate.getPlaceHolders().get("htmlBody").equals("")
                    ? emailTemplate.getPlaceHolders().get("htmlBody")
                    : FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).processTemplate(emailTemplate.getTemplatePath(), emailTemplate.getPlaceHolders());
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(
                            new Destination().withToAddresses(emailTemplate.getToEmail()))
                    .withSource(configFromEmail)
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withHtml(new Content()
                                            .withCharset("UTF-8").withData(htmlMessage))
                            )
                            .withSubject(new Content()
                                    .withCharset("UTF-8").withData(emailTemplate.getSubject())));
            SendEmailResult result = client.sendEmail(request);

            LOG.info("Success : " + result.getMessageId());

            GeneralResponse successResponse = new GeneralResponse<>();
            successResponse.setStatus(200);
            return successResponse;
        } catch (Exception ex) {
            LOG.error("Failed : " + ex);
            ex.printStackTrace();
            GeneralResponse failureResponse = new GeneralResponse<>();
            failureResponse.setStatus(400);
            failureResponse.setBody(ex.getMessage());
            return failureResponse;
        }
    }
}