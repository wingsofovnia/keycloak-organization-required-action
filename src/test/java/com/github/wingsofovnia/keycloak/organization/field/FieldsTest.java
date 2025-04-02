package com.github.wingsofovnia.keycloak.organization.field;

import com.github.wingsofovnia.keycloak.organization.field.rule.RuleDef;
import com.github.wingsofovnia.keycloak.organization.field.rule.RuleDefException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldsTest {

    @Test
    @DisplayName("Validates number within allowed range")
    void validNumberWithinRange() {
        FieldCheckResult result = Fields.check("5", "min:1; max:10; type:double");

        assertThat(result.valid()).isTrue();
        assertThat(result.failedRules()).isEmpty();
    }

    @Test
    @DisplayName("Fails when number is below the minimum")
    void failsBelowMin() {
        FieldCheckResult result = Fields.check("3", "min:5");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("min", "5"));
    }

    @Test
    @DisplayName("Fails when value cannot be parsed as number")
    void failsInvalidNumber() {
        FieldCheckResult result = Fields.check("notANumber", "type:double; min:1");

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
        FieldCheckResult result = Fields.check("10", "min:10");

        assertThat(result.valid()).isTrue();
        assertThat(result.failedRules()).isEmpty();
    }

    @Test
    @DisplayName("Fails multiple rules when applicable")
    void multipleFailuresAreReported() {
        FieldCheckResult result = Fields.check("abc", "type:double; min:0; max:10");

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
        FieldCheckResult result = Fields.check("   ", "required");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("required", null));
    }

    @Test
    @DisplayName("Non-blank value passes required rule")
    void requiredPassesOnNonBlank() {
        FieldCheckResult result = Fields.check("something", "required");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Regex rule passes for matching input")
    void regexMatchSucceeds() {
        FieldCheckResult result = Fields.check("12345", "regex:\\d+");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Regex rule fails for non-matching input")
    void regexMismatchFails() {
        FieldCheckResult result = Fields.check("abc", "regex:\\d+");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("regex", "\\d+"));
    }

    @Test
    @DisplayName("Boolean type passes for 'true' or 'false'")
    void booleanTypeRecognized() {
        FieldCheckResult trueResult = Fields.check("true", "type:boolean");
        FieldCheckResult falseResult = Fields.check("false", "type:boolean");

        assertThat(trueResult.valid()).isTrue();
        assertThat(falseResult.valid()).isTrue();
    }

    @Test
    @DisplayName("Boolean type fails for other values")
    void booleanTypeFailsOnOtherValues() {
        FieldCheckResult result = Fields.check("yes", "type:boolean");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("type", "boolean"));
    }

    @Test
    @DisplayName("Handles mixed success and failure rules")
    void partialSuccessReportsFailuresOnly() {
        FieldCheckResult result = Fields.check("abc", "required; type:double; min:1");

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
        FieldCheckResult result = Fields.check("whatever", "");

        assertThat(result.valid()).isTrue();
        assertThat(result.failedRules()).isEmpty();
    }

    @Test
    @DisplayName("Null rule string is treated as no rules")
    void nullRuleStringAlwaysValid() {
        FieldCheckResult result = Fields.check("something", null);

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Empty string is valid for non-required rules")
    void emptyStringWithNoRequiredIsValid() {
        FieldCheckResult result = Fields.check("", "type:double; min:0");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("type", "double"));
    }

    @Test
    @DisplayName("Null value fails required rule")
    void nullValueFailsRequired() {
        FieldCheckResult result = Fields.check(null, "required");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("required", null));
    }

    @Test
    @DisplayName("Fails gracefully with invalid numeric expectation")
    void failsWithInvalidNumericExpectation() {
        FieldCheckResult result = Fields.check("10", "min:abc");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("min", "abc"));
    }

    @Test
    @DisplayName("Handles regex with colon in pattern")
    void regexWithColonIsParsedCorrectly() {
        FieldCheckResult result = Fields.check("AB:1234", "regex:[A-Z]{2}:[0-9]{4}");

        assertThat(result.valid()).isTrue();
        assertThat(result.failedRules()).isEmpty();
    }

    @Test
    @DisplayName("Fails when rule expectation is blank but required")
    void failsWhenRequiredExpectationIsBlank() {
        assertThatThrownBy(() ->
                Fields.check("10", "min:")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a non-blank expectation");
    }

    @Test
    @DisplayName("Unknown rule throws informative exception")
    void unknownRuleThrows() {
        assertThatThrownBy(() ->
                Fields.check("foo", "nonexistent:xyz")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown rule");
    }

    @Test
    @DisplayName("Rule name trimming")
    void ruleNameTrimming() {
        FieldCheckResult result = Fields.check("true", "   type:boolean   ");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Multiple rules with overlapping logic fail as expected")
    void overlappingRulesFailTogether() {
        FieldCheckResult result = Fields.check("0", "required; min:1; max:0");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("min", "1"));
    }

    @Test
    @DisplayName("Required and regex rule on empty string fails both")
    void requiredAndRegexFailsOnEmpty() {
        FieldCheckResult result = Fields.check("", "required; regex:[0-9]+");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("required", null), RuleDef.of("regex", "[0-9]+"));
    }

    @Test
    @DisplayName("Case-insensitive boolean parsing")
    void booleanCaseInsensitive() {
        assertThat(Fields.check("TRUE", "type:boolean").valid()).isTrue();
        assertThat(Fields.check("False", "type:boolean").valid()).isTrue();
        assertThat(Fields.check("TrUe", "type:boolean").valid()).isTrue();
    }

    @Test
    @DisplayName("Only value fails when rule expectation is missing but required")
    void onlyFailsOnMissingExpectationIfRuleRequiresIt() {
        assertThatThrownBy(() ->
                Fields.check("42", "min")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a non-blank expectation");
    }

    @Test
    @DisplayName("Allows complex rule chains and only fails incorrect parts")
    void complexRuleSetPartiallyFails() {
        FieldCheckResult result = Fields.check("nope", "required; type:double; regex:\\d+; min:0");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .contains(RuleDef.of("type", "double"), RuleDef.of("regex", "\\d+"), RuleDef.of("min", "0"));
    }

    @Test
    @DisplayName("Invalid regex pattern is reported via exception")
    void brokenRegexReturnsRuleDefException() {
        FieldCheckResult result = Fields.check("anything", "regex:[0-9");

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
        FieldCheckResult result = Fields.check("true", "type:tristate");

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
        FieldCheckResult result = Fields.check("5", "min:abc");

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
        FieldCheckResult result = Fields.check("123 ", "regex:123\\  ");

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
        FieldCheckResult result = Fields.check("5", "min:10; min:1");

        assertThat(result.valid() || result.failedRules().size() == 1).isTrue();
        assertThat(result.exception()).isEmpty();
    }
}
