package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import com.github.wingsofovnia.keycloak.organization.attribute.Attributes;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents a validation rule that can be applied to a string value.
 * Implementations define how the rule behaves, what kind of expectation it requires,
 * and how validation is performed.
 * <p>
 * Rules are intended to be stateless, reusable, and used via the
 * {@link Attributes}.
 */
public interface Rule {

    /**
     * Returns the unique name of this rule, e.g. {@code "min"}, {@code "required"}, {@code "regex"}.
     *
     * @return the rule name
     */
    String ruleName();

    /**
     * Indicates whether this rule requires an expectation (e.g. {@code min:5} requires {@code 5}).
     * If {@code true}, the expectation must be non-null and non-blank for this rule to be valid.
     *
     * @return true if the rule requires an expectation value
     */
    boolean requiresExpectation();

    /**
     * Applies the rule to the given value and optional expectation.
     *
     * @param valueStr       the value to validate; never null
     * @param expectationStr the expected value or constraint; may be null if not required
     * @return {@code true} if the value satisfies this rule, {@code false} otherwise
     * @throws RuleDefException if the rule or its expectation is misconfigured (e.g. invalid number, bad regex)
     */
    boolean check(@Nonnull String valueStr, @Nullable String expectationStr);
}
