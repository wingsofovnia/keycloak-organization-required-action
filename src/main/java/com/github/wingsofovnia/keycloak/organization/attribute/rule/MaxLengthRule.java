package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

public class MaxLengthRule extends RuleWithNumericExpectation {

    @Override
    public String ruleName() {
        return "maxLength";
    }

    @Override
    protected boolean checkAgainstNumericExpectation(@Nonnull String valueStr, @Nonnull Double expectation) {
        final String trimmedValueStr = valueStr.trim();
        return trimmedValueStr.length() <= expectation.intValue();
    }
}
