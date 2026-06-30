package org.keycloak.userprofile.validator;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DuplicateUsernameMobileValidator implements SimpleValidator {

    public static final String ID = "up-mobile-exists-as-username";

    private static final Logger logger = Logger.getLogger(DuplicateUsernameMobileValidator.class);

    // here we check if any user has the same number as username or mobile number
    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        logger.info("userprofile : validate : DuplicateUsernameMobileValidator : validate");

        List<String> values = (List<String>) input;

        if (values == null || values.isEmpty()) {
            return context;
        }

        String value = values.get(0);

        if (Validation.isBlank(value))
            return context;

        KeycloakSession session = context.getSession();
        RealmModel realm = session.getContext().getRealm();

        Stream<UserModel> usersStream = session.users().getUsersStream(realm);
        List<UserModel> usersList = usersStream.filter(x -> x.getMobileNumber()!=null && x.getMobileNumber().contains(value)).collect(Collectors.toList());

        UserModel user = UserProfileAttributeValidationContext.from(context).getAttributeContext().getUser();
        UserModel userByUsername = session.users().getUserByUsername(realm,value);
        if(realm.isDuplicateMobNumAllowed())
            return context;
        if (!usersList.isEmpty() || ( userByUsername !=null && user.getUsername().equals(userByUsername.getUsername()))) {
            context.addError(new ValidationError(ID, inputHint, Messages.PHONE_NUMBER_EXISTS)
                    .setStatusCode(Response.Status.CONFLICT));
        }

        return context;
    }

    @Override
    public String getId() {
        return ID;
    }
}
