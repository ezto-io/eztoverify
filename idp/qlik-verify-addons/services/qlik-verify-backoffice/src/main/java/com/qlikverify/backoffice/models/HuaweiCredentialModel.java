package com.qlikverify.backoffice.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class HuaweiCredentialModel {
    private String clientId;
    private String clientSecret;
}
