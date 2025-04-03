package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Abstract base class for rules that are defined with an expectation value.
 * <p>
 * Provides a structure for implementing validation rules that compare a given value
 * against a specific expectation. The expectation is passed during object construction
 * and cannot be null.
 * <p>
 * Throws {@link RuleDefException} if the expectation is null.
 *
 * @param <T> the type of the expectation value associated with this rule
 */
public abstract class RuleWithExpectation<T> extends Rule {

    private final T expectation;

    RuleWithExpectation(T expectation) {
        if (expectation == null) {
            throw new RuleDefException("Expectation cannot be null");
        }

        this.expectation = expectation;
    }

    @Nonnull
    public T expectation() {
        return expectation;
    }

    @Override
    public final boolean check(String valueStr) {
        return check(valueStr, expectation);
    }

    protected abstract boolean check(String valueStr, @Nonnull T expectation);

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RuleWithExpectation<?> that = (RuleWithExpectation<?>) o;
        return Objects.equals(expectation, that.expectation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), expectation);
    }
}
