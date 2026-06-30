package com.qlikverify.backoffice.models.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionsHistoryModel {
    String requestId;
    String clientId;
    Long time;
}
