package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

public class RequiredRule extends Rule {

    public static final String NAME = "required";

    @Nonnull
    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean check(String valueStr) {
        return valueStr != null && !valueStr.trim().isEmpty();
    }

    public static class Factory implements RuleFactory<RequiredRule> {
        @Override
        public String ruleName() {
            return NAME;
        }

        @Override
        public RequiredRule create(Object... params) throws RuleDefException {
            return new RequiredRule();
        }
    }
}
