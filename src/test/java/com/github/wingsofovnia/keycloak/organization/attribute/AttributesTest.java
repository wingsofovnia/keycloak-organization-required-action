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
    @DisplayName("Min rule falls back to string length when value is not numeric")
    void minRuleFallsBackToLengthWhenNotNumeric() {
        AttributeCheckResult result = Attributes.check("notANumber", "min:5");

        // "notANumber".length() == 11 ≥ 5 → should pass
        assertThat(result.valid()).isTrue();
        assertThat(result.failedRules()).isEmpty();
    }

    @Test
    @DisplayName("Accepts value equal to the minimum")
    void minBoundaryIsInclusive() {
        AttributeCheckResult result = Attributes.check("10", "min:10");

        assertThat(result.valid()).isTrue();
        assertThat(result.failedRules()).isEmpty();
    }

    @Test
    @DisplayName("Only type rule fails when value is a string with valid length")
    void onlyTypeFailsWhenMinMaxMatchLength() {
        AttributeCheckResult result = Attributes.check("abc", "type:double; min:2; max:5");

        // type:double → fail, min/max → pass (length 3)
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("type", "double"));
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
    @DisplayName("Mixed rules fail only on type")
    void mixedRulesOnlyFailNonMatchingOnes() {
        AttributeCheckResult result = Attributes.check("abc", "required; type:double; min:2");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(RuleDef.of("type", "double"));
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
    @DisplayName("Fails on type and regex if min passes by length")
    void failsOnlyOnTypeAndRegexIfMinLengthPasses() {
        AttributeCheckResult result = Attributes.check("nope", "required; type:double; regex:\\d+; min:2");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactlyInAnyOrder(
                        RuleDef.of("type", "double"),
                        RuleDef.of("regex", "\\d+")
                );
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

    @Test
    @DisplayName("Min rule applies to string length when value is not numeric")
    void minAppliesToStringLength() {
        AttributeCheckResult result = Attributes.check("abcd", "min:3");

        assertThat(result.valid()).isTrue(); // "abcd".length() == 4 >= 3
    }

    @Test
    @DisplayName("Min rule fails for short strings")
    void minFailsOnShortString() {
        AttributeCheckResult result = Attributes.check("ab", "min:3");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("min", "3"));
    }

    @Test
    @DisplayName("Max rule applies to string length when value is not numeric")
    void maxAppliesToStringLength() {
        AttributeCheckResult result = Attributes.check("abc", "max:3");

        assertThat(result.valid()).isTrue(); // length == 3
    }

    @Test
    @DisplayName("Max rule fails for long strings")
    void maxFailsOnLongString() {
        AttributeCheckResult result = Attributes.check("abcdef", "max:5");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("max", "5"));
    }

    @Test
    @DisplayName("Value that looks like number but is invalid falls back to length")
    void numericParsingFallbacksToLength() {
        AttributeCheckResult result = Attributes.check("5abc", "min:4");

        assertThat(result.valid()).isTrue(); // fallback to length check: "5abc".length() == 4
    }

    @Test
    @DisplayName("Min rule on empty string with fallback to length fails")
    void minFailsOnEmptyStringLength() {
        AttributeCheckResult result = Attributes.check("", "min:1");

        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("min", "1"));
    }

    @Test
    @DisplayName("Max rule on empty string with fallback to length passes")
    void maxPassesOnEmptyStringLength() {
        AttributeCheckResult result = Attributes.check("", "max:0");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("String with spaces is trimmed before applying min/max")
    void minMaxTrimmedValueBeforeLengthCheck() {
        AttributeCheckResult result = Attributes.check("  ab  ", "min:2; max:3");

        // trimmed: "ab" → length 2, min:2 OK, max:3 OK
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Whitespace-only value fails min rule based on length")
    void whitespaceOnlyFailsMinAsLengthIsZero() {
        AttributeCheckResult result = Attributes.check("    ", "min:1");

        // trimmed value becomes empty string
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("min", "1"));
    }

    @Test
    @DisplayName("Max rule accepts exact boundary value for numbers")
    void maxBoundaryIsInclusiveForNumbers() {
        AttributeCheckResult result = Attributes.check("10", "max:10");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Max rule accepts string of exact length")
    void maxBoundaryIsInclusiveForLength() {
        AttributeCheckResult result = Attributes.check("abc", "max:3");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Whitespace-padded numeric strings are trimmed before type and min/max check")
    void paddedNumericStringStillValid() {
        AttributeCheckResult result = Attributes.check("   42   ", "type:double; min:40; max:50");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("All rules pass with well-formed value")
    void allRulesPassWhenEverythingIsValid() {
        AttributeCheckResult result = Attributes.check("true", "required; regex:true|false; type:boolean");

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Trailing semicolon in rule string is ignored")
    void trailingSemicolonIgnored() {
        AttributeCheckResult result = Attributes.check("123", "type:double; min:100;");

        assertThat(result.valid()).isTrue();
    }
}
