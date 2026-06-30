package com.qlikverify.services.models.JsonInputModel;


public class Attributes {
    private Long minLength;
    private Long maxLength;
    private String regex;
    private String allowedChars;
    private Boolean optional;
    private String mask;
    private String dateFormat;
    private int otpExpiry;
    private int otpRetryTime;
    private String smsMsgTemplate;
    private String region;
    private boolean editable = true;
    private JSONButton actionButton;
    private ResendCodeModel resendCodeModel;
    private boolean saveInputs;
    private Boolean capitalize;

    public Attributes() {
        // NOP
    }

    public Long getMinLength() {
        return minLength;
    }

    public void setMinLength(Long minLength) {
        this.minLength = minLength;
    }

    public Long getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Long maxLength) {
        this.maxLength = maxLength;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getAllowedChars() {
        return allowedChars;
    }

    public void setAllowedChars(String allowedChars) {
        this.allowedChars = allowedChars;
    }

    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public int getOtpExpiry() {
        return otpExpiry;
    }

    public void setOtpExpiry(int otpExpiry) {
        this.otpExpiry = otpExpiry;
    }

    public int getOtpRetryTime() {
        return otpRetryTime;
    }

    public void setOtpRetryTime(int otpRetryTime) {
        this.otpRetryTime = otpRetryTime;
    }

    public String getSmsMsgTemplate() {
        return smsMsgTemplate;
    }

    public void setSmsMsgTemplate(String smsMsgTemplate) {
        this.smsMsgTemplate = smsMsgTemplate;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public JSONButton getActionButton() {
        return actionButton;
    }

    public void setActionButton(JSONButton actionButton) {
        this.actionButton = actionButton;
    }

    public ResendCodeModel getResendCodeModel() {
        return resendCodeModel;
    }

    public void setResendCodeModel(ResendCodeModel resendCodeModel) {
        this.resendCodeModel = resendCodeModel;
    }

    public boolean isSaveInputs() {
        return saveInputs;
    }

    public void setSaveInputs(boolean saveInputs) {
        this.saveInputs = saveInputs;
    }

    public Boolean getCapitalize() { return capitalize;}

    public void setCapitalize(Boolean capitalize) { this.capitalize = capitalize;}

    @Override
    public String toString() {
        return "Attributes{" +
                "minLength=" + minLength +
                ", maxLength=" + maxLength +
                ", regex='" + regex + '\'' +
                ", optional=" + optional +
                ", mask='" + mask + '\'' +
                ", dateFormat='" + dateFormat + '\'' +
                ", otpExpiry='" + otpExpiry + '\'' +
                ", otpRetryTime=" + otpRetryTime +
                ", smsMsgTemplate='" + smsMsgTemplate + '\'' +
                ", region='" + region + '\'' +
                ", editable=" + editable +
                ", actionButton=" + actionButton +
                ", resendCodeModel=" + resendCodeModel +
                ", capitalize=" + capitalize +
                '}';
    }
}
