package com.qlikverify.services.theme;

import com.qlikverify.services.theme.helpers.GeneralHelpers;
import com.qlikverify.services.theme.migration.qvmigrators.QVMigrationModelManager;
import com.qlikverify.services.theme.responses.QlikVerifyResponse;
import com.qlikverify.services.theme.subscriptionpolicies.*;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.actiontoken.ActionTokenConstants;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Version;
import org.keycloak.common.util.Base64Url;
import org.keycloak.component.ComponentModel;
import org.keycloak.locale.LocaleType;
import org.keycloak.models.*;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.onboarding.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.clientregistration.policy.impl.CanMultiTenantRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.impl.CheckLicenseClientRegistrationPolicyFactory;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.qlikverify.services.theme.helpers.GeneralHelpers.convertMultiToRegularMap;
import static com.qlikverify.services.theme.onboarding.Constants.*;
import static org.keycloak.models.CibaConfig.OIDC_CIBA_GRANT_ENABLED;
import static org.keycloak.models.Constants.ADMIN_CONSOLE_CLIENT_ID;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.services.messages.Messages.*;
import static org.keycloak.utils.GeneralHelper.isOnboardingStepRequired;

public class QlikVerifyOnboardingProvider implements OnboardingProvider, OnboardingProviderFactory {

    public static final String QV_IS_ACCOUNT_CREATOR = "qvIsAccountCreator";

    KeycloakSession session;

    protected static final Logger logger = Logger.getLogger(QlikVerifyOnboardingProvider.class);

    @Override
    public void onApplicationOnboarding(ClientModel clientModel, RealmModel realmModel) {
        String cibaOIDCGrant = clientModel.getAttribute(OIDC_CIBA_GRANT_ENABLED);
        boolean isCiba = cibaOIDCGrant != null && Boolean.parseBoolean(cibaOIDCGrant);

        if (isCiba) {
            ProtocolMapperModel mapper = new ProtocolMapperModel();
            mapper.setName(clientModel.getName() + "_mapper");
            mapper.setProtocolMapper("ciba_protocol-mapper");
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            clientModel.addProtocolMapper(mapper);
        }
    }

    @Override
    public void onTenantOnboarding(RealmModel realm, RealmRepresentation realmRepresentation) {
        realm.setUserInviteWay("otp");
        //setUp ClientRegistrationPolicies
        addAuthPolicies(realm, ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.AUTHENTICATED));

    }

    @Override
    public Response onTenantPostOnboarding(KeycloakSession session, RealmModel createdRealm, String adminEmailId) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel adminRealm = realmManager.getKeycloakAdminstrationRealm();
        RealmRepresentation realmRep = new RealmRepresentation();
        realmRep.setRealm(createdRealm.getName());
        if(createdRealm.getMode() != null)
        {
            realmRep.setMode(createdRealm.getMode());
        }

        if(createdRealm.getPreferredLoginLocalesStream() == null || !createdRealm.getPreferredLoginLocalesStream().findAny().isPresent()){
            HashSet<String> preferredLocales = new HashSet<>();
            preferredLocales.add("en");
            realmRep.setPreferredLoginLocales(preferredLocales);
        }

        RepresentationToModel.importRealm(session, realmRep, createdRealm, false);
        createdRealm.setEnabled(true);
        //create AdminUser
        UserModel adminUser = session.users().addUser(createdRealm, adminEmailId);
        adminUser.setEmail(adminEmailId);
        adminUser.setEnabled(true);

        //Set RealmAdmin Role
        RoleModel role = createdRealm.getClientByClientId(ActionTokenConstants.REALM_MGMT_CLIENT).getRole(ActionTokenConstants.ADMIN_USER_ROLE);
        adminUser.grantRole(role);
        adminUser.setEmail(adminEmailId);
        adminUser.setEmailVerified(true);

        UserRepresentation rep = ModelToRepresentation.toBriefRepresentation(adminUser);
        List<CredentialRepresentation> credentials = new ArrayList<>();
        CredentialRepresentation pass = new CredentialRepresentation();
        pass.setType(PASSWORD);
        pass.setValue(HmacOTP.generateSecret(10));
        pass.setTemporary(true);
        pass.setUserLabel("userInvited");
        credentials.add(pass);
        rep.setCredentials(credentials);
        RepresentationToModel.createCredentials(rep, session, createdRealm, adminUser, true);
        adminUser.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Invited");
        adminUser.setSingleAttribute(UserModel.USER_INVITE_CLIENT, ADMIN_CONSOLE_CLIENT_ID);
        if(session.users().getUsersStream(createdRealm).count() == 1 && adminUser.getRoleMappingsStream().anyMatch(x -> x.getName().equalsIgnoreCase("workspace-admin")))
            adminUser.setSingleAttribute(QV_IS_ACCOUNT_CREATOR, String.valueOf(true));

        // Tenant-scoped login URL = tenant admin console. On-prem (single-host, DASHBOARD_URL set)
        // getWorkspaceUrl returns the bare base, so the tenant must be carried in the path;
        // on cloud the base is the tenant subdomain. Either way append /auth/admin/{tenant}/console/.
        String ssoUrl = ModelToRepresentation.getWorkspaceUrl(session, createdRealm)
                + "/auth/admin/" + createdRealm.getName() + "/console/";

        boolean isEmailSent = GeneralHelpers.sendVerificationEmail(session, adminRealm, ssoUrl, pass.getValue(), createdRealm, adminEmailId);

        if (isEmailSent) {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(VERIFICATION_EMAIL_SENT, OK);
            Map<String, String> qvResponseData = new HashMap<>();
            qvResponseData. put("tenantId", createdRealm.getId());
            qvResponse.setData(qvResponseData);

            return Response.status(Response.Status.OK).entity(qvResponse).build();
        } else {
            QlikVerifyResponse qvResponse = new QlikVerifyResponse(UNABLE_TO_SEND_EMAIL, GENERAL_ERROR);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(qvResponse).build();
        }
    }

    @Override
    public void onMasterOnboarding(RealmModel realmModel) {
        // NOP
    }

    @Override
    public void onWelcomePreOnboarding(RealmModel realmModel) {
        // NOP
    }

    @Override
    public Response onWelcomePostOnboarding(RealmModel realmModel, MultivaluedMap<String, String> formData, HttpHeaders headers, String stepProviderId) {
        csrfCheck(formData, headers);

        OnboardingStepProviderFactory onboardingStepProviderFactory = (OnboardingStepProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(OnboardingStepProvider.class, stepProviderId);

        if (onboardingStepProviderFactory == null)
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        boolean isSwitchStep = Boolean.parseBoolean(formData.getFirst("isSwitch"));

        if (isSwitchStep)
            return createWelcomePage(null, null, formData, formData.getFirst("switchStep"));


        Response response = onboardingStepProviderFactory.process(formData, realmModel);

        expireCsrfCookie();

        if (response.getStatus() != 200) {
            return createWelcomePage(null, response.getEntity(), formData, null);
        }


        return createWelcomePage(response.getEntity(), null, formData, null);
    }

    @Override
    public void onMigrate(KeycloakSession session) {
        QVMigrationModelManager.migrate(session);
    }

    @Override
    public void onLazyUserMigrate(KeycloakSession session, RealmModel realmModel, UserModel userModel) {
        QVMigrationModelManager.lazyUserMigrate(session, realmModel, userModel);
    }

    @Override
    public OnboardingRepresentation welcomeOnboardingSteps(KeycloakSession session, String switchStep) {
        OnboardingRepresentation onboardingRepresentation = new OnboardingRepresentation();
        List<OnboardingStep> steps = new ArrayList<>();
        RealmModel realmModel = session.getContext().getRealm();

        Set<OnboardingStepProvider> onboardingStepProvider = session.getAllProviders(OnboardingStepProvider.class);
        if (onboardingStepProvider == null || onboardingStepProvider.isEmpty())
            return onboardingRepresentation;

        onboardingStepProvider = onboardingStepProvider.stream().filter(OnboardingStepProvider::shouldIncludeBasedOnMode).collect(Collectors.toSet());
        List<OnboardingStepProvider> sorted = onboardingStepProvider.stream().sorted(Comparator.comparingInt(OnboardingStepProvider::priority)).collect(Collectors.toList());

        final String lastStepProviderId = sorted.get(sorted.size() - 1).getProviderId();
        final OnboardingStepProviderFactory lastProviderFactory =
                (OnboardingStepProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(OnboardingStepProvider.class, lastStepProviderId);
        boolean islastStepCompleted = lastProviderFactory.isCompleted(session.getContext().getRealm());
        if (!Objects.equals(realmModel.getName(), Config.getAdminRealm())) {
            sorted = addOwnProvidersToList(sorted, realmModel, islastStepCompleted);

            //Triggers when subscription changes after tenant onboarding completed
            //Should only change if last step is completed
        }

        List<OnboardingStepProvider> finalSorted = sorted;
        sorted.forEach(x -> {
            OnboardingStep step = new OnboardingStep();
            step.setTitle(x.getTitle());
            step.setDescription(x.getDescription());
            step.setSkippable(x.isSkippable());
            step.setStepProviderId(x.getProviderId());
            OnboardingStepProviderFactory providerFactory = (OnboardingStepProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(OnboardingStepProvider.class, x.getProviderId());
            step.setProperties(providerFactory.getConfig());
            boolean isStepCompleted = providerFactory.isCompleted(session.getContext().getRealm());
            step.setStepCompleted(isStepCompleted);
            if (switchStep != null) {
                GeneralHelpers.addToCompletedStep(realmModel,
                        finalSorted.get(Integer.parseInt(switchStep) > 0 ? Integer.parseInt(switchStep) - 1 : 0).getProviderId());
                onboardingRepresentation.setCurrentStep(Integer.parseInt(switchStep));
            } else {
                if (!isStepCompleted && onboardingRepresentation.getCurrentStep() < 0) {
                    onboardingRepresentation.setCurrentStep(finalSorted.indexOf(x));
                }
            }
            boolean isFirstStep = x.priority() == finalSorted.get(0).priority();
            boolean isLastStep = x.priority() == finalSorted.get(finalSorted.size() - 1).priority();
            step.setFirstStep(isFirstStep);
            step.setLastStep(isLastStep);

            steps.add(step);
        });

        onboardingRepresentation.setSteps(steps);

        return onboardingRepresentation;
    }

    public List<OnboardingStepProvider> addOwnProvidersToList(List<OnboardingStepProvider> sorted, RealmModel realmModel, boolean islastStepCompleted){
        sorted = sorted.stream().filter(x -> !Objects.equals(x.getProviderId(), OB_ADMIN_SETUP)).collect(Collectors.toList());
        if (!realmModel.isEnabledOwnEmailProvider()) {
            sorted =
                    sorted.stream().filter(x -> !Objects.equals(x.getProviderId(), OB_ADMIN_EMAIL_SETUP)).collect(Collectors.toList());
        }

        if (!realmModel.isEnabledOwnSmsProvider()) {
            sorted =
                    sorted.stream().filter(x -> !Objects.equals(x.getProviderId(), OB_ADMIN_SMS_SETUP)).collect(Collectors.toList());
        }

        if (!realmModel.isEnabledOwnWhatsappProvider()) {
            sorted =
                    sorted.stream().filter(x -> !Objects.equals(x.getProviderId(), OB_ADMIN_WHATSAPP_SETUP)).collect(Collectors.toList());
        }

        final boolean obSubscriptionChange = session.getContext().getRealm().getAttribute("obSubscriptionChange", false);
        if (obSubscriptionChange && islastStepCompleted) {
            sorted = getIncompletedSteps(session, realmModel, sorted);
        }

        return sorted;
    }

    private static List<OnboardingStepProvider> getIncompletedSteps(KeycloakSession session, RealmModel realmModel,
                                                                    List<OnboardingStepProvider> sorted) {
        if (session.getContext().getRealm().getAttribute(CUSTOM_SUBSCRIPTION) != null) {
            List<String> changedList = Arrays.asList(session.getContext().getRealm().getAttribute(CUSTOM_SUBSCRIPTION).split(","));

            List<OnboardingStepProvider> updatedList = sorted.stream().filter(
                    x -> changedList.contains(x.getProviderId())
            ).collect(Collectors.toList());

            List<OnboardingStepProvider> completedStepsList = updatedList.stream().filter(x -> {
                final OnboardingStepProviderFactory providerFactory =
                        (OnboardingStepProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(OnboardingStepProvider.class, x.getProviderId());

                return !providerFactory.isCompleted(realmModel);
            }).collect(Collectors.toList());

            if (completedStepsList.isEmpty()) {
                realmModel.removeAttribute(CUSTOM_SUBSCRIPTION);
                return new ArrayList<>();
            } else {
                return updatedList;
            }

        }
        return sorted;
    }

    @Override
    public boolean isOnboardingCompleted() {
        if (session.getContext().getRealm() == null)
            session.getContext().setRealm(session.realms().getRealmByName(Config.getAdminRealm()));

        Set<OnboardingStepProvider> onboardingStepProvider = session.getAllProviders(OnboardingStepProvider.class);
        if (onboardingStepProvider == null || onboardingStepProvider.isEmpty())
            return false;
        onboardingStepProvider =
                onboardingStepProvider.stream().filter(OnboardingStepProvider::shouldIncludeBasedOnMode).collect(Collectors.toSet());
        List<OnboardingStepProvider> sorted = onboardingStepProvider.stream()
                .sorted(Comparator.comparingInt(OnboardingStepProvider::priority)).collect(Collectors.toList());
        if (!Objects.equals(session.getContext().getRealm().getName(), Config.getAdminRealm())) {
            sorted = sorted.stream().filter(x -> !Objects.equals(x.getProviderId(), OB_ADMIN_SETUP)).collect(Collectors.toList());
            if (!session.getContext().getRealm().isEnabledOwnEmailProvider()) {
                sorted =
                        sorted.stream().filter(x -> !Objects.equals(x.getProviderId(), OB_ADMIN_EMAIL_SETUP)).collect(Collectors.toList());
            }
            if (!session.getContext().getRealm().isEnabledOwnSmsProvider()) {
                sorted =
                        sorted.stream().filter(x -> !Objects.equals(x.getProviderId(), OB_ADMIN_SMS_SETUP)).collect(Collectors.toList());
            }
            if (!session.getContext().getRealm().isEnabledOwnWhatsappProvider()) {
                sorted =
                        sorted.stream().filter(x -> !Objects.equals(x.getProviderId(), OB_ADMIN_WHATSAPP_SETUP)).collect(Collectors.toList());
            }
        }

        AtomicInteger counter = new AtomicInteger();
        sorted.forEach(x -> {
            OnboardingStepProviderFactory providerFactory = (OnboardingStepProviderFactory) session
                    .getKeycloakSessionFactory().getProviderFactory(OnboardingStepProvider.class, x.getProviderId());
            boolean isStepCompleted = providerFactory.isCompleted(session.getContext().getRealm());
            if (isStepCompleted)
                counter.getAndIncrement();
        });

        return sorted.size() == counter.get();
    }

    @Override
    public void onDNSCompleted(RealmModel realmModel, String dnsName) {
        // NOP
    }

    @Override
    public void onDNSRemoved(RealmModel realmModel, String dnsName) {
        cleanupAttributes(realmModel);
    }

    @Override
    public OnboardingProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return "qlikverify-onboarding";
    }


    private void setupSSODesktop(RealmModel realm) {
        ClientModel accountClient = realm.getClientByClientId(Constants.SSO_DESKTOP_CLIENT_ID);
        if (accountClient == null) {

            ClientRepresentation rep = new ClientRepresentation();
            rep.setName("SSO Desktop");
            rep.setAppType("browser_based");
            List<String> redirectUris = new ArrayList<>();
            String env = EnvironmentReader.getValue("ENV");
            String desktopLoginRedirectUri = "https://ssodesktop" + (Objects.equals(env, "dev") ? "-dev" : "") +
                    ".qlikverify" +
                    ".com/login/success/"
                    + realm.getName().toLowerCase();
            String desktopLogoutRedirectUri = "https://ssodesktop" + (Objects.equals(env, "dev") ? "-dev" : "") +
                    ".qlikverify" +
                    ".com/logout/success/"
                    + realm.getName().toLowerCase();
            redirectUris.add(desktopLoginRedirectUri);
            redirectUris.add(desktopLogoutRedirectUri);
            rep.setRedirectUris(redirectUris);
            Map<String, String> attributes = new HashMap<>();
            attributes.put("login_theme", "default");
            attributes.put("isInBuilt", "true");
            rep.setAttributes(attributes);
            ClientModel clientModel = ClientManager.createClient(session, realm, rep, true);
            clientModel.setClientId(Constants.SSO_DESKTOP_CLIENT_ID);

            clientModel.setImplicitFlowEnabled(true);
            clientModel.setStandardFlowEnabled(false);
            clientModel.setDirectAccessGrantsEnabled(false);
            clientModel.setServiceAccountsEnabled(false);
            clientModel.setBearerOnly(false);

            setupDefaultActionAndPoliciesSSO(realm, clientModel, Constants.SSO_DESKTOP_CLIENT_ID);
        }
    }

    public static void addAuthenticationPolicy(RealmModel realm) {
        Optional<ComponentModel> componentModel = realm.getComponentsStream().filter(x -> x.getProviderId().equalsIgnoreCase(AuthenticationPolicyFactory.PROVIDER_ID)).findFirst();
        if(!componentModel.isPresent()) {
            String authPolicyType = ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.AUTHENTICATED);
            ComponentModel authCheckModel = createModelInstance("Check Authentication", realm, AuthenticationPolicyFactory.PROVIDER_ID, authPolicyType);
            realm.addComponentModel(authCheckModel);
        }
    }

    private void setupDefaultActionAndPoliciesSSO(RealmModel realm, ClientModel adminConsole, String clientId) {
        PolicyModel model = new PolicyModel();

        model.setPolicyName(clientId + "_temppwd_policy");
        model.setPolicyType(ACTION);
        model.setPolicyValue("temppassword() and decision()");
        model.setRealmPolicy(false);
        model.setInBuilt(true);
        model = session.policies().addPolicy(realm, model);


        PolicyModel emailPolicyModel = new PolicyModel();

        emailPolicyModel.setPolicyName(clientId + "_email_policy");
        emailPolicyModel.setPolicyType(ACTION);
        emailPolicyModel.setPolicyValue("isemailverified(false) and decision()");
        emailPolicyModel.setRealmPolicy(false);
        emailPolicyModel.setInBuilt(true);

        emailPolicyModel = session.policies().addPolicy(realm, emailPolicyModel);


        ActionModel actionModel = new ActionModel();
        actionModel.setAlias(clientId + "_action");
        actionModel.setInBuilt(true);
        actionModel = realm.addAction(actionModel);

        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setAlias(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setName(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setProviderId(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setDefaultAction(false);
        requiredAction.setPolicyId(model.getId());
        requiredAction.setPriority(0);
        requiredAction.setEnabled(true);
        requiredAction.setActionId(actionModel.getId());
        realm.addRequiredActionProvider(requiredAction);

        RequiredActionProviderModel emailRA = new RequiredActionProviderModel();
        emailRA.setAlias(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setName(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setProviderId(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setDefaultAction(false);
        emailRA.setPolicyId(emailPolicyModel.getId());
        emailRA.setPriority(1);
        emailRA.setEnabled(true);
        emailRA.setActionId(actionModel.getId());
        realm.addRequiredActionProvider(emailRA);

        adminConsole.setAuthenticationFlowBindingOverride(ACTION, actionModel.getId());
    }

    private void csrfCheck(final MultivaluedMap<String, String> formData, HttpHeaders headers) {
        String formStateChecker = formData.getFirst("stateChecker");
        Cookie cookie = headers.getCookies().get(KEYCLOAK_STATE_CHECKER);
        if (cookie == null) {
            throw new ForbiddenException();
        }

        String cookieStateChecker = cookie.getValue();

        if (cookieStateChecker == null || !cookieStateChecker.equals(formStateChecker)) {
            throw new ForbiddenException();
        }
    }

    private Response createWelcomePage(Object successMessage, Object errorMessage, MultivaluedMap<String, String> formData, String switchStep) {
        try {
            Theme theme = getTheme();

            Map<String, Object> map = new HashMap<>();

            Locale locale = session.getContext().resolveLocale(null, LocaleType.ADMIN);
            map.put("locale", locale.toLanguageTag());
            map.put("locales", Constants.SUPPORTED_LOCALES);
            map.put("baseUri", session.getContext().getUri().getBaseUri());
            Properties messages = theme.getMessages(locale);
            map.put("msg", new MessageFormatterMethod(locale, messages));

            //setup common atttrs
            map.put("productName", Version.NAME);
            map.put("productNameFull", Version.NAME_FULL);
            map.put("properties", theme.getProperties());
            map.put("resourceUrl", session.getContext().getUri(UrlType.FRONTEND).getBaseUriBuilder().build());
            map.put("adminUrl", session.getContext().getUri(UrlType.ADMIN).getBaseUriBuilder().path("/admin/").path(
                    session.getContext().getRealm().getName()).path("/console/").build());
            map.put("resourcesPath", "resources/" + Version.RESOURCES_VERSION + "/" + theme.getType().toString().toLowerCase() + "/" + theme.getName());
            map.put("resourcesCommonPath", "resources/" + Version.RESOURCES_VERSION + "/common/keycloak");
            map.put("variant",EnvironmentReader.getValue("VARIANT", "ezto verify"));
            map.put("mode", session.getContext().getRealm().getMode());
            String websiteContactLink =  EnvironmentReader.getValue(WEBSITE_CONTACT_LINK, EZTO_CONTACT_LINK);
            String supportUrl = session.getContext().getRealm().getAttribute(SUPPORT_URL);
            if (Boolean.parseBoolean(session.getContext().getRealm().getAttribute(SUPPORT_CUSTOM_TOGGLE)) && StringUtil.isNotBlank(supportUrl)) {
                websiteContactLink = supportUrl;
            }
            map.put("websiteContactLink", websiteContactLink);
            map.put("privacyPolicyLink", EnvironmentReader.getValue("PRIVACY_POLICY_LINK", "https://app.termly.io/document/privacy-policy/5c0d5314-9eb1-4498-948a-d4f6751d060c"));
            map.put("termsAndConditionLink", EnvironmentReader.getValue("TERMS_AND_CONDITION_LINK", "https://app.termly.io/document/terms-of-service/0e94947b-0d93-415e-acbb-34a03f2f3283"));
            map.put("docsLink", EnvironmentReader.getValue("DOCS_LINK", ""));
            map.put("websiteLink", EnvironmentReader.getValue("WEBSITE_LINK", "https://ezto.io/"));
            map.put("marketplaceLink", EnvironmentReader.getValue("MARKETPLACE_LINK", "https://eztoauth.com/marketplace"));
            map.put("authenticationLink", EnvironmentReader.getValue("AUTHENTICATION_LINK", "https://eztoauth.com/platform/authentication"));
            map.put("applicationLink", EnvironmentReader.getValue("APPLICATION_LINK", "https://eztoauth.com/platform"));
            //setup onBoarding Steps
            OnboardingRepresentation obSteps = welcomeOnboardingSteps(session, switchStep);
            map.put("obSteps", obSteps);
            map.put("tenantName", session.getContext().getRealm().getDisplayName());
            boolean isOnboardingCompleted = isOnboardingCompleted();
            map.put("onboardingCompleted", isOnboardingCompleted);
            map.put("showMultiLanSelect", obSteps.getSteps() != null && obSteps.getCurrentStep() >= 0 &&
                    "ob-admin-workspace-setup".equals(obSteps.getSteps().get(obSteps.getCurrentStep()).getStepProviderId()));
            map.put("showPreview", isOnboardingStepRequired(obSteps));
            RealmModel masterRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
            HashSet<String> availableCategories = new HashSet<>();
            masterRealm.getEztovrfyMarketplaceCategoriesStream().map(EztovrfyMarketplaceCategoryModel::getCategory).forEach(availableCategories::add);
            map.put("availableCategories", availableCategories);

            if (isOnboardingCompleted) {
                GeneralHelpers.markAllStepsAsCompleted(session);
            }


            boolean isLocal = isLocal();
            map.put("localUser", isLocal);

            if (isLocal) {
                String stateChecker = setCsrfCookie();
                map.put("stateChecker", stateChecker);
            }
            if (successMessage != null) {
                map.put("successMessage", successMessage);
            }
            if (errorMessage != null) {

                map.put("errorMessage", errorMessage);
            }
            if (formData != null) {
                map.put("submittedData", convertMultiToRegularMap(formData));
            }

            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);

            Response.ResponseBuilder rb = Response.status(errorMessage == null ? Response.Status.OK : Response.Status.BAD_REQUEST)
                    .entity(result)
                    .cacheControl(CacheControlUtil.noCache());
            return rb.build();
        } catch (Exception e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void expireCsrfCookie() {
        String cookiePath = session.getContext().getUri().getPath();
        boolean secureOnly = session.getContext().getUri().getRequestUri().getScheme().equalsIgnoreCase("https");
        CookieHelper.addCookie(KEYCLOAK_STATE_CHECKER, "", cookiePath, null, null, 0, secureOnly, true);
    }

    private boolean isLocal() {
        if (!Objects.equals(session.getContext().getRealm().getName(), Config.getAdminRealm())) {
            return true;
        }
        ClientConnection clientConnection = session.getContext().getConnection();
        String[] allowedIP = EnvironmentReader.getValue("ROOT_ACCESS_IP").split(",");
        return Arrays.stream(allowedIP).anyMatch(x -> x.equals(clientConnection.getRemoteAddr()));
    }

    private Theme getTheme() {
        try {
            return session.theme().getTheme(Theme.Type.WELCOME);
        } catch (IOException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean shouldBootstrap() {
        return KeycloakApplication.BOOTSTRAP_ADMIN_USER.get() || new ApplianceBootstrap(session).isNoMasterUser();
    }

    private String setCsrfCookie() {
        String stateChecker = Base64Url.encode(KeycloakModelUtils.generateSecret());
        String cookiePath = session.getContext().getUri().getPath();
        boolean secureOnly = session.getContext().getUri().getRequestUri().getScheme().equalsIgnoreCase("https");
        CookieHelper.addCookie(KEYCLOAK_STATE_CHECKER, stateChecker, cookiePath, null, null, 300, secureOnly, true);
        return stateChecker;
    }

    private static ComponentModel createModelInstance(String name, RealmModel realm, String providerId, String policyType) {
        ComponentModel model = new ComponentModel();
        model.setName(name);
        model.setParentId(realm.getId());
        model.setProviderId(providerId);
        model.setProviderType(ClientRegistrationPolicy.class.getName());
        model.setSubType(policyType);
        return model;
    }

    private static void addAuthPolicies(RealmModel realm, String policyTypeKey) {

        addMaxRealmPolicy(realm, policyTypeKey);

        addMaxUsersPolicy(realm, policyTypeKey);

        addCheckLicensePolicy(realm, policyTypeKey);

        addSmsPolicy(realm);

        addEmailPolicy(realm, false);

        addWhatsappPolicy(realm);

        addAuthenticationPolicy(realm);
    }

    public static void addMaxRealmPolicy(RealmModel realm, String policyTypeKey) {
        ComponentModel maxClientsModel = createModelInstance("Client Realm Policy", realm, CanMultiTenantRegistrationPolicyFactory.PROVIDER_ID, policyTypeKey);
        maxClientsModel.put(CanMultiTenantRegistrationPolicyFactory.MAX_REALMS, CanMultiTenantRegistrationPolicyFactory.DEFAULT_MAX_REALMS);
        realm.addComponentModel(maxClientsModel);
    }

    public static void addMaxClientsPolicy(RealmModel realm, String policyTypeKey) {
        ComponentModel maxClientsModel = createModelInstance("Client Check Policy", realm, MaxClientsClientRegistrationPolicyFactory.PROVIDER_ID, policyTypeKey);
        maxClientsModel.put(MaxClientsClientRegistrationPolicyFactory.MAX_CLIENTS, MaxClientsClientRegistrationPolicyFactory.DEFAULT_MAX_CLIENTS);
        realm.addComponentModel(maxClientsModel);
    }

    public static void addMaxUsersPolicy(RealmModel realm, String policyTypeKey) {
        ComponentModel maxUsersModel = createModelInstance("User Check Policy", realm, MaxUsersRegistrationPolicyFactory.PROVIDER_ID, policyTypeKey);
        maxUsersModel.put(MaxUsersRegistrationPolicyFactory.MAX_USERS, MaxUsersRegistrationPolicyFactory.DEFAULT_MAX_USERS);
        realm.addComponentModel(maxUsersModel);
    }

    public static void addCheckLicensePolicy(RealmModel realm, String policyTypeKey) {
        ComponentModel licenseCheckModel = createModelInstance("Check License", realm, CheckLicenseClientRegistrationPolicyFactory.PROVIDER_ID, policyTypeKey);
        realm.addComponentModel(licenseCheckModel);
    }

    public static void addWhatsappPolicy(RealmModel realm) {
        Optional<ComponentModel> componentModel = realm.getComponentsStream().filter(x -> x.getProviderId().equalsIgnoreCase(WhatsappPolicyFactory.PROVIDER_ID)).findFirst();
        if (!componentModel.isPresent()) {
            String authPolicyType = ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.AUTHENTICATED);
            ComponentModel whatappCheckModel = createModelInstance("Check Whatsapp", realm, WhatsappPolicyFactory.PROVIDER_ID, authPolicyType);
            realm.addComponentModel(whatappCheckModel);
        }
    }

    public static void addEmailPolicy(RealmModel realm, boolean isAvailable) {
        if (!isAvailable) {
            String authPolicyType = ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.AUTHENTICATED);
            ComponentModel emailCheckModel = createModelInstance("Check Email", realm, EmailPolicyFactory.PROVIDER_ID, authPolicyType);
            realm.addComponentModel(emailCheckModel);
        }
    }

    public static void addSmsPolicy(RealmModel realm) {
        Optional<ComponentModel> componentModel = realm.getComponentsStream().filter(x -> x.getProviderId().equalsIgnoreCase(SmsPolicyFactory.PROVIDER_ID)).findFirst();
        if (!componentModel.isPresent()) {
            String authPolicyType = ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.AUTHENTICATED);
            ComponentModel smsCheckModel = createModelInstance("Check SMS", realm, SmsPolicyFactory.PROVIDER_ID, authPolicyType);
            realm.addComponentModel(smsCheckModel);
        }
    }

    private void cleanupAttributes(RealmModel realm) {
        realm.removeAttribute("DNS_NAME");
        realm.removeAttribute("DNS_TXT_RECORD_VALUE");
        realm.removeAttribute("DNS_A_RECORD_VERIFICATION");
        realm.removeAttribute("DNS_TXT_RECORD_VERIFICATION");
        realm.removeAttribute("DNS_COMPLETED");
    }
}
