package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record RuleDef(String ruleName, @Nullable String expectation) {
    public static RuleDef of(String ruleName, @Nullable String expectation) {
        return new RuleDef(ruleName, expectation);
    }

    @Override
    @Nonnull
    public String toString() {
        return ruleName + ": " + expectation;
    }
}
