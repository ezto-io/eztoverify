package com.qlikverify.extensions.admin.controllers.apiconnector.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@NamedQueries({
        @NamedQuery(name = "getByVerifierNameRealmId", query = "SELECT u FROM ApiPriorityEntity u where u.verifierName = :verifierName and" +
                " u.realmId = :realmId"),
        @NamedQuery(name = "getVerifierCategories", query = "SELECT DISTINCT u.category FROM ApiPriorityEntity u WHERE u.realmId = :realmId " +
                "order by u.category ASC"),
        @NamedQuery(name = "getConfigByRealmIdAsc", query = "SELECT u FROM ApiPriorityEntity u where u.realmId = :realmId and lower(u.verifierName) like lower(concat('%',:search,'%')) and (:category = '' OR u.category = :category) order by u.verifierName ASC"),
        @NamedQuery(name = "getConfigByRealmIdDesc", query = "SELECT u FROM ApiPriorityEntity u where u.realmId = :realmId and lower(u.verifierName) like lower(concat('%',:search,'%')) and (:category = '' OR u.category = :category) order by u.verifierName DESC"),

})
@Table(name = "API_PRIORITY")
@Entity
@Getter
@Setter
public class ApiPriorityEntity {
    @Id
    @Column(name = "ID")
    private String id;
    @Column(name = "VERIFIER_NAME", nullable = false)
    protected String verifierName;
    @Column(name = "SELECTED_PROVIDER", nullable = false)
    protected String selectedProvider;
    @Column(name = "AVAILABLE_PROVIDERS", nullable = false)
    protected String availableProviders;
    @Column(name = "REALM_ID", nullable = false)
    protected String realmId;
    @Column(name = "IS_ENABLED", nullable = false)
    protected Boolean isEnabled;
    @Column(name = "CATEGORY", nullable = false)
    protected String category;
}
