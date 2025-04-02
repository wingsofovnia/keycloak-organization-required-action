package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

public class TypeRule extends RuleWithExpectation {
    @Override
    public String ruleName() {
        return "type";
    }

    @Override
    protected boolean checkAgainstExpectation(@Nonnull String valueStr, @Nonnull String expectationStr) {
        switch (expectationStr.toLowerCase()) {
            case "double", "number" -> {
                try {
                    Double.parseDouble(valueStr);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            case "boolean" -> {
                return valueStr.equalsIgnoreCase("true") || valueStr.equalsIgnoreCase("false");
            }
            default -> throw new RuleDefException("Unsupported type: " + expectationStr);
        }
    }

}
