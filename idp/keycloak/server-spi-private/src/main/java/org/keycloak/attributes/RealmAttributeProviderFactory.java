package org.keycloak.attributes;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderFactory;

public interface RealmAttributeProviderFactory extends ProviderFactory<RealmAttributeProvider> {

    String getDisplayName();

    String getAttributeName();

    String getValue(KeycloakSession session,RealmModel realmModel);

    boolean isSecret();

    boolean shouldRotate();

    boolean canBeViewed();

    boolean isReadOnly();

    boolean canUpdate();

    default boolean canGenerate(RealmModel realmModel){
        return false;
    }

    String tabName();

    String getHelpText();

    boolean isUnderTrial(KeycloakSession session,RealmModel realmModel);

    String subscribeLink(KeycloakSession session,RealmModel realmModel);

    default boolean isJson() {
        return false;
    }
    default boolean isScript() {return false;}
    default boolean isToggle() {return false;}

    default boolean showInSubTenant(RealmModel realmModel) {
        return !realmModel.getMode().equalsIgnoreCase("bo");
    }

    default boolean hidden(UserModel userModel) {
       return false;
    }
    default int priority() {
        return 0;
    }

    default boolean isNew() {
        return  false;
    }

    default String[] supportedRegions() {
        return new String[] {"all"};
    }
}
