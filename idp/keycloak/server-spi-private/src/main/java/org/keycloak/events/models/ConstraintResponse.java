package org.keycloak.events.models;

import java.util.List;

public class ConstraintResponse {
    private List<ConstraintData> data;
    private boolean isSuccess;

    public ConstraintResponse(boolean isSuccess, List<ConstraintData> data) {
        this.data = data;
        this.isSuccess = isSuccess;
    }

    public ConstraintResponse() {
        this.data = null;
        this.isSuccess = true;
    }

    public List<ConstraintData> getData() {
        return data;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}
