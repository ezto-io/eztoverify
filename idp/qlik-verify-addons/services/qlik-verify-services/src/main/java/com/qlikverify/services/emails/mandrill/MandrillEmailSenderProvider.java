package com.qlikverify.services.emails.mandrill;

import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.backgroundService.backgroundEmailService.BackgroundEmailRequestBody;
import com.qlikverify.services.emails.mandrill.models.MailChimp;
import com.qlikverify.services.emails.mandrill.models.MailChimpResponse;
import com.qlikverify.services.emails.mandrill.models.Message;
import com.qlikverify.services.emails.mandrill.models.To;
import com.qlikverify.services.helpers.GeneralHelper;
import okhttp3.Response;
import org.keycloak.EnvironmentReader;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MandrillEmailSenderProvider implements EmailSenderProvider {

    KeycloakSession session;
    EventBuilder eventBuilder;
    MandrillEmailSenderProviderFactory providerFactory;

    public MandrillEmailSenderProvider(KeycloakSession session,MandrillEmailSenderProviderFactory factory) {
        this.session = session;
        this.providerFactory= factory;
        this.eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());

    }

    @Override
    public void send(Map<String, String> config, UserModel user, String subject, String textBody, String htmlBody) throws EmailException {
        String email = retrieveEmailAddress(user);
        send(config, email, subject, textBody ,htmlBody);
    }

    @Override
    public void send(Map<String, String> config, String email, String subject, String textBody, String htmlBody) throws EmailException {

        List<String> validateConfig = GeneralHelper.validateConfig(providerFactory.getConfigProperties(session),config);

        if(!validateConfig.isEmpty())
        {
            throw new EmailException(String.join("\n",validateConfig));
        }

        String apiKey = config.get("MANDRILL_API_KEY");
        String configFromEmail = config.get("FROM");
        Message message = new Message();
        message.setFromEmail(configFromEmail);
        message.setSubject(subject);
        List<Object> tos = new ArrayList<>();
        To to = new To();
        to.setEmail(email);
        to.setType("to");
        tos.add(to);
        message.setTo(tos);
        if (textBody != null) {
            message.setText(textBody);
        }
        if (htmlBody != null) {
            message.setHtml(htmlBody);
        }
        message.setImportant(true);

        MailChimp mailChimp = new MailChimp();
        mailChimp.setKey(apiKey);
        mailChimp.setMessage(message);


        HttpUtility httpUtility = new HttpUtility();
        try {
            Response response = httpUtility.callRequest("application/json", "POST", "https://mandrillapp.com/api/1.0/messages/send", JsonSerialization.writeValueAsString(mailChimp), null);
            if(response == null) {
                throw new EmailException("Failed to send Email");
            }
            MailChimpResponse[] mailChimpResponse = JsonSerialization.readValue(response.body().string(),MailChimpResponse[].class);
            if(mailChimpResponse.length == 0) {
                throw new EmailException("Invalid response");
            }

            if(mailChimpResponse.length > 0){
                MailChimpResponse mailChimpResponse1 = mailChimpResponse[0];
                if(!mailChimpResponse1.getStatus().equals("sent"))
                {
                    throw new EmailException(mailChimpResponse1.getRejectReason());
                }
            }
            eventBuilder.event(EventType.MANDRILL_EMAIL).success();
        } catch (IOException e) {
            eventBuilder.event(EventType.MANDRILL_EMAIL_ERROR).error(e.getMessage());
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
            eventBuilder.event(EventType.MANDRILL_EMAIL_ERROR).error(e.getMessage());
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
