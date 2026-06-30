package com.qlikverify.services.responses;

import java.util.ArrayList;

public class StatusObject {
    private String state;
    private String reason;
    private boolean success;
    private int probability;
    private ArrayList<String> eval;

    public StatusObject() {
        // NOP
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getProbability() {
        return probability;
    }

    public void setProbability(int probability) {
        this.probability = probability;
    }

    public ArrayList<String> getEval() {
        return eval;
    }

    public void setEval(ArrayList<String> eval) {
        this.eval = eval;
    }
}
