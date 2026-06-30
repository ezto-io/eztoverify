package com.qlikverify.services.theme.onboarding;

import com.qlikverify.services.theme.helpers.GeneralHelpers;
import static com.qlikverify.services.theme.onboarding.Constants.*;
import org.jetbrains.annotations.NotNull;
import org.keycloak.Config;
import org.keycloak.authentication.actiontoken.ActionTokenConstants;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.DefaultRoles;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.onboarding.OnboardingStepProvider;
import org.keycloak.onboarding.OnboardingStepProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.SuccessResponse;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.GeneralHelper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.models.Constants.ADMIN_CONSOLE_CLIENT_ID;
import static org.keycloak.models.Constants.SSO_MANAGEMENT_CLIENT_ID;
import static org.keycloak.models.utils.ModelToRepresentation.getDashboardUrl;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

public class OBAdminWorkspaceSetup implements OnboardingStepProvider, OnboardingStepProviderFactory {
    KeycloakSession session;

    @Override
    public List<ProviderConfigProperty> getConfig() {
        List<ProviderConfigProperty> workspaceSettings = new ArrayList<>();

        RealmModel realmModel = session.getContext().getRealm();

        ProviderConfigProperty displayName = getDisplayNameConfigProperty();
        workspaceSettings.add(displayName);

        ProviderConfigProperty workspaceLogo = getWorkspaceConfigProperty(realmModel);
        workspaceSettings.add(workspaceLogo);

        ProviderConfigProperty inviteAdmin = getInviteAdminConfigProperty(realmModel);
        workspaceSettings.add(inviteAdmin);

        ProviderConfigProperty defaultPhoneRegion = getPhoneDefaultRegionConfigProperty(displayName);
        workspaceSettings.add(defaultPhoneRegion);

        return workspaceSettings;
    }

    @NotNull
    private ProviderConfigProperty getPhoneDefaultRegionConfigProperty(ProviderConfigProperty displayName) {
        // Display name
        ProviderConfigProperty defaultPhoneRegion = new ProviderConfigProperty();
        defaultPhoneRegion.setType(ProviderConfigProperty.PHONE_REGION);
        defaultPhoneRegion.setName(DEFAULT_REGION);
        defaultPhoneRegion.setLabel("default-region");
        defaultPhoneRegion.setDefaultValue(session.getContext().getRealm().getAttribute(DEFAULT_REGION));
        return defaultPhoneRegion;
    }

    @NotNull
    private ProviderConfigProperty getInviteAdminConfigProperty(RealmModel realmModel) {
        // Invite admin
        ProviderConfigProperty inviteAdmin = new ProviderConfigProperty();
        inviteAdmin.setType(ProviderConfigProperty.EMAIL_TABLE);
        inviteAdmin.setName(INVITE_ADMIN_USERS);
        inviteAdmin.setHelpText("invite-admin-user-helptext");
        inviteAdmin.setLabel("invite-admin-user");
        List<String> userModels =
                session.users().searchForUserStream(realmModel,new HashMap<>(),
                        -1, -1, true).map(UserModel::getEmail).collect(Collectors.toList());

        final UserModel userModel = session.users().searchForUserByUserAttributeStream(realmModel, "qv_is_account_creator"
                        , "true", true)
                .findFirst()
                .orElse(null);

        if (userModel != null) {
            userModels =
                    userModels.stream().filter(x -> !Objects.equals(x, userModel.getEmail())).collect(Collectors.toList());
        }

        List<String> finalUserModels = userModels;
        Map<String, Object> defaultValue = new HashMap<>();
        defaultValue.put("invitedUsers", finalUserModels);
        defaultValue.put("realmAdmin", userModel == null ? null : userModel.getEmail());
        inviteAdmin.setDefaultValue(defaultValue);
        return inviteAdmin;
    }

    @NotNull
    private ProviderConfigProperty getInviteLinkExpirationConfigProperty() {
        // Invite url expiry time
        ProviderConfigProperty inviteLinkExpiry = new ProviderConfigProperty();
        inviteLinkExpiry.setType(ProviderConfigProperty.TIME);
        inviteLinkExpiry.setName(INVITE_LINK_EXPIRY);
        inviteLinkExpiry.setHelpText("invite-link-expiration-helptext");
        inviteLinkExpiry.setLabel("invite-link-expiration");
        final String inviteLinkExpiryValue = session.getContext().getRealm().getAttribute(INVITE_LINK_EXPIRY_ATTRIBUTE);
        inviteLinkExpiry.setDefaultValue(inviteLinkExpiryValue != null ? inviteLinkExpiryValue : "300");
        Map<String, Object> dependsOn = new HashMap<>();
        dependsOn.put(USER_INVITE_WAY, VIA_LINK);
        inviteLinkExpiry.setDependsOn(dependsOn);
        return inviteLinkExpiry;
    }

    @NotNull
    private ProviderConfigProperty getUserOnBoardingWayConfigProperty() {
        // User Onboarding Way
        ProviderConfigProperty userOnboardingWay = new ProviderConfigProperty();
        userOnboardingWay.setType(ProviderConfigProperty.LIST_TYPE);
        userOnboardingWay.setName(USER_INVITE_WAY);
        userOnboardingWay.setHelpText("user-invite-helptext");
        userOnboardingWay.setLabel("userInvite");
        List<String> userOnboardOptions = new ArrayList<>();
        userOnboardOptions.add("Via OTP");
        userOnboardOptions.add(VIA_LINK);
        userOnboardingWay.setOptions(userOnboardOptions);
        userOnboardingWay.setDefaultValue(Objects.equals(session.getContext().getRealm().getUserInviteWay(), "otp") ?
                "Via OTP" : VIA_LINK);
        return userOnboardingWay;
    }

    @NotNull
    private static ProviderConfigProperty getWorkspaceConfigProperty(RealmModel realmModel) {
        // Workspace logo
        ProviderConfigProperty workspaceLogo = new ProviderConfigProperty();
        workspaceLogo.setType(ProviderConfigProperty.IMAGE_UPLOAD);
        workspaceLogo.setName(IMAGE_UPLOAD);
        workspaceLogo.setHelpText("realm-logo-helptext");
        workspaceLogo.setLabel("realm-logo");
        if (realmModel.getRealmLogo() != null) {
            workspaceLogo.setDefaultValue(realmModel.getRealmLogo());
        }
        return workspaceLogo;
    }

    @NotNull
    private ProviderConfigProperty getDisplayNameConfigProperty() {
        // Display name
        ProviderConfigProperty displayName = new ProviderConfigProperty();
        displayName.setType(ProviderConfigProperty.TEXT_TYPE);
        displayName.setName(DISPLAY_NAME);
        displayName.setHelpText("diplayname-helptext");
        displayName.setLabel("displayName");
        displayName.setRequired(true);
        displayName.setDefaultValue(session.getContext().getRealm().getDisplayName());
        return displayName;
    }

    @Override
    public Response process(MultivaluedMap<String, String> params, RealmModel realmModel) {

        // need to save locale to realm here

        // Display name


        String availableLang = params.getFirst("available-lang");
        if (availableLang == null || availableLang.isEmpty()) {
            return ErrorResponse.error("minimum-one-lang-needed", Response.Status.BAD_REQUEST);
        }

        String recommendedMarketplaceCategories = params.getFirst("recommendedMarketplaceCategories");
        if (recommendedMarketplaceCategories != null && !recommendedMarketplaceCategories.isEmpty()) {
            realmModel.setAttribute("recommendedMarketplaceCategories", recommendedMarketplaceCategories);
        }

        Set<String> preferredLoginLocalses = new HashSet<>(Arrays.asList(availableLang.split(",")));

        realmModel.setPreferredLoginLocales(preferredLoginLocalses);

        String displayName = params.getFirst(DISPLAY_NAME);
        if (displayName == null || displayName.isEmpty()) {
            return ErrorResponse.error("displayname-required", Response.Status.BAD_REQUEST);
        } else if (!displayName.trim().matches("^(?!-.)[a-z0-9A-Z- ]{3,55}$")){
            return ErrorResponse.error("displayname-should-be-alpha-numeric", Response.Status.BAD_REQUEST);
        }

        if(displayName.trim().length() > 55) {
            return ErrorResponse.error("display-name-should-be-under-55-char",
                    Response.Status.BAD_REQUEST);
        }

        // availableLocale = [en,ar]
//        Set<String> availableLocales = new HashSet<>();
//        try {
//            if(params.containsKey("availableLocale")) {
//                availableLocales = JsonSerialization.readValue(params.getFirst("availableLocale"), Set.class);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        realmModel.setPreferredLoginLocales(availableLocales);
        realmModel.setDisplayName(displayName);

        //Default region

        setDefaultRegion(realmModel, params);

        // Invite admins
        inviteAdmins(realmModel, params);

        // Image
        String file = params.getFirst(IMAGE_UPLOAD);
        if(file != null) {
            String[] fileContents = file.split(",");
            if (fileContents[0] != null && !fileContents[0].isEmpty() && !checkForCorrectExtension(fileContents[0]))
                return ErrorResponse.error("unsupported-file-type", Response.Status.BAD_REQUEST);
            String logoUrl = saveLogo(fileContents);
            if (logoUrl != null)
                realmModel.setRealmLogo(logoUrl);
        }

         // Invite link expiry
        setInviteLinkExpiry(realmModel, params);

        // Mark the onboarded realm as a workspace. MigrateTo9_0_5 only stamps these flags
        // on realms that already exist at startup; the master realm is created later via the
        // onboarding required action, so without this it never becomes a workspace
        // (isWorkflowEnabled stays false -> the Workspace Attributes / FIDO tabs stay hidden).
        realmModel.setAttribute("isPlatformWorkspace", "true");
        realmModel.setAttribute("isApiWorkspace", "true");

        session.getContext().setRealm(realmModel);
        GeneralHelpers.addToCompletedStep(realmModel,
                this.getProviderId());
        return SuccessResponse.success("workspace-setup-completed", Response.Status.OK);
    }

    public void inviteAdmins(RealmModel realmModel, MultivaluedMap<String, String> params){
        String[] emails = params.getFirst(INVITE_ADMIN_USERS).split(",");
        for (String email : emails) {
            if (email != null && !email.isEmpty()) {

                UserModel adminUser = session.users().addUser(session.getContext().getRealm(), email);
                adminUser.setEnabled(true);
                adminUser.setEmail(email);
                adminUser.setEmailVerified(false);
                grantAdminRole(realmModel, adminUser);

                DefaultRoles.addDefaultRoles(realmModel, adminUser);
                UserRepresentation userRepresentation = new UserRepresentation();
                userRepresentation.setEmail(email);
                List<CredentialRepresentation> credentials = new ArrayList<>();
                CredentialRepresentation tempPassword = new CredentialRepresentation();
                tempPassword.setType(PASSWORD);
                tempPassword.setValue(HmacOTP.generateSecret(10));
                tempPassword.setTemporary( !realmModel.getMode().equalsIgnoreCase("bo"));
                tempPassword.setUserLabel("userInvited");
                credentials.add(tempPassword);
                userRepresentation.setCredentials(credentials);
                RepresentationToModel.createCredentials(userRepresentation, session, realmModel,adminUser , true);

                adminUser.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Invited");

                ClientModel clientModel = realmModel.getMode().equals("bo") ?
                        realmModel.getClientByClientId(ADMIN_CONSOLE_CLIENT_ID) :
                        realmModel.getClientByClientId(SSO_MANAGEMENT_CLIENT_ID);
                adminUser.setSingleAttribute(UserModel.USER_INVITE_CLIENT,clientModel.getClientId());

                session.getContext().setRealm(realmModel);
                AdminEventBuilder adEvt = new AdminEventBuilder(realmModel, new AdminAuth(realmModel,
                        null, null, clientModel), session,
                        null);

                adEvt.operation(OperationType.CREATE).realm(realmModel.getId()).resource(ResourceType.ADMIN_USER).representation(userRepresentation).success();
            }
        }
    }

    public void setDefaultRegion(RealmModel realmModel, MultivaluedMap<String, String> params){
        String defaultRegion = params.getFirst(DEFAULT_REGION);
        if(defaultRegion != null){
            realmModel.setAttribute(DEFAULT_REGION, defaultRegion);
        }
    }

    public void grantAdminRole(RealmModel realmModel, UserModel adminUser){
        if (Objects.equals(realmModel.getName(), Config.getAdminRealm())) {
            RoleModel adminRole = session.getContext().getRealm().getRole(AdminRoles.ADMIN);
            adminUser.grantRole(adminRole);
        } else {
            RoleModel role =
                    realmModel.getClientByClientId(ActionTokenConstants.REALM_MGMT_CLIENT).getRole(ActionTokenConstants.ADMIN_USER_ROLE);
            adminUser.grantRole(role);
            adminUser.setMfaEnabled(false);
        }
    }

    public void setInviteLinkExpiry(RealmModel realmModel, MultivaluedMap<String, String> params){
        String inviteLinkExpiry = params.getFirst(INVITE_LINK_EXPIRY);
        if (inviteLinkExpiry != null){
            realmModel.setAttribute(INVITE_LINK_EXPIRY_ATTRIBUTE, inviteLinkExpiry);
        }
    }

    private String saveLogo(String[] fileContents) {
        if (fileContents[0] == null || fileContents[0].isEmpty()) {
            return null;
        }
        String fileName = session.getContext().getRealm().getName() + "-logo." + GeneralHelpers.getFileName(fileContents[0]);
        if(!GeneralHelper.isValidSvgFile(new String(Base64.getDecoder().decode(fileContents[1]))))
            return null;
        InputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(fileContents[1]));
        String url = session.getProvider(AvatarProvider.class, "avatar-storage-minio")
                .saveImgAndReturnUrl(session.getContext().getRealm().getName(), "public/logos/", fileName,
                        fileContents[0],
                        stream
                );
        return  getDashboardUrl() + "/" + url;
    }

    private boolean checkForCorrectExtension(String extension) {
        boolean isCorrect = false;
        String[] extTypeArray = new String[]{"jpg", "svg", "png", "jpeg"};
        if (extension != null) {
            isCorrect = Arrays.stream(extTypeArray).anyMatch(extension::contains);
        }
        return isCorrect;
    }

    @Override
    public boolean validate(MultivaluedMap<String, String> params, RealmModel realmModel) {
        return false;
    }

    @Override
    public boolean isCompleted(RealmModel realmModel) {
        return GeneralHelpers.checkForCurrentStep(realmModel, this.getProviderId());
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public String getTitle() {
        return "workspace-setup";
    }

    @Override
    public String getDescription() {
        return "update-workspace-settings";
    }

    @Override
    public OnboardingStepProvider create(KeycloakSession session) {
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
    public String getProviderId() {
        return Constants.OB_ADMIN_WORKSPACE_SETUP;
    }

    @Override
    public boolean isSkippable() {
        return false;
    }

    @Override
    public String getId() {
        return Constants.OB_ADMIN_WORKSPACE_SETUP;
    }

}
