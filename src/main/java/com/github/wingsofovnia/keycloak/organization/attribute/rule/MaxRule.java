package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

public class MaxRule extends RuleWithNumericExpectation {

    @Override
    public String ruleName() {
        return "max";
    }

    @Override
    protected boolean checkAgainstNumericExpectation(@Nonnull String valueStr, @Nonnull Double expectation) {
        final String trimmedValueStr = valueStr.trim();
        try {
            final double num = Double.parseDouble(trimmedValueStr);
            return num <= expectation;
        } catch (NumberFormatException e) {
            // Also support: check string length
            return trimmedValueStr.length() <= expectation;
        }
    }
}
