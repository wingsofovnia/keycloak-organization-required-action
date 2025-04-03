package com.github.wingsofovnia.keycloak.organization.attribute.rule;

public abstract class RuleWithExpectationFactory<T> implements RuleFactory<RuleWithExpectation<T>> {

    @Override
    public final RuleWithExpectation<T> create(Object... params) throws RuleDefException {
        if (params == null || params.length != 1) {
            throw new RuleDefException("Expected exactly one parameter - rule expectation");
        }
        if (!(params[0] instanceof String)) {
            throw new RuleDefException("Expectation must be a string");
        }

        return create((String) params[0]);
    }

    protected abstract RuleWithExpectation<T> create(String expectationStr) throws RuleDefException;
}
