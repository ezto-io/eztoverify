package com.qlikverify.transactionAttribute.spi.infinispan;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeWith;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

@SerializeWith(TransactionAttributeEntity.ExternalizerImpl.class)
public class TransactionAttributeEntity {
    private Map<String, String> trxAttributes;
    private long createdAt;

    public TransactionAttributeEntity(Map<String, String> attributes, long createdAt) {
        this.trxAttributes = new HashMap<>(attributes);
        this.createdAt = createdAt;
    }

    public Map<String, String> getTrxAttributes() {
        return trxAttributes;
    }

    public void setTrxAttributes(Map<String, String> trxAttributes) {
        this.trxAttributes = trxAttributes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public static class ExternalizerImpl implements Externalizer<TransactionAttributeEntity> {
        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, TransactionAttributeEntity t) throws IOException {
            output.writeByte(VERSION_1);

            output.writeObject(t.trxAttributes);
            output.writeLong(t.createdAt);
        }

        @Override
        public TransactionAttributeEntity readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            byte version = input.readByte();

            if (version != VERSION_1) {
                throw new IOException("Invalid version: " + version);
            }

            return new TransactionAttributeEntity(
                    (Map<String, String>) input.readObject(),
                    input.readLong()
            );
        }
    }
}
