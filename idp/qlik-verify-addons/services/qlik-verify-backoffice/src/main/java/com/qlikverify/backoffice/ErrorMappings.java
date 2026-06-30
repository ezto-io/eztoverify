package com.qlikverify.backoffice;

public class ErrorMappings {

    private ErrorMappings() {
        throw new UnsupportedOperationException("ErrorMappings class cannot be instantiated");
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

    public static final String MAXIMUM_ATTEMPTS_REACHED_USER_TEMP_DISABLED = "max_attempts_reached_user_temp_disabled";
    public static final String INVALID_REQUEST = "invalid-request-error";
    public static final String USER_REQUIRED = "user-required-error";
    public static final String AUTHENTICATORS_NULL = "authenticators-null";

    //Email Errors
    public static final String FAILED_TO_SEND_EMAIL = "failed-to-send-email-error";
    public static final String MAGIC_LINK_EXPIRED = "magic-link-expired-error";
    public static final String MAGIC_LINK_NOT_VALID = "magic-link-not-valid-error";
    public static final String EMAIL_REGISTERED = "email-registered-error";
    public static final String EMAIL_NOT_REGISTERED = "email-not-registered-error";
    public static final String EMAIL_NOT_FOUND = "email-not-found";
    public static final String EMAIL_REQUIRED = "email-required-error";
    public static final String EMAIL_INVALID = "email-invalid-error";
    public static final String USER_EXISTS_SAME_EMAIL = "user-exists-same-email-error";
    public static final String RESTRICTED_DOMAIN_EMAIL = "user-restricted-email-domain-error";
    public static final String EMAIL_NOT_VERIFIED = "email-not-verified";
    public static final String USER_NOT_ONBOARDED = "user_not_onboarded";
    public static final String USER_CANCELLED = "user_cancelled";
    public static final String USER_SKIPPED = "user_skipped";

    //Phone Number Errors
    public static final String PHONE_ALREADY_REGISTERED = "phone-already-registered-error";
    public static final String PHONE_NUMBER_NOT_REGISTERED = "phone-number-not-registered-error";
    public static final String PHONE_NUMBER_REQUIRED = "phone-number-required-error";
    public static final String PHONE_NUMBER_INVALID = "phone-number-invalid-error";
    public static final String PHONE_NUMBER_NOT_VERIFIED = "phone-number-not-verified";

    //OTP Errors
    public static final String FAILED_TO_SEND_WHATSAPP_OTP = "failed-to-send-whatsapp-otp";
    public static final String FAILED_TO_SEND_SMS_OTP = "failed-to-send-sms-otp-error";
    public static final String FAILED_TELEGRAM_SEND_OTP = "failed-telegram-send-otp";
    public static final String WRONG_OTP_ERROR = "wrong-otp-error";
    public static final String OTP_EXPIRED = "otp-expired-error";
    public static final String FAILED_TO_SEND_OTP = "failed-to-send-otp";

    //Password Errors
    public static final String PASSWORD_REQUIRED = "password-required-error";
    public static final String CONFIRM_PASSWORD_REQUIRED = "confirm-password-required-error";
    public static final String PASSWORD_AND_CONFIRM_PASSWORD_REQUIRED = "password-and-confirm-password-required-error";
    public static final String PASSWORD_INCORRECT = "password-incorrect-error";
    public static final String PASSWORD_MISMATCH_ERROR = "password-mismatch";
    public static final String PASSWORD_MISMATCH = "password-mismatch-error";
    public static final String CREDENTIAL_SETUP_REQUIRED = "credentials-setup-required";

    //PHONE OTP Errors
    public static final String PHONE_NUMBER_NOT_CONFIGURED = "phone_number_not_configured";

    //Reset Password Errors
    public static final String NO_AVAILABLE_WAYS_TO_RESET = "no-available-ways-to-reset";

    //Whatsapp Errors
    public static final String WP_PUSH_NOTIFICATION_EXPIRED = "wp-push-notifications-expired";

    //Config Erros
    public static final String CONFIGURATION_IS_NOT_VALID_PLEASE_CHECK_WITH_ADMIN = "config-is-not-valid-check-with-admin";
    public static final String JSON_CONFIG_NOTVALID_ERROR = "json-config-notvalid-error";
    public static final String EMAIL_AND_PHONE_MISSING_ERROR = "email-phone-missing-error";


    public static final String AADHAAR_REQUIRED = "aadhaar-required-error";
    public static final String AADHAAR_INVALID = "aadhaar-invalid-error";
    public static final String AADHAAR_REACH_ERROR = "aadhaar-server-reach-error";
    public static final String AADHAAR_INVALID_PHOTO_ERROR = "aadhaar-invalid-photo-error";
    public static final String AADHAAR_INVALID_FRONT_PHOTO_ERROR = "aadhaar-invalid-front-photo-error";
    public static final String AADHAAR_INVALID_BACK_PHOTO_ERROR = "aadhaar-invalid-back-photo-error";
    public static final String AADHAAR_NUMBER_MISMATCH = "aadhaar-number-mismatch";
    public static final String AADHAAR_INVALID_OTP_ERROR = "aadhaar-invalid-otp-error";
    public static final String AADHAR_UNABLE_TO_FETCH = "aadhaar-unable-to-fetch";
    public static final String AADHAAR_NUMBER_INVALID = "aadhaar-number-invalid";
    public static final String AADHAAR_CARD_INVALID = "aadhaar-card-invalid";

    //Pan
    public static final String PAN_REQUIRED = "pan-required-error";
    public static final String PAN_INVALID = "pan-invalid-error";
    public static final String PAN_REACH_ERROR = "pan-server-reach-error";
    public static final String PAN_NUMBER_INVALID = "pan-number-invalid";
    public static final String PAN_CARD_INVALID = "pan-card-invalid";
    public static final String PAN_INVALID_PHOTO_ERROR = "pan-invalid-photo-error";
    public static final String API_EXHAUSTED = "api-limit-exhausted";
    public static final String INVALID_DETAILS = "invalid-details";
    public static final String TOO_MANY_REQUESTS = "too-many-requests";
    public static final String METADATA_MISMATCH = "metadata-mismatch";
    public static final String CONTACT_ADMIN = "contact-admin";
    public static final String PAN_MISMATCH_ERROR = "pan_mismatch_error";

    //Driving License
    public static final String DRIVING_LICENSE_INVALID_PHOTO_ERROR = "pan-invalid-photo-error";
    public static final String DRIVING_LICENSE_REQUIRED = "driving-license-required-error";
    public static final String DRIVING_LICENSE_INVALID = "driving-license-invalid-error";
    public static final String DRIVING_LICENSE_REACH_ERROR = "driving-license-server-reach-error";
    public static final String DRIVING_LICENSE_MISMATCH_ERROR = "driving_license_mismatch_error";
    public static final String DRIVING_LICENSE_NUMBER_INVALID = "driving-license-number-invalid";
    public static final String DRIVING_LICENSE_CARD_INVALID = "driving-license-invalid";
    public static final String DOB_REQUIRED = "dob-required-error";
    public static final String DOB_FORMAT_ERROR = "dob-format-error";

    // Bank verification
    public static final String BANK_ACCOUNT_NO_ERROR = "account-no-required";
    public static final String IFSC_CODE_NO_ERROR = "ifsc-code-required";
    public static final String BANK_DETAILS_INVALID = "ifsc-code-required";
    public static final String BANK_SERVER_REACH_ERROR = "bank-server-reach-error";
    public static final String UNPROCESSABLE_DATA = "unprocessable-data";
    public static final String ACCOUNT_NOT_EXISTS = "bank-account-not-exists";

    //Voter ID
    public static final String VOTER_ID_REQUIRED = "voter-id-required-error";
    public static final String VOTER_ID_INVALID = "voter-id-invalid-error";
    public static final String VOTER_ID_REACH_ERROR = "voter-id-server-reach-error";
    public static final String VOTER_ID_INVALID_PHOTO_ERROR = "voter-id-invalid-photo-error";
    public static final String VOTER_ID_INVALID_FRONT_PHOTO_ERROR = "voter-id-invalid-front-photo-error";
    public static final String VOTER_ID_INVALID_BACK_PHOTO_ERROR = "voter-id-invalid-back-photo-error";
    public static final String VOTER_ID_UNABLE_TO_FETCH = "voter-id-unable-to-fetch";
    public static final String VOTER_ID_NUMBER_INVALID = "voter-id-number-invalid";
    public static final String VOTER_ID_CARD_INVALID = "voter-id-card-invalid-error";


    //Tan
    public static final String TAN_REQUIRED = "tan-required-error";
    public static final String TAN_INVALID = "tan-invalid-error";
    public static final String TAN_REACH_ERROR = "tan-server-reach-error";
    public static final String TAN_MISMATCH_ERROR = "tan_mismatch_error";
    public static final String TAN_NUMBER_INVALID = "tan-number-invalid";

    //Business Verification
    public static final String CIN_NUMBER_ERROR = "cin-number-error";
    public static final String SERVER_REACH_ERROR = "server-reach-error";
    public static final String INVALID_BUSINESS_DETAILS = "invalid-business-details";

    // IEC
    public static final String IEC_NUM_REQUIRED = "iec-num-required";
    public static final String IEC_NUM_INVALID = "iec-num-invalid-error";
    public static final String IEC_REACH_ERROR = "iec-server-reach-error";
    public static final String IEC_NUM_UNABLE_TO_FETCH = "iec-num-unable-to-fetch";
    public static final String IEC_NUMBER_INVALID = "iec-number-invalid";
    //GSTIN
    public static final String GSTIN_REQUIRED = "gstin-required-error";
    public static final String GSTIN_INVALID = "gstin-invalid-error";
    public static final String GSTIN_REACH_ERROR = "gstin-server-reach-error";
    public static final String GSTIN_MISMATCH_ERROR = "gstin_mismatch_error";
    public static final String GST_INVALID_OTP_ERROR = "gst-invalid-otp-error";
    public static final String GST_NUMBER_INVALID = "gst-number-invalid";


    // UDYAM
    public static final String UDYAM_NUM_REQUIRED = "udyam-num-required";
    public static final String UDYAM_NUM_INVALID = "udyam-num-invalid-error";
    public static final String UDYAM_REACH_ERROR = "udyam-server-reach-error";
    public static final String UDYAM_NUM_UNABLE_TO_FETCH = "udyam-num-unable-to-fetch";
    public static final String UDYAM_NUMBER_INVALID = "udyam-number-invalid";

    //FSSAI
    public static final String FSSAI_REQUIRED = "fssai-required-error";
    public static final String FSSAI_INVALID = "fssai-invalid-error";
    public static final String FSSAI_REACH_ERROR = "fssai-server-reach-error";
    public static final String FSSAI_MISMATCH_ERROR = "fssai_mismatch_error";
    public static final String FSSAI_NUMBER_INVALID = "fssai-number-invalid";

    // TDS-206 Compliance
    public static final String TDS206_PAN_NUM_ERROR = "tds206-pan-field-error";
    public static final String INVALID_PAN_DETAILS = "invalid-pan-details";
    //EPFO
    public static final String UAN_FIELD_ERROR = "uan-field-error";
    public static final String DETAILS_DOESNT_MATCH = "details-does-not-match";
    public static final String OTP_VERIFICATION_FAILED = "otp-verification-failed";

    // FORM 26AS
    public static final String FORM_26AS_USERNAME_REQUIRED = "form-26as-username-required-error";
    public static final String FORM_26AS_PASSWORD_REQUIRED = "form-26as-password-required-error";
    public static final String FORM_26AS_REACH_ERROR = "form-26as-server-reach-error";
    public static final String FORM_26AS_USERNAME_PASSWORD_INVALID = "form-26as-username-password-invalid";
    public static final String FORM_26AS_CREATE_CLIENT_ERROR = "form-26as-create-client-error";
    public static final String FORM_26AS_INVALID_CLIENT_ID_ERROR = "form-26as-invalid-client-error";
    public static final String FORM_26AS_CHECK_CREDENTIALS_ERROR = "form-26as-check-credentials-error";
    public static final String FORM_26AS_DOWNLOAD_26AS_DETAILS_FAILED = "form-26as-download-26as-details-failed";
    public static final String FORM_26AS_DOWNLOAD_26AS_DETAILS_NULL = "form-26as-details-null";
    public static final String TDS_DETAILS_NULL = "tds-details-null";
    public static final String TDS_DETAILS_ERROR = "tds-details-error";

    // ITR
    public static final String ITR_USERNAME_REQUIRED = "itr-username-required-error";
    public static final String ITR_PASSWORD_REQUIRED = "itr-password-required-error";
    public static final String ITR_REACH_ERROR = "itr-server-reach-error";
    public static final String ITR_USERNAME_PASSWORD_INVALID = "itr-username-password-invalid";
    public static final String ITR_CREATE_CLIENT_ERROR = "itr-create-client-error";
    public static final String ITR_INVALID_CLIENT_ID_ERROR = "itr-invalid-client-error";
    public static final String ITR_CHECK_CREDENTIALS_ERROR = "itr-check-credentials-error";
    public static final String ITR_DOWNLOAD_ITR_DETAILS_FAILED = "itr-download-itr-details-failed";
    public static final String ITR_DOWNLOAD_ITR_DETAILS_NULL = "itr-details-null";
    public static final String ITR_DETAILS_ERROR = "itr-details-error";
    public static final String ITR_DETAILS_NULL = "itr-details-null";

    // liveliness
    public static final String LIVELINESS_VIDEO_INVALID_ERROR = "liveliness-video-invalid-error";
    public static final String LIVELINESS_AUDIO_INVALID_ERROR = "liveliness-audio-invalid-error";
    public static final String INVALID_CONFIGS = "invalid_configs_liveness";

    public static final String WEB_2_DEVICE_NO_CLIENT_ERROR = "client-not-found";
    public static final String WEB_2_DEVICE_NO_DEVICE_ERROR = "user-no-device";
    public static final String WEB_2_DEVICE_GENERAL_ERROR = "general-error";
    public static final String RESULT_NOTIFICATION_ERROR = "result-notification-error";
    public static final String INVALID_FACE_COMPARISON_FILE = "invalid-face-comparison-file";

    // EMAIL VALIDATION
    public static final String EMAIL_VALIDATION_REACH_ERROR = "email-validation-server-reach-error";
    public static final String EMAIL_VALIDATION_QUALITY_SCORE_ERROR = "email-validation-quality-score-error";
    public static final String EMAIL_VALIDATION_CONFIG_MISSING = "email-validation-config-missing";

    // Phone Validation
    public static final String PHONE_VALIDATION_REACH_ERROR = "phone-validation-reach-error";
    public static final String PHONE_VALIDATION_CONFIG_MISSING = "phone-validation-config-missing";
    public static final String INVALID_PHONE_NUMBER = "invalid-phone-number";
    public static final String VIDEO_KYC_NOT_SUPPORTED = "video-kyc-not-supported";
    public static final String VIDEO_KYC_LIMIT_REACHED = "video-kyc-limit-reached";
}
