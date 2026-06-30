package org.keycloak.models.jpa.entities;

import javax.persistence.*;
import java.util.Collection;

@Entity
@Table(name = "BRANDING")
@NamedQueries({
        @NamedQuery(name = "getBrandsByRealm", query = "select brand from BrandingEntity brand where brand.realm = :realm"),
        @NamedQuery(name = "getBrandByBrandName", query = "select brand from BrandingEntity brand where brand.realm = :realm and brand.name = :brandName"),
})
public class BrandingEntity {

    @Id
    @Column(name = "ID", length = 36)
    @Access(AccessType.PROPERTY)
    // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "NAME")
    protected String name;

    @Column(name = "IS_REALM_BRAND")
    private boolean isRealmBrand;

    @OneToMany(cascade = {CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "branding")
    private Collection<BrandingAttributesEntity> attributes;

    @Column(name = "REALM_ID")
    private String realm;

    @Column(name = "CREATED_TIMESTAMP")
    private Long createdTimestamp;

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<BrandingAttributesEntity> getAttributes() {
        return attributes;
    }

    public void setAttributes(Collection<BrandingAttributesEntity> attributes) {
        this.attributes = attributes;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean isRealmBrand() {
        return isRealmBrand;
    }

    public void setRealmBrand(boolean realmBrand) {
        isRealmBrand = realmBrand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof BrandingEntity)) return false;

        BrandingEntity that = (BrandingEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
