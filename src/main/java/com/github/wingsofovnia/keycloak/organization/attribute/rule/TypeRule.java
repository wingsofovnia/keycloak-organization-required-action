package com.github.wingsofovnia.keycloak.organization.attribute.rule;

import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TypeRule extends RuleWithExpectation<TypeRule.Type> {

    public static final String NAME = "type";

    public TypeRule(Type expectation) {
        super(expectation);
    }

    @Nonnull
    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected boolean check(String valueStr, @Nonnull Type expectation) {
        if (valueStr == null || valueStr.isBlank()) {
            return expectation == Type.STRING;
        }

        switch (expectation) {
            case DOUBLE -> {
                try {
                    Double.parseDouble(valueStr);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            case FLOAT -> {
                try {
                    Float.parseFloat(valueStr);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            case INTEGER -> {
                try {
                    Integer.parseInt(valueStr);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            case BOOLEAN -> {
                return valueStr.equalsIgnoreCase("true") || valueStr.equalsIgnoreCase("false");
            }
            case STRING -> {
                return true;
            }
            default -> throw new RuleDefException("Unsupported type: " + expectation);
        }
    }


    public static class Factory extends RuleWithExpectationFactory<TypeRule.Type> {
        @Override
        public String ruleName() {
            return NAME;
        }

        @Override
        protected TypeRule create(String expectationStr) throws RuleDefException {
            final Type type = Type.of(expectationStr)
                    .orElseThrow(() -> new RuleDefException("Invalid expected type: " + expectationStr));
            return new TypeRule(type);
        }
    }

    public enum Type {
        DOUBLE("double", "decimal"),
        FLOAT("float", "number"),
        INTEGER("int", "integer"),
        BOOLEAN("boolean", "bool"),
        STRING("string", "str");

        private final List<String> names;

        Type(String... names) {
            if (names == null || names.length == 0) {
                throw new IllegalArgumentException("Names cannot be null or empty");
            }
            this.names = Arrays.asList(names);
        }

        public static Optional<Type> of(String name) {
            for (Type type : Type.values()) {
                if (type.names.contains(name == null ? "" : name.trim().toLowerCase())) {
                    return Optional.of(type);
                }
            }

            return Optional.empty();
        }
    }
}
