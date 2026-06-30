package com.qlikverify.extensions.admin.controllers.workflows.misc.helper;

import com.qlikverify.extensions.admin.controllers.workflows.handlers.YoutrackHandler;
import com.qlikverify.extensions.admin.misc.helpers.JsonUtil;
import org.jboss.logging.Logger;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import static com.qlikverify.extensions.admin.controllers.workflows.misc.Constants.*;

/**
 * This class represents a Singleton for general utility functions.
 * It ensures that only one instance of WorkflowHelper is created in a thread-safe manner.
 * The Initialization-on-demand holder idiom is used for lazy loading and efficiency.
 */
public class WorkflowHelper {

    //~ Instance fields ====================================================================================================================

    private static final Logger logger = Logger.getLogger(WorkflowHelper.class);
    private static final String LOG_TAG = "WORKFLOWS:GENERAL:HELPER:: ";

    //~ Constructor =======================================================================================================================

    protected WorkflowHelper() {}

    //~ Methods ============================================================================================================================

    /**
     * Retrieves the singleton instance of the WorkflowHelper class.
     *
     * @return The singleton instance of the WorkflowHelper.
     */
    public static WorkflowHelper getInstance() {
        return WorkflowHelperInstanceHolder.INSTANCE;
    }

    //~ ===================================================================================================================================

    public Optional<UserModel> getAccountCreatorByRealm(KeycloakSession session, RealmModel realmModel) {
        return session.users().getUsersStream(realmModel)
                .filter(userModel -> !Validation.isBlank(userModel.getFirstAttribute("qv_is_account_creator"))).findFirst();
    }

    //~ ===================================================================================================================================



    //~ Inner Classes ======================================================================================================================

    /**
     * A static inner class used for lazy initialization of the FeaturesModuleGeneralHelper singleton instance.
     * This design pattern ensures that the singleton instance is created only when needed and is thread-safe.
     */
    private static class WorkflowHelperInstanceHolder {

        //~ Static fields/initializers =====================================================================================================

        private static final WorkflowHelper INSTANCE = new WorkflowHelper();
    }
}
