package org.keycloak.representations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RealmAttributeCollection {
    private String tabName;
    private boolean underTrial;
    private String subscribeLink;
    private List<RealmAttributeRepresentation> attributes;

    public boolean isUnderTrial() {
        return underTrial;
    }

    public void setUnderTrial(boolean underTrial) {
        this.underTrial = underTrial;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    public List<RealmAttributeRepresentation> getAttributes() {
        if(attributes == null)
            attributes = new ArrayList<>();

        return attributes;
    }

    public String getSubscribeLink() {
        return subscribeLink;
    }

    public void setSubscribeLink(String subscribeLink) {
        this.subscribeLink = subscribeLink;
    }

    public void setAttributes(List<RealmAttributeRepresentation> attributes) {
        this.attributes = attributes;
    }
}
