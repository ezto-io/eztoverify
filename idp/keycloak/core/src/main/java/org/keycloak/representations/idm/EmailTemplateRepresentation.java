package org.keycloak.representations.idm;

public class EmailTemplateRepresentation {

    private String templateName;
    private String displayName;
    private boolean templateEnabled;
    private String textBody;
    private String htmlBody;

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isTemplateEnabled() {
        return templateEnabled;
    }

    public void setTemplateEnabled(boolean templateEnabled) {
        this.templateEnabled = templateEnabled;
    }

    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(String textBody) {
        this.textBody = textBody;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }
}
