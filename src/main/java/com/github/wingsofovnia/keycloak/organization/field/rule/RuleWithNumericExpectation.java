package com.github.wingsofovnia.keycloak.organization.field.rule;

import jakarta.annotation.Nonnull;

/**
 * Specialization of {@link RuleWithExpectation} for rules that use numeric comparisons.
 * <p>
 * This class parses both the expectation and value as {@code double}, handles parsing errors,
 * and delegates the actual numeric logic to {@link #numericCheckAgainstExpectation(Double, Double)}.
 *
 * <p><b>Behavior:</b></p>
 * <ul>
 *     <li>Throws {@link RuleDefException} if the expectation is not a valid number</li>
 *     <li>Returns {@code false} if the value is not a valid number</li>
 * </ul>
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

        final double value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return false;
        }

        return numericCheckAgainstExpectation(value, expectation);
    }

    protected abstract boolean numericCheckAgainstExpectation(@Nonnull Double value, @Nonnull Double expectation);
}
