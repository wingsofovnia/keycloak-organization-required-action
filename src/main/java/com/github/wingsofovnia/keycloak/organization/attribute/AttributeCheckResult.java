package com.github.wingsofovnia.keycloak.organization.attribute;

import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleDef;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;

public record AttributeCheckResult(
        boolean valid,
        @Nonnull List<RuleDef> failedRules,
        @Nonnull Optional<Throwable> exception
) {
    public static AttributeCheckResult success() {
        return new AttributeCheckResult(true, List.of(), Optional.empty());
    }

    public static AttributeCheckResult failure(@Nonnull List<RuleDef> failedRules) {
        return new AttributeCheckResult(false, failedRules, Optional.empty());
    }

    public static AttributeCheckResult failure(@Nonnull RuleDef ruleDef, @Nonnull Throwable exception) {
        return new AttributeCheckResult(false, List.of(ruleDef), Optional.of(exception));
    }
}
