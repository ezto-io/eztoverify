package com.qlikverify.services.models;

public class PassPolicy {

    private String policyName;
    private int count;
    private String message;

    public PassPolicy(String message,int count, String policyName)
    {
        this.policyName = policyName;
        this.count = count;
        this.message = message;
    }

    public String getPolicyName() {
        return policyName;
    }

    public int getCount() {
        return count;
    }

    public String getMessage() {
        return message;
    }
}
