/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.services.resources.admin.info;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.branding.RootBrandProvider;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.ciba.*;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentFactory;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.MFAProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.MarketplaceConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;
import org.keycloak.policy.ciba.CibaPolicy;
import org.keycloak.policy.password.PasswordPolicyProvider;
import org.keycloak.policy.password.PasswordPolicyProviderFactory;
import org.keycloak.policy.username.UsernamePolicyProvider;
import org.keycloak.policy.username.UsernamePolicyProviderFactory;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.info.*;
import org.keycloak.sms.SmsProvider;
import org.keycloak.sms.SmsProviderFactory;
import org.keycloak.sms.SmsType;
import org.keycloak.theme.Theme;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ServerInfoAdminResource {

    @Context
    private KeycloakSession session;

    private RealmModel realmModel;

    /**
     * Get themes, social providers, auth providers, and event listeners available on this server
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ServerInfoRepresentation getInfo(@QueryParam("realmName") String realmName) {
        if (realmName != null && session.realms().getRealmByName(realmName) != null) {
            this.realmModel = session.realms().getRealmByName(realmName);
        } else {
            this.realmModel = session.getContext().getRealm();
        }
        ServerInfoRepresentation info = new ServerInfoRepresentation();
        info.setSystemInfo(SystemInfoRepresentation.create(session.getKeycloakSessionFactory().getServerStartupTimestamp()));
        info.setMemoryInfo(MemoryInfoRepresentation.create());
        info.setProfileInfo(ProfileInfoRepresentation.create());

        setSocialProviders(info);
        setIdentityProviders(info);
        setThemes(info);
        setProviders(info);
        setProtocolMapperTypes(info);
        setBuiltinProtocolMappers(info);
        setClientInstallations(info);
        setPasswordPolicies(info);
        setUsernamePolicies(info);
        setActionPolicies(info);
        setEventListeners(info);
        setEmailProviders(info);
        setSmsProviders(info);
        setMFAProviders(info);
        info.setEnums(createEnumsMap(session.rootBrand().toExcludeEvtTypes(), EventType.class, OperationType.class, ResourceType.class));
        setBrandingDetails(info, realmModel);
        setupCiba(info, realmModel);
        setupIWS(info, realmModel);
        setupOIDC(info, realmModel);
        info.setThirdPartyReqEnabled(session.rootBrand().isThirdPartyRequestEnabled());
        info.setRootAccountName(Config.getAdminRealm());
        info.setVariant(EnvironmentReader.getValue("VARIANT", "ezto verify"));
        info.setDebuggerUrl(EnvironmentReader.getValue("DEBUGGER_URL", "https://debugger.qa.eztoauth.com/debug"));
        showSecuritySettings(info);
        info.setSupportedLocales(Constants.SUPPORTED_LOCALES);
        return info;
    }

    private void showSecuritySettings(ServerInfoRepresentation info) {
        info.setShowSecuritySettings(true);
    }

    private void setupCiba(ServerInfoRepresentation infoRep, RealmModel realmModel) {
        RootBrandProvider rootBrand = session.rootBrand();
        infoRep.setCibaFeatureEnabled(rootBrand.isCibaFeatureEnabled(realmModel));
        if (rootBrand.isCibaFeatureEnabled(realmModel)) {
            setCibaAuthProviders(infoRep, realmModel);
            setCibaNotificationProviders(infoRep);
            setCibaDeadLetterHookNotificationProviders(infoRep);
            infoRep.setCibaKeyTypes(CibaPolicy.getTypes());
            infoRep.setCibaComparators(CibaPolicy.getComparators());
        }
    }

    private void setupOIDC(ServerInfoRepresentation infoRep, RealmModel realmModel) {
        RootBrandProvider rootBrand = session.rootBrand();
        infoRep.setOIDCEnabled(rootBrand.isOIDCEnabled(realmModel));
    }

    private void setupIWS(ServerInfoRepresentation infoRep, RealmModel realmModel) {
        RootBrandProvider rootBrand = session.rootBrand();
        infoRep.setIWSEnabled(rootBrand.isIWSFeatureEnabled(realmModel));
    }

    private void setProviders(ServerInfoRepresentation info) {
        info.setComponentTypes(new HashMap<>());
        LinkedHashMap<String, SpiInfoRepresentation> spiReps = new LinkedHashMap<>();

        List<Spi> spis = new LinkedList<>(session.getKeycloakSessionFactory().getSpis());
        Collections.sort(spis, new Comparator<Spi>() {
            @Override
            public int compare(Spi s1, Spi s2) {
                return s1.getName().compareTo(s2.getName());
            }
        });

        for (Spi spi : spis) {
            SpiInfoRepresentation spiRep = new SpiInfoRepresentation();
            spiRep.setInternal(spi.isInternal());

            List<String> providerIds = new LinkedList<>(session.listProviderIds(spi.getProviderClass()));
            Collections.sort(providerIds);

            Map<String, ProviderRepresentation> providers = new HashMap<>();

            if (providerIds != null) {
                for (String name : providerIds) {
                    ProviderRepresentation provider = new ProviderRepresentation();
                    ProviderFactory<?> pi = session.getKeycloakSessionFactory().getProviderFactory(spi.getProviderClass(), name);
                    provider.setOrder(pi.order());
                    if (pi instanceof RequiredActionFactory) {
                        RequiredActionFactory requiredActionFactory = ((RequiredActionFactory) pi);
                        Map<String, String> maps = new HashMap<>();
                        maps.put("is_policy_required", String.valueOf(requiredActionFactory.isPolicyRequired()));
                        provider.setAdditionalInfo(maps);
                    }
//                    if (ServerInfoAwareProviderFactory.class.isAssignableFrom(pi.getClass())) {
//                        provider.setOperationalInfo(((ServerInfoAwareProviderFactory) pi).getOperationalInfo());
//                    }
                    if (pi instanceof ConfiguredProvider) {
                        if (name != "kerberos") {
                            ComponentTypeRepresentation rep = new ComponentTypeRepresentation();
                            rep.setId(pi.getId());
                            ConfiguredProvider configured = (ConfiguredProvider) pi;
                            rep.setHelpText(configured.getHelpText());
                            List<ProviderConfigProperty> configProperties = configured.getConfigProperties(session);
                            if (configProperties == null || configProperties.isEmpty())
                                configProperties = Collections.EMPTY_LIST;
                            rep.setProperties(ModelToRepresentation.toRepresentation(configProperties));
                            if (pi instanceof ComponentFactory) {
                                rep.setMetadata(((ComponentFactory) pi).getTypeMetadata());
                            }
                            List<ComponentTypeRepresentation> reps = info.getComponentTypes().get(spi.getProviderClass().getName());
                            if (reps == null) {
                                reps = new LinkedList<>();
                                info.getComponentTypes().put(spi.getProviderClass().getName(), reps);
                            }
                            reps.add(rep);
                        }
                    }
                    providers.put(name, provider);
                }
            }
            spiRep.setProviders(providers);

            spiReps.put(spi.getName(), spiRep);
        }
        info.setProviders(spiReps);
    }

    private void setThemes(ServerInfoRepresentation info) {
        info.setThemes(new HashMap<>());

        for (Theme.Type type : Theme.Type.values()) {
            List<String> themeNames = filterThemes(type, new LinkedList<>(session.theme().nameSet(type)));
            Collections.sort(themeNames);

            List<ThemeInfoRepresentation> themes = new LinkedList<>();
            info.getThemes().put(type.toString().toLowerCase(), themes);

            for (String name : themeNames) {
                try {
                    Theme theme = session.theme().getTheme(name, type);
                    ThemeInfoRepresentation ti = new ThemeInfoRepresentation();
                    ti.setName(name);

                    String locales = theme.getProperties().getProperty("locales");
                    if (locales != null) {
                        ti.setLocales(locales.replaceAll(" ", "").split(","));
                    }

                    themes.add(ti);
                } catch (IOException e) {
                    throw new WebApplicationException("Failed to load themes", e);
                }
            }
        }
    }

    private LinkedList<String> filterThemes(Theme.Type type, LinkedList<String> themeNames) {
        LinkedList<String> filteredNames = new LinkedList<>(themeNames);

        boolean filterAccountV2 = (type == Theme.Type.ACCOUNT) &&
                !Profile.isFeatureEnabled(Profile.Feature.ACCOUNT2);
        boolean filterAdminV2 = (type == Theme.Type.ADMIN) &&
                !Profile.isFeatureEnabled(Profile.Feature.ADMIN2);

        if (filterAccountV2 || filterAdminV2) {
            filteredNames.remove("keycloak.v2");
            filteredNames.remove("rh-sso.v2");
        }

        return filteredNames;
    }

    private void setSocialProviders(ServerInfoRepresentation info) {
        info.setSocialProviders(new LinkedList<>());
        Stream<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class);
        setIdentityProviders(providerFactories, info.getSocialProviders(), "Social");
    }

    private void setIdentityProviders(ServerInfoRepresentation info) {
        info.setIdentityProviders(new LinkedList<>());
        Stream<ProviderFactory> providerFactories = session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class);
        setIdentityProviders(providerFactories, info.getIdentityProviders(), "User-defined");

        providerFactories = session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class);
        setIdentityProviders(providerFactories, info.getIdentityProviders(), "Social");
    }

    public void setIdentityProviders(Stream<ProviderFactory> factories, List<Map<String, String>> providers, String groupName) {
        List<Map<String, String>> providerMaps = factories
                .map(IdentityProviderFactory.class::cast)
                .map(factory -> {
                    Map<String, String> data = new HashMap<>();
                    data.put("groupName", groupName);
                    data.put("name", factory.getName());
                    data.put("id", factory.getId());
                    return data;
                })
                .collect(Collectors.toList());

        providers.addAll(providerMaps);
    }

    private void setClientInstallations(ServerInfoRepresentation info) {
        HashMap<String, List<ClientInstallationRepresentation>> clientInstallations = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(ClientInstallationProvider.class)
                .map(ClientInstallationProvider.class::cast)
                .collect(
                        Collectors.toMap(
                                ClientInstallationProvider::getProtocol,
                                this::toClientInstallationRepresentation,
                                (l1, l2) -> listCombiner(l1, l2),
                                HashMap::new
                        )
                );
        info.setClientInstallations(clientInstallations);

    }

    private void setProtocolMapperTypes(ServerInfoRepresentation info) {
        HashMap<String, List<ProtocolMapperTypeRepresentation>> protocolMappers = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(ProtocolMapper.class)
                .map(ProtocolMapper.class::cast)
                .collect(
                        Collectors.toMap(
                                ProtocolMapper::getProtocol,
                                this::toProtocolMapperTypeRepresentation,
                                (l1, l2) -> listCombiner(l1, l2),
                                HashMap::new
                        )
                );
        info.setProtocolMapperTypes(protocolMappers);
    }

    private void setBuiltinProtocolMappers(ServerInfoRepresentation info) {
        Map<String, List<ProtocolMapperRepresentation>> protocolMappers = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(LoginProtocol.class)
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> {
                            LoginProtocolFactory factory = (LoginProtocolFactory) p;
                            return factory.getBuiltinMappers().values().stream()
                                    .map(ModelToRepresentation::toRepresentation)
                                    .collect(Collectors.toList());
                        })
                );
        info.setBuiltinProtocolMappers(protocolMappers);
    }

    private void setPasswordPolicies(ServerInfoRepresentation info) {
        List<PasswordPolicyTypeRepresentation> passwordPolicyTypes = session.getKeycloakSessionFactory().getProviderFactoriesStream(PasswordPolicyProvider.class)
                .map(PasswordPolicyProviderFactory.class::cast)
                .map(factory -> {
                    PasswordPolicyTypeRepresentation rep = new PasswordPolicyTypeRepresentation();
                    rep.setId(factory.getId());
                    rep.setDisplayName(factory.getDisplayName());
                    rep.setConfigType(factory.getConfigType());
                    rep.setDefaultValue(factory.getDefaultConfigValue());
                    rep.setMultipleSupported(factory.isMultiplSupported());
                    return rep;
                })
                .collect(Collectors.toList());
        info.setPasswordPolicies(passwordPolicyTypes);
    }

    private void setUsernamePolicies(ServerInfoRepresentation info) {
        info.setUsernamePolicies(new LinkedList<>());
        for (ProviderFactory f : session.getKeycloakSessionFactory().getProviderFactories(UsernamePolicyProvider.class)) {
            UsernamePolicyProviderFactory factory = (UsernamePolicyProviderFactory) f;
            UsernamePolicyTypeRepresentation rep = new UsernamePolicyTypeRepresentation();
            rep.setId(factory.getId());
            rep.setDisplayName(factory.getDisplayName());
            rep.setConfigType(factory.getConfigType());
            rep.setDefaultValue(factory.getDefaultConfigValue());
            rep.setMultipleSupported(factory.isMultiplSupported());
            info.getUsernamePolicies().add(rep);
        }
    }

    private void setActionPolicies(ServerInfoRepresentation info) {
        info.setActionPolicies(new LinkedList<>());
        Stream<ActionPolicyProviderFactory> actionPolicyProviderFactoryStream = session.getKeycloakSessionFactory().getProviderFactoriesStream(ActionPolicyProvider.class).map(x -> (ActionPolicyProviderFactory) x);
        String[] supportedModes = realmModel.getMode().equalsIgnoreCase("bo") ? new String[]{"bo"} : new String[]{"wiam", "ciam"};
        actionPolicyProviderFactoryStream
                .filter(x -> Arrays.stream(x.supportedModes())
                        .anyMatch(mode -> Arrays.asList(supportedModes).contains(mode)))
                .forEach(factory -> {
                    ActionPolicyTypeRepresentation rep = new ActionPolicyTypeRepresentation();
                    rep.setId(factory.getId());
                    rep.setDisplayName(factory.getDisplayName());
                    rep.setConfigType(factory.getConfigType());
                    rep.setDefaultValue(factory.getDefaultConfigValue());
                    rep.setMultipleSupported(factory.isMultiplSupported());
                    if (ActionPolicyProvider.LIST_CONFIG_TYPE.equalsIgnoreCase(factory.getConfigType())) {
                        rep.setConfigValues(factory.getList(session));
                    }
                    info.getActionPolicies().add(rep);
                });
    }

    private void setEventListeners(ServerInfoRepresentation info) {
        info.setEventListeners(new LinkedList<>());
        session.getKeycloakSessionFactory().getProviderFactoriesStream(EventListenerProvider.class)
                .filter(x -> !((EventListenerProviderFactory) x).isInBuilt())
                .map(f -> (EventListenerProviderFactory) f).forEach(factory -> {
                    EventListenerRepresentation rep = new EventListenerRepresentation();
                    rep.setId(factory.getId());
                    rep.setInBuilt(factory.isInBuilt());
                    rep.setHasConfig(factory.isConfigurable());
                    rep.setDescription(factory.getHelpText());
                    rep.setDisplayName(factory.getDisplayName());
                    rep.setLogo(factory.getLogo());
                    rep.setHowToDo(factory.getHowTodo());
                    info.getEventListeners().add(rep);
                });
    }

    private void setEmailProviders(ServerInfoRepresentation info) {
        info.setEmailProviders(new LinkedList<>());
        for (ProviderFactory f : session.getKeycloakSessionFactory().getProviderFactories(EmailSenderProvider.class)) {

            EmailSenderProviderFactory factory = (EmailSenderProviderFactory) f;
            if (!factory.isInBuilt()) {
                EmailProviderRepresentation rep = new EmailProviderRepresentation();
                rep.setId(factory.getId());
                rep.setProviderClass(factory.getProviderClass());
                rep.setInBuilt(factory.isInBuilt());
                rep.setHasConfig(factory.isConfigurable());
                rep.setDisplayName(factory.getDisplayName());
                rep.setLogo(factory.getLogo());
                rep.setHowToDo(factory.getHowTodo());
                info.getEmailProviders().add(rep);
            }
        }
    }

    private void setCibaAuthProviders(ServerInfoRepresentation info, RealmModel realmModel) {
        info.setCibaAuthProviders(new LinkedList<>());
        for (ProviderFactory f : session.getKeycloakSessionFactory().getProviderFactories(CibaAuthProvider.class)) {

            CibaAuthProviderFactory factory = (CibaAuthProviderFactory) f;
            CibaAuthProviderRepresentation rep = new CibaAuthProviderRepresentation();
            rep.setId(factory.getId());
            rep.setHasConfig(factory.isConfigurable());
            rep.setDisplayName(factory.getDisplayName());
            rep.setTemplateSupported(factory.isTemplateSupported());
            rep.setConfigured(isCibaAuthorizerConfiguredForRealm(factory.getId()));
            info.getCibaAuthProviders().add(rep);
        }
    }

    // Authorizers that need a realm-level prerequisite (a configured vendor
    // account) report false here so the admin console greys them out until the
    // prerequisite exists. Authorizers configured per-client in-place default to
    // true (never greyed). Currently only Twilio IVR has a realm prerequisite.
    private boolean isCibaAuthorizerConfiguredForRealm(String providerId) {
        if (!"twilio-ivr-ciba-authorizer".equals(providerId)) {
            return true;
        }
        RealmModel adminRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
        if (adminRealm == null) {
            return false;
        }
        String fromNo = adminRealm.getAttribute("TWILIO_FROM_NO");
        String flowUrl = adminRealm.getAttribute("TWILIO_FLOW_URL");
        return fromNo != null && !fromNo.isEmpty() && flowUrl != null && !flowUrl.isEmpty();
    }

    private void setCibaNotificationProviders(ServerInfoRepresentation info) {
        info.setCibaNotificationProviders(new LinkedList<>());
        for (ProviderFactory f : session.getKeycloakSessionFactory().getProviderFactories(CibaNotificationProvider.class)) {

            CibaNotificationProviderFactory factory = (CibaNotificationProviderFactory) f;
            CibaNotificationProviderRepresentation rep = new CibaNotificationProviderRepresentation();
            rep.setId(factory.getId());
            rep.setHasConfig(factory.isConfigurable());
            rep.setDisplayName(factory.getDisplayName());
            info.getCibaNotificationProviders().add(rep);
        }
    }

    private void setCibaDeadLetterHookNotificationProviders(ServerInfoRepresentation info) {
        info.setCibaDeadLetterHookNotificationProviders(new LinkedList<>());
        for (ProviderFactory f : session.getKeycloakSessionFactory().getProviderFactories(CibaDeadLetterHookNotificationProvider.class)) {

            CibaDeadLetterHookNotificationProviderFactory factory = (CibaDeadLetterHookNotificationProviderFactory) f;
            CibaDeadLetterHookNotificationProviderRepresentation rep = new CibaDeadLetterHookNotificationProviderRepresentation();
            rep.setId(factory.getId());
            rep.setHasConfig(factory.isConfigurable());
            rep.setDisplayName(factory.getDisplayName());
            info.getCibaDeadLetterHookNotificationProviders().add(rep);
        }
    }

    private void setSmsProviders(ServerInfoRepresentation info) {
        info.setSmsProviders(new HashMap<>());
        Map<String, List<SmsProviderRepresentation>> smsproviders = new HashMap<>();
        List<SmsProviderRepresentation> normalProviders = new LinkedList<>();
        List<SmsProviderRepresentation> premiumProviders = new LinkedList<>();
        for (ProviderFactory f : session.getKeycloakSessionFactory().getProviderFactories(SmsProvider.class)) {
            SmsProviderFactory factory = (SmsProviderFactory) f;
            SmsProviderRepresentation rep = new SmsProviderRepresentation();
            rep.setId(factory.getId());
            rep.setProviderClass(factory.getProviderClass());
            rep.setHasConfig(factory.isConfigurable());
            rep.setDisplayName(factory.getDisplayName());
            rep.setLogo(factory.getLogo());
            rep.setNew(factory.isNew());
            rep.setHowToDo(factory.getHowTodo());
            rep.setDescription(factory.getHelpText());

            if (factory.getType().equals(SmsType.PREMIUM)) {
                premiumProviders.add(rep);
            } else {
                normalProviders.add(rep);
            }
        }
        smsproviders.put("normal", normalProviders);
        smsproviders.put("premium", premiumProviders);
        info.setSmsProviders(smsproviders);
    }

    private void setMFAProviders(ServerInfoRepresentation info) {
        info.setMfaProviders(new LinkedList<>());
        for (ProviderFactory f : session.getKeycloakSessionFactory().getProviderFactories(MFAProvider.class)) {

            MFAProviderFactory factory = (MFAProviderFactory) f;
            boolean isDefault = factory.isDefault();
            if (!isDefault) {
                Optional<MarketplaceConfigModel> configModel = realmModel.getMarketplaceConfigsStream().filter(x -> x.getProviderId().equalsIgnoreCase(factory.getId())).findFirst();
                isDefault = configModel.isPresent();
            }
            if (isDefault) {
                MFAProviderRepresentation rep = new MFAProviderRepresentation();
                rep.setId(factory.getId());
                rep.setProviderClass(factory.getProviderClass());
                rep.setHasConfig(factory.isConfigurable());
                rep.setDisplayName(factory.getDisplayName());
                rep.setLogo(factory.getLogo());
                rep.setHowToDo(factory.getHowTodo());
                rep.setDescription(factory.getHelpText());
                rep.setNew(factory.isNew());
                rep.setDefault(factory.isDefault());
                info.getMfaProviders().add(rep);
            }
        }
    }

    private void setBrandingDetails(ServerInfoRepresentation info, RealmModel realmModel) {
        BrandRepresentation brandRepresentation = new BrandRepresentation();
        RootBrandProvider rootBrandProvider = session.rootBrand();
        brandRepresentation.setBrandingEnabled(rootBrandProvider.isBrandingEnabled());
        brandRepresentation.setModeEnabled(rootBrandProvider.isModeEnabled());
        brandRepresentation.setAttributes(rootBrandProvider.getAdminBrandAttributes(realmModel));
        info.setBrandInfo(brandRepresentation);
    }

    private List<ClientInstallationRepresentation> toClientInstallationRepresentation(ClientInstallationProvider provider) {
        ClientInstallationRepresentation rep = new ClientInstallationRepresentation();
        rep.setId(provider.getId());
        rep.setHelpText(provider.getHelpText());
        rep.setDisplayType(provider.getDisplayType());
        rep.setProtocol(provider.getProtocol());
        rep.setDownloadOnly(provider.isDownloadOnly());
        rep.setFilename(provider.getFilename());
        rep.setMediaType(provider.getMediaType());
        return Arrays.asList(rep);
    }

    private List<ProtocolMapperTypeRepresentation> toProtocolMapperTypeRepresentation(ProtocolMapper mapper) {
        ProtocolMapperTypeRepresentation rep = new ProtocolMapperTypeRepresentation();
        rep.setId(mapper.getId());
        rep.setName(mapper.getDisplayType());
        rep.setHelpText(mapper.getHelpText());
        rep.setCategory(mapper.getDisplayCategory());
        rep.setPriority(mapper.getPriority());
        List<ProviderConfigProperty> configProperties = mapper.getConfigProperties(session);
        rep.setProperties(ModelToRepresentation.toRepresentation(configProperties));
        return Arrays.asList(rep);
    }

    private static <T> List<T> listCombiner(List<T> list1, List<T> list2) {
        return Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toList());
    }

    private static Map<String, List<String>> createEnumsMap(List<Enum> toExcludeEnum, Class... enums) {
        Map<String, List<String>> m = new HashMap<>();
        for (Class e : enums) {
            String n = e.getSimpleName();
            n = Character.toLowerCase(n.charAt(0)) + n.substring(1);

            List<String> l = new LinkedList<>();
            for (Object c : e.getEnumConstants()) {
                if (toExcludeEnum.contains(c)) {
                    continue;
                }
                l.add(c.toString());

            }
            Collections.sort(l);

            m.put(n, l);
        }
        return m;
    }

}
