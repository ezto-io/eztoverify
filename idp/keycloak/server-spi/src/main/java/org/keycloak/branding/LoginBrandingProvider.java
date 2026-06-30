package org.keycloak.branding;

import org.keycloak.models.BrandingModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface LoginBrandingProvider extends Provider {
    BrandingModel getAppBranding(RealmModel realmModel, String clientId);
    BrandingModel getDefaultBrand(RealmModel realmModel);
    boolean updateAppBranding(RealmModel realmModel,String clientId, String brandId);
    BrandingModel createBranding(RealmModel realmModel,String brandName, boolean isRealmBrand, Map<String,String> config);
    BrandingModel updateBranding(RealmModel realmModel,String id, Map<String,String> config);
    boolean removeBranding(RealmModel realmModel,String brandId);
    Stream<BrandingModel> getAllBrands(RealmModel realmModel, Integer first, Integer max);
    BrandingModel getBrandById(RealmModel realmModel,String id);
    BrandingModel getBrandByName(RealmModel realmModel,String name);
    List<ProviderConfigProperty> getBrandingConfig(RealmModel realmModel);
}
