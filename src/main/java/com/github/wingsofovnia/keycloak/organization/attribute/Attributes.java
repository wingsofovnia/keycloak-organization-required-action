package com.github.wingsofovnia.keycloak.organization.attribute;

import com.github.wingsofovnia.keycloak.organization.attribute.rule.MaxRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MinRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RegexRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RequiredRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.Rule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleDef;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleDefException;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.TypeRule;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple rule-based validation engine for validating attribute values against
 * a configurable set of validation rules expressed as a semicolon-separated string.
 * <p>
 * Each rule is expressed in the format {@code ruleName[:expectation]}.
 * Rules are applied independently and results are returned as a {@link AttributeCheckResult},
 * indicating whether the value passed validation or which rules failed.
 * <p>
 * Supported rules include:
 * <ul>
 *     <li><b>required</b> – value must be non-blank</li>
 *     <li><b>type:double|boolean</b> – checks if the value matches the type</li>
 *     <li><b>min:X</b> – numeric minimum (inclusive)</li>
 *     <li><b>max:X</b> – numeric maximum (inclusive)</li>
 *     <li><b>regex:pattern</b> – regex pattern match</li>
 * </ul>
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

    private static final List<Rule> RULES = List.of(
            new MinRule(),
            new MaxRule(),
            new RequiredRule(),
            new RegexRule(),
            new TypeRule()
    );
    private static final Map<String, Rule> RULES_MAP = RULES.stream()
            .collect(Collectors.toMap(Rule::ruleName, rule -> rule));

    private static final String RULES_DEF_SET_SEPARATOR = ";";
    private static final String RULE_EXPECTATION_SEPARATOR = ":";

    /**
     * Validates a single attribute value against a semicolon-separated set of rules.
     * <p>
     * Example: {@code "required;type:double;min:1;max:10"}
     *
     * @param value         the attribute value to validate (can be null)
     * @param ruleDefSetStr rule string defining validation logic; may be null or blank
     * @return a {@link AttributeCheckResult} object indicating whether the value passed validation and, if not, which rules failed
     */
    public static AttributeCheckResult check(String value, String ruleDefSetStr) {
        if (ruleDefSetStr == null || ruleDefSetStr.isBlank()) {
            return AttributeCheckResult.success();
        }
        return check(value, ruleDefsOf(ruleDefSetStr));
    }

    private static AttributeCheckResult check(String value, Set<RuleDef> ruleDefSet) {
        final Map<String, String> ruleDefSetMap = new HashMap<>(ruleDefSet.size());
        for (RuleDef ruleDef : ruleDefSet) {
            final String ruleName = ruleDef.ruleName();
            final @Nullable String expectation = ruleDef.expectation();
            ruleDefSetMap.put(ruleName, expectation);
        }

        return check(value, ruleDefSetMap);
    }

    private static AttributeCheckResult check(String value, Map<String, String> ruleDefSetMap) {
        final List<RuleDef> failedRules = new ArrayList<>(ruleDefSetMap.size());

        for (Map.Entry<String, String> ruleEntry : ruleDefSetMap.entrySet()) {
            final String ruleName = ruleEntry.getKey();
            final Rule rule = getRule(ruleName).orElseThrow(() -> new IllegalArgumentException("Unknown rule: " + ruleName));
            final @Nullable String ruleExpectation = ruleEntry.getValue();

            if (rule.requiresExpectation() && (ruleExpectation == null || ruleExpectation.isBlank())) {
                throw new IllegalArgumentException("Rule " + ruleName + " requires a non-blank expectation");
            }

            try {
                if (!rule.check(value, ruleExpectation)) {
                    failedRules.add(RuleDef.of(ruleName, ruleExpectation));
                }
            } catch (RuleDefException e) {
                return AttributeCheckResult.failure(RuleDef.of(ruleName, ruleExpectation), e);
            }
        }

        if (!failedRules.isEmpty()) {
            return AttributeCheckResult.failure(failedRules);
        }
        return AttributeCheckResult.success();
    }

    private static Set<RuleDef> ruleDefsOf(String ruleDefSetStr) {
        return Stream.of(ruleDefSetStr.split(RULES_DEF_SET_SEPARATOR))
                .map(String::trim)
                .map(Attributes::ruleDefOf)
                .collect(Collectors.toSet());
    }

    private static RuleDef ruleDefOf(String ruleDefStr) {
        final String[] ruleNameAndExpectation = ruleDefStr.split(RULE_EXPECTATION_SEPARATOR, 2);
        if (ruleNameAndExpectation.length < 1) {
            throw new IllegalArgumentException("Rule definition '" + ruleDefStr + "' is invalid");
        }

        final String ruleName = ruleNameAndExpectation[0].trim();
        if (ruleName.isBlank()) {
            throw new IllegalArgumentException("Rule name cannot be blank");
        }

        final String ruleExpectation;
        if (ruleNameAndExpectation.length == 2) {
            ruleExpectation = ruleNameAndExpectation[1].trim();
        } else {
            ruleExpectation = null;
        }

        return new RuleDef(ruleName, ruleExpectation);
    }

    private static Optional<Rule> getRule(String ruleName) {
        return Optional.ofNullable(RULES_MAP.get(ruleName));
    }

}
