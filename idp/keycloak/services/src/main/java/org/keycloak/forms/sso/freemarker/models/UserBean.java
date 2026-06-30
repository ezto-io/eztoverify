package org.keycloak.forms.sso.freemarker.models;

import org.keycloak.common.util.Time;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class UserBean {

    private UserModel user;
    private String accessUrl;

    public UserBean(UserModel userModel, String accUrl) {
        user = userModel;

        if(accUrl != null) {
            try {
                URL url = new URL(accUrl);
                if(url.getQuery() != null && !url.getQuery().isEmpty())
                {
                    accessUrl = accUrl+"&userid=";
                }
                else
                {
                    accessUrl = accUrl+"?userid=";
                }
                accessUrl += userModel.getId();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    public String getDisplayName(){
        String displayName= "";
        if(user.getFirstName() != null )
        {
            displayName += user.getFirstName();
        }

        if(user.getLastName() != null)
        {
            displayName += " " + user.getLastName();
        }

        if(!displayName.trim().isEmpty())
            return displayName;

        if(user.getUsername() != null && !user.getUsername().isEmpty())
            return user.getUsername();

        if(user.getEmail() != null && !user.getEmail().isEmpty())
            return user.getEmail();

        return "Guest";
    }

    public String getId(){return user.getId();}

    public String getEmail(){
        return user.getEmail();
    }

    public boolean isEmailVerified(){
        return user.isEmailVerified();
    }

    public String getFirstName(){
        return user.getFirstName();
    }

    public String getLastName(){
        return user.getLastName();
    }

    public String getUsername(){
        return user.getUsername();
    }

    public Date getCreatedOn(){
        return Time.toDate(user.getCreatedTimestamp());
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public String getUserPicture(){
        return ModelToRepresentation.getLogoAbsURL(user.getFirstAttribute(UserModel.USER_PICTURE));
    }
}
