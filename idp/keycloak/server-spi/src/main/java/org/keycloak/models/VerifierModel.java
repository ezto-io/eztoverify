package org.keycloak.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifierModel implements Serializable {

        String id;
        String verifierName;
        int priority;
        String realmId;
        boolean live;
        boolean customPriority;
        ProviderModel apiProvider;

    }
