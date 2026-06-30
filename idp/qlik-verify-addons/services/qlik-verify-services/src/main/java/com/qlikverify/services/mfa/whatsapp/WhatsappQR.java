package com.qlikverify.services.mfa.whatsapp;

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
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.common.util.Base64;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.recovery.MFARecovery;
import org.keycloak.models.MFAConfigModel;
import org.keycloak.models.SmsConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
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

import static com.qlikverify.services.constants.Constants.ERROR_FTL;
import static com.qlikverify.services.constants.Constants.ERROR_MESSAGE;
import static org.keycloak.authentication.authenticators.utils.GeneralHelper.redirectToErrorPage;


public class WhatsappQR extends MFARecovery implements MFAProvider {
    private static final Logger logger = Logger.getLogger(WhatsappQR.class);
    private final String id;

    public WhatsappQR(String id) {
        this.id = id;
    }

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WhatsappQR:PreLogin");
        showQRView(configModel, context, null, true, false);
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WhatsappQR:PostLogin");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst("cancel") != null) {
            context.failure();
            return false;
        }

        if (formData.getFirst("retry") != null) {
            showQRView(configModel, context, null, true, false);
            return false;
        }

        String expiry = context.getAuthenticationSession().getAuthNote("EXPIRY");
        if (System.currentTimeMillis() > Long.parseLong(expiry)) {
            redirectToError(context, "Session Expired, try again");
            return false;
        }

        if (formData.getFirst("hookdata") != null) {
            try {
                String phoneNumber = null;
                try {
                    phoneNumber = getPhoneNumber(context);
                } catch (SmsException e) {
                    logger.error(e.getMessage());
                    redirectToErrorPage(context, e.getMessage());
                    return false;
                }

                HookRepresentation hookRepresentation = context.getSession().getProvider(HookService.class).findHook(
                        "whatsapp:" + phoneNumber);
                Map<String, List<String>> data = JsonSerialization.readValue(hookRepresentation.getHookResult(), Map.class);
                return processData(context, data, false);

            } catch (IOException e) {
                e.printStackTrace();
                showQRView(configModel, context, "Data Processing error", false, false);
                return false;
            }
        }

        showQRView(configModel, context, null, true, false);
        return false;
    }

    @Override
    public boolean onPostRegistration(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:WhatsappQR:PostRegistration");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.getFirst("cancel") != null) {
            context.failure();
            return false;
        }

        if (formData.getFirst("retry") != null) {
            showQRView(configModel, context, null, true, true);
            return false;
        }

        String expiry = context.getAuthenticationSession().getAuthNote("EXPIRY");
        if (System.currentTimeMillis() > Long.parseLong(expiry)) {
            redirectToError(context, "Session Expired, try again");
            return false;
        }

        if (formData.getFirst("hookdata") != null) {
            try {
                String phoneNumber = null;
                try {
                    phoneNumber = getPhoneNumber(context);
                } catch (SmsException e) {
                    logger.error(e.getMessage());
                    redirectToErrorPage(context, e.getMessage());
                    return false;
                }

                HookRepresentation hookRepresentation = context.getSession().getProvider(HookService.class).findHook(
                        "whatsapp:" + phoneNumber);
                Map<String, List<String>> data = JsonSerialization.readValue(hookRepresentation.getHookResult(), Map.class);
                return processData(context, data, true);
            } catch (IOException e) {
                e.printStackTrace();
                showQRView(configModel, context, "Data Processing error", false, true);
                return false;
            }
        }

        showQRView(configModel, context, null, true, true);
        return false;
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel, RequiredActionContext context, boolean canSkip) {
        logger.info("QlikVerify:MFA:WhatsappQR:PreRegistration");
        showQRView(configModel, context, null, true, true);
    }

    @Override
    public void close() {
        // NOP
    }

    private void showQRView(MFAConfigModel configModel, RequiredActionContext context, String error, boolean retry,
                            boolean isReg) {
        try {
            String verificationExpiryDuration = GeneralHelper.getConfigValue(configModel, "SESSION_EXPIRY", "5m");
            if (verificationExpiryDuration == null || verificationExpiryDuration.isEmpty()) {
                verificationExpiryDuration = "5m";
            }

            String expiry = context.getAuthenticationSession().getAuthNote("EXPIRY");
            if (expiry == null || retry) {
                expiry = String.valueOf(
                        GeneralHelper.getExpiryDuration(verificationExpiryDuration));
            }

            String otp = context.getAuthenticationSession().getAuthNote("OTP");
            if (otp == null || retry) {
                otp = GeneralHelper.getOtp(6);
            }

            String chatCode = context.getAuthenticationSession().getAuthNote("CHATCODE");
            if (chatCode == null) {
                chatCode = UUID.randomUUID().toString();
            }

            showQRView(configModel, context, error, "whatsapp-mfa-qr.ftl", expiry, otp, chatCode, retry, isReg);
        } catch (Exception err) {
            err.printStackTrace();
            redirectToError(context, "Something went wrong, try again");
        }

    }

    private SmsSenderProvider getSmsProvider(RequiredActionContext context) {
        return context.getSession().getProvider(SmsSenderProvider.class, "default");
    }

    private String getPhoneNumber(RequiredActionContext context) throws SmsException {
        UserModel userModel = context.getUser();
        NumberFormat numberFormat = getSmsProvider(context).parseNumber(userModel.getMobileNumber(),
                userModel.getMobileNumberRegion().toUpperCase());
        return numberFormat.getE16Format();
    }

    private void showQRView(MFAConfigModel configModel, RequiredActionContext context, String error, String ftl,
                            String expiry, String otp, String chatCode, boolean retry, boolean isReg) {
        SmsConfigModel hasWhatsAppProvider = GeneralHelper.getWhatsappSender(context);
        if (hasWhatsAppProvider == null){
            GeneralHelper.handleConfigurationError(context,
                    EventType.WHATSAPP_CONFIGURATION_ERROR, ERROR_FTL,
                    "Whatsapp" +
                    " configuration is missing for the selected authentication flow");
            return;
        }

        String whatsappLink = "https://wa.me/";
        String whatsappNumber = hasWhatsAppProvider.getConfig().get("from");
        String qrCode = qrCode(whatsappLink + whatsappNumber + "?text=" + otp);

        context.getAuthenticationSession().setAuthNote("EXPIRY", expiry);
        context.getAuthenticationSession().setAuthNote("OTP", otp);
        context.getAuthenticationSession().setAuthNote("CHATCODE", chatCode);

        String phoneNumber = null;
        try {
            UserModel userModel = context.getUser();
            if(userModel.getMobileNumber() == null){
                logger.error("No mobile number is associated with this user. Please contact admin.");
                redirectToError(context, "no-mobile-number");
                return;
            }
            phoneNumber = getPhoneNumber(context);
        } catch (SmsException e) {
            logger.error(e.getMessage());
            redirectToError(context, e.getMessage());
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

        if (!isReg) {
            formsProvider.setAttribute("showTroubleShoot", isMFARecoveryEnabled(context, configModel));
        }

        Response challenge = formsProvider
                .setAttribute("title", isReg ? "whatsapp-qr-two-factor-reg" : "whatsapp-qr-two-factor-verify")
                .setAttribute("socketUrl", EnvironmentReader.getValue("SOCKET_URL"))
                .setAttribute("mfaProviderId", id)
                .setAttribute("pageType", "mfa")
                .createForm(ftl);
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

    private boolean processData(RequiredActionContext context, Map<String, List<String>> data,
                                boolean isFromRegistration) {
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        String userPhone = data.get("From").get(0);
        String receivedSecretCode = data.get("Body").get(0);

        if (userPhone == null) {
            redirectToError(context, "phone-num-missing");
            return false;
        }

        if (receivedSecretCode == null || receivedSecretCode.isEmpty()) {
            redirectToError(context, "secret-code-missing");
            return false;
        }

        String secretCode = context.getAuthenticationSession().getAuthNote("OTP");
        if (!(secretCode != null && secretCode.equals(receivedSecretCode))) {
            if (context.getRealm().getMFAConfigByProviderId("whatsapp-mfa") != null) {
                showQRView(context.getRealm().getMFAConfigByProviderId("whatsapp-mfa"), context, "Invalid Secret code" +
                        ". Please use this new secret code or scan the above QR", true, isFromRegistration);
                return false;
            }
            redirectToError(context, "invalid-secret-code-try-again");
            return false;
        }
        NumberFormat numberFormat = null;
        try {
            numberFormat = smsSenderProvider.parseNumber(userPhone.split(":")[1], null);
        } catch (SmsException e) {
            e.printStackTrace();
            redirectToError(context, ErrorMappings.PHONE_NUMBER_INVALID);
            return false;
        }

        if (numberFormat.getE16Format() == null || numberFormat.getE16Format().equalsIgnoreCase("invalid")) {
            redirectToError(context, ErrorMappings.PHONE_NUMBER_INVALID);
            return false;
        }

        String phoneNumber = numberFormat.getE16Format().substring(numberFormat.getCountryCode().length() + 1);

        if (!context.getUser().getMobileNumber().equalsIgnoreCase(phoneNumber)) {
            redirectToError(context, ErrorMappings.PHONE_NUMBER_NOT_REGISTERED);
            return false;
        }
        context.getUser().setSingleAttribute(Constants.ATTRIBUTE_PHONE_VERIFIED, "TRUE");
        if (isFromRegistration) {
            OTPCredentialModel otpCredentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), "test",
                    context.getAuthenticationSession().getClient().getClientId());
            otpCredentialModel.setProviderId(id);
            otpCredentialModel.setClientId(context.getAuthenticationSession().getClient().getClientId());
            OTPCredentialProvider otpCredentialProvider =
                    (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
            otpCredentialProvider.createCredential(context.getRealm(), context.getUser(), otpCredentialModel);
        }
        return true;
    }

    private void redirectToError(RequiredActionContext context, String message) {
        Response challenge = context.form()
                .setAttribute(ERROR_MESSAGE, message)
                .createForm(ERROR_FTL);
        context.challenge(challenge);
    }

}
