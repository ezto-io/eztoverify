package com.qlikverify.backoffice.services.costCalculation;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CostCalculationForWorkFlowService {

    private static final Logger logger = Logger.getLogger(CostCalculationForWorkFlowService.class);

    private CostCalculationForWorkFlowService() {
        throw new IllegalStateException("Utility class");
    }

    public static Response calculateCost(KeycloakSession session, RealmModel realm, String actionId) {
        Map<String, Object> responseEntity = new HashMap<>();
        responseEntity.put("estimatedCost", Arrays.asList(0.0));
        responseEntity.put("costForEachVerificationModule", new HashMap<>());
        responseEntity.put("currentPlan", "Lifetime Free Plan");
        responseEntity.put("walletCurrency", "USD");
        responseEntity.put("planDescription", "estimated-cost-description");
        return Response.status(Response.Status.OK).entity(responseEntity).build();
    }
}
