package org.keycloak.models;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class APIPriorityModel {

        private String id;
        private String verifierName;
        private String selectedProvider;
        private List<String> availableProviders;
        private String realmId;
        private Boolean isEnabled;
        private String category;
}
