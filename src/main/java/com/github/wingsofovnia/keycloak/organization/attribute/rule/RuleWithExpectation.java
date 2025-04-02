package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base class for rules that require a non-blank expectation.
 * <p>
 * This abstract class handles common logic for checking whether the expectation string
 * is present and delegates actual validation to {@link #checkAgainstExpectation(String, String)}.
 * <p>
 * Leading and trailing whitespace in both the value and expectation are trimmed before comparison.
 *
 * @see RuleWithNumericExpectation
 */
abstract class RuleWithExpectation implements Rule {

    @Override
    public boolean requiresExpectation() {
        return true;
    }

    @Override
    public boolean check(@Nonnull String valueStr, @Nullable String expectationStr) {
        if (expectationStr == null || expectationStr.isBlank()) {
            throw new RuleDefException("Expectation cannot be null or blank");
        }

        return checkAgainstExpectation(valueStr.trim(), expectationStr.trim());
    }

    protected abstract boolean checkAgainstExpectation(@Nonnull String valueStr, @Nonnull String expectationStr);
}
