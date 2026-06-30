
package org.keycloak.representations.idm.license;
public class UsageRequest {

    private String subItemId;
    private Integer usage;
    private long timestamp;

    public String getSubItemId() {
        return subItemId;
    }

    public void setSubItemId(String subItemId) {
        this.subItemId = subItemId;
    }

    public Integer getUsage() {
        return usage;
    }

    public void setUsage(Integer usage) {
        this.usage = usage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
