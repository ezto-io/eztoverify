package com.qlikverify.helpers.constants;

public class ErrorMappings {
    private ErrorMappings(){

    }
    public static final String ERROR = "error";
    public static final String SOMETHING_WENT_WRONG = "something-went-wrong-error";
    public static final String GENERAL_INSTRUCTION_MESSAGE = "general-instruction-message";
    public static final String FIRSTNAME_REQUIRED = "firstname-required";
    public static final String LASTNAME_REQUIRED = "lastname-required";
    public static final String FIRSTNAME_LASTNAME_REQUIRED = "firstname-lastname-required";
    public static final String ERROR_TYPE = "errorType";
    public static final String ENTER_OTP = "invalid-otp-error";

    public static final String USERNAME_REQUIRED = "username-required-error";
    public static final String TELEGRAM_ACCOUNT_REGISTERED = "telegram-account-registered";
    public static final String USERNAME_REGISTERED = "usename-registered";
    public static final String USERNAME_NOT_REGISTERED = "username-not-registered";
    public static final String USER_TELEGRAM_NOT_REGISTERED = "user-telegram-not-registered";
    public static final String WHATSAPP_NUMBER_REQUIRED = "whatsapp-number-required";
    public static final String WHATSAPP_NUMBER_ALREADY_REGISTERED = "whatsapp-number-already-registered";

    public static final String USERNAME_ALREADY_EXISTS_ERROR = "";

    public static final String MAXIMUM_ATTEMPTS_REACHED_USER_TEMP_DISABLED="max_attempts_reached_user_temp_disabled";
    public static final String INVALID_REQUEST="invalid-request-error";
    public static final String USER_REQUIRED = "user-required-error";
    public static final String AUTHENTICATORS_NULL = "authenticators-null";

    //Email Errors
    public static final String FAILED_TO_SEND_EMAIL="failed-to-send-email-error";
    public static final String MAGIC_LINK_EXPIRED="magic-link-expired-error";
    public static final String MAGIC_LINK_NOT_VALID="magic-link-not-valid-error";
    public static final String EMAIL_REGISTERED = "email-registered-error";
    public static final String EMAIL_NOT_REGISTERED = "email-not-registered-error";
    public static final String EMAIL_NOT_FOUND = "email-not-found";
    public static final String EMAIL_REQUIRED = "email-required-error";
    public static final String EMAIL_INVALID = "email-invalid-error";
    public static final String USER_EXISTS_SAME_EMAIL="user-exists-same-email-error";
    public static final String EMAIL_NOT_VERIFIED = "email-not-verified";

    //Phone Number Errors
    public static final String PHONE_ALREADY_REGISTERED = "phone-already-registered-error";
    public static final String PHONE_NUMBER_NOT_REGISTERED = "phone-number-not-registered-error";
    public static final String PHONE_NUMBER_REQUIRED = "phone-number-required-error";
    public static final String PHONE_NUMBER_INVALID = "phone-number-invalid-error";
    public static final String PHONE_NUMBER_NOT_VERIFIED = "phone-number-not-verified";

    //OTP Errors
    public static final String FAILED_TO_SEND_WHATSAPP_OTP = "failed-to-send-whatsapp-otp";
    public static final String FAILED_TO_SEND_SMS_OTP = "failed-to-send-sms-otp-error";
    public static final String FAILED_TELEGRAM_SEND_OTP="failed-telegram-send-otp";
    public static final String WRONG_OTP_ERROR = "wrong-otp-error";
    public static final String OTP_EXPIRED = "otp-expired-error";

    //Password Errors
    public static final String PASSWORD_REQUIRED = "password-required-error";
    public static final String CONFIRM_PASSWORD_REQUIRED = "confirm-password-required-error";
    public static final String PASSWORD_AND_CONFIRM_PASSWORD_REQUIRED = "password-and-confirm-password-required-error";
    public static final String PASSWORD_INCORRECT = "password-incorrect-error";
    public static final String PASSWORD_MISMATCH_ERROR = "password-mismatch";
    public static final String PASSWORD_MISMATCH = "password-mismatch-error";
    public static final String CREDENTIAL_SETUP_REQUIRED="credentials-setup-required";

    //PHONE OTP Errors
    public static final String PHONE_NUMBER_NOT_CONFIGURED="phone_number_not_configured";

    //Reset Password Errors
    public static final String NO_AVAILABLE_WAYS_TO_RESET="no-available-ways-to-reset";

    //Whatsapp Errors
    public static final String WP_PUSH_NOTIFICATION_EXPIRED="wp-push-notifications-expired";

    //Config Erros
    public static final String CONFIGURATION_IS_NOT_VALID_PLEASE_CHECK_WITH_ADMIN = "config-is-not-valid-check-with-admin";
    public static final String JSON_CONFIG_NOTVALID_ERROR="json-config-notvalid-error";

}
