package com.github.wingsofovnia.keycloak.organization.field.rule;

import jakarta.annotation.Nonnull;

public class MaxRule extends RuleWithNumericExpectation {

    @Override
    public String ruleName() {
        return "max";
    }

    @Override
    protected boolean numericCheckAgainstExpectation(@Nonnull Double value, @Nonnull Double expectation) {
        return value <= expectation;
    }
}
