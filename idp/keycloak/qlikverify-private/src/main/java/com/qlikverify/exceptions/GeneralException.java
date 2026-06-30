package com.qlikverify.exceptions;

public class GeneralException extends Exception{
    private String errorMessage;

    public GeneralException(String err)
    {
        this.errorMessage = err;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
