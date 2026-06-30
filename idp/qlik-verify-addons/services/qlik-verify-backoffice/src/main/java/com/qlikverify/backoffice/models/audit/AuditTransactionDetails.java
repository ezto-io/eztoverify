package com.qlikverify.backoffice.models.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditTransactionDetails {
    String eventName;
    String error;
    Long time;
    boolean direction;
    boolean skipped;
}
