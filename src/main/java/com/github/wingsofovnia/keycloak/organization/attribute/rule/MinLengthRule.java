package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

public class MinLengthRule extends RuleWithExpectation<Double> {

    public static final String NAME = "minLength";

    public MinLengthRule(double expectation) {
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

        return valueStr.trim().length() >= expectation;
    }

    public static class Factory extends RuleWithExpectationFactory<Double> {

        @Override
        public String ruleName() {
            return NAME;
        }

        @Override
        protected MinLengthRule create(String expectationStr) throws RuleDefException {
            try {
                return new MinLengthRule(Double.parseDouble(expectationStr));
            } catch (NumberFormatException e) {
                throw new RuleDefException("Invalid minLength expectation: " + expectationStr);
            }
        }
    }
}
