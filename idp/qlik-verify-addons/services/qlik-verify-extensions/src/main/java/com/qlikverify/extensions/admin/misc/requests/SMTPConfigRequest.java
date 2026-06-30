package com.qlikverify.extensions.admin.misc.requests;

public class SMTPConfigRequest {

    private String replyToDisplayName;
    private String starttls;
    private String envelopeFrom;
    private String ssl;
    private String port;
    private String host;
    private String replyTo;
    private String from;
    private String fromDisplayName;
    private String user;
    private String password;

    public SMTPConfigRequest() {
    }

    public SMTPConfigRequest(String replyToDisplayName, String starttls, String envelopeFrom, String ssl, String port, String host, String replyTo, String from, String fromDisplayName, String user, String password) {
        this.replyToDisplayName = replyToDisplayName;
        this.starttls = starttls;
        this.envelopeFrom = envelopeFrom;
        this.ssl = ssl;
        this.port = port;
        this.host = host;
        this.replyTo = replyTo;
        this.from = from;
        this.fromDisplayName = fromDisplayName;
        this.user = user;
        this.password = password;
    }

    public String getReplyToDisplayName() {
        return replyToDisplayName;
    }

    public void setReplyToDisplayName(String replyToDisplayName) {
        this.replyToDisplayName = replyToDisplayName;
    }

    public String getStarttls() {
        return starttls;
    }

    public void setStarttls(String starttls) {
        this.starttls = starttls;
    }

    public String getEnvelopeFrom() {
        return envelopeFrom;
    }

    public void setEnvelopeFrom(String envelopeFrom) {
        this.envelopeFrom = envelopeFrom;
    }

    public String getSsl() {
        return ssl;
    }

    public void setSsl(String ssl) {
        this.ssl = ssl;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFromDisplayName() {
        return fromDisplayName;
    }

    public void setFromDisplayName(String fromDisplayName) {
        this.fromDisplayName = fromDisplayName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
