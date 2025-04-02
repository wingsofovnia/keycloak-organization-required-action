package com.github.wingsofovnia.keycloak.organization.field.rule;

import jakarta.annotation.Nonnull;

public class MinRule extends RuleWithNumericExpectation {

    @Override
    public String ruleName() {
        return "min";
    }

    @Override
    protected boolean numericCheckAgainstExpectation(@Nonnull Double value, @Nonnull Double expectation) {
        return value >= expectation;
    }
}
