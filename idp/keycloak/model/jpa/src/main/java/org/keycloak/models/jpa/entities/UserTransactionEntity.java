package org.keycloak.models.jpa.entities;

import org.hibernate.annotations.Nationalized;

import javax.persistence.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@NamedQueries({
        @NamedQuery(name = "findByTrxAndUserId", query = "select u from UserTransactionEntity u where u.trxId = :trxId and u.userId = :userId"),
        @NamedQuery(name = "updateTrxStatus", query = "update UserTransactionEntity u set u.status = :currentStatus, u.realmId = :realmId where u.trxId = :trxId and u.userId = :userId"),
        @NamedQuery(name = "findByUserIdAndRealmId", query = "select u from UserTransactionEntity u where u.userId = :userId and u.realmId = :realmId"),
        @NamedQuery(name = "findByUserId", query = "select u from UserTransactionEntity u where u.userId = :userId"),
        @NamedQuery(name = "removeTrxStatus", query = "delete from UserTransactionEntity u where u.userId = :userId and u.realmId = :realmId")
})

@Table(name="USER_TRANSACTIONS")
public class UserTransactionEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "TRX_ID")
    protected String trxId;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "USER_ID")
    protected String userId;

    @Column(name = "STATUS")
    protected String status;

    @Column(name = "CREATED_AT")
    private Long createdAt;

    @Column(name = "EXPIRES_AT")
    private Long expiresAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTrxId() {
        return trxId;
    }

    public void setTrxId(String trxId) {
        this.trxId = trxId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof UserTransactionEntity)) return false;

        UserTransactionEntity that = (UserTransactionEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


    public Long getCreatedTimestamp() {
        return createdAt;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = System.currentTimeMillis();
    }
}