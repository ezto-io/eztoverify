package org.keycloak.models.jpa.entities;

import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@NamedQueries({
        @NamedQuery(name = "getProviderByNameAndRealmId", query = "select u from ProviderEntity u where u.providerName = :providerName and u.realmId = :realmId"),
        @NamedQuery(name = "getProviderByIdAndRealmId", query = "select u from ProviderEntity u where u.id = :id and u.realmId = :realmId ORDER BY u.providerPriority ASC"),
        @NamedQuery(name = "getProviderByProviderIdAndRealmId", query = "select u from ProviderEntity u where u.providerId = :providerId and " +
                "u.realmId = :realmId ORDER BY u.providerPriority ASC"),
        @NamedQuery(name = "getAllProvidersByRealmId", query = "select u from ProviderEntity u where u.realmId = :realmId and lower(u.providerName) like lower(:search) ORDER BY u.providerPriority ASC")
})

@Table(name = "PROVIDERS")
@Entity
@Getter
@Setter
public class ProviderEntity  {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "PROVIDER_ID", nullable = false)
    protected String providerId;

    @Column(name = "PROVIDER_NAME", nullable = false)
    protected String providerName;

    @Column(name = "PROVIDER_PRIORITY")
    protected int providerPriority;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "LIVE")
    protected boolean live;

    @Column(name = "CUSTOM_PRIORITY")
    protected boolean customPriority;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<VerifierEntity> verifiers;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof ProviderEntity)) return false;

        ProviderEntity that = (ProviderEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


}

