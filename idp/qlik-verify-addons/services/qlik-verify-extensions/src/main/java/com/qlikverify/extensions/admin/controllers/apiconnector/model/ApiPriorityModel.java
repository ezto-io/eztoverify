package com.qlikverify.extensions.admin.controllers.apiconnector.model;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ApiPriorityModel {
    private String id;
    private String verifierName;
    private String selectedProvider;
    private List<String> availableProviders;
    private String realmId;
    private Boolean isEnabled;
}
