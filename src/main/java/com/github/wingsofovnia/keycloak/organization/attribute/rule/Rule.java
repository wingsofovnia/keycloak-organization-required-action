package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public abstract class Rule {

    /**
     * Returns the unique name of this rule, e.g. {@code "min"}, {@code "required"}, {@code "regex"}.
     *
     * @return the rule name
     */
    @Nonnull
    public abstract String name();

    /**
     * Applies the rule to the given value.
     *
     * @param valueStr the value to validate
     * @return {@code true} if the value satisfies this rule, {@code false} otherwise
     */
    public abstract boolean check(String valueStr);

    @Override
    public int hashCode() {
        return Objects.hashCode(name());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Rule that = (Rule) o;
        return Objects.equals(name(), that.name());
    }
}
