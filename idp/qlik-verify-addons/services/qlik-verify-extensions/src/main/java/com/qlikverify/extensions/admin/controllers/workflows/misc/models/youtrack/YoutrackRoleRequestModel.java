package com.qlikverify.extensions.admin.controllers.workflows.misc.models.youtrack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutrackRoleRequestModel {
    private String name;
    private String description;
    private List<Object> permissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Object> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Object> permissions) {
        this.permissions = permissions;
    }
}
