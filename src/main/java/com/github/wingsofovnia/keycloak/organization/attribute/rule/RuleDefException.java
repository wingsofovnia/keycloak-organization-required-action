package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

/**
 * Thrown when a rule definition is structurally invalid or cannot be interpreted properly.
 * This may happen if:
 * <ul>
 *     <li>The expectation value cannot be parsed (e.g. {@code min:abc})</li>
 *     <li>A regex pattern is malformed (e.g. {@code regex:[0-9})</li>
 *     <li>An unsupported type is specified (e.g. {@code type:tristate})</li>
 * </ul>
 */
public class RuleDefException extends IllegalArgumentException {
    public RuleDefException(@Nonnull String message) {
        super(message);
    }

    public RuleDefException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }

    public RuleDefException(@Nonnull Throwable cause) {
        super(cause);
    }
}
