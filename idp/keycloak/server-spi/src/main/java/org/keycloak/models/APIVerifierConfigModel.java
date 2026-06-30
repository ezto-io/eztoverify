package org.keycloak.models;

import lombok.*;

import java.util.List;
import java.util.Map;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class APIVerifierConfigModel {

    private String id;
    String providerName;
    String verifierName;
    String requestBody;
    String responseBody;
    Map<String, String> claims;
    String category;
}
