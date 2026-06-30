package com.qlikverify.services.authenticators.login.whatsapp;

import com.qlikverify.services.authenticators.misc.whatsapp.WhatsappOTP;
import com.qlikverify.services.authenticators.misc.whatsapp.WhatsappPush;
import com.qlikverify.services.authenticators.misc.whatsapp.WhatsappQR;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class WhatsappLoginAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(WhatsappLoginAuthenticator.class);

    private WhatsappQR whatsappQR;
    private WhatsappOTP whatsappOTP;
    private WhatsappPush whatsappPush;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:WhatsappLoginAuthenticator:Authenticate");
        if(context.getUser() != null && GeneralHelper.isTemporarilyDisabledByBruteForce(context, context.getUser(),null)){
            return;
        }
        String verifyWay = GeneralHelper.getConfigValue(context, "whatsapp_verify_options");

        if (verifyWay.equals("OTP")) {
            getWhatsappOTP().onPreLogin( context);
        } else if (verifyWay.equals("PUSH")) {
            getWhatsappPush().onPreLogin(context);
        } else{
            getWhatsappQR().onPreLogin(context);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:WhatsappLoginAuthenticator:Action");
        if(context.getUser() != null && GeneralHelper.isTemporarilyDisabledByBruteForce(context, context.getUser(),null)){
            return;
        }

        String verifyWay = GeneralHelper.getConfigValue(context, "whatsapp_verify_options");
        if (verifyWay.equals("OTP")) {
            getWhatsappOTP().onPostLogin( context);
        } else if (verifyWay.equals("PUSH")) {
            getWhatsappPush().onPostLogin(context);
        } else {
            getWhatsappQR().onPostLogin(context);
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    private WhatsappQR getWhatsappQR() {
        if (whatsappQR == null) {
            whatsappQR = new WhatsappQR();
        }
        return whatsappQR;
    }

    public WhatsappOTP getWhatsappOTP() {
        if (whatsappOTP == null) {
            whatsappOTP = new WhatsappOTP();
        }
        return whatsappOTP;
    }

    public WhatsappPush getWhatsappPush() {
        if (whatsappPush == null) {
            whatsappPush = new WhatsappPush();
        }
        return whatsappPush;
    }

}
