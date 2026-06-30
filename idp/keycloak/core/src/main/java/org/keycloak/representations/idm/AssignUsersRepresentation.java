package org.keycloak.representations.idm;

import java.util.List;

public class AssignUsersRepresentation {

    protected List<String> userIds;

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }
}
