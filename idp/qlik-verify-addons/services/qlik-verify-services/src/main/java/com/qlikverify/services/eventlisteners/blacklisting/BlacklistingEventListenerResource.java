package com.qlikverify.services.eventlisteners.blacklisting;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.validation.Validation;

import java.util.List;
import java.util.Map;

import static com.qlikverify.services.constants.Constants.*;

public class BlacklistingEventListenerResource {

    //~ Static fields/initializers =========================================================================================================

    private static final Logger logger = Logger.getLogger(BlacklistingEventListenerResource.class);
    private static final String LOG_TAG = "EZTO:BLACKLISTING_EVENT_LISTENER: ";
    private final KeycloakSession session;
    private final Event event;
    private final RealmModel realm;
    private UserModel user;
    boolean blockAuthFlow;
    private static final String BLOCK_FLOW = "blackListingBlockFlow";

    //~ Constructor ========================================================================================================================

    public BlacklistingEventListenerResource(KeycloakSession session, Event event) {
        this.session = session;
        this.event = event;
        this.realm = session.getContext().getRealm() != null ? session.getContext().getRealm() : null;
    }

    //~ Methods ============================================================================================================================

    public void checkBlacklistConfig() {
        if(event.getUser() == null || realm == null) return;
        if(realm.getAttribute(BLACKLISTING_ENABLED) == null || !realm.getAttribute(BLACKLISTING_ENABLED).equalsIgnoreCase("true"))
            return;
        this.user = event.getUser() != null ? session.users().getUserById(realm, event.getUser().getId()) : null;
        if(realm.getEventConfigByProviderId(BLACKLISTING_PROVIDER_ID) != null) {
            Map<String, String> eventConfigModel = realm.getEventConfigByProviderId(BLACKLISTING_PROVIDER_ID).getConfig();
            blockAuthFlow = false;
            if(eventConfigModel == null || eventConfigModel.isEmpty()) {
                logger.info(LOG_TAG + "checkBlacklistConfig: eventConfigModel is empty");
                return;
            }
            if(user == null && !(event.getDetails().containsKey("bl_email") ||
                    event.getDetails().containsKey("bl_phone") ||
                    event.getDetails().containsKey("bl_username")))
                return;
            eventConfigModel.forEach((key, value) -> {
                if(blockAuthFlow)
                    return;
                if(Validation.isBlank(value))
                    return;
                blockAuthFlow = checkBlacklistValues(key, value, event, blockAuthFlow);
            });
            if(blockAuthFlow) {
                logger.warn(LOG_TAG + "addHeaders: authentication request through blacklisted config - blocked");
                session.getContext().getAuthenticationSession().setAuthNote(BLOCK_FLOW,"true");
                if(user != null && user.getUsername() != null)
                    event.setError("blocked " + user.getUsername() + " - login through blacklisted config");
                else
                    event.setError("blocked user onboarding,onboarding through blacklisted config");
            }
        }
    }

    //~ ====================================================================================================================================

    private boolean checkBlacklistValues(String configType, String configValue, Event event, boolean blockAuthFlow) {
        List<String> defaultValues = List.of(configValue.split("##"));
        if(defaultValues.isEmpty())
            return false;
        switch (configType) {
            case BL_IP_ADDRESS:
                return checkForBlockedIp(defaultValues, event, blockAuthFlow);
            case BL_EMAIL:
                String email = user == null || user.getEmail() == null ? event.getDetails().get("bl_email") : user.getEmail();
                return checkForBlockedEmail(defaultValues, blockAuthFlow, email);
            case BL_PHONE:
                String phone = user == null ? null : user.getMobileNumberRegion() + user.getMobileNumber();
                if(Validation.isBlank(phone))
                    phone = event.getDetails().get("bl_phone_region") + event.getDetails().get("bl_phone");
                if(Validation.isBlank(phone))
                    return false;
                UserModel userModel = session.users().getUserByMobile(phone, realm);
                if(userModel == null)
                    return false;
                return checkForBlockedMobileNumbers(userModel, defaultValues, blockAuthFlow);
            case BL_USERNAME:
                String username = user == null || user.getUsername() == null ? event.getDetails().get("bl_username") : user.getUsername();
                return checkForBlockedUsernames(defaultValues, blockAuthFlow, username);
            default:
                return false;
        }
    }

    //~ ====================================================================================================================================

    private boolean checkForBlockedIp(List<String> defaultValues, Event event, boolean blockAuthFlow) {
        if(!Validation.isBlank(event.getIpAddress()) && defaultValues.contains(event.getIpAddress()))
            blockAuthFlow = true;
        return blockAuthFlow;
    }

    //~ ====================================================================================================================================

    private boolean checkForBlockedEmail(List<String> defaultValues, boolean blockAuthFlow, String email) {
        if(Validation.isBlank(email))
            return false;
        if(defaultValues.stream().filter(x -> x.startsWith("*")).anyMatch(e -> {
            String emailDomain = e.split("@")[1];
            return email.endsWith(emailDomain);
        }))
            blockAuthFlow = true;
        if(defaultValues.contains(email))
            blockAuthFlow = true;
        return blockAuthFlow;
    }

    //~ ====================================================================================================================================

    private boolean checkForBlockedMobileNumbers(UserModel user, List<String> defaultValues, boolean blockAuthFlow) {
        String mobile = phoneNoWithCountryCode(user);
        if(mobile == null)
            return false;
        if(defaultValues.contains(mobile))
            blockAuthFlow = true;
        return blockAuthFlow;
    }

    //~ ====================================================================================================================================

    private boolean checkForBlockedUsernames(List<String> defaultValues, boolean blockAuthFlow, String username) {
        if(Validation.isBlank(username))
            return false;
        if(defaultValues.contains(username))
            blockAuthFlow = true;
        return blockAuthFlow;
    }

    //~ ====================================================================================================================================


    public static String phoneNoWithCountryCode(UserModel user) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber parsed;
        if (phoneNumberHasCode(user)) {
            return user.getMobileNumber();
        } else {
            try {
                parsed = phoneUtil.parse(user.getMobileNumber(), user.getMobileNumberRegion().toUpperCase());
            } catch (NumberParseException e) {
                throw new RuntimeException(e);
            }
        }
        return parsed.getCountryCode() + user.getMobileNumber();
    }

    //~ ====================================================================================================================================

    public static boolean phoneNumberHasCode(UserModel user) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            if (phoneUtil.parse(user.getMobileNumber(), "").hasCountryCode()) {
                return true;
            }
        } catch (NumberParseException e) {
            return false;
        }
        return false;
    }

}
