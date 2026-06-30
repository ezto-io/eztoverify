package org.keycloak.models.jpa.entities;

import javax.persistence.*;

@Table(name="EZTOVRFY_MARKETPLACE_CATEGORY")
@Entity
public class EztovrfyMarketplaceCategoryEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name="CATEGORY")
    protected String category;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
