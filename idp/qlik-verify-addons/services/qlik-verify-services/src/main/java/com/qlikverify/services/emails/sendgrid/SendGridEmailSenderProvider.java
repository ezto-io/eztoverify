package com.qlikverify.services.emails.sendgrid;

import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.backgroundService.backgroundEmailService.BackgroundEmailRequestBody;
import com.qlikverify.services.helpers.GeneralHelper;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.keycloak.EnvironmentReader;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendGridEmailSenderProvider implements EmailSenderProvider {

    KeycloakSession session;
    EventBuilder eventBuilder;
    SendGridEmailSenderProviderFactory providerFactory;

    public SendGridEmailSenderProvider(KeycloakSession session,SendGridEmailSenderProviderFactory factory) {
        this.session = session;
        this.providerFactory = factory;
        this.eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
    }

    @Override
    public void send(Map<String, String> config, UserModel user, String subject, String textBody, String htmlBody) throws EmailException {
        String email = retrieveEmailAddress(user);
        send(config, email, subject, textBody,htmlBody);
    }

    @Override
    public void send(Map<String, String> config, String email, String subject, String textBody, String htmlBody) throws EmailException {

        List<String> validateConfig = GeneralHelper.validateConfig(providerFactory.getConfigProperties(session),config);

        if(!validateConfig.isEmpty())
        {
            throw new EmailException(String.join("\n",validateConfig));
        }

        String configFromEmail = config.get("FROM");
        Email from = new Email(configFromEmail);
        Email to = new Email(email);
        Content content = null;

        if (htmlBody != null) {
            content = new Content("text/html", htmlBody);
        } else if (textBody != null) {
            content = new Content("text/plain", textBody);
        }

        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(config.get("SEND_GRID_API_KEY"));
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if(response.getStatusCode() == 200 || response.getStatusCode() == 201 || response.getStatusCode() == 202) {
                eventBuilder.event(EventType.SEND_GRID_EMAIL).success();
            }
            else
            {
                eventBuilder.event(EventType.SEND_GRID_EMAIL_ERROR).error(response.getBody());
                throw new EmailException(response.getBody());
            }
        } catch (IOException ex) {
            eventBuilder.event(EventType.SEND_GRID_EMAIL_ERROR).error(ex.getMessage());
            throw new EmailException(ex.getMessage());
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
                    bodyAttributes, GeneralHelper.getLocale(session, userModel))
            );
        } catch (Exception e) {
            eventBuilder.event(EventType.SEND_GRID_EMAIL_ERROR).error(e.getMessage());
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
