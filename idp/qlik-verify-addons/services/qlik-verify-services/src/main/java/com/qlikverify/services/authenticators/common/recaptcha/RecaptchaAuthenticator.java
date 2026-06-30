package com.qlikverify.services.authenticators.common.recaptcha;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.locale.LocaleType;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.qlikverify.services.authenticators.common.recaptcha.RecaptchaAuthenticatorFactory.*;
import static com.qlikverify.services.constants.Constants.ERROR;
import static com.qlikverify.services.constants.Constants.RECAPTCHA_FTL;

public class RecaptchaAuthenticator implements Authenticator {

    KeycloakSession session;
    public static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";
    private static final Logger logger = Logger.getLogger(RecaptchaAuthenticator.class);
    public static final String RECAPTCHA_DOMAIN = "google.com";


    public RecaptchaAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        LoginFormsProvider form = context.form();

        AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
        form.setAttribute("recaptchaVersion",captchaConfig.getConfig().get(CAPTCHA_VERSION).toUpperCase(Locale.ROOT));

        String userLanguageTag = context.getSession().getContext().resolveLocale(context.getUser(), LocaleType.ADMIN).toLanguageTag();
        if (captchaConfig == null || captchaConfig.getConfig() == null
                || captchaConfig.getConfig().get(RecaptchaAuthenticatorFactory.SITE_KEY) == null
                || captchaConfig.getConfig().get(SITE_SECRET) == null
        ) {
            form.setAttribute(ERROR,Messages.RECAPTCHA_NOT_CONFIGURED);
            showForm(context, form);
            return;
        }

        if (captchaConfig.getConfig().get(RecaptchaAuthenticatorFactory.CAPTCHA_VERSION).equalsIgnoreCase("v3") && (captchaConfig.getConfig().get(SITE_SCORE) == null
                || captchaConfig.getConfig().get(SITE_ACTION) == null)) {
            form.setAttribute(ERROR, Messages.RECAPTCHA_NOT_CONFIGURED);
            showForm(context, form);
            return;
        }
        String siteKey = captchaConfig.getConfig().get(RecaptchaAuthenticatorFactory.SITE_KEY);
        form.setAttribute("recaptchaSiteKey", siteKey);

        if(captchaConfig.getConfig().get(RecaptchaAuthenticatorFactory.CAPTCHA_VERSION).equalsIgnoreCase("v3"))
        {
            form.setAttribute("recaptchaActionName", captchaConfig.getConfig().get(SITE_ACTION));
            form.addScript("https://www." + getRecaptchaDomain(captchaConfig) + "/recaptcha/api.js?hl=" + userLanguageTag +"&render=" + siteKey + "&onload=onRecaptchaLoaded");
        }
        else {
            form.addScript("https://www." + getRecaptchaDomain(captchaConfig) + "/recaptcha/api.js?hl=" + userLanguageTag);
        }

        showForm(context, form);
    }

    private void showForm(AuthenticationFlowContext context, LoginFormsProvider form) {
        Response challenge = form.createForm(RECAPTCHA_FTL);
        context.challenge(challenge);
    }

    private String getRecaptchaDomain(AuthenticatorConfigModel config) {
        return RECAPTCHA_DOMAIN;
    }

    protected boolean validateRecaptcha(AuthenticationFlowContext context, boolean success, String captcha, String secret,String captchaVersion) {
        HttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        HttpPost post = new HttpPost("https://www." + getRecaptchaDomain(context.getAuthenticatorConfig()) + "/recaptcha/api/siteverify");
        List<NameValuePair> formparams = new LinkedList<>();
        formparams.add(new BasicNameValuePair("secret", secret));
        formparams.add(new BasicNameValuePair("response", captcha));
        formparams.add(new BasicNameValuePair("remoteip", context.getConnection().getRemoteAddr()));
        try {
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);
            HttpResponse response = httpClient.execute(post);
            InputStream content = response.getEntity().getContent();
            try {
                Map json = JsonSerialization.readValue(content, Map.class);
                Object val = json.get("success");
                logger.info(captchaVersion);
                if (captchaVersion.equalsIgnoreCase("v2")) {
                    success = Boolean.TRUE.equals(val);
                } else {
                    Double userScore = Double.parseDouble(json.get("score").toString());
                    AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
                    Double configScore = Double.parseDouble(captchaConfig.getConfig().get(SITE_SCORE));
                    success = userScore > configScore;
                }
            } finally {
                content.close();
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.recaptchaFailed(e);
        }
        return success;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        boolean success = false;

        String captcha = formData.getFirst(G_RECAPTCHA_RESPONSE);
        if (!Validation.isBlank(captcha)) {
            AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
            String secret = captchaConfig.getConfig().get(SITE_SECRET);
            success = validateRecaptcha(context, success, captcha, secret, captchaConfig.getConfig().get(CAPTCHA_VERSION));
        }

        if (!success) {
            formData.remove(G_RECAPTCHA_RESPONSE);
            AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
            String siteKey = captchaConfig.getConfig().get(RecaptchaAuthenticatorFactory.SITE_KEY);
            String userLanguageTag = context.getSession().getContext().resolveLocale(context.getUser(), LocaleType.ADMIN).toLanguageTag();

            LoginFormsProvider form = context.form();
            form.setAttribute(ERROR, Messages.RECAPTCHA_FAILED);
            form.setAttribute("recaptchaVersion",captchaConfig.getConfig().get(CAPTCHA_VERSION).toUpperCase(Locale.ROOT));
            form.setAttribute("recaptchaSiteKey", siteKey);
            if(captchaConfig.getConfig().get(RecaptchaAuthenticatorFactory.CAPTCHA_VERSION).equalsIgnoreCase("v3"))
            {
                form.setAttribute("recaptchaActionName", captchaConfig.getConfig().get(SITE_ACTION));
                form.addScript("https://www." + getRecaptchaDomain(captchaConfig) + "/recaptcha/api.js?hl=" + userLanguageTag +"&render=" + siteKey + "&onload=onRecaptchaLoaded");
            }
            else {
                form.addScript("https://www." + getRecaptchaDomain(captchaConfig) + "/recaptcha/api.js?hl=" + userLanguageTag);
            }

            showForm(context, form);
            return;
        }

        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }
}
