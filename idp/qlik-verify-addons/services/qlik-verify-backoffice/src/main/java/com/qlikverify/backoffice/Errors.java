package com.qlikverify.backoffice;

public enum Errors {
    // API hits 401
    EV0001("EV0001", "Something went wrong. Please try again, or contact support if the issue persists."),
    // API hits 403
    EV0002("EV0002", "Something went wrong. Please try again, or contact support if the issue persists."),
    // API hits 404
    EV0003("EV0003", "Something went wrong. Please try again later or contact support for assistance."),
    // API hits 408
    EV0004("EV0004", "Something went wrong. Please try again later or contact support for assistance."),
    // API hits 409
    EV0005("EV0005", "Something went wrong. Please try again later or contact support for assistance."),
    // API hits 429
    EV0006("EV0006", "Too many requests. Please try again later or contact support for assistance."),
    // API hits 500
    EV0007("EV0007", "Something went wrong. Please try again later or contact support for assistance."),
    EV0008("EV0008", "Flow cancelled by user."),

    EVPAN0001("EVPAN0001", "Unable to establish a connection with PAN services.", "verify_pan"),
    EVPAN0002("EVPAN0002", "No records found for the given ID.", "verify_pan"),

    EVPAN0003("EVPAN0003", "Unable to verify PAN.", "verify_pan"),
    EVPAN0004("EVPAN0004", "An error occurred during PAN verification.", "verify_pan"),

    EVPAL0001("EVPAL0001", "The PAN is linked to a different Aadhaar.", "pan_aadhaar_link_status"),
    EVPAL0002("EVPAL0002", "The PAN does not exist.", "pan_aadhaar_link_status"),
    EVPAL0003("EVPAL0003", "Enter a valid PAN. Aadhaar-PAN linking can be done only for individual taxpayers.", "pan_aadhaar_link_status"),
    EVPAL0004("EVPAL0004", "The PAN is not linked to Aadhaar.", "pan_aadhaar_link_status"),
    EVPAL0005("EVPAL0005", "Enter a valid 12-digit Aadhaar number.", "pan_aadhaar_link_status"),
    EVPAL0006("EVPAL0006", "You have exceeded the limit to invoke the service on this network. Please try after 24 hours.", "pan_aadhaar_link_status"),

    EVSR0001("EVSR0001", "Spoof detected.", "selfie_registration"),
    EVSR0002("EVSR0002", "File download failed.", "selfie_registration"),
    EVSR0003("EVSR0003", "Location access denied.", "selfie_registration"),
    EVSV0001("EVSV0001", "Low Confidence.", "selfie_verification"),
    EVSV0002("EVSV0002", "Face did not match.", "selfie_verification"),
    EVSV0003("EVSV0003", "Location access denied.", "selfie_verification"),
    EVAPI0001("EVAPI0001", "Please check the target API service.", "api_request"),
    EVAPI0002("EVAPI0002", "Unauthorized", "api_request"),
    EVAPI0003("EVAPI0003", "Forbidden", "api_request"),
    EVAPI0004("EVAPI0004", "Too Many Requests", "api_request"),
    EVAPI0005("EVAPI0005", "Not Found", "api_request"),
    EVAPI0006("EVAPI0006", "Request could not be completed due to a conflict.", "api_request"),
    EVAPI0007("EVAPI0007", "Something went wrong.", "api_request"),

    EVNS0001("EVNS0001", "Comparison failed because one of the values is empty.", "name_similarity_check"),
    EVNS0002("EVNS0002", "Comparison value contains characters other than alphabets.", "name_similarity_check"),

    EVAOTP0001("EVAOTP0001", "We are unable to establish a connection with Aadhaar services. Please, try again later.", "verify_otp_aadhaar"),
    EVAOTP0002("EVAOTP0002", "UIDAI responded with an unexpected response. Please try again.", "verify_otp_aadhaar"),
    EVAOTP0003("EVAOTP0003", "Please try again by resending the OTP.", "verify_otp_aadhaar"),
    EVBANK0001("EVBANK0001", "We are unable to establish a connection with Bank services. Please, try again later", "verify_bank"),
    EVBANK0002("EVBANK0002", "Bank Account does not exist.", "verify_bank"),
    EVBANK0003("EVBANK0003", "Invalid account details.", "verify_bank"),
    EVBANK0004("EVBANK0004", "This account number is invalid. Please check and try again.", "verify_bank"),
    EVBANK0005("EVBANK0005", "Invalid Bank details.", "verify_bank"),
    EVBANK0006("EVBANK0006", "We are unable to establish a connection with Bank Penny Drop services. Please, try again later", "verify_bank"),
    EVDIGI0001("EVDIGI0001", "Consent denied", "digilocker"),
    EVDIGI0002("EVDIGI0002", "User cancelled pull document confirmation request", "digilocker"),
    EVDIGI0003("EVDIGI0003", "Unable to get pull documentation confirmation from the user", "digilocker"),
    EVDIGI0004("EVDIGI0004", "User cancelled select state request", "digilocker"),
    EVDIGI0005("EVDIGI0005", "Access not provided for the required document.", "digilocker"),
    EVDIGI0006("EVDIGI0006", "User cancelled pull document", "digilocker"),
    EVDIGI0007("EVDIGI0007", "Digilocker access code not found", "digilocker"),
    EVDIGI0008("EVDIGI0008", "Full Name did not match with PAN Data", "digilocker"),
    EVDIGI0009("EVDIGI0009", "Document pull failed.", "digilocker"),
    EVDIGI0010("EVDIGI0010", "PAN number is invalid", "digilocker"),
    EVDIGI0011("EVDIGI0011", "Chasis number entered by you did not match.", "digilocker"),
    EVDIGI0012("EVDIGI0012", "Requested document not found.", "digilocker"),
    EVDIGI0013("EVDIGI0013", "Doc owner name did not match.", "digilocker"),
    EVGEO0001("EVGEO0001", "Location access denied.", "geo_tagging"),

    EVOCRDL0001("EVOCRDL0001", "The provided Driving License image is invalid.", "ocr_driving_license"),
    EVOCRDL0002("EVOCRDL0002", "Invalid response for the uploaded Driving License.", "ocr_driving_license"),
    EVOCRDL0003("EVOCRDL0003", "OCR Driving License API limit exhausted. Try after some time.", "ocr_driving_license"),
    EVOCRDL0004("EVOCRDL0004", "The uploaded Driving License is invalid.", "ocr_driving_license"),
    EVOCRDL0005("EVOCRDL0005", "Invalid input. Please upload a valid Driving License image.", "ocr_driving_license"),
    EVOCRDL0006("EVOCRDL0006", "Driving License OCR processing limit exceeded. Please wait and try again later.", "ocr_driving_license"),
    EVOCRDL0007("EVOCRDL0007", "Something went wrong. Please, try again later.", "ocr_driving_license"),


    EVOCRAD0001("EVOCRAD0001", "The provided Aadhaar image is invalid.", "verify_ocr_aadhaar"),
    EVOCRAD0002("EVOCRAD0002", "Invalid response for the uploaded Aadhaar front image.", "verify_ocr_aadhaar"),
    EVOCRAD0003("EVOCRAD0003", "OCR Aadhaar API limit exhausted. Try after some time.", "verify_ocr_aadhaar"),
    EVOCRAD0004("EVOCRAD0004", "The uploaded Aadhaar is invalid.", "verify_ocr_aadhaar"),
    EVOCRAD0005("EVOCRAD0005", "Invalid input. Please upload a valid Aadhaar image.", "verify_ocr_aadhaar"),
    EVOCRAD0006("EVOCRAD0006", "Aadhaar OCR processing limit exceeded. Please wait and try again later.", "verify_ocr_aadhaar"),
    EVOCRAD0007("EVOCRAD0007", "Something went wrong. Please wait and try again later.", "verify_ocr_aadhaar"),
    EVOCRAD0008("EVOCRAD0008", "Invalid response for the uploaded Aadhaar back image.", "verify_ocr_aadhaar"),
    EVOCRAD0009("EVOCRAD0009", "The Aadhaar number is masked in the provided image.", "verify_ocr_aadhaar"),
    EVOCRAD0010("EVOCRAD0010", "The Aadhaar number in front and back image does not match.", "verify_ocr_aadhaar"),
    EVVKYC0001("EVVKYC0001", "User denied.", "video_kyc"),
    EVVKYC0002("EVVKYC0002", "Failed to send meeting link to the agent.", "video_kyc"),
    EVVKYC0003("EVVKYC0003", "Something went wrong. Please, try again later.", "video_kyc"),
    EVVKYC0004("EVVKYC0004", "No video files found for User.", "video_kyc"),
    EVVKYC0005("EVVKYC0005", "No video files found for Agent.", "video_kyc"),
    EVVKYC0006("EVVKYC0006", "Failed to process the video files.", "video_kyc"),
    EVVKYC0007("EVVKYC0007", "Agent can't verify the user.", "video_kyc"),
    EVVKYC0008("EVVKYC0008", "Agent rejected the user.", "video_kyc");


    private String errorCode;
    private String errorReason;
    private String templateName;

    Errors(String errorCode, String errorReason) {
        this.errorCode = errorCode;
        this.errorReason = errorReason;
    }

    Errors(String errorCode, String errorReason, String templateName) {
        this.errorCode = errorCode;
        this.errorReason = errorReason;
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    @Override
    public String toString() {
        return "{" +
                "\"error_code\":" + "\"" + errorCode + "\"" +
                ", \"error_reason\":" + "\"" + errorReason + "\"" +
                ", \"template_name\":" + "\"" + templateName + "\"" +
                "}";
    }
}

