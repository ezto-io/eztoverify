package org.keycloak.onboarding;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;

public interface OnboardingStepProviderFactory extends ProviderFactory<OnboardingStepProvider> {

    List<ProviderConfigProperty> getConfig();

    Response process(MultivaluedMap<String, String> params, RealmModel realmModel);

    boolean validate(MultivaluedMap<String, String> params,RealmModel realmModel);

    boolean isCompleted(RealmModel realmModel);
}
