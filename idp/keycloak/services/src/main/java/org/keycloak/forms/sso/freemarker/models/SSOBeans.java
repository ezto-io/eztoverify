package org.keycloak.forms.sso.freemarker.models;

import org.keycloak.marketplace.template.SSOBean;

import java.util.List;

public class SSOBeans {

    List<SSOBean> ssoBeanList;

    public SSOBeans(List<SSOBean> ssoBeanList) {
        this.ssoBeanList = ssoBeanList;
    }

    public List<SSOBean> getSsoBeanList() {
        return ssoBeanList;
    }
}
