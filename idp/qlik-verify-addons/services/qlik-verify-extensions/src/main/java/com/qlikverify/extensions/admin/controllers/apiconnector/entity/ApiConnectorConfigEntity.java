package com.qlikverify.extensions.admin.controllers.apiconnector.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@NamedQueries({
        @NamedQuery(name = "getAllConfig", query = "SELECT u FROM ApiConnectorConfigEntity u"),
        @NamedQuery(name = "getConfigByProviderNameVerifierName",
                query = "SELECT u FROM ApiConnectorConfigEntity u where u.verifierName = :verifierName and u.providerName = :providerName"),
        @NamedQuery(name = "deleteAllConfig", query = "DELETE FROM ApiConnectorConfigEntity")
})

@Table(name = "API_CONNECTOR_CONFIG")
@Entity
@Getter
@Setter
public class ApiConnectorConfigEntity {
    @Id
    @Column(name = "ID")
    private String id;
    @Column(name = "PROVIDER_NAME", nullable = false)
    protected String providerName;
    @Column(name = "VERIFIER_NAME", nullable = false)
    protected String verifierName;
    @Column(name = "REQUEST_BODY", nullable = false)
    protected String requestBody;
    @Column(name = "RESPONSE_BODY", nullable = false)
    protected String responseBody;
    @Column(name = "CLAIMS", nullable = false)
    protected String claims;
    @Column(name = "CATEGORY", nullable = false)
    protected String category;
}
