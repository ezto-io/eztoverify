package com.qlikverify.extensions.admin.controllers.workflows.misc;

import org.keycloak.models.UserModel;

import java.util.*;

public class WorkflowBaseUserRequestModel {

    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Map<String, Object> metaData;

    public WorkflowBaseUserRequestModel(UserModel userModel) {
        this.setId(userModel.getId());
        this.setUsername(userModel.getUsername());
        this.setFirstName(userModel.getFirstName() == null ? "" : userModel.getFirstName());
        this.setLastName(userModel.getLastName() == null ? "" : userModel.getLastName());
        this.setEmail(userModel.getEmail());
        this.setMetaData(new HashMap<>());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
    }
}
