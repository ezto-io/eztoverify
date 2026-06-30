package com.qlikverify.services.emails.amazonses;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.backgroundService.backgroundEmailService.BackgroundEmailRequestBody;
import com.qlikverify.services.helpers.GeneralHelper;
import org.keycloak.EnvironmentReader;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmazonSESEmailSenderProvider implements EmailSenderProvider {

    KeycloakSession session;
    EventBuilder eventBuilder;
    AmazonSESEmailSenderProviderFactory providerFactory;

    public AmazonSESEmailSenderProvider(KeycloakSession session,AmazonSESEmailSenderProviderFactory factory) {
        this.session = session;
        this.providerFactory = factory;
        this.eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
    }

    @Override
    public void send(Map<String, String> config, UserModel user, String subject, String textBody, String htmlBody) throws EmailException {
        String email = retrieveEmailAddress(user);
        send(config, email, subject, textBody ,htmlBody);
    }

    @Override
    public void send(Map<String, String> config, String email, String subject, String textBody, String htmlBody) throws EmailException {
        try {
            List<String> validateConfig = GeneralHelper.validateConfig(providerFactory.getConfigProperties(session),config);

            if(!validateConfig.isEmpty())
            {
                throw new EmailException(String.join("\n",validateConfig));
            }

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
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(
                            new Destination().withToAddresses(email))
                    .withSource(configFromEmail)
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withHtml(new Content()
                                            .withCharset("UTF-8").withData(htmlBody))
                                    .withText(new Content()
                                            .withCharset("UTF-8").withData(textBody)))
                            .withSubject(new Content()
                                    .withCharset("UTF-8").withData(subject)));
            client.sendEmail(request);
            eventBuilder.event(EventType.AWS_SES_EMAIL).success();
        } catch (Exception ex) {
            eventBuilder.event(EventType.AWS_SES_EMAIL_ERROR).error("The email was not sent. Error message: "
                    + ex.getMessage());
            throw new EmailException("The email was not sent. Error message: "
                    + ex.getMessage());
        }
    }

    @Override
    public void send(UserModel userModel, Map<String, String> config, String email, String subject, String ftl, Map<String, Object> bodyAttributes) throws EmailException {
        Map<String, Object> provider_attributes = new HashMap<>(config);
        provider_attributes.put("actionToken", BackgroundJobActionTokenVerifier.generateToken(session,Long.parseLong(String.valueOf(GeneralHelper.getExpiryDuration("1h"))), email));
        provider_attributes.put("realm", session.getContext().getRealm().getId());
        bodyAttributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
        try {
            BackgroundServiceImpl.getInstance(session).createBackgroundEmailJob(new BackgroundEmailRequestBody(
                    providerFactory.getId(),
                    ftl.replace(".ftl", ""),
                    email,
                    subject, provider_attributes,
                    bodyAttributes, GeneralHelper.getLocale(session, userModel)));
        } catch (Exception e) {
            eventBuilder.event(EventType.AWS_SES_EMAIL_ERROR).error(e.getMessage());
        }
    }

    @Override
    public void close() {
        // NOP
    }

    protected String retrieveEmailAddress(UserModel user) {
        return user.getEmail();
    }
}
