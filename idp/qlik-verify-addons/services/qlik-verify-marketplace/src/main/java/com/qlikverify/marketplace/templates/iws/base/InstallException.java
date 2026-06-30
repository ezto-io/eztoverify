package com.qlikverify.marketplace.templates.iws.base;

import org.keycloak.marketplace.InstallResponse;

public class InstallException extends Exception{
    final transient InstallResponse installResponse;

    public InstallException(InstallResponse installResponse)
    {
        this.installResponse = installResponse;
    }

    public InstallResponse getInstallResponse() {
        return installResponse;
    }
}
