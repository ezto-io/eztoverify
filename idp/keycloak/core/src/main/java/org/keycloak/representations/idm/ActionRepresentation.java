package org.keycloak.representations.idm;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Data
@Getter
@Setter
public class ActionRepresentation {
    private String alias;
    private String actionId;
    private Long lastUpdatedTimestamp;
    private boolean isLive;
    private boolean isInBuilt;
    private String actionType;
    private String actionVariables;
    private boolean isSubWorkflow;
    private boolean isMarketplace;
}
