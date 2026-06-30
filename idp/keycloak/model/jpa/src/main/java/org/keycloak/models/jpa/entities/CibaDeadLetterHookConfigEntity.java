package org.keycloak.models.jpa.entities;


import javax.persistence.*;
import java.util.Map;

@Table(name="CIBA_DEAD_LETTER_HOOK_NOTIFICATION_CONFIG")
@Entity
@NamedQueries({ @NamedQuery(name = "findByCibaDeadLetterHookNotificationProviderAndRealm", query = "select u from CibaDeadLetterHookConfigEntity u where u.providerId = :providerId and u.clientId = :clientId and u.realm = :realmId"),
        @NamedQuery(name = "findByCibaDeadLetterHookNotificationProviderByClient", query = "select u from CibaDeadLetterHookConfigEntity u where u.clientId = :clientId and u.realm = :realmId")})
public class CibaDeadLetterHookConfigEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name="PROVIDER_ID")
    protected String providerId;

    @Column(name="CLIENT_ID")
    protected String clientId;

    @Column(name = "REALM_ID")
    protected String realm;

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE")
    @CollectionTable(
            name="CIBA_DEAD_LETTER_HOOK_NOTIFICATION_CONFIG_ENTRY",
            joinColumns={ @JoinColumn(name="CONFIG_ID") },
            uniqueConstraints = { @UniqueConstraint(columnNames = {"CONFIG_ID", "NAME"})}
    )
    private Map<String, String> config;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CibaDeadLetterHookConfigEntity)) return false;

        CibaDeadLetterHookConfigEntity that = (CibaDeadLetterHookConfigEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
