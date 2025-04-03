package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

public class MinRule extends RuleWithExpectation<Double> {

    public static final String NAME = "min";

    public MinRule(double expectation) {
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
            return 0 >= expectation;
        }

        try {
            return Double.parseDouble(valueStr.trim()) >= expectation;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static class Factory extends RuleWithExpectationFactory<Double> {

        @Override
        public String ruleName() {
            return NAME;
        }

        @Override
        protected MinRule create(String expectationStr) throws RuleDefException {
            try {
                return new MinRule(Double.parseDouble(expectationStr));
            } catch (NumberFormatException e) {
                throw new RuleDefException("Invalid min expectation: " + expectationStr);
            }
        }
    }
}
