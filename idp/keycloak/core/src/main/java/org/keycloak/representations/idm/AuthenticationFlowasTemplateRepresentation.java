package org.keycloak.representations.idm;

import java.io.Serializable;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationFlowasTemplateRepresentation implements Serializable {

    private String id;
    private String alias;
    private String description;
    private String providerId;
    private boolean topLevel;
    private boolean builtIn;
    private boolean installed;
    private String templateAlias;
    private String templateType;
    private String templateId;
    protected List<BlockMap> authenticationExecutions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    public void setTopLevel(boolean topLevel) {
        this.topLevel = topLevel;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }


    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getTemplateAlias() {
        return templateAlias;
    }

    public void setTemplateAlias(String templateAlias) {
        this.templateAlias = templateAlias;
    }

    public List<BlockMap> getAuthenticationExecutions() {
        return authenticationExecutions;
    }

    public void setAuthenticationExecutions(List<BlockMap> authenticationExecutions) {
        this.authenticationExecutions = authenticationExecutions;
    }
}
