package com.github.wingsofovnia.keycloak.organization.field.rule;

import jakarta.annotation.Nullable;

public record RuleDef(String ruleName, @Nullable String expectation) {
    public static RuleDef of(String ruleName, @Nullable String expectation) {
        return new RuleDef(ruleName, expectation);
    }
}
