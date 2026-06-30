package com.qlikverify.extensions.admin.controllers.apiconnector.model;

import lombok.*;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ApiConnectorConfigModel {
    String providerName;
    String verifierName;
    Map<String, Object> requestBody;
    Map<String, Object> responseBody;
    Map<String, String> claims;
}
