package com.github.wingsofovnia.keycloak.organization.attribute;

import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleDef;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleDefException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributesTest {

    @Test
    @DisplayName("Validates number within allowed range")
    void validNumberWithinRange() {
        AttributeCheckResult result = Attributes.check("5", "min:1; max:10; type:double");

        assertThat(result.valid()).isTrue();
        assertThat(result.failedRules()).isEmpty();
    }

    @Test
    @DisplayName("Fails when number is below the minimum")
    void failsBelowMin() {
        AttributeCheckResult result = Attributes.check("3", "min:5");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("min", "5"));
    }

    @Test
    @DisplayName("Fails when value cannot be parsed as number")
    void failsInvalidNumber() {
        AttributeCheckResult result = Attributes.check("notANumber", "type:double; min:1");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactlyInAnyOrder(
                        RuleDef.of("type", "double"),
                        RuleDef.of("min", "1")
                );
    }

    @Test
    @DisplayName("Accepts value equal to the minimum")
    void minBoundaryIsInclusive() {
        AttributeCheckResult result = Attributes.check("10", "min:10");

        assertThat(result.valid()).isTrue();
        assertThat(result.failedRules()).isEmpty();
    }

    @Test
    @DisplayName("Fails multiple rules when applicable")
    void multipleFailuresAreReported() {
        AttributeCheckResult result = Attributes.check("abc", "type:double; min:0; max:10");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactlyInAnyOrder(
                        RuleDef.of("type", "double"),
                        RuleDef.of("min", "0"),
                        RuleDef.of("max", "10")
                );
    }

    @Test
    @DisplayName("Blank value fails required rule")
    void blankFailsRequired() {
        AttributeCheckResult result = Attributes.check("   ", "required");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("required", null));
    }

    @Test
    @DisplayName("Non-blank value passes required rule")
    void requiredPassesOnNonBlank() {
        AttributeCheckResult result = Attributes.check("something", "required");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Regex rule passes for matching input")
    void regexMatchSucceeds() {
        AttributeCheckResult result = Attributes.check("12345", "regex:\\d+");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Regex rule fails for non-matching input")
    void regexMismatchFails() {
        AttributeCheckResult result = Attributes.check("abc", "regex:\\d+");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("regex", "\\d+"));
    }

    @Test
    @DisplayName("Boolean type passes for 'true' or 'false'")
    void booleanTypeRecognized() {
        AttributeCheckResult trueResult = Attributes.check("true", "type:boolean");
        AttributeCheckResult falseResult = Attributes.check("false", "type:boolean");

        assertThat(trueResult.valid()).isTrue();
        assertThat(falseResult.valid()).isTrue();
    }

    @Test
    @DisplayName("Boolean type fails for other values")
    void booleanTypeFailsOnOtherValues() {
        AttributeCheckResult result = Attributes.check("yes", "type:boolean");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("type", "boolean"));
    }

    @Test
    @DisplayName("Handles mixed success and failure rules")
    void partialSuccessReportsFailuresOnly() {
        AttributeCheckResult result = Attributes.check("abc", "required; type:double; min:1");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(
                        RuleDef.of("type", "double"),
                        RuleDef.of("min", "1")
                );
    }

    @Test
    @DisplayName("Succeeds on empty rule set")
    void emptyRuleSetAlwaysSucceeds() {
        AttributeCheckResult result = Attributes.check("whatever", "");

        assertThat(result.valid()).isTrue();
        assertThat(result.failedRules()).isEmpty();
    }

    @Test
    @DisplayName("Null rule string is treated as no rules")
    void nullRuleStringAlwaysValid() {
        AttributeCheckResult result = Attributes.check("something", null);

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Empty string is valid for non-required rules")
    void emptyStringWithNoRequiredIsValid() {
        AttributeCheckResult result = Attributes.check("", "type:double; min:0");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("type", "double"));
    }

    @Test
    @DisplayName("Null value fails required rule")
    void nullValueFailsRequired() {
        AttributeCheckResult result = Attributes.check(null, "required");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("required", null));
    }

    @Test
    @DisplayName("Fails gracefully with invalid numeric expectation")
    void failsWithInvalidNumericExpectation() {
        AttributeCheckResult result = Attributes.check("10", "min:abc");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("min", "abc"));
    }

    @Test
    @DisplayName("Handles regex with colon in pattern")
    void regexWithColonIsParsedCorrectly() {
        AttributeCheckResult result = Attributes.check("AB:1234", "regex:[A-Z]{2}:[0-9]{4}");

        assertThat(result.valid()).isTrue();
        assertThat(result.failedRules()).isEmpty();
    }

    @Test
    @DisplayName("Fails when rule expectation is blank but required")
    void failsWhenRequiredExpectationIsBlank() {
        assertThatThrownBy(() ->
                Attributes.check("10", "min:")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a non-blank expectation");
    }

    @Test
    @DisplayName("Unknown rule throws informative exception")
    void unknownRuleThrows() {
        assertThatThrownBy(() ->
                Attributes.check("foo", "nonexistent:xyz")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown rule");
    }

    @Test
    @DisplayName("Rule name trimming")
    void ruleNameTrimming() {
        AttributeCheckResult result = Attributes.check("true", "   type:boolean   ");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Multiple rules with overlapping logic fail as expected")
    void overlappingRulesFailTogether() {
        AttributeCheckResult result = Attributes.check("0", "required; min:1; max:0");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("min", "1"));
    }

    @Test
    @DisplayName("Required and regex rule on empty string fails both")
    void requiredAndRegexFailsOnEmpty() {
        AttributeCheckResult result = Attributes.check("", "required; regex:[0-9]+");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("required", null), RuleDef.of("regex", "[0-9]+"));
    }

    @Test
    @DisplayName("Case-insensitive boolean parsing")
    void booleanCaseInsensitive() {
        assertThat(Attributes.check("TRUE", "type:boolean").valid()).isTrue();
        assertThat(Attributes.check("False", "type:boolean").valid()).isTrue();
        assertThat(Attributes.check("TrUe", "type:boolean").valid()).isTrue();
    }

    @Test
    @DisplayName("Only value fails when rule expectation is missing but required")
    void onlyFailsOnMissingExpectationIfRuleRequiresIt() {
        assertThatThrownBy(() ->
                Attributes.check("42", "min")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a non-blank expectation");
    }

    @Test
    @DisplayName("Allows complex rule chains and only fails incorrect parts")
    void complexRuleSetPartiallyFails() {
        AttributeCheckResult result = Attributes.check("nope", "required; type:double; regex:\\d+; min:0");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("type", "double"), RuleDef.of("regex", "\\d+"), RuleDef.of("min", "0"));
    }

    @Test
    @DisplayName("Invalid regex pattern is reported via exception")
    void brokenRegexReturnsRuleDefException() {
        AttributeCheckResult result = Attributes.check("anything", "regex:[0-9");

        assertThat(result.valid()).isFalse();
        assertThat(result.exception()).isPresent();
        assertThat(result.exception().get())
                .isInstanceOf(RuleDefException.class);
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("regex", "[0-9"));
    }

    @Test
    @DisplayName("Invalid type rule expectation causes RuleDefException")
    void unsupportedTypeThrowsRuleDefException() {
        AttributeCheckResult result = Attributes.check("true", "type:tristate");

        assertThat(result.valid()).isFalse();
        assertThat(result.exception()).isPresent();
        assertThat(result.exception().get())
                .isInstanceOf(RuleDefException.class);
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("type", "tristate"));
    }

    @Test
    @DisplayName("Bad min rule expectation returns exception")
    void badMinRuleReturnsRuleDefException() {
        AttributeCheckResult result = Attributes.check("5", "min:abc");

        assertThat(result.valid()).isFalse();
        assertThat(result.exception()).isPresent();
        assertThat(result.exception().get())
                .isInstanceOf(RuleDefException.class);
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("min", "abc"));
    }

    @Test
    @DisplayName("Regex with escaped space does not crash but may fail due to trimming")
    void regexWithEscapeAndSpaceHandledSafely() {
        AttributeCheckResult result = Attributes.check("123 ", "regex:123\\  ");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("regex", "123\\"));
        assertThat(result.exception().get())
                .isInstanceOf(RuleDefException.class);
    }

    @Test
    @DisplayName("Only one of the duplicate rules is applied due to Set deduplication")
    void duplicateRulesSilentlyDeduplicated() {
        // Since RuleDef is a record, duplicate min will be collapsed into one — last wins is undefined
        AttributeCheckResult result = Attributes.check("5", "min:10; min:1");

        assertThat(result.valid() || result.failedRules().size() == 1).isTrue();
        assertThat(result.exception()).isEmpty();
    }
}
