package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

public class MaxRule extends RuleWithNumericExpectation {

    @Override
    public String ruleName() {
        return "max";
    }

    @Override
    protected boolean checkAgainstNumericExpectation(@Nonnull String valueStr, @Nonnull Double expectation) {
        try {
            return (valueStr.isBlank() ? 0 : Double.parseDouble(valueStr.trim())) <= expectation;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
