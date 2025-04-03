package com.github.wingsofovnia.keycloak.organization.attribute;

import com.github.wingsofovnia.keycloak.organization.attribute.rule.Rule;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;

public record AttributeCheckResult(
        boolean valid,
        @Nonnull List<Rule> failedRules,
        @Nonnull Optional<Throwable> exception
) {
    public static AttributeCheckResult success() {
        return new AttributeCheckResult(true, List.of(), Optional.empty());
    }

    public static AttributeCheckResult failure(@Nonnull List<Rule> failedRules) {
        return new AttributeCheckResult(false, failedRules, Optional.empty());
    }

    public static AttributeCheckResult failure(@Nonnull Rule failedRule, @Nonnull Throwable exception) {
        return new AttributeCheckResult(false, List.of(failedRule), Optional.of(exception));
    }
}
