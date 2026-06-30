package com.qlikverify.backoffice;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class FormPartValue {

    private String value;
    private InputStream inputStream;
    private MediaType mediaType;

    public FormPartValue(String value) {
        this.value = value;
    }

    public FormPartValue(InputStream inputStream) {
        this.inputStream = inputStream;
        this.value = null;
    }

    public FormPartValue(InputStream inputStream, MediaType mediaType) {
        this.inputStream = inputStream;
        this.mediaType = mediaType;

    }

    public String asString() {
        if (inputStream != null) {
            throw new IllegalStateException("Value is a input stream");
        }
        return value;
    }

    public InputStream asInputStream() {
        if (inputStream == null) {
            return new ByteArrayInputStream(new byte[0]);
        }
        return inputStream;
    }

    public MediaType asMediaType() {
        return mediaType;
    }
}
