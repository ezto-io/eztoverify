package com.qlikverify.extensions.admin.controllers.ssodashboard.jpa;

import org.hibernate.annotations.GenericGenerator;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "SSO_DASHBOARD", indexes = {
        @Index(name = "idx_realm_id_user_id", columnList = "REALM_ID, USER_ID")
})
public class SSODashboardEntity {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name="REALM_ID", nullable = false)
    private String realmId;

    @Column(name="USER_ID", nullable = false)
    private String userId;

    @Column(name="REPRESENTATION_JSON", length = 25500)
    private String representationJson;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRepresentationJson() {
        return representationJson;
    }

    public void setRepresentationJson(String representationJson) {
        this.representationJson = representationJson;
    }

    @PrePersist
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    public void onCreate() {
        if (Objects.isNull(this.id)) {
            this.id = KeycloakModelUtils.generateId();
        }
    }
}
