package com.qlikverify.services.emails.mailgun;

import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.backgroundService.backgroundEmailService.BackgroundEmailRequestBody;
import com.qlikverify.services.helpers.GeneralHelper;
import net.sargue.mailgun.Configuration;
import net.sargue.mailgun.Mail;
import net.sargue.mailgun.MailBuilder;
import net.sargue.mailgun.Response;
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

public class MailGunEmailSenderProvider implements EmailSenderProvider {

    KeycloakSession session;
    EventBuilder eventBuilder;
    MailGunEmailSenderProviderFactory providerFactory;

    public MailGunEmailSenderProvider(KeycloakSession session,MailGunEmailSenderProviderFactory factory) {
        this.session = session;
        this.providerFactory = factory;
        this.eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());

    }

    @Override
    public void send(Map<String, String> config, UserModel user, String subject, String textBody, String htmlBody) throws EmailException {
        String email = retrieveEmailAddress(user);
        send(config, email, subject, textBody, htmlBody);
    }

    @Override
    public void send(Map<String, String> config, String email, String subject, String textBody, String htmlBody) throws EmailException {
        List<String> validateConfig = GeneralHelper.validateConfig(providerFactory.getConfigProperties(session),config);

        if(!validateConfig.isEmpty())
        {
            throw new EmailException(String.join("\n",validateConfig));
        }

        String domain = config.get("MAIL_GUN_DOMAIN");
        String apiKey = config.get("MAIL_GUN_API_KEY");
        String configFromEmail = config.get("FROM");

        Configuration mailgunConfig = new Configuration()
                .domain(domain)
                .apiKey(apiKey)
                .from(configFromEmail);

        MailBuilder mailBuilder = Mail.using(mailgunConfig)
                                        .to(email)
                                        .subject(subject);

        if (htmlBody != null) {
            mailBuilder.html(htmlBody);
        }
        if (textBody != null) {
            mailBuilder.text(textBody);
        }

        try {
            Response resp = mailBuilder.build().send();
            if(!resp.isOk()){
                throw new EmailException(resp.responseCode() + " " + resp.responseMessage());
            }
            eventBuilder.event(EventType.MAIL_GUN_EMAIL).success();
        } catch (Exception e) {
            eventBuilder.event(EventType.MAIL_GUN_EMAIL_ERROR).error(e.getMessage());
            throw new EmailException(e.getMessage());
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
            eventBuilder.event(EventType.MAIL_GUN_EMAIL_ERROR).error(e.getMessage());
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
