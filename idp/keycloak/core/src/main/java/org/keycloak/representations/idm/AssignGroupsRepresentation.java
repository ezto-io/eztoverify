package org.keycloak.representations.idm;

import java.util.List;

public class AssignGroupsRepresentation {

    protected List<String> grpIds;

    public List<String> getGrpIds() {
        return grpIds;
    }

    public void setGrpIds(List<String> grpIds) {
        this.grpIds = grpIds;
    }
}
