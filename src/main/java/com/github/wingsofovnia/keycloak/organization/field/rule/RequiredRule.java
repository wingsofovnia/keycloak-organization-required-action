package com.github.wingsofovnia.keycloak.organization.field.rule;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RequiredRule implements Rule {
    @Override
    public String ruleName() {
        return "required";
    }

    @Override
    public boolean requiresExpectation() {
        return false;
    }

    @Override
    public boolean check(@Nonnull String valueStr, @Nullable String expectationStr) {
        return valueStr != null && !valueStr.trim().isEmpty();
    }
}
