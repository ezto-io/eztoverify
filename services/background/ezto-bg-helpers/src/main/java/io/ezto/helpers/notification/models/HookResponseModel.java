package io.ezto.helpers.notification.models;

import lombok.*;

@Data
@Getter
@Setter
@Builder
@NonNull
public class HookResponseModel {
    private int code;
    private boolean success;
    private String message;
    private String clientId;
    private String realmId;
}
