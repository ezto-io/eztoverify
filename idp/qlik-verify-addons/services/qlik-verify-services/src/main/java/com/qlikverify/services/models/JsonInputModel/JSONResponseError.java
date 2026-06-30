package com.qlikverify.services.models.JsonInputModel;

import java.util.List;
import java.util.Map;

public class JSONResponseError {
    private Map<String, String> title;
    private String errorCode;
    private Map<String, String> errorMessage;
    private List<JSONButton> buttons;

    public Map<String, String> getTitle() {
        return title;
    }

    public void setTitle(Map<String, String> title) {
        this.title = title;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Map<String, String> getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(Map<String, String> errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<JSONButton> getButtons() {
        return buttons;
    }

    public void setButtons(List<JSONButton> buttons) {
        this.buttons = buttons;
    }

    @Override
    public String toString() {
        return "JSONResponseError{" +
                "title=" + title +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage=" + errorMessage +
                ", buttons=" + buttons +
                '}';
    }
}
