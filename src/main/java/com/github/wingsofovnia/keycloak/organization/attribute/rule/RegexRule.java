package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexRule extends RuleWithExpectation {

    @Override
    public String ruleName() {
        return "regex";
    }

    @Override
    protected boolean checkAgainstExpectation(@Nonnull String valueStr, @Nonnull String expectationStr) {
        try {
            Pattern pattern = Pattern.compile(expectationStr);
            return pattern.matcher(valueStr).matches();
        } catch (PatternSyntaxException e) {
            throw new RuleDefException("Invalid regex: " + expectationStr, e);
        }
    }
}
