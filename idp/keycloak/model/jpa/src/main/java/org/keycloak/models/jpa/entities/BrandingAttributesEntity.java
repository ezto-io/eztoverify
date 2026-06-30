package org.keycloak.models.jpa.entities;

import javax.persistence.*;
import java.io.Serializable;

@NamedQueries({
        @NamedQuery(name="deleteBrandingAttributesByRealm", query="delete from BrandingAttributesEntity attr where attr.branding IN (select u from BrandingEntity u where u.realm=:realmId)"),
        @NamedQuery(name="deleteBrandingAttributesByNameAndBrand", query="delete from  BrandingAttributesEntity attr where attr.branding.id = :brandId and attr.name = :name"),
        @NamedQuery(name="deleteBrandingAttributesByNameAndBrandOtherThan", query="delete from  BrandingAttributesEntity attr where attr.branding.id = :brandId and attr.name = :name and attr.id <> :attrId")
})
@Table(name = "BRANDING_ATTRIBUTES")
@Entity
public class BrandingAttributesEntity implements Serializable {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BRAND_ID")
    protected BrandingEntity branding;

    @Column(name = "NAME")
    protected String name;

    @Column(name = "VALUE", length = 5000)
    protected String value;

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public BrandingEntity getBranding() {
        return branding;
    }

    public void setBranding(BrandingEntity branding) {
        this.branding = branding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof BrandingAttributesEntity)) return false;

        BrandingAttributesEntity that = (BrandingAttributesEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
