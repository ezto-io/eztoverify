package org.keycloak.mfa;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.models.MFAConfigModel;
import org.keycloak.provider.Provider;

public interface MFAProvider extends Provider {
     /**
      * Its triggered to show the authentication screen for MFA
      * @param context
      */
     void onPreLogin(MFAConfigModel configModel, RequiredActionContext context);

     /**
      * Its triggered after the user submitted the authentication for MFA
      * @param context
      */
     boolean onPostLogin(MFAConfigModel configModel,RequiredActionContext context);

     /**
      * Its triggered after user submitted form to register for MFA
      * @param context
      */
     boolean onPostRegistration(MFAConfigModel configModel,RequiredActionContext context);

     /**
      * Its triggered to show registration screen for MFA
      * @param context
      */
     void onPreRegistration(MFAConfigModel configModel,RequiredActionContext context, boolean canSkip);
}
