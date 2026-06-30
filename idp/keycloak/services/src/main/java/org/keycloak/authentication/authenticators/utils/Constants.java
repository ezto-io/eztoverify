package org.keycloak.authentication.authenticators.utils;

import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;

public class Constants {

    public static final String FORM_PHONE_NUMBER = "phoneNumber";
    public static final String PHONE_NUMBER_REGION = "phoneNumberRegion";
    public static final String PHONE_NUMBER_WITH_COUNTRY_CODE = "phoneNumberWithCountryCode";

    //FTL Names
    public static final String MFA_OPTIONS_FTL = "mfa-options.ftl";
    public static final String ADD_EMAIL_FORM_FTL = "add-email-form.ftl";
    public static final String ADD_EMAIL_VER_FTL = "add-email-ver.ftl";
    public static final String MFA_LOGIN_OTP_FTL = "mfa-login-otp.ftl";
    public static final String ONBOARD_EMAIL_FORM_FTL = "onboarding-email-form.ftl";
    public static final String LOGIN_RESET_PASSWORD_FTL = "login-reset-password-form.ftl";
    public static final String UPDATE_PROFILE_FTL = "login-update-profile.ftl";
    public static final String RESET_PASSWORD_EMAIL_LINK_SENT_FTL = "reset-password-email-link-sent.ftl";
    public static final String OTP_VERIFICATION_FTL = "otp-verify.ftl";

    //Interaction Const
    public static final String FORM_OTP = "otp";
    public static final String FORM_EDIT = "edit";
    public static final String FORM_RESEND ="resendOTP";
    public static final String INVALID_OTP = "Invalid OTP";
    public static final String SESSION_MOBILE_OTP = "SESSION_MOBILE_OTP";
    public static final String SESSION_EMAIL_OTP = "SESSION_EMAIL_OTP";
    public static final String SESSION_MOBILE_OTP_EXPIRY = "SESSION_MOBILE_OTP_EXPIRY";
    public static final String SESSION_EMAIL_OTP_EXPIRY = "SESSION_EMAIL_OTP_EXPIRY";
    public static final String VERIFICATION_EXPIRY_CONFIG = "verify-expiry-time";
    public static final String EMAIL_VER_EXPIRY_CONFIG = "email-verify-expiry-time";
    public static final String MOB_VER_EXPIRY_CONFIG = "mob-verify-expiry-time";
    public static final String VER_SUCCESS_TITLE= "verSuccessTitle";
    public static final String VER_SUCCESS_MSG= "verSuccessMsg";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ATTRIBUTE_OLD_PHONE_NUMBER = "oldPhone";
    public static final String ATTRIBUTE_PHONE_NUMBER = "phoneNumber";
    public static final String ATTRIBUTE_PHONE_VERIFIED = "PhoneNumberVerified";
    public static final String EXPIRY_TIME_ATTRIBUTE = "expiry";
    public static final String DEFAULT_MESSAGE_TXT="Your Verification Code is: {code}";
    public static final String PASS_POLICY="passwordPolicy";
    public static final String PASS_POLICY_LABEL="Password Policy";
    public static final String EMAIL_OPTION_CONFIG = "email-option";
    public static final String EMAIL_SENDER="email-sender";
    public static final String SMS_SENDER="sms-sender";
    public static final String WHATSAPP_SENDER="whatsapp-sender";
    public static final String OTP = "otp";
    public static final String MAGIC_LINK = "magic_link";
    public static final String WEBAUTHN_PASSWORDLESS = "webauthn_passwordless";
    public static final String USER_INVITE = "user_invite";
    public static final String EMAIL_SENDER_LABEL="Email Sender";
    public static final String SMS_SENDER_LABEL="SMS Sender";
    public static final String WHATSAPP_SENDER_LABEL="whatsapp-sender-label";
    public static final String EMAIL_VER_EXPIRY_LABEL="Email Verification Expiry Duration";
    public static final String EMAIL_OTP="emailOtp";
    public static final String EMAIL_OTP_LABEL="Use Email OTP";
    public static final String EMAIL_ML="magic_link";
    public static final String EMAIL_ML_LABEL="Use Email Magic Link";
    public static final String MOB_OTP="mobileOtp";
    public static final String MOB_OTP_LABEL="Use Phone OTP";
    public static final String PASSWORD="password";
    public static final String PASSWORD_LABEL="Use Password";
    public static final String OTP_VER_EXPIRY_LABEL="OTP Verification Expiry Duration";
    public static final String EMAIL_EXP_LABEL="Email Verification Expiry Duration";
    public static final String MOB_EXP_LABEL="Phone Verification Expiry Duration";
    public static final String EMAIL_VER_OTP_LENGTH="email_ver_otp_len";
    public static final String EMAIL_VER_OTP_LENGTH_LABEL="Email Verification OTP Length";
    public static final String PHONE_VER_OTP_LENGTH="sms_ver_otp_len";
    public static final String PHONE_VER_OTP_LENGTH_LABEL="Phone Verification OTP Length";
    public static final String WHAT_VER_OTP_LENGTH_LABEL="OTP Length";
    public static final String PHONE_VER_OTP_TXT="sms_ver_otp_txt";


    //Error mappings
    public static final String EMAIL_REGISTERED = "email-registered-error";
    public static final String EMAIL_NOT_REGISTERED = "email-not-registered-error";
    public static final String EMAIL_PROVIDER_NOT_CONFIGURED="email-provider-not-configured";
    public static final String USER_ONBOARD_FAILED="something-went-wrong-in-creating-user";
    public static final String PHONE_ALREADY_REGISTERED = "phone-already-registered-error";
    public static final String PHONE_NUMBER_NOT_REGISTERED = "phone-number-not-registered-error";
    public static final String PHONE_NUMBER_REQUIRED = "phone-number-required-error";
    public static final String PHONE_NUMBER_INVALID = "phone-number-invalid-error";
    public static final String PHONE_NUMBER_NOT_VERIFIED = "phone-number-not-verified-error";

    //OTP errors
    public static final String FAILED_TO_SEND_RESET_EMAIL="failed-to-send-reset-email";
    public static final String FAILED_TO_SEND_EMAIL_OTP="failed-to-send-email-check-email-address";
    public static final String FAILED_TO_SEND_SMS_OTP = "failed-to-send-sms-check-number";
    public static final String WRONG_OTP_ERROR = "wrong-otp-error";
    public static final String OTP_EXPIRED = "otp-expired-error";
    public static final String EMAIL_REQUIRED = "email-required-error";
    public static final String EMAIL_INVALID = "email-invalid-error";
    public static final String USER_EXISTS_SAME_EMAIL="user-exists-same-email-error";
    public static final String INVALID_BRAND_NAME = "Failed to create Branding with invalid name. Check Audit > Admin Logs for further information";
    public static final String HTML_INJECTED_IN_FIELDS = "HTML has been injected in the input field(s). Enter a valid input.";
    public static final String VALIDATOR_NAME_ALREADY_EXISTS = "Validator name already exists";
    public static final String INVALID_VALIDATOR_NAME = "invalid-validator-name";
    public static final String INVALID_POLICY_NAME = "Failed to create. Enter a valid name";
    public static final String INVALID_ACTION_NAME = "Failed to create template with invalid name. Check Audit > Admin Logs for further information";
    public static final String WORKFLOW_NAME_EXISTS = "workflow-name-exists";
    public static final String INVALID_CONFIG_INPUT = "invalid-input-config";
    public static final String INVALID_APP_NAME = "Failed to create application with invalid name. Check Audit > Admin Logs for further information";
    public static final String EMAIL_SEND_FAILED = "Failed to send email. Please contact admin";
    public static final String GIF_ID = "id";
    public static final String GIF_URL = "gifUrl";
    public static final String WEB_TO_DEVICE = "bo-web-to-device";
    public static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);
    public static final String TEMPLATE_PATH = "/templates/";
    public static final String CIBA_NOTIFY = "ciba-notify";
    public static final String HOOK_WAY = "hook-way";
    public static final String AUTH_WAY_MESSAGE = "auth_way_message";
    public static final String RESULT_DELIVERY_MODE_MESSAGE = "result_delivery_mode_message";
    public static final String TEST_CONFIG = "test_config";
    public static final String HOOK_POLL = "hook@poll";
    public static final String HOOK_PUSH = "hook@push";
    public static final String HOOK_PING = "hook@ping";
    public static final String HOOK_MOBILE_SDK = "hook@mobile_sdk";
    public static final String QUEUE_KAFKA = "queue@kafka";
    public static final String TWILIO_IVR_CIBA_AUTHORIZER = "twilio-ivr-ciba-authorizer";
    public static final String CIBA_DEFAULT_MANAGED_AUTH = "ciba-default-managed-auth";
    public static final String CIBA_DESKTOP_SDK = "ciba-desktop-sdk";
    public static final String CIBA_MOBILE_SDK = "ciba-mobile-sdk";
    public static final String TRANSACTION_LINK = "transaction_link";
    public static final String AUTH_WAY_LINK_MSG = "auth-way-link-msg";
    public static final String AUTH_WAY_DESKTOP_MSG = "auth-way-desktop-msg";
    public static final String AUTH_WAY_EMAIL_MSG = "auth-way-email-msg";
    public static final String AUTH_WAY_SMS_MSG = "auth-way-sms-msg";
    public static final String AUTH_WAY_WHATSAPP_MSG = "auth-way-whatsapp-msg";
    public static final String AUTH_WAY_QR_MSG = "auth-way-qr-msg";
    public static final String AUTH_WAY_MOBILE_MSG = "auth-way-mobile-msg";
    public static final String AUTH_WAY_IVR_MSG = "auth-way-ivr-msg";
    public static final String PUSH_WAY = "push-way";
    public static final String AZURE_B2C_CIBA_AUTHORIZER = "azure-b2c-ciba-authorizer";
    public static final String HOOK_NOTIFIER = "hook-notifier";
    public static final String NOTIFICATION_WAY_POLL_MSG = "notification-way-poll-msg";
    public static final String NOTIFICATION_WAY_PUSH_MSG = "notification-way-push-msg";
    public static final String NOTIFICATION_WAY_PING_MSG = "notification-way-ping-msg";
    public static final String  NOTIFICATION_WAY_MOBILE_MSG = "notification-way-mobile-msg";
    public static final String NOTIFICATION_WAY_KAFKA_MSG = "notification-way-kafka-msg";
    public static final String SDK = "SDK";
    public static final String POLL = "poll";
    public static final String PING = "ping";
    public static final String PUSH = "push";
    public static final String EMAIL = "Email";
    public static final String SMS = "Sms";
    public static final String WHATSAPP = "Whatsapp";
    public static final String LINK = "Link";
    public static final String QR = "QR";
    public static final String TERMS_AND_CONDITONS_VERSION = "terms_and_condition_ver";
    public static final String TERMS_AND_CONDITIONS_TYPE = "terms_and_condition_type";
    public static final String INTEGRATION_TYPE = "integration-type";

    // marketplace - verify
    public static final String WORKFLOW_NAME = "workflowName";
    public static final String WORKFLOW_DESCRIPTION = "workflowDescription";
    public static final String IMAGE_URL = "imageURL";
    public static final String WORKFLOW_CATEGORY = "workflowCategory";
    public static final String TEMPLATE_TAG = "templateTag";
    public static final String COUNTRY = "country";
    public static final String WORKFLOW_JSON = "workflowJson";
    public static final String WORKFLOW_PUBLISHED = "isPublished";

}
