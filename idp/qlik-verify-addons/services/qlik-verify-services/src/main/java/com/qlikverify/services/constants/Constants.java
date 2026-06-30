package com.qlikverify.services.constants;

public class Constants {

    // general
    public static final String OK = "OK";
    public static final String TRUE="true";
    public static final String ACCESS_DENIED = "Access Denied";

    public static final String USER_INVALID = "user-invalid-error";
    public static final String USER_DISABLED = "user-disabled-error";
    public static final String USER_DISABLED_BRUTEFORCE = "user-disabled-bruteforce";

    public static final String ATTRIBUTE_OLD_PHONE_NUMBER = "oldPhone";
    public static final String ATTRIBUTE_PHONE_NUMBER = "phoneNumber";
    public static final String ATTRIBUTE_PHONE_VERIFIED = "PhoneNumberVerified";
    public static final String ATTRIBUTE_POLICY = "policy";

    public static final String FORM_EDIT = "edit";
    public static final String FORM_RESEND = "resendOTP";
    public static final String FORM_IS_PHONE_REGISTRATION = "isPhoneNumberRegistration";
    public static final String FORM_IS_OTP_VERIFICATION = "isOtpVerification";
    public static final String FORM_IS_EMAIL_UPDATE = "isEmailUpdate";
    public static final String FORM_OTP = "otp";
    public static final String FORM_PHONE_NUMBER = "phoneNumber";
    public static final String PHONE_NUMBER_REGION = "phoneNumberRegion";
    public static final String PHONE_NUMBER_WITH_COUNTRY_CODE = "phoneNumberWithCountryCode";
    public static final String FORM_PASSWORD = "password";
    public static final String FORM_CONFIRM_PASSWORD = "confirmPassword";
    public static final String FORM_IS_LOGIN_PASSWORD = "isLoginPasswordForm";
    public static final String FORM_IS_LOGIN_OTP = "isLoginOtpForm";
    public static final String FORM_IS_RESET_OTP = "isResetOtpForm";

    public static final String ERROR = "error";
    public static final String NO_SMS_CONFIG = "sms-config-not-available-setup-under-sms-section";

    public static  final String CONTACT_ADMIN = "error-occured-contact-admin";

    public static final String MAGIC_LINK_ACTION = "isMagicLink";
    public static final String OLD_EMAIL = "oldEmail";
    public static final String EMAIL = "email";
    public static final String FORM_IS_EMAIL_REGISTRATION = "isEmailRegistration";
    public static final String LOGIN_WITH = "loginWith";
    public static final String LOGIN_VALUE = "loginValue";
    public static final String IS_EMAIL_OTP_LOGIN = "emailOtpLoginOption";
    public static final String IS_EMAIL_MAGIC_LINK_LOGIN = "emailMagicLinkLoginOption";
    public static final String IS_MOBILE_OTP_LOGIN = "mobileOtpLoginOption";
    public static final String IS_EMAIL_MOBILE_OTP = "emailPhoneLoginOption";
    public static final String IS_PASSWORD_LOGIN = "passwordLoginOption";
    public static final String RP_WITH_EMAIL_OTP = "emailOtpReset";
    public static final String RP_WITH_EMAIL_LINK= "emailLinkReset";
    public static final String RP_WITH_MOB_NUM= "mobOtpReset";

    public static final String USERNAME = "username";
    public static final String PHONE_NUMBER = "phoneNumber";

    public static final String ADDITIONAL_INFO_TEXT = "additionalText";
    public static final String ADDITIONAL_INFO_TEXT_EMAIL_OTP = "otp-sent-to-registered-email";
    public static final String ADDITIONAL_INFO_TEXT_MOBILE_OTP = "otp-sent-to-registered-phone";

    public static final String SESSION_MOBILE_OTP = "SESSION_MOBILE_OTP";
    public static final String SESSION_EMAIL_OTP = "SESSION_EMAIL_OTP";
    public static final String SESSION_MOBILE_OTP_EXPIRY = "SESSION_MOBILE_OTP_EXPIRY";
    public static final String SESSION_EMAIL_OTP_EXPIRY = "SESSION_EMAIL_OTP_EXPIRY";
    public static final String SESSION_EMAIL_MAGIC_LINK_EXPIRY = "SESSION_EMAIL_MAGIC_LINK_EXPIRY";
    public static final String SESSION_EMAIL_MOBILE_OTP = "SESSION_EMAIL_MOBILE_OTP";
    public static final String SESSION_EMAIL_MOBILE_OTP_EXPIRY = "SESSION_EMAIL_MOBILE_OTP_EXPIRY";
    public static final String VERIFICATION_EXPIRY_CONFIG = "verify-expiry-time";
    public static final String EMAIL_VER_EXPIRY_CONFIG = "email-verify-expiry-time";
    public static final String MOB_VER_EXPIRY_CONFIG = "mob-verify-expiry-time";
    public static final String VER_SUCCESS_TITLE= "verSuccessTitle";
    public static final String VER_SUCCESS_MSG= "verSuccessMsg";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String EXPIRY_TIME_ATTRIBUTE = "expiry";
    public static final String DEFAULT_MESSAGE_TXT="Your Verification Code is: {code}";

    //Success Messages
    public static final String EMAIL_VER_SUCCESS_TITLE="email-verifies-txt";
    public static final String EMAIL_VER_SUCCESS_MSG="email-verified-successfully-login-to-application";

    //Authenticator FTLS
    public static final String EMAIL_REGISTRATION_FTL = "email-registration.ftl";
    public static final String EMAIL_FORM_FTL = "email-form.ftl";
    public static final String PHONE_NUMBER_FORM_FTL = "phone-number-form.ftl";
    public static final String MULTIPLE_LOGIN_OPTIONS_FTL = "multiple-login-options.ftl";
    public static final String PHONE_NUMBER_REGISTRATION_FTL = "phone-number-registration.ftl";
    public static final String OTP_VERIFICATION_FTL = "otp-verify.ftl";
    public static final String EMAIL_MFA_OTP_VERIFICATION_FTL = "email-mfa-verify.ftl";
    public static final String MFA_OTP_VERIFICATION_FTL = "phone-mfa-verify.ftl";
    public static final String PASSWORD_REGISTRATION_FTL = "password-register.ftl";
    public static final String ERROR_FTL = "error.ftl";
    public static final String QV_ERROR_FTL = "qv-error.ftl";
    public static final String USERNAME_REG_FTL = "username-registration.ftl";
    public static final String USERNAME_FTL = "username.ftl";
    public static final String LOGIN_PASSWORD_FTL = "login-password.ftl";
    public static final String MULTI_LOGIN_OTP_FTL = "multi-login-otp.ftl";
    public static final String VIEW_EMAIL_FTL = "view-email.ftl";
    public static final String VERIFICATION_PENDING_FTL="verification-pending.ftl";
    public static final String VERIFICATION_SUCCESS_FTL="verification-success.ftl";
    public static final String RESET_PASSWORD_FIND_USER_FTL="reset-password-find-user.ftl";
    public static final String RESET_PASSWORD_CHOOSE_WAY_FTL="reset-password-choose-way.ftl";
    public static final String RESET_PASSWORD_OTP_FTL = "reset-password-otp.ftl";
    public static final String RESET_PHONE_NUMBER_FTL = "reset-phone-number.ftl";
    public static final String RESET_PASSWORD_EMAIL_LINK_SENT_FTL = "reset-password-email-link-sent.ftl";
    public static final String RECAPTCHA_FTL = "recaptcha.ftl";
    public static final String UPDATE_EMAIL_FTL = "update-email.ftl";
    public static final String BO_VERIFY_EMAIL_FTL = "bo-verify-email.ftl";
    public static final String BO_VERIFY_PHONE_NUMBER_FTL = "bo-verify-phone.ftl";
    public static final String UPDATE_PHONE_NUMBER_FTL = "update-phone-number.ftl";

    //Config Names & Labels
    public static final String PASS_POLICY="passwordPolicy";
    public static final String PASS_POLICY_LABEL="password-policy-label";
    public static final String EMAIL_OPTION_CONFIG = "email-option";
    public static final String EMAIL_SENDER="email-sender";
    public static final String SMS_SENDER="sms-sender";
    public static final String WHATSAPP_SENDER="whatsapp-sender";
    public static final String OTP = "otp";
    public static final String MAGIC_LINK = "magic_link";
    public static final String WEBAUTHN_PASSWORDLESS = "webauthn_passwordless";
    public static final String USER_INVITE = "user_invite";
    public static final String SKIP = "skip";
    public static final String EMAIL_SENDER_LABEL="email-sender-label";
    public static final String SMS_SENDER_LABEL="sms-sender-label";
    public static final String WHATSAPP_SENDER_LABEL="whatsapp-sender-label";
    public static final String EMAIL_VER_EXPIRY_LABEL="email-config-expiry-duration";
    public static final String EMAIL_OTP="emailOtp";
    public static final String EMAIL_OTP_LABEL="use-email-otp-label";
    public static final String EMAIL_ML="magic_link";
    public static final String EMAIL_ML_LABEL="use-email-ml-label";
    public static final String MOB_OTP="mobileOtp";
    public static final String MOB_OTP_LABEL="use-phone-otp-label";
    public static final String EMAIL_PHONE_OTP = "emailPhoneOtp";
    public static final String EMAIL_PHONE_OTP_LABEL = "send-otp-email-mob";
    public static final String EMAIL_PHONE_OTP_LEN = "emailMobOtpLen";
    public static final String EMAIL_PHONE_EXP_TIME = "emailMobExp";
    public static final String PASSWORD="password";
    public static final String PASSWORD_LABEL="use-password-label";
    public static final String OTP_VER_EXPIRY_LABEL="otp-ver-expiry-label";
    public static final String EMAIL_EXP_LABEL="email-exp-label";
    public static final String MOB_EXP_LABEL="mob-exp-label";
    public static final String EMAIL_VER_OTP_LENGTH="email_ver_otp_len";
    public static final String OTP_LENGTH_ATTR = "otp_len";
    public static final String EMAIL_VER_OTP_LENGTH_LABEL="email-verify-otp-label";
    public static final String PHONE_VER_OTP_LENGTH="sms_ver_otp_len";
    public static final String PHONE_VER_OTP_LENGTH_LABEL="phone-verify-otp-label";
    public static final String WHAT_VER_OTP_LENGTH_LABEL="what-ver-otp-length-label";
    public static final String PHONE_VER_OTP_TXT="sms_ver_otp_txt";
    public static final String RP_USING_EMAIL_OTP="use-email-otp";
    public static final String RP_USING_EMAIL_OTP_LABEL="use-email-otp-label";
    public static final String RP_USING_EMAIL_OTP_HELP_TEXT="rp-using-email-otp-hlp-txt";
    public static final String RP_USING_EMAIL_LINK="use-email-link";
    public static final String RP_USING_EMAIL_LINK_LABEL="use-email-link-label";
    public static final String RP_USING_EMAIL_LINK_HELP_TEXT="use-email-link-hlp-txt";
    public static final String RP_USING_MOBILE_NUM="use-mob-otp";
    public static final String RP_USING_MOBILE_NUM_LABEL="use-phone-otp-label";
    public static final String RP_USING_MOBILE_NUM_HELP_TEXT="rp-using-mobile-num-hlp-txt";
    public static final String RP_USING_EMAIL_OR_MOB_EXP_LABEL="rp-using-email-or-mob-exp-label";
    public static final String EMAIL_PHONE_OTP_LOGIN="get-otp-email-mob-login";
    public static final String SAME_OTP_EMAIL_PHONE= "mul-use-same-otp";
    public static final String ENABLE_SAME_OTP_EMAIL_PHONE= "enable-to-use-same-otp";
    public static final String OTP_LENGTH="mul-otp-length";
    public static final String OTP_EXPIRY="mul-otp-expiry-duration";

    //Help Texts Constants
    public static final String DUO_MFA_HELP_TEXT="duoMfaHelpText";
    public static final String GOOGLE_MFA_HELP_TEXT="googleMfaHelpText";
    public static final String AUTHY_MFA_HELP_TEXT="authyMfaHelpText";
    public static final String FREEOTP_MFA_HELP_TEXT="freeOTPMfaHelpText";
    public static final String EMAIL_MFA_HELP_TEXT="emailMfaHelpText";
    public static final String PHONE_MFA_HELP_TEXT="phoneMfaHelpText";
    public static final String WHATAPP_MFA_HELP_TEXT="whatsappMfaHelpText";

    public static final String CIBA_POLICY="cibaPolicy";
    public static final String CIBA_POLICY_LABEL = "CIBA Policy";

    public static final String CIBA_NOTIFY = "ciba-notify";
    public static final String CIBA_EMAIL = "Email";
    public static final String CIBA_WHATSAPP = "Whatsapp";
    public static final String CIBA_SMS = "Sms";

    public static final String ATTRIBUTE_MESSAGE = "attrMessage";
    public static final String ATTRIBUTE_MESSAGE_TYPE = "messageType";
    public static final String ATTRIBUTE_MESSAGE_TITLE = "messageTitle";
    
    public static final String SKIP_USER_CREATION = "skipUserCreation";
    public static final String EMAIL_CONFIG_MISSING_ERROR = "email-config-missing-error";
    public static final String SMS_CONFIG_MISSING_ERROR = "sms-config-missing-error";
    public static final String CONTINUE = "continue";
    public static final String FALSE = "false";
    public static final String WHATSAPP_VERIFY_OPTIONS = "whatsapp_verify_options";
    public static final String RESEND_OTP = "resendOTP";
    public static final String EXPIRY = "EXPIRY";
    public static final String CONFIG_JSON = "config_json";
    public static final String CHOSEN_TYPE = "chosenType";
    public static final String ENABLE_AUTH = "ENABLE_AUTH";
    public static final String DIFF_OS_TRIGGER = "DIFF_OS_TRIGGER";
    public static final String DIFF_BROWSER_TRIGGER = "DIFF_BROWSER_TRIGGER";
    public static final String DIFF_IP_TRIGGER = "DIFF_IP_TRIGGER";
    public static final String ANOMALY_TRIIGER_COUNT = "ANOMALY_TRIIGER_COUNT";
    public static final String AUTHORIZATION = "AUTHORIZATION";
    public static final String AUTHENTICATION = "AUTHENTICATION";
    public static final String REPORT_TYPE = "report_type";
    public static final String SPLUNK_PORT = "SPLUNK_PORT";
    public static final String WEBHOOK_URL = "WEBHOOK_URL";
    public static final String MFA_OTP_REG_FTL = "mfa-otp-reg.ftl";
    public static final String APPLICATION_NAME = "applicationName";
    public static final String SOCKET_URL = "SOCKET_URL";
    public static final String CYLOCK_SERVER = "cylock_server";
    public static final String SIG_RESPONSE = "sig_response";
    public static final String GOOGLE_AUTHENTICATOR = "Google Authenticator";
    public static final String MFA_PROVIDER_ID = "mfaProviderId";
    public static final String EDIT_SUPPORTED_APPS = "edit_supported_apps";
    public static final String CUSTOM_SUBSCRIPTION = "custom_subscription";

    // help-desk
    public static final String SUPPORT = "SUPPORT";
    public static final String SUPPORT_URL = "support_url";
    public static final String SUPPORT_URL_HELPTEXT = "support_url_helptext";
    public static final String SUPPORT_EMAIL = "support_email";
    public static final String SUPPORT_CUSTOM_MESSAGE = "support_custom_message";
    public static final String SUPPORT_CUSTOM_MESSAGE_HELPTEXT = "support_custom_message_helptext";
    public static final String SUPPORT_EMAIL_HELPTEXT = "support_email_helptext";
    public static final String SUPPORT_CUSTOM_TOGGLE = "support_custom_toggle";
    public static final String SUPPORT_CUSTOM_HELPTEXT = "support_custom_toggle_helptext";

    // black-listing
    public static final String BLACKLISTING_ENABLED= "blackListingEnabled";
    public static final String BLACKLISTING_PROVIDER_ID = "black-listing-listener";
    public static final String BL_IP_ADDRESS = "ip-address";
    public static final String BL_EMAIL = "email";
    public static final String BL_PHONE = "phone";
    public static final String BL_USERNAME = "username";
    public static final String BL_COUNTRY = "bl-country";
    public static final String BL_CITY = "bl-city";
    public static final String BL_LOCATION = "bl-location";

}
