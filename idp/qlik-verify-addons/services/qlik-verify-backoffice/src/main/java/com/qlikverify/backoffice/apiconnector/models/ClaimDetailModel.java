package com.qlikverify.backoffice.apiconnector.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDetailModel {
    private List<List<ClaimDetail>> otherClaims;
    private List<ClaimDetail> commonClaims;
}
