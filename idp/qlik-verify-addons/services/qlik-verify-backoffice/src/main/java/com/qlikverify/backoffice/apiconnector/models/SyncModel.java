package com.qlikverify.backoffice.apiconnector.models;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncModel {
    private String providerName;
    private String verifierName;
    private Map<String, Object> requestBody;
    private Map<String, Object> responseBody;
}