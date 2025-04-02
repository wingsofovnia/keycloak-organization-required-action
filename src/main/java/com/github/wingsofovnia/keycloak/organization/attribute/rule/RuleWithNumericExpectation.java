package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

/**
 * Specialization of {@link RuleWithExpectation} for rules where the expectation is numeric.
 * Value can be interpreted freely by subclasses.
 * <p>
 * Throws {@link RuleDefException} if the expectation is not a valid number
 */
abstract class RuleWithNumericExpectation extends RuleWithExpectation {

    @Override
    protected boolean checkAgainstExpectation(@Nonnull String valueStr, @Nonnull String expectationStr) {
        final double expectation;
        try {
            expectation = Double.parseDouble(expectationStr);
        } catch (NumberFormatException e) {
            throw new RuleDefException("Expectation must be a number", e);
        }

        return checkAgainstNumericExpectation(valueStr, expectation);
    }

    protected abstract boolean checkAgainstNumericExpectation(@Nonnull String valueStr, @Nonnull Double expectation);
}
