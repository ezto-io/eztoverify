package org.keycloak.models.jpa.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;


@NamedQueries({
        @NamedQuery(name = "getVerifiersByRealmId", query = "select u from VerifierEntity u where u.realmId = :realmId and lower(u.verifierName) like lower(:search) ORDER BY \n" +
                "    CASE WHEN :sortBy = 'ASC' THEN u.verifierName END ASC,\n" +
                "    CASE WHEN :sortBy = 'DESC' THEN u.verifierName END DESC"),
        @NamedQuery(name = "getVerifierByIdAndRealmId", query = "select u from VerifierEntity u where u.id = :id and u.realmId = :realmId")
})

@Table(name = "VERIFIERS")
@Entity
@Getter
@Setter
public class VerifierEntity {

    @Id
    @Column(name = "ID", length = 36)
    @Access(AccessType.PROPERTY)
    // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "VERIFIER_NAME")
    protected String verifierName;

    @Column(name = "PRIORITY")
    protected int priority;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "LIVE")
    protected boolean live;

    @Column(name = "CUSTOM_PRIORITY")
    protected boolean customPriority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROVIDER_ID", referencedColumnName = "ID")
    protected ProviderEntity provider;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof VerifierEntity)) return false;

        VerifierEntity that = (VerifierEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
