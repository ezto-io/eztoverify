package com.qlikverify.services.models.JsonInputModel;

import java.util.Map;

public class ResendCodeModel {
    private boolean resend;
    private Map<String, String> resendButtonText;
    private Map<String, String> resendTimerText;
    private Long timer;

    public ResendCodeModel() {
        // NOP
    }

    public boolean isResend() {
        return resend;
    }

    public void setResend(boolean resend) {
        this.resend = resend;
    }

    public Map<String, String> getResendButtonText() {
        return resendButtonText;
    }

    public void setResendButtonText(Map<String, String> resendButtonText) {
        this.resendButtonText = resendButtonText;
    }

    public Map<String, String> getResendTimerText() {
        return resendTimerText;
    }

    public void setResendTimerText(Map<String, String> resendTimerText) {
        this.resendTimerText = resendTimerText;
    }

    public Long getTimer() {
        return timer;
    }

    public void setTimer(Long timer) {
        this.timer = timer;
    }

    @Override
    public String toString() {
        return "ResendCodeModel{" +
                "resend=" + resend +
                ", resendButtonText=" + resendButtonText +
                ", resendTimerText=" + resendTimerText +
                ", timer=" + timer +
                '}';
    }
}
