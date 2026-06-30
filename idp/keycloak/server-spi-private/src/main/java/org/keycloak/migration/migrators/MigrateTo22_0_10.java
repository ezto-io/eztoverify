package org.keycloak.migration.migrators;

import org.jboss.logging.Logger;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultRequiredActions;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo22_0_10 implements Migration {

    private static final Logger LOG = Logger.getLogger(MigrateTo22_0_10.class);

    public static final ModelVersion VERSION = new ModelVersion("22.0.10");

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realm -> migrateRealm(session, realm));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(session, realm);
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    private void migrateRealm(KeycloakSession session, RealmModel realm) {
        DefaultRequiredActions.addDeleteCredentialAction(realm);
    }
}