package com.github.wingsofovnia.keycloak.organization.attribute;

import com.github.wingsofovnia.keycloak.organization.attribute.rule.MaxLengthRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MaxRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MinLengthRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MinRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RegexRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RequiredRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.Rule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleDefException;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleFactory;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.TypeRule;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * A simple rule-based validation engine for validating attribute values against
 * a configurable set of validation rules expressed as a semicolon-separated string.
 * <p>
 * Each rule is expressed in the format {@code ruleName[:expectation]}.
 * Rules are applied independently but in the provided order and results are returned as a
 * {@link AttributeCheckResult}, indicating whether the value passed validation or which rules failed.
 * <p>
 * Supported rules include:
 * <ul>
 *     <li><b>required</b> – value must be non-blank</li>
 *     <li><b>type:(double|number)|boolean</b> – checks if the value can be parsed into the type</li>
 *     <li><b>min:X</b> – numeric minimum (inclusive)</li>
 *     <li><b>max:X</b> – numeric maximum (inclusive)</li>
 *     <li><b>minLength:X</b> – minimum string length (inclusive)</li>
 *     <li><b>maxLength:X</b> – maximum string length (inclusive)</li>
 *     <li><b>regex:pattern</b> – regex pattern match</li>
 * </ul>
 * <p>
 * Notice: {@code type} rules is merely an assertion that the value can be parsed/converted to the provided
 * expectation. All the rules treat value as string and parse input if needed individually. For example, if {@code min}
 * is set, it will attempt to parse value to a number even if {@code type: number} is not in the ruleset.
 *
 * <p><b>Example usage:</b></p>
 *
 * <pre>{@code
 * AttributeCheckResult result = Attributes.check("42", "required;type:double;min:0;max:100");
 * if (result.valid()) {
 *     // valid input
 * } else {
 *     for (RuleDef failed : result.failedRules()) {
 *         System.out.println("Failed rule: " + failed.ruleName());
 *     }
 * }
 * }</pre>
 *
 * <p>
 * If a rule is misconfigured (e.g. invalid regex or unsupported type), validation will return a failed result
 * and include the cause in {@link AttributeCheckResult#exception()}.
 * </p>
 */
public final class Attributes {

    private Attributes() {
        throw new AssertionError();
    }

    private static final List<RuleFactory<?>> RULE_FACTORIES = List.of(
            new MinRule.Factory(),
            new MaxRule.Factory(),
            new MinLengthRule.Factory(),
            new MaxLengthRule.Factory(),
            new RequiredRule.Factory(),
            new RegexRule.Factory(),
            new TypeRule.Factory()
    );
    private static final Map<String, RuleFactory<?>> RULES_FACTORY_MAP = RULE_FACTORIES.stream()
            .collect(Collectors.toMap(RuleFactory::ruleName, rule -> rule));

    private static final String RULES_DEF_SET_SEPARATOR = ";";
    private static final String RULE_EXPECTATION_SEPARATOR = ":";

    /**
     * Validates a single attribute value against a semicolon-separated set of rules.
     * <p>
     * Example: {@code "required;type:double;min:1;max:10"}
     *
     * @param value         the attribute value to validate (can be null)
     * @param ruleDefsStr rule string defining validation logic; may be null or blank
     * @return a {@link AttributeCheckResult} object indicating whether the value passed validation and, if not, which rules failed
     */
    public static AttributeCheckResult check(String value, String ruleDefsStr) {
        if (ruleDefsStr == null || ruleDefsStr.isBlank()) {
            return AttributeCheckResult.success();
        }
        return check(value, parseRules(ruleDefsStr));
    }

    private static AttributeCheckResult check(String value, List<Rule> rules) {
        final List<Rule> failedRules = rules.stream()
                .filter(rule -> !rule.check(value))
                .collect(Collectors.toList());

        if (failedRules.isEmpty()) {
            return AttributeCheckResult.success();
        }

        return AttributeCheckResult.failure(failedRules);
    }

    public static List<Rule> parseRules(String ruleDefsStr) {
        return Stream.of(ruleDefsStr.split(RULES_DEF_SET_SEPARATOR))
                .filter(ruleDefStr -> !ruleDefStr.isBlank())
                .map(String::trim)
                .map(Attributes::parseRule)
                .collect(toList());
    }

    public static Rule parseRule(String ruleDefStr) {
        final String[] ruleNameAndMaybeExpectation = ruleDefStr.split(RULE_EXPECTATION_SEPARATOR, 2);
        if (ruleNameAndMaybeExpectation.length < 1) {
            throw new RuleDefException("Rule definition '" + ruleDefStr + "' is invalid: missing rule name");
        }

        final String ruleName = ruleNameAndMaybeExpectation[0].trim();
        if (ruleName.isBlank()) {
            throw new RuleDefException("Rule name cannot be blank");
        }

        final RuleFactory<?> ruleFactory = RULES_FACTORY_MAP.get(ruleName);
        if (ruleFactory == null) {
            throw new RuleDefException("Unknown rule '" + ruleName + "'");
        }

        final Object[] ruleParams = ruleNameAndMaybeExpectation.length == 2
                ? new Object[]{ruleNameAndMaybeExpectation[1]}
                : new Object[0];

        return ruleFactory.create(ruleParams);
    }
}
