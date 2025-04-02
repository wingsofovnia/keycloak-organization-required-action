package com.github.wingsofovnia.keycloak.organization.field;

import com.github.wingsofovnia.keycloak.organization.field.rule.RuleDef;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;

public record FieldCheckResult(
        boolean valid,
        @Nonnull List<RuleDef> failedRules,
        @Nonnull Optional<Throwable> exception
) {
    public static FieldCheckResult success() {
        return new FieldCheckResult(true, List.of(), Optional.empty());
    }

    public static FieldCheckResult failure(@Nonnull List<RuleDef> failedRules) {
        return new FieldCheckResult(false, failedRules, Optional.empty());
    }

    public static FieldCheckResult failure(@Nonnull RuleDef ruleDef, @Nonnull Throwable exception) {
        return new FieldCheckResult(false, List.of(ruleDef), Optional.of(exception));
    }
}
