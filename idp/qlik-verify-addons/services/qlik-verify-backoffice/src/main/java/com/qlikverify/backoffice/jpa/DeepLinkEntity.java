package com.qlikverify.backoffice.jpa;


import javax.persistence.*;

@NamedQueries({
        @NamedQuery(name = "getPayloadByDeepLink", query = "select d from DeepLinkEntity d where d.trxId = :trxId")

})

@Table(name="DEEP_LINK")
@Entity
public class DeepLinkEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "TRX_ID")
    protected String trxId;

    @Column(name = "PAYLOAD")
    protected String payload;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrxId() {
        return trxId;
    }

    public void setTrxId(String trxId) {
        this.trxId = trxId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
