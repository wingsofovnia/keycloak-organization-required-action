package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

public class MinRule extends RuleWithNumericExpectation {

    @Override
    public String ruleName() {
        return "min";
    }

    @Override
    protected boolean checkAgainstNumericExpectation(@Nonnull String valueStr, @Nonnull Double expectation) {
        final String trimmedValueStr = valueStr.trim();
        try {
            return Double.parseDouble(trimmedValueStr) >= expectation;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
