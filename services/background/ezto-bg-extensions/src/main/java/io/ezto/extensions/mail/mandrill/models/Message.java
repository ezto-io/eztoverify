
package io.ezto.extensions.mail.mandrill.models;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "html",
    "text",
    "subject",
    "from_email",
    "from_name",
    "to",
    "headers",
    "important",
    "track_opens",
    "track_clicks",
    "auto_text",
    "auto_html",
    "inline_css",
    "url_strip_qs",
    "preserve_recipients",
    "view_content_link",
    "bcc_address",
    "tracking_domain",
    "signing_domain",
    "return_path_domain",
    "merge",
    "merge_language",
    "global_merge_vars",
    "merge_vars",
    "tags",
    "subaccount",
    "google_analytics_domains",
    "google_analytics_campaign",
    "metadata",
    "recipient_metadata",
    "attachments",
    "images"
})

public class Message {

    @JsonProperty("html")
    private String html;
    @JsonProperty("text")
    private String text;
    @JsonProperty("subject")
    private String subject;
    @JsonProperty("from_email")
    private String fromEmail;
    @JsonProperty("from_name")
    private String fromName;
    @JsonProperty("to")
    private List<Object> to = null;
    @JsonProperty("headers")
    private Headers headers;
    @JsonProperty("important")
    private Boolean important;
    @JsonProperty("track_opens")
    private Boolean trackOpens;
    @JsonProperty("track_clicks")
    private Boolean trackClicks;
    @JsonProperty("auto_text")
    private Boolean autoText;
    @JsonProperty("auto_html")
    private Boolean autoHtml;
    @JsonProperty("inline_css")
    private Boolean inlineCss;
    @JsonProperty("url_strip_qs")
    private Boolean urlStripQs;
    @JsonProperty("preserve_recipients")
    private Boolean preserveRecipients;
    @JsonProperty("view_content_link")
    private Boolean viewContentLink;
    @JsonProperty("bcc_address")
    private String bccAddress;
    @JsonProperty("tracking_domain")
    private String trackingDomain;
    @JsonProperty("signing_domain")
    private String signingDomain;
    @JsonProperty("return_path_domain")
    private String returnPathDomain;
    @JsonProperty("merge")
    private Boolean merge;
    @JsonProperty("merge_language")
    private String mergeLanguage;
    @JsonProperty("global_merge_vars")
    private List<Object> globalMergeVars = null;
    @JsonProperty("merge_vars")
    private List<Object> mergeVars = null;
    @JsonProperty("tags")
    private List<Object> tags = null;
    @JsonProperty("subaccount")
    private String subaccount;
    @JsonProperty("google_analytics_domains")
    private List<Object> googleAnalyticsDomains = null;
    @JsonProperty("google_analytics_campaign")
    private String googleAnalyticsCampaign;
    @JsonProperty("metadata")
    private Metadata metadata;
    @JsonProperty("recipient_metadata")
    private List<Object> recipientMetadata = null;
    @JsonProperty("attachments")
    private List<Object> attachments = null;
    @JsonProperty("images")
    private List<Object> images = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonProperty("html")
    public String getHtml() {
        return html;
    }

    @JsonProperty("html")
    public void setHtml(String html) {
        this.html = html;
    }

    @JsonProperty("text")
    public String getText() {
        return text;
    }

    @JsonProperty("text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("subject")
    public String getSubject() {
        return subject;
    }

    @JsonProperty("subject")
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @JsonProperty("from_email")
    public String getFromEmail() {
        return fromEmail;
    }

    @JsonProperty("from_email")
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    @JsonProperty("from_name")
    public String getFromName() {
        return fromName;
    }

    @JsonProperty("from_name")
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    @JsonProperty("to")
    public List<Object> getTo() {
        return to;
    }

    @JsonProperty("to")
    public void setTo(List<Object> to) {
        this.to = to;
    }

    @JsonProperty("headers")
    public Headers getHeaders() {
        return headers;
    }

    @JsonProperty("headers")
    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    @JsonProperty("important")
    public Boolean getImportant() {
        return important;
    }

    @JsonProperty("important")
    public void setImportant(Boolean important) {
        this.important = important;
    }

    @JsonProperty("track_opens")
    public Boolean getTrackOpens() {
        return trackOpens;
    }

    @JsonProperty("track_opens")
    public void setTrackOpens(Boolean trackOpens) {
        this.trackOpens = trackOpens;
    }

    @JsonProperty("track_clicks")
    public Boolean getTrackClicks() {
        return trackClicks;
    }

    @JsonProperty("track_clicks")
    public void setTrackClicks(Boolean trackClicks) {
        this.trackClicks = trackClicks;
    }

    @JsonProperty("auto_text")
    public Boolean getAutoText() {
        return autoText;
    }

    @JsonProperty("auto_text")
    public void setAutoText(Boolean autoText) {
        this.autoText = autoText;
    }

    @JsonProperty("auto_html")
    public Boolean getAutoHtml() {
        return autoHtml;
    }

    @JsonProperty("auto_html")
    public void setAutoHtml(Boolean autoHtml) {
        this.autoHtml = autoHtml;
    }

    @JsonProperty("inline_css")
    public Boolean getInlineCss() {
        return inlineCss;
    }

    @JsonProperty("inline_css")
    public void setInlineCss(Boolean inlineCss) {
        this.inlineCss = inlineCss;
    }

    @JsonProperty("url_strip_qs")
    public Boolean getUrlStripQs() {
        return urlStripQs;
    }

    @JsonProperty("url_strip_qs")
    public void setUrlStripQs(Boolean urlStripQs) {
        this.urlStripQs = urlStripQs;
    }

    @JsonProperty("preserve_recipients")
    public Boolean getPreserveRecipients() {
        return preserveRecipients;
    }

    @JsonProperty("preserve_recipients")
    public void setPreserveRecipients(Boolean preserveRecipients) {
        this.preserveRecipients = preserveRecipients;
    }

    @JsonProperty("view_content_link")
    public Boolean getViewContentLink() {
        return viewContentLink;
    }

    @JsonProperty("view_content_link")
    public void setViewContentLink(Boolean viewContentLink) {
        this.viewContentLink = viewContentLink;
    }

    @JsonProperty("bcc_address")
    public String getBccAddress() {
        return bccAddress;
    }

    @JsonProperty("bcc_address")
    public void setBccAddress(String bccAddress) {
        this.bccAddress = bccAddress;
    }

    @JsonProperty("tracking_domain")
    public String getTrackingDomain() {
        return trackingDomain;
    }

    @JsonProperty("tracking_domain")
    public void setTrackingDomain(String trackingDomain) {
        this.trackingDomain = trackingDomain;
    }

    @JsonProperty("signing_domain")
    public String getSigningDomain() {
        return signingDomain;
    }

    @JsonProperty("signing_domain")
    public void setSigningDomain(String signingDomain) {
        this.signingDomain = signingDomain;
    }

    @JsonProperty("return_path_domain")
    public String getReturnPathDomain() {
        return returnPathDomain;
    }

    @JsonProperty("return_path_domain")
    public void setReturnPathDomain(String returnPathDomain) {
        this.returnPathDomain = returnPathDomain;
    }

    @JsonProperty("merge")
    public Boolean getMerge() {
        return merge;
    }

    @JsonProperty("merge")
    public void setMerge(Boolean merge) {
        this.merge = merge;
    }

    @JsonProperty("merge_language")
    public String getMergeLanguage() {
        return mergeLanguage;
    }

    @JsonProperty("merge_language")
    public void setMergeLanguage(String mergeLanguage) {
        this.mergeLanguage = mergeLanguage;
    }

    @JsonProperty("global_merge_vars")
    public List<Object> getGlobalMergeVars() {
        return globalMergeVars;
    }

    @JsonProperty("global_merge_vars")
    public void setGlobalMergeVars(List<Object> globalMergeVars) {
        this.globalMergeVars = globalMergeVars;
    }

    @JsonProperty("merge_vars")
    public List<Object> getMergeVars() {
        return mergeVars;
    }

    @JsonProperty("merge_vars")
    public void setMergeVars(List<Object> mergeVars) {
        this.mergeVars = mergeVars;
    }

    @JsonProperty("tags")
    public List<Object> getTags() {
        return tags;
    }

    @JsonProperty("tags")
    public void setTags(List<Object> tags) {
        this.tags = tags;
    }

    @JsonProperty("subaccount")
    public String getSubaccount() {
        return subaccount;
    }

    @JsonProperty("subaccount")
    public void setSubaccount(String subaccount) {
        this.subaccount = subaccount;
    }

    @JsonProperty("google_analytics_domains")
    public List<Object> getGoogleAnalyticsDomains() {
        return googleAnalyticsDomains;
    }

    @JsonProperty("google_analytics_domains")
    public void setGoogleAnalyticsDomains(List<Object> googleAnalyticsDomains) {
        this.googleAnalyticsDomains = googleAnalyticsDomains;
    }

    @JsonProperty("google_analytics_campaign")
    public String getGoogleAnalyticsCampaign() {
        return googleAnalyticsCampaign;
    }

    @JsonProperty("google_analytics_campaign")
    public void setGoogleAnalyticsCampaign(String googleAnalyticsCampaign) {
        this.googleAnalyticsCampaign = googleAnalyticsCampaign;
    }

    @JsonProperty("metadata")
    public Metadata getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    @JsonProperty("recipient_metadata")
    public List<Object> getRecipientMetadata() {
        return recipientMetadata;
    }

    @JsonProperty("recipient_metadata")
    public void setRecipientMetadata(List<Object> recipientMetadata) {
        this.recipientMetadata = recipientMetadata;
    }

    @JsonProperty("attachments")
    public List<Object> getAttachments() {
        return attachments;
    }

    @JsonProperty("attachments")
    public void setAttachments(List<Object> attachments) {
        this.attachments = attachments;
    }

    @JsonProperty("images")
    public List<Object> getImages() {
        return images;
    }

    @JsonProperty("images")
    public void setImages(List<Object> images) {
        this.images = images;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
