package org.keycloak.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtherClaimModel implements Serializable {

    String id;
    String verification;
    String otherClaims;
}
