package com.qlikverify.backoffice.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PricingModel {

    float amount;
    String currency;
}
