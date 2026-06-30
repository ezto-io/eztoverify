package com.qlikverify.services.authenticators.misc.whatsapp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.qlikverify.hook.spi.HookRepresentation;
import com.qlikverify.hook.spi.HookService;
import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.util.Base64;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.SmsConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sms.SmsException;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.models.NumberFormat;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.qlikverify.services.constants.Constants.*;
import static com.qlikverify.services.helpers.GeneralHelper.redirectToError;


public class WhatsappQR implements AuthProvider {
    private String registationTitle = "whatsapp-qr-reg-title";
    private String loginTitle = "whatsapp-qr-login-title";

    private static final Logger logger = Logger.getLogger(WhatsappQR.class);

    @Override
    public void onPreLogin(AuthenticationFlowContext context) {
        showQRView( context, null, true, false);
    }

    @Override
    public void onPostLogin(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst("cancel") != null) {
            context.cancelLogin();
            return;
        }

        if (formData.getFirst("retry") != null) {
            showQRView( context, null, true, false);
            return;
        }

        String expiry = context.getAuthenticationSession().getAuthNote(EXPIRY);
        if (System.currentTimeMillis() > Long.parseLong(expiry)) {
            GeneralHelper.redirectToErrorPage(context, "session-expired-error");
            return;
        }

        if (formData.getFirst("hookdata") != null) {
            try {
                String phoneNumber = null;
                try {
                    phoneNumber = getPhoneNumber(context);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    GeneralHelper.handleConfigurationError(context, EventType.WHATSAPP_CONFIGURATION_ERROR, ERROR_FTL, e.getMessage());
                    return;
                }

                HookRepresentation hookRepresentation = context.getSession().getProvider(HookService.class).findHook("whatsapp:" + phoneNumber);
                Map<String, List<String>> data = JsonSerialization.readValue(hookRepresentation.getHookResult(), Map.class);
                processData(context, data, false);
                context.getSession().getProvider(HookService.class).deleteHook(hookRepresentation.getChatCode(),context.getRealm().getId());
                return;
            } catch (IOException e) {
                logger.error(e.getMessage());
                showQRView( context, "data-processing-error", false, false);
            }
        }

        showQRView( context, null, true, false);
    }

    @Override
    public void onPostRegistration(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst("cancel") != null) {
            context.cancelLogin();
            return;
        }

        if (formData.getFirst("retry") != null) {
            showQRView( context, null, true, true);
            return;
        }

        String expiry = context.getAuthenticationSession().getAuthNote(EXPIRY);
        if (System.currentTimeMillis() > Long.parseLong(expiry)) {
            GeneralHelper.redirectToErrorPage(context, "session-expired-error");
            return;
        }

        if (formData.getFirst("hookdata") != null) {
            try {
                String phoneNumber = null;
                try {
                    phoneNumber = getPhoneNumber(context);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    GeneralHelper.handleConfigurationError(context, EventType.WHATSAPP_CONFIGURATION_ERROR, ERROR_FTL, e.getMessage());
                    return;
                }

                HookRepresentation hookRepresentation = context.getSession().getProvider(HookService.class).findHook("whatsapp:" + phoneNumber);
                Map<String, List<String>> data = JsonSerialization.readValue(hookRepresentation.getHookResult(), Map.class);
                processData(context, data, true);
                context.getSession().getProvider(HookService.class).deleteHook(hookRepresentation.getChatCode(),context.getRealm().getId());
                return;
            } catch (IOException e) {
                logger.error(e.getMessage());
                showQRView( context, "data-processing-error", false, true);
            }
        }

        showQRView(context, null, true, true);
    }

    @Override
    public void onPreRegistration(AuthenticationFlowContext context) {
        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        showQRView( context, null, true, true);
    }

    private void showQRView(AuthenticationFlowContext context, String error, boolean retry, boolean fromReg) {
        String verificationExpiry = GeneralHelper.getConfigValue(context, "SESSION_EXPIRY", "5m");
        String expiry             = context.getAuthenticationSession().getAuthNote(EXPIRY);
        String otp = null;
        String chatCode           = context.getAuthenticationSession().getAuthNote("CHATCODE");
        if(chatCode != null){
                context.getSession().getProvider(HookService.class).deleteHook(chatCode,context.getRealm().getId());
        }
        chatCode = null;
        if(verificationExpiry == null || verificationExpiry.isEmpty()) {
            verificationExpiry = "5m";
        }
        if (expiry == null || retry) {
            expiry = String.valueOf(GeneralHelper.getExpiryDuration(verificationExpiry));
        }
        if (otp == null || retry) {
            otp = GeneralHelper.getOtp(6);
        }
        if (chatCode == null) {
            chatCode = UUID.randomUUID().toString();
        }
        showQRView(context, error, "whatsapp-mfa-qr.ftl", expiry, otp, chatCode, retry, fromReg);
    }

    private SmsSenderProvider getSmsProvider(AuthenticationFlowContext context) {
        return context.getSession().getProvider(SmsSenderProvider.class, "default");
    }

    private String getPhoneNumber(AuthenticationFlowContext context) throws Exception {
        UserModel userModel = context.getUser();
        NumberFormat numberFormat;

        if (userModel == null) {
            if(context.getAuthenticationSession().getAuthNote(UserModel.MOBILE_NUMBER) != null && context.getAuthenticationSession().getAuthNote(UserModel.MOBILE_NUMBER_REGION) != null) {
                numberFormat = getSmsProvider(context).parseNumber(context.getAuthenticationSession().getAuthNote(UserModel.MOBILE_NUMBER), context.getAuthenticationSession().getAuthNote(UserModel.MOBILE_NUMBER_REGION).toUpperCase());
            } else {
                throw new Exception("Phone number not configured");
            }
        } else {
            numberFormat = getSmsProvider(context).parseNumber(userModel.getMobileNumber(),
                    userModel.getMobileNumberRegion().toUpperCase());
        }
        return numberFormat.getE16Format();
    }

    private void showQRView(AuthenticationFlowContext context, String error, String ftl, String expiry, String otp, String chatCode, boolean retry, boolean fromReg) {
        SmsConfigModel hasWhatsAppProvider = GeneralHelper.getWhatsappSender(context);
        if (hasWhatsAppProvider == null){
            GeneralHelper.handleConfigurationError(context,EventType.WHATSAPP_CONFIGURATION_ERROR, ERROR_FTL, "whatsapp-configuration-missing-for-selected-flow");
            return;
        }

        String whatsappLink     = "https://wa.me/";
        String whatsappNumber   = hasWhatsAppProvider.getConfig().get("from");
        String qrCode           = qrCode(whatsappLink + whatsappNumber + "?text=" + otp);

        context.getAuthenticationSession().setAuthNote(EXPIRY, expiry);
        context.getAuthenticationSession().setAuthNote("OTP", otp);
        context.getAuthenticationSession().setAuthNote("CHATCODE", chatCode);

        String phoneNumber = null;
        try {
            phoneNumber = getPhoneNumber(context);
        } catch (Exception e) {
            logger.error(e.getMessage());
            GeneralHelper.handleConfigurationError(context, EventType.WHATSAPP_CONFIGURATION_ERROR, ERROR_FTL, e.getMessage());
            return;
        }

        if (retry) {
            String finalPhoneNumber = phoneNumber;
            HookRepresentation hookRepresentation = new HookRepresentation();
            hookRepresentation.setRealmId(context.getRealm().getId());
            hookRepresentation.setKey("whatsapp:" + finalPhoneNumber);
            hookRepresentation.setChatCode(chatCode);
            context.getSession().getProvider(HookService.class).addHook(hookRepresentation);
        }

        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }

        formsProvider.setAttribute("expiry", String.valueOf(expiry));
        formsProvider.setAttribute("secretcode", otp);
        formsProvider.setAttribute("qrcode", qrCode);
        formsProvider.setAttribute("chatcode", chatCode);
        formsProvider.setAttribute("isFromReg",fromReg);

        Response challenge = formsProvider
                .setAttribute("socketUrl", EnvironmentReader.getValue("SOCKET_URL"))
                .setAttribute("title", fromReg ? registationTitle : loginTitle).createForm(ftl);
        context.challenge(challenge);
    }

    private String qrCode(String text) {
        try {
            int width = 512;
            int height = 512;

            QRCodeWriter writer = new QRCodeWriter();
            final BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
            bos.close();

            return Base64.encodeBytes(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void processData(AuthenticationFlowContext context, Map<String, List<String>> data, boolean isFromRegistration) {
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        String userPhone            = data.get("From").get(0);
        String userProfile          = data.get("ProfileName").get(0);
        String receivedSecretCode   = data.get("Body").get(0);

        if (userPhone == null) {
            showQRView(context, "phone-num-missing",true,isFromRegistration);
            return;
        }

        if (receivedSecretCode == null || receivedSecretCode.isEmpty()) {
            showQRView(context, "secret-code-missing",true,isFromRegistration);
            return;
        }

        String secretCode = context.getAuthenticationSession().getAuthNote("OTP");
        if (!(secretCode != null && secretCode.equals(receivedSecretCode))) {
            showQRView(context, "invalid-secret-code-msg",true,
                    isFromRegistration);
            return;
        }
        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(userPhone.split(":")[1], null);
        } catch (SmsException e) {
            e.printStackTrace();
            showQRView(context, ErrorMappings.PHONE_NUMBER_INVALID,true,isFromRegistration);
            return;
        }

        if (numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")) {
            showQRView(context, ErrorMappings.PHONE_NUMBER_INVALID,true,isFromRegistration);
            return;
        }

        String phoneNumber  = numberFormat.getE16Format().substring(numberFormat.getCountryCode().length() + 1);
        String mobNum       = numberFormat.getRegion().toLowerCase() + phoneNumber;
        UserModel userModel = context.getSession().users().getUserByMobile(mobNum, context.getRealm());

        if (userModel == null && !isFromRegistration) {
            showQRView(context, ErrorMappings.PHONE_NUMBER_NOT_REGISTERED,true,isFromRegistration);
            return;
        }

        //check user was locked by bruteforce
        if(userModel != null && GeneralHelper.isTemporarilyDisabledByBruteForce(context,userModel,USER_DISABLED_BRUTEFORCE)){
            context.getSession().getProvider(HookService.class).deleteHook(context.getSession().getProvider(HookService.class).findHook(userPhone).getChatCode(),context.getRealm().getId());
            return;
        }

        context.getSession().getProvider(HookService.class).deleteHook(context.getSession().getProvider(HookService.class).findHook(userPhone).getChatCode(),context.getRealm().getId());

        if (isFromRegistration) {
            if(userModel != null) {
                showQRView(context, ErrorMappings.PHONE_ALREADY_REGISTERED,true,isFromRegistration);
                return;
            }
            UserModel user = context.getSession().users().addUser(context.getRealm(), phoneNumber);
            user.setFirstName(userProfile);
            user.setEnabled(true);
            user.setSingleAttribute(UserModel.MOBILE_NUMBER_REGION, numberFormat.getRegion().toLowerCase());
            user.setSingleAttribute(UserModel.MOBILE_NUMBER,phoneNumber);
            user.setSingleAttribute(ATTRIBUTE_PHONE_VERIFIED, "TRUE");
            context.setUser(user);
        } else {
            context.setUser(userModel);
            context.getUser().setSingleAttribute(Constants.ATTRIBUTE_PHONE_VERIFIED, "TRUE");
        }
        context.success();
    }

}
