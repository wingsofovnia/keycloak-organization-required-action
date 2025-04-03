package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

public class MaxLengthRule extends RuleWithExpectation<Double> {

    public static final String NAME = "maxLength";

    public MaxLengthRule(double expectation) {
        super(expectation);
    }

    @Nonnull
    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected boolean check(String valueStr, @Nonnull Double expectation) {
        if (valueStr == null || valueStr.isBlank()) {
            return 0 <= expectation;
        }

        return valueStr.trim().length() <= expectation;
    }

    public static class Factory extends RuleWithExpectationFactory<Double> {

        @Override
        public String ruleName() {
            return NAME;
        }

        @Override
        protected MaxLengthRule create(String expectationStr) throws RuleDefException {
            try {
                return new MaxLengthRule(Double.parseDouble(expectationStr));
            } catch (NumberFormatException e) {
                throw new RuleDefException("Invalid maxLength expectation: " + expectationStr);
            }
        }
    }
}
