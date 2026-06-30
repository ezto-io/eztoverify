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

package org.keycloak.email.freemarker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qlikverify.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.Version;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.component.ComponentModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.EventBean;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.locale.LocaleType;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.utils.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;

import static org.apache.commons.io.FilenameUtils.removeExtension;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerEmailTemplateProvider implements EmailTemplateProvider {

    public static final String EMAIL_POLICY_CHECKER = "email-policy-checker";
    protected KeycloakSession session;
    /**
     * authenticationSession can be null for some email sendings, it is filled only for email sendings performed as part of the authentication session (email verification, password reset, broker link
     * etc.)!
     */
    protected AuthenticationSessionModel authenticationSession;
    protected FreeMarkerUtil freeMarker;
    protected RealmModel realm;
    protected UserModel user;
    protected String emailProviderId;
    protected boolean isCustomSubject;
    protected EmailConfigModel testEmailConfigModel;
    private static int year = Calendar.getInstance().get(Calendar.YEAR);
    private static final Logger logger = Logger.getLogger(FreeMarkerEmailTemplateProvider.class);
    protected final Map<String, Object> attributes = new HashMap<>();

    public FreeMarkerEmailTemplateProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        this.session = session;
        this.freeMarker = freeMarker;
    }

    @Override
    public EmailTemplateProvider setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public EmailTemplateProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    @Override
    public EmailTemplateProvider setEmailProviderId(String emailProviderId) {
        this.emailProviderId = emailProviderId;
        return this;
    }

    @Override
    public EmailTemplateProvider setCustomSubject(boolean customSubject) {
        this.isCustomSubject = customSubject;
        return this;
    }

    @Override
    public EmailTemplateProvider setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    @Override
    public EmailTemplateProvider setAuthenticationSession(AuthenticationSessionModel authenticationSession) {
        this.authenticationSession = authenticationSession;
        return this;
    }

    protected String getRealmName() {
        if (realm.getDisplayName() != null) {
            return realm.getDisplayName();
        } else {
            return ObjectUtil.capitalize(realm.getName());
        }
    }

    @Override
    public void sendEvent(Event event) throws EmailException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", new ProfileBean(user));
        attributes.put("event", new EventBean(event));

        send(toCamelCase(event.getType()) + "Subject", "event-" + event.getType().toString().toLowerCase() + ".ftl", attributes);
    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        attributes.put("realmName", getRealmName());
        attributes.put("year", String.valueOf(year));

        send("passwordResetSubject", "password-reset.ftl", attributes);
    }

    @Override
    public void sendSmtpTestEmail(Map<String, String> config, UserModel user) throws EmailException, ConfigException {
        EmailConfigModel emailConfigModel = new EmailConfigModel();
        emailConfigModel.setEmailProviderId(emailProviderId);
        emailConfigModel.setConfig(config);
        testEmailConfigModel = emailConfigModel;
        setRealm(session.getContext().getRealm());
        setUser(user);

        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        attributes.put("realm_name", realm.getName());
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

        EmailTemplate emailTemplate = processTemplate(user, "emailTestSubject", Collections.emptyList(), "email-test.ftl", attributes);
        sendEmail(emailTemplate.getSubject(),user.getEmail(),"email-test.ftl", attributes);
    }

    @Override
    public void sendSmtpTestEmail(Map<String, String> config, UserModel userModel, String email) throws EmailException, ConfigException {
        EmailConfigModel emailConfigModel = new EmailConfigModel();
        emailConfigModel.setEmailProviderId(emailProviderId);
        emailConfigModel.setConfig(config);
        testEmailConfigModel = emailConfigModel;

        setRealm(session.getContext().getRealm());

        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("realm_name", realm.getName());
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

        EmailTemplate emailTemplate = processTemplate(userModel, "emailTestSubject", Collections.emptyList(), "email-test.ftl", attributes);
        send(email, emailTemplate.getSubject(), emailTemplate.getTextBody(), emailTemplate.getHtmlBody());
    }

    @Override
    public void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        ProfileBean profile =  new ProfileBean(user);
        attributes.put("user",profile.getAttributes());
//        attributes.put("user", new ProfileBean(user));
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        attributes.put("realm_name", getRealmName());

        BrokeredIdentityContext brokerContext = (BrokeredIdentityContext) this.attributes.get(IDENTITY_PROVIDER_BROKER_CONTEXT);
        String idpAlias = brokerContext.getIdpConfig().getAlias();
        idpAlias = ObjectUtil.capitalize(idpAlias);

        attributes.put("identity_provider_context", brokerContext.getUsername());
        attributes.remove(IDENTITY_PROVIDER_BROKER_CONTEXT);
        attributes.put("identity_provider_alias", idpAlias);
        attributes.put("year", String.valueOf(year));
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

        List<Object> subjectAttrs = Arrays.asList(idpAlias);
        send("identityProviderLinkSubject", subjectAttrs, "identity-provider-link.ftl", attributes);
    }

    @Override
    public void sendExecuteActions(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        attributes.put("realm_name", getRealmName());

        send("executeActionsSubject", "executeActions.ftl", attributes);
    }

    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        ProfileBean profile =  new ProfileBean(user);
        attributes.put("user",profile.getAttributes());
//        attributes.put("user", new ProfileBean(user));
        attributes.put("year", String.valueOf(year));
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);

        attributes.put("realm_name", getRealmName());
        attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));

        send("emailVerificationSubject", "email-verification.ftl", attributes);
    }

    /**
     * Add link info into template attributes.
     *
     * @param link                to add
     * @param expirationInMinutes to add
     * @param attributes          to add link info into
     */
    protected void addLinkInfoIntoAttributes(String link, long expirationInMinutes, Map<String, Object> attributes) throws EmailException {
        attributes.put("link", link);
        attributes.put("link_expiration", expirationInMinutes);
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        URI baseUri = uriInfo.getBaseUri();
        try {
            Locale locale =session.getContext().resolveLocale(user, LocaleType.ADMIN) ;
//            attributes.put("linkExpirationFormatter", new LinkExpirationFormatterMethod(getTheme().getMessages(locale), locale));
            UrlBean urlBean = new UrlBean(realm, getTheme(), baseUri, null);
            attributes.put("url",urlBean.getResourcesUrl());

        } catch (IOException e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    @Override
    public void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException {
        send(subjectFormatKey, Collections.emptyList(), bodyTemplate, bodyAttributes);
    }

    @Override
    public void send(String emailaddress, String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException, ConfigException {
        send(emailaddress, subjectFormatKey, Collections.emptyList(), bodyTemplate, bodyAttributes);
    }

    protected EmailTemplate processTemplate(UserModel user,String subjectKey, List<Object> subjectAttributes, String template, Map<String, Object> attributes) throws EmailException {
        try {
            Thread.sleep(100); // to avoid ConcurrentModificationError
            Theme theme = getTheme();

            Locale locale = GeneralHelper.getLocale(user);
            attributes.put("locale", locale);
            Properties rb = theme.getMessages(locale);
            Map<String, String> localizationTexts = realm.getRealmLocalizationTextsByLocale(locale.toLanguageTag());
            rb.putAll(localizationTexts);
            int year = Calendar.getInstance().get(Calendar.YEAR);
            attributes.put("year", String.valueOf(year));
            attributes.put("msg", new MessageFormatterMethod(locale, rb));
            attributes.put("properties", theme.getProperties());
            String subject = isCustomSubject ? subjectKey : new MessageFormat(rb.getProperty(subjectKey, subjectKey), locale).format(subjectAttributes.toArray());
            subject = subject.replace("QlikVerify", attributes.getOrDefault("variant", "ezto verify").toString());
            String textTemplate = String.format("text/%s", template);
            String textBody;
            try {
                textBody = freeMarker.processTemplate(attributes, textTemplate, theme);
            } catch (final FreeMarkerException e) {
                throw new EmailException("Failed to template plain text email.", e);
            }
            String htmlTemplate = String.format("html/%s", template);
            String htmlBody;

            try {
                String emailTemp = getEmailTemplate(session, realm.getName(), removeExtension(template));
                if (emailTemp != null && emailTemp.trim().length() > 0) {
                    htmlBody = freeMarker.processTemplate(attributes, emailTemp);
                } else {
                    logger.warn("Template with name " + template + " not Found in Minio");
                    htmlBody = freeMarker.processTemplate(attributes, htmlTemplate, theme);
                }
            } catch (final FreeMarkerException e) {
                logger.error(e.getMessage());
                throw new EmailException("Failed to template html email.", e);
            }

            return new EmailTemplate(subject, textBody, htmlBody);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new EmailException("Failed to template email", e);
        }
    }

    public String processTemplate(RealmModel realm, String templateName) {
        String htmlTemplate = String.format("html/%s", templateName);
        String htmlBody = "";
        try {
            Theme theme = getTheme();
            Locale locale = session.getContext().resolveLocale(user, LocaleType.ADMIN);
            int year = Calendar.getInstance().get(Calendar.YEAR);
            String websiteContactLink =  EnvironmentReader.getValue("WEBSITE_CONTACT_LINK", "https://support.ezto.io");
            String supportUrl = realm.getAttribute("support_url");
            if (Boolean.parseBoolean(realm.getAttribute("support_custom_toggle")) && StringUtil.isNotBlank(supportUrl)) {
                websiteContactLink = supportUrl;
            }
            attributes.put("year", String.valueOf(year));
            attributes.put("locale", locale);
            Properties rb = theme.getMessages(locale);
            Map<String, String> localizationTexts = realm.getRealmLocalizationTextsByLocale(locale.toLanguageTag());
            rb.putAll(localizationTexts);
            attributes.put("msg", new MessageFormatterMethod(locale, rb));
            attributes.put("properties", theme.getProperties());
            attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
            attributes.put("websiteContactLink", websiteContactLink);
            attributes.put("privacyPolicyLink", EnvironmentReader.getValue("PRIVACY_POLICY_LINK", "https://app.termly.io/document/privacy-policy/5c0d5314-9eb1-4498-948a-d4f6751d060c"));
            attributes.put("termsAndConditionLink", EnvironmentReader.getValue("TERMS_AND_CONDITION_LINK", "https://app.termly.io/document/terms-of-service/0e94947b-0d93-415e-acbb-34a03f2f3283"));
            attributes.put("docsLink", EnvironmentReader.getValue("DOCS_LINK", ""));
            attributes.put("websiteLink", EnvironmentReader.getValue("WEBSITE_LINK", "https://ezto.io/"));
            attributes.put("marketplaceLink", EnvironmentReader.getValue("MARKETPLACE_LINK", "https://eztoauth.com/marketplace"));
            attributes.put("authenticationLink", EnvironmentReader.getValue("AUTHENTICATION_LINK", "https://eztoauth.com/platform/authentication"));
            attributes.put("applicationLink", EnvironmentReader.getValue("APPLICATION_LINK", "https://eztoauth.com/platform"));
            attributes.putAll(getBrandingDetails(realm.getMode()));

            try {
                htmlBody = freeMarker.processTemplate(attributes, htmlTemplate, theme);
            } catch (final FreeMarkerException e) {
                logger.error("Failed to template html email.", e);
            }

        } catch (Exception e) {
            logger.error("Failed to template email", e);
        }
        return htmlBody;
    }

    private Map<String, String> getBrandingDetails(String mode) {
        Map<String, String> brandColors = new HashMap<>();
        if(mode.equalsIgnoreCase("wiam")) {
            brandColors.put("bodyColor", "#4CA355");
            brandColors.put("footerColor", "#2d5b3a");

        } else if (mode.equalsIgnoreCase("ciam")) {
            brandColors.put("bodyColor", "#2E2E2E");
            brandColors.put("footerColor", "#2E2E2E");

        } else if (mode.equalsIgnoreCase("bo")) {
            brandColors.put("bodyColor", "#4B72FA");
            brandColors.put("footerColor", "#05173f");
        }
        return brandColors;
    }

    public String processTemplate(String subjectKey, List<Object> subjectAttributes, String bodyTemplate) {
        try {
            Theme theme = getTheme();
            Locale locale = session.getContext().resolveLocale(user, LocaleType.ADMIN);
            Properties rb = theme.getMessages(Locale.getDefault());
            String subject = isCustomSubject ? subjectKey : new MessageFormat(rb.getProperty(subjectKey, subjectKey), locale).format(subjectAttributes.toArray());
            return subject;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getEmailTemplate(KeycloakSession session, String realmId, String templateName) {
        AvatarProvider avatarProvider = session.getProvider(AvatarProvider.class, "avatar-storage-minio");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {

            InputStream res = avatarProvider.loadEmailTemplate(realmId, templateName);
            if (res != null) {

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = res.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
            }

        } catch (IOException e) {
            logger.error(e);
        }
        return buffer.toString();
    }

    protected Theme getTheme() throws IOException {
        return session.theme().getTheme(Theme.Type.EMAIL);
    }

    @Override
    public void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException {
        try {
//            String subject = processTemplate(subjectFormatKey, subjectAttributes, bodyTemplate);
            bodyAttributes.put("subjectAttributes",subjectAttributes);
//            send(email.getSubject(), email.getTextBody(), email.getHtmlBody());
            send(user.getEmail(), subjectFormatKey, subjectAttributes, bodyTemplate, bodyAttributes);
        } catch (EmailException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    @Override
    public void send(String emailaddress, String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException, ConfigException {
        try {
            ClientRegistrationPolicyManager.triggerBeforeRegister(session, RegistrationAuth.AUTHENTICATED, EMAIL_POLICY_CHECKER);
            EmailConfigModel emailConfigModel = getEmailConfig();
            if(emailConfigModel == null){
                throw new ConfigException("Email is not configured. Please check with admin");
            }
            bodyAttributes.put("to",emailaddress);
            EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class, emailConfigModel.getEmailProviderId());
            if(session.getContext().getClient() != null){
                bodyAttributes.put("clientName", session.getContext().getClient().getName());
            }
            String websiteContactLink =  EnvironmentReader.getValue("WEBSITE_CONTACT_LINK", "https://support.ezto.io");
            String supportUrl = realm.getAttribute("support_url");
            if (Boolean.parseBoolean(realm.getAttribute("support_custom_toggle")) && StringUtil.isNotBlank(supportUrl)) {
                websiteContactLink = supportUrl;
            }
            bodyAttributes.put("websiteContactLink", websiteContactLink);
            bodyAttributes.put("privacyPolicyLink", EnvironmentReader.getValue("PRIVACY_POLICY_LINK", "https://app.termly.io/document/privacy-policy/5c0d5314-9eb1-4498-948a-d4f6751d060c"));
            bodyAttributes.put("termsAndConditionLink", EnvironmentReader.getValue("TERMS_AND_CONDITION_LINK", "https://app.termly.io/document/terms-of-service/0e94947b-0d93-415e-acbb-34a03f2f3283"));
            bodyAttributes.put("docsLink", EnvironmentReader.getValue("DOCS_LINK", ""));
            bodyAttributes.put("websiteLink", EnvironmentReader.getValue("WEBSITE_LINK", "https://ezto.io/"));
            bodyAttributes.put("marketplaceLink", EnvironmentReader.getValue("MARKETPLACE_LINK", "https://eztoauth.com/marketplace"));
            bodyAttributes.put("authenticationLink", EnvironmentReader.getValue("AUTHENTICATION_LINK", "https://eztoauth.com/platform/authentication"));
            bodyAttributes.put("applicationLink", EnvironmentReader.getValue("APPLICATION_LINK", "https://eztoauth.com/platform"));
            bodyAttributes.put("mode", realm.getMode());

            if(GeneralHelper.checkWalletBalance(session.getContext().getRealm())) {
                throw new EmailException("Wallet balance too low");
            }

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession innerSession) throws JsonProcessingException {
                    try {
                        emailSender.send(innerSession.users().getUserByEmail(realm, emailaddress), emailConfigModel.getConfig(), emailaddress, subjectFormatKey,bodyTemplate,bodyAttributes);
                    } catch (EmailException e) {
                        throw new RuntimeException(e);
                    }
                    RealmModel realmModel = session.getContext().getRealm();
                    Optional<ComponentModel> optionalComponentModel = realmModel.getComponentsStream(realmModel.getId()).filter(x->x.getProviderId().equalsIgnoreCase(EMAIL_POLICY_CHECKER)).findFirst();
                    session.subPolicies().addEmailPolicy(realmModel,optionalComponentModel.isPresent());
                    ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, EMAIL_POLICY_CHECKER);
                }
            });
        } catch (EmailException | ConfigException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e);
            throw new EmailException("Failed to template email", e);
        }
    }

    protected void sendEmail(String subjectKey,String toEmail,String template,Map<String, Object> attributes) throws EmailException, ConfigException{
        ClientRegistrationPolicyManager.triggerBeforeRegister(session, RegistrationAuth.AUTHENTICATED, EMAIL_POLICY_CHECKER);
        EmailConfigModel emailConfigModel = getEmailConfig();
        if(emailConfigModel == null){
            throw new ConfigException("Email is not configured. Please check with admin");
        }
        EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class, emailConfigModel.getEmailProviderId());
        try {
            attributes.put("to", toEmail);
            emailSender.send(session.users().getUserByEmail(realm, toEmail), emailConfigModel.getConfig(), toEmail,subjectKey, template,attributes);
            RealmModel realmModel = session.getContext().getRealm();
            Optional<ComponentModel> optionalComponentModel = realmModel.getComponentsStream(realmModel.getId()).filter(x->x.getProviderId().equalsIgnoreCase(EMAIL_POLICY_CHECKER)).findFirst();
            session.subPolicies().addEmailPolicy(realmModel,optionalComponentModel.isPresent());
            ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, EMAIL_POLICY_CHECKER);
        } catch (EmailException err) {
            throw err;
        }
    }

    private EmailConfigModel getEmailConfig() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(testEmailConfigModel != null)
            return testEmailConfigModel;
        EmailConfigModel[] emailConfigModel = {null};


        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession session) throws JsonProcessingException {
                RealmModel realmModel = (realm == null) ? session.getContext().getRealm() : realm;
                if (Version.OFFERING.equalsIgnoreCase("cloud")) {
                    if (session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getName().equalsIgnoreCase(realm.getName()) && emailProviderId != null && !emailProviderId.isEmpty()) {
                        EmailConfigModel emailConfigByProviderId = session.realms().getRealmByName(Config.getAdminRealm()).getEmailConfigByProviderId(emailProviderId);
                        if(emailConfigByProviderId == null && emailProviderId.equals("default")){
                            Optional<EmailConfigModel> optEmailConfig = session.realms().getRealmByName(Config.getAdminRealm()).getEmailConfigsStream().findFirst();
                            emailConfigModel[0] = optEmailConfig.orElse(null);
                        }
                        emailConfigModel[0] = emailConfigByProviderId;
                    } else {
                        Optional<EmailConfigModel> optEmailConfig = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getEmailConfigsStream().findFirst();
                        optEmailConfig.ifPresent(configModel -> emailConfigModel[0] = configModel);
                    }
                }
                else {
                    if (emailProviderId == null || emailProviderId.isEmpty()) {
                        if (realmModel.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm()) || realmModel.isEnabledOwnEmailProvider()) {
                            //go with own email provider
                            Optional<EmailConfigModel> optEmailConfig = realmModel.getEmailConfigsStream().findFirst();
                            optEmailConfig.ifPresent(configModel -> emailConfigModel[0] = configModel);
                        } else {
                            Optional<EmailConfigModel> optEmailConfig = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getEmailConfigsStream().findFirst();
                            optEmailConfig.ifPresent(configModel -> emailConfigModel[0] = configModel);
                        }
                    } else {
                        emailConfigModel[0] = realmModel.getEmailConfigByProviderId(emailProviderId);
                    }
                }
            }
        });

        return emailConfigModel[0];
    }


    protected void send(String emailaddress, String subject, String textBody, String htmlBody) throws EmailException, ConfigException {
        ClientRegistrationPolicyManager.triggerBeforeRegister(session, RegistrationAuth.AUTHENTICATED, EMAIL_POLICY_CHECKER);
        EmailConfigModel emailConfigModel = getEmailConfig();
        if(emailConfigModel == null){
            throw new ConfigException("Email is not configured. Please check with admin");
        }
        EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class, emailConfigModel.getEmailProviderId());
        try {
            emailSender.send(emailConfigModel.getConfig(), emailaddress, subject, textBody, htmlBody);
            RealmModel realmModel = session.getContext().getRealm();
            Optional<ComponentModel> optionalComponentModel = realmModel.getComponentsStream(realmModel.getId()).filter(x->x.getProviderId().equalsIgnoreCase(EMAIL_POLICY_CHECKER)).findFirst();
            session.subPolicies().addEmailPolicy(realmModel,optionalComponentModel.isPresent());
            ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, EMAIL_POLICY_CHECKER);
        } catch (EmailException err) {
            logger.error(err.getMessage());
            throw err;
        }
    }

    @Override
    public void close() {
    }

    protected String toCamelCase(EventType event) {
        StringBuilder sb = new StringBuilder("event");
        for (String s : event.name().toLowerCase().split("_")) {
            sb.append(ObjectUtil.capitalize(s));
        }
        return sb.toString();
    }

    protected static class EmailTemplate {

        private String subject;
        private String textBody;
        private String htmlBody;

        public EmailTemplate(String subject, String textBody, String htmlBody) {
            this.subject = subject;
            this.textBody = textBody;
            this.htmlBody = htmlBody;
        }

        public String getSubject() {
            return subject;
        }

        public String getTextBody() {
            return textBody;
        }

        public String getHtmlBody() {
            return htmlBody;
        }
    }

}
