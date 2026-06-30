package com.qlikverify.services.models.JsonInputModel;

public class GeneralError {
    String errorTitle;
    String error;
    String errorCode;
    boolean isSevere;

    public GeneralError errorTitle(String errorTitle) {
        this.errorTitle = errorTitle;
        return this;
    }

    public GeneralError error(String error) {
        this.error = error;
        return this;
    }

    public GeneralError errorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public GeneralError isSevere(boolean isSevere) {
        this.isSevere = isSevere;
        return this;
    }

    public String getErrorTitle() {
        return errorTitle;
    }

    public String getError() {
        return error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isSevere() {
        return isSevere;
    }
}
