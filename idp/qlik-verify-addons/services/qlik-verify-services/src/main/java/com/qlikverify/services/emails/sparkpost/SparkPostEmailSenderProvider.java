package com.qlikverify.services.emails.sparkpost;

import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.backgroundService.backgroundEmailService.BackgroundEmailRequestBody;
import com.qlikverify.services.helpers.GeneralHelper;
import com.sparkpost.Client;
import com.sparkpost.exception.SparkPostException;
import com.sparkpost.model.responses.Response;
import com.sparkpost.transport.IRestConnection;
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

public class SparkPostEmailSenderProvider implements EmailSenderProvider {

    KeycloakSession session;
    EventBuilder eventBuilder;
    SparkPostEmailSenderProviderFactory providerFactory;

    public SparkPostEmailSenderProvider(KeycloakSession session,SparkPostEmailSenderProviderFactory factory) {
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

        String apiKey = config.get("SPARK_POST_API_KEY");
        String sparkPostRegion = config.get("SPARK_POST_REGION");
        String configFromEmail = config.get("FROM");

        Client client = new Client(apiKey);
        if (sparkPostRegion != null && sparkPostRegion.equals("SparkPost-EU")) {
            client = new Client(apiKey, IRestConnection.SPC_EU_ENDPOINT);
        }

        try {
            Response response = client.sendMessage(configFromEmail, email, subject, textBody, htmlBody);
            if (response.getResponseCode() != 200) {
                eventBuilder.event(EventType.SPARK_POST_EMAIL_ERROR).error(response.getResponseBody());
                return;
            }
            eventBuilder.event(EventType.SPARK_POST_EMAIL).success();

        } catch (SparkPostException e) {
            eventBuilder.event(EventType.SPARK_POST_EMAIL_ERROR).error(e.getMessage());
            throw new EmailException(e.getMessage());
        }
    }

    @Override
    public void send(UserModel userModel, Map<String, String> config, String email, String subject, String ftl, Map<String, Object> bodyAttributes) throws EmailException {
        Map<String, Object> provider_attributes = new HashMap<>(config);
        provider_attributes.putAll(bodyAttributes);
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
            eventBuilder.event(EventType.SPARK_POST_EMAIL_ERROR).error(e.getMessage());
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
