package com.qlikverify.backoffice.models.fidoAndroidAsset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Target {
    private String namespace;
    private String site;
    @JsonProperty("package_name")
    private String packageName;
    @JsonProperty("sha256_cert_fingerprints")
    private List<String> sha256CERTFingerprints;

    public String getNamespace() { return namespace; }
    public void setNamespace(String value) { this.namespace = value; }

    public String getSite() { return site; }
    public void setSite(String value) { this.site = value; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String value) { this.packageName = value; }

    public List<String> getSha256CERTFingerprints() { return sha256CERTFingerprints; }
    public void setSha256CERTFingerprints(List<String> value) { this.sha256CERTFingerprints = value; }
}
