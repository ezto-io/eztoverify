package com.qlikverify.backoffice.validators;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class ValidatorPolicyCollection {

    private String decision;

    private List<ValidatorPolicy> policyList;
}
