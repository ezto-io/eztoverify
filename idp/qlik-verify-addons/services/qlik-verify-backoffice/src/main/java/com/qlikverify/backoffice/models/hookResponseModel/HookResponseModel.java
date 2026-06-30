package com.qlikverify.backoffice.models.hookResponseModel;

import lombok.*;
@Data
@Getter
@AllArgsConstructor
public class HookResponseModel {
    private int code;
    private boolean success;
    private String message;
    private String clientId;
    private String realmId;

    public HookResponseModel() {
    }
}
