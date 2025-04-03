package com.github.wingsofovnia.keycloak.organization.attribute;

import com.github.wingsofovnia.keycloak.organization.attribute.rule.Rule;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;

public record AttributeCheckResult(
        String value,
        boolean isValid,
        @Nonnull List<Rule> failedRules,
        @Nonnull Optional<Throwable> exception
) {
    public static AttributeCheckResult success(String value) {
        return new AttributeCheckResult(value, true, List.of(), Optional.empty());
    }

    public static AttributeCheckResult failure(String value, @Nonnull List<Rule> failedRules) {
        return new AttributeCheckResult(value, false, failedRules, Optional.empty());
    }

    public static AttributeCheckResult failure(String value, @Nonnull Rule failedRule, @Nonnull Throwable exception) {
        return new AttributeCheckResult(value, false, List.of(failedRule), Optional.of(exception));
    }
}
