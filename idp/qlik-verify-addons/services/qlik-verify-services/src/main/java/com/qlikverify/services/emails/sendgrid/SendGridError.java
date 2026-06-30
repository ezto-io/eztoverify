package com.qlikverify.services.emails.sendgrid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;


public class SendGridError {
    List<Map<String, String>> errors;
    @JsonCreator
    public SendGridError(@JsonProperty("errors") List<Map<String, String>> errors) {
        this.errors = errors;
    }

    public List<Map<String, String>> getErrors() {
        return errors;
    }
}
