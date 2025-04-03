package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexRule extends RuleWithExpectation<Pattern> {

    public static final String NAME = "regex";

    public RegexRule(Pattern expectation) {
        super(expectation);
    }

    @Nonnull
    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected boolean check(String valueStr, @Nonnull Pattern expectation) {
        return expectation.matcher(valueStr == null ? "" : valueStr.trim()).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RegexRule that = (RegexRule) o;
        return Objects.equals(name(), that.name())
                && Objects.equals(expectation().pattern(), that.expectation().pattern());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), expectation().pattern());
    }

    public static class Factory extends RuleWithExpectationFactory<Pattern> {

        @Override
        public String ruleName() {
            return NAME;
        }

        @Override
        protected RegexRule create(String expectationStr) throws RuleDefException {
            try {
                return new RegexRule(Pattern.compile(expectationStr));
            } catch (PatternSyntaxException e) {
                throw new RuleDefException("Invalid regex expectation pattern: " + expectationStr, e);
            }
        }
    }
}
