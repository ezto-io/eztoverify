package org.keycloak.models;

import java.io.Serializable;
import lombok.*;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProviderModel implements Serializable {

        String id;
        String providerName;
        String providerId;
        int providerPriority;
        String realmId;
        boolean live;
        boolean customPriority;

    }
