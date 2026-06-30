/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlikverify.services.emails.smtp;

import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.backgroundService.backgroundEmailService.BackgroundEmailRequestBody;
import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.helpers.GeneralHelper;
import com.sun.mail.smtp.SMTPMessage;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.truststore.HostnameVerificationPolicy;
import org.keycloak.truststore.JSSETruststoreConfigurator;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class SmtpEmailSenderProvider implements EmailSenderProvider {

    private static final String SUPPORTED_SSL_PROTOCOLS = getSupportedSslProtocols();

    private static final Logger logger = Logger.getLogger(SmtpEmailSenderProvider.class);

    private final KeycloakSession session;
    private SmtpEmailSenderProviderFactory providerFactory;
    EventBuilder eventBuilder;

    public SmtpEmailSenderProvider(KeycloakSession session, SmtpEmailSenderProviderFactory factory) {
        this.session = session;
        this.providerFactory = factory;
        this.eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
    }

    @Override
    public void send(Map<String, String> config, UserModel user, String subject, String textBody, String htmlBody) throws EmailException {
        String address = retrieveEmailAddress(user);
        send(config, address, subject, textBody, htmlBody);
    }

    @Override
    public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) throws EmailException {

        List<String> validateConfig = GeneralHelper.validateConfig(providerFactory.getConfigProperties(session), config);

        if (!validateConfig.isEmpty()) {
            throw new EmailException(String.join("\n", validateConfig));
        }

        Transport transport = null;
        try {

            Properties props = new Properties();

            if (config.containsKey("HOST")) {
                props.setProperty("mail.smtp.host", config.get("HOST"));
            }

            boolean auth = Constants.TRUE.equals(config.get("ENABLE_AUTH"));
            boolean ssl = Constants.TRUE.equals(config.get("ENABLE_SSL"));
            boolean starttls = Constants.TRUE.equals(config.get("ENABLE_STARTTLS"));

            if (config.containsKey("PORT") && config.get("PORT") != null) {
                props.setProperty("mail.smtp.port", config.get("PORT"));
            }

            if (auth) {
                props.setProperty("mail.smtp.auth", "true");
            }

            if (ssl) {
                props.setProperty("mail.smtp.ssl.enable", "true");
            }

            if (starttls) {
                props.setProperty("mail.smtp.starttls.enable", "true");
            }

            if (ssl || starttls) {
                props.put("mail.smtp.ssl.protocols", SUPPORTED_SSL_PROTOCOLS);

                setupTruststore(props);
            }

            props.setProperty("mail.smtp.timeout", "10000");
            props.setProperty("mail.smtp.connectiontimeout", "10000");

            String from = config.get("FROM");
            String fromDisplayName = config.get("FROM_DISPLAY_NAME");
            String replyTo = config.get("REPLY_TO");
            String replyToDisplayName = config.get("REPLY_TO_DISPLAY_NAME");
            String envelopeFrom = config.get("ENVELOPE_FROM");

            Session session = Session.getInstance(props);

            Multipart multipart = new MimeMultipart("alternative");

            if (textBody != null) {
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(textBody, "UTF-8");
                multipart.addBodyPart(textPart);
            }

            if (htmlBody != null) {
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
                multipart.addBodyPart(htmlPart);
            }

            SMTPMessage msg = new SMTPMessage(session);
            msg.setFrom(toInternetAddress(from, fromDisplayName));

            msg.setReplyTo(new Address[]{toInternetAddress(from, fromDisplayName)});
            if (replyTo != null && !replyTo.isEmpty()) {
                msg.setReplyTo(new Address[]{toInternetAddress(replyTo, replyToDisplayName)});
            }
            if (envelopeFrom != null && !envelopeFrom.isEmpty()) {
                msg.setEnvelopeFrom(envelopeFrom);
            }

            msg.setHeader("To", address);
            msg.setSubject(subject, "utf-8");
            msg.setContent(multipart);
            msg.saveChanges();
            msg.setSentDate(new Date());

            transport = session.getTransport("smtp");
            if (auth) {
                transport.connect(config.get("USERNAME"), config.get("PASSWORD"));
            } else {
                transport.connect();
            }
            transport.sendMessage(msg, new InternetAddress[]{new InternetAddress(address)});
        } catch (Exception e) {
            e.printStackTrace();
            ServicesLogger.LOGGER.failedToSendEmail(e);
            throw new EmailException(e);
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    logger.warn("Failed to close transport", e);
                }
            }
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
                    subject,
                    provider_attributes,
                    bodyAttributes, GeneralHelper.getLocale(session, userModel))
            );
        } catch (Exception e) {
            eventBuilder.event(EventType.SMTP_EMAIL_ERROR).error(e.getMessage());
        }
    }

    protected InternetAddress toInternetAddress(String email, String displayName) throws UnsupportedEncodingException, AddressException, EmailException {
        if (email == null || "".equals(email.trim())) {
            throw new EmailException("Please provide a valid address", null);
        }
        if (displayName == null || "".equals(displayName.trim())) {
            return new InternetAddress(email);
        }
        return new InternetAddress(email, displayName, "utf-8");
    }

    protected String retrieveEmailAddress(UserModel user) {
        return user.getEmail();
    }

    private void setupTruststore(Properties props) {
        boolean checkServerIdentity = true;

        JSSETruststoreConfigurator configurator = new JSSETruststoreConfigurator(session);

        SSLSocketFactory factory = configurator.getSSLSocketFactory();
        if (factory != null) {
            props.put("mail.smtp.ssl.socketFactory", factory);
            if (configurator.getProvider().getPolicy() == HostnameVerificationPolicy.ANY) {
                props.setProperty("mail.smtp.ssl.trust", "*");
                checkServerIdentity = false;
            }
        }

        if (checkServerIdentity) {
            props.put("mail.smtp.ssl.checkserveridentity", "true");
        }
    }

    @Override
    public void close() {
        // NOP
    }

    private static String getSupportedSslProtocols() {
        try {
            String[] protocols = SSLContext.getDefault().getSupportedSSLParameters().getProtocols();
            if (protocols != null) {
                return String.join(" ", protocols);
            }
        } catch (Exception e) {
            logger.warn("Failed to get list of supported SSL protocols", e);
        }
        return null;
    }

}
