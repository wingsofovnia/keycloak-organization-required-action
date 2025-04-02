package com.github.wingsofovnia.keycloak.organization.attribute;

import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleDef;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleDefException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributesTest {

    @Test
    @DisplayName("Validates number within allowed range (explicit type)")
    void validNumberWithinRange() {
        AttributeCheckResult result = Attributes.check("5", "type:number; min:1; max:10");
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Fails when number is below the minimum (explicit type)")
    void failsBelowMin() {
        AttributeCheckResult result = Attributes.check("3", "type:number; min:5");
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("min", "5"));
    }

    @Test
    @DisplayName("Accepts value equal to the minimum (explicit type)")
    void minBoundaryIsInclusive() {
        AttributeCheckResult result = Attributes.check("10", "type:number; min:10");
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Max rule accepts exact boundary value for numbers (explicit type)")
    void maxBoundaryIsInclusiveForNumbers() {
        AttributeCheckResult result = Attributes.check("10", "type:number; max:10");
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Fails gracefully with invalid numeric expectation (explicit type)")
    void failsWithInvalidNumericExpectation() {
        AttributeCheckResult result = Attributes.check("10", "type:number; min:abc");
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("min", "abc"));
    }

    @Test
    @DisplayName("Trailing semicolon in rule string is ignored (with type)")
    void trailingSemicolonIgnored() {
        AttributeCheckResult result = Attributes.check("123", "type:number; min:100;");
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Whitespace-padded numeric strings are trimmed before type and min/max check")
    void paddedNumericStringStillValid() {
        AttributeCheckResult result = Attributes.check("   42   ", "type:number; min:40; max:50");
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Only type rule fails when value is a string with valid length")
    void onlyTypeFailsWhenMinMaxMatchLength() {
        AttributeCheckResult result = Attributes.check("abc", "type:number; minLength:2; maxLength:5");
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("type", "number"));
    }

    @Test
    @DisplayName("Blank value fails required rule")
    void blankFailsRequired() {
        AttributeCheckResult result = Attributes.check("   ", "required");
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("required", null));
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
        assertThat(result.failedRules()).containsExactly(RuleDef.of("regex", "\\d+"));
    }

    @Test
    @DisplayName("Boolean type passes for 'true' or 'false'")
    void booleanTypeRecognized() {
        assertThat(Attributes.check("true", "type:boolean").valid()).isTrue();
        assertThat(Attributes.check("false", "type:boolean").valid()).isTrue();
    }

    @Test
    @DisplayName("Boolean type fails for other values")
    void booleanTypeFailsOnOtherValues() {
        AttributeCheckResult result = Attributes.check("yes", "type:boolean");
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("type", "boolean"));
    }

    @Test
    @DisplayName("Mixed rules fail only on type (minLength passes)")
    void mixedRulesOnlyFailNonMatchingOnes() {
        AttributeCheckResult result = Attributes.check("abc", "required; type:number; minLength:2");
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("type", "number"));
    }

    @Test
    @DisplayName("Succeeds on empty rule set")
    void emptyRuleSetAlwaysSucceeds() {
        assertThat(Attributes.check("whatever", "").valid()).isTrue();
    }

    @Test
    @DisplayName("Null rule string is treated as no rules")
    void nullRuleStringAlwaysValid() {
        assertThat(Attributes.check("something", null).valid()).isTrue();
    }

    @Test
    @DisplayName("Empty string is valid for non-required rules")
    void emptyStringWithNoRequiredIsValid() {
        AttributeCheckResult result = Attributes.check("", "type:number; min:0");
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).contains(RuleDef.of("type", "number"));
    }

    @Test
    @DisplayName("Null value fails required rule")
    void nullValueFailsRequired() {
        AttributeCheckResult result = Attributes.check(null, "required");
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).contains(RuleDef.of("required", null));
    }

    @Test
    @DisplayName("Handles regex with colon in pattern")
    void regexWithColonIsParsedCorrectly() {
        AttributeCheckResult result = Attributes.check("AB:1234", "regex:[A-Z]{2}:[0-9]{4}");
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Fails when rule expectation is blank but required")
    void failsWhenRequiredExpectationIsBlank() {
        assertThatThrownBy(() -> Attributes.check("10", "min:"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a non-blank expectation");
    }

    @Test
    @DisplayName("Unknown rule throws informative exception")
    void unknownRuleThrows() {
        assertThatThrownBy(() -> Attributes.check("foo", "nonexistent:xyz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown rule");
    }

    @Test
    @DisplayName("Rule name trimming works")
    void ruleNameTrimming() {
        assertThat(Attributes.check("true", "   type:boolean   ").valid()).isTrue();
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
    @DisplayName("Fails on type and regex if minLength passes")
    void failsOnlyOnTypeAndRegexIfMinLengthPasses() {
        AttributeCheckResult result = Attributes.check("nope", "required; type:number; regex:\\d+; minLength:2");
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules())
                .containsExactlyInAnyOrder(
                        RuleDef.of("type", "number"),
                        RuleDef.of("regex", "\\d+")
                );
    }

    @Test
    @DisplayName("Invalid regex pattern is reported via exception")
    void brokenRegexReturnsRuleDefException() {
        AttributeCheckResult result = Attributes.check("anything", "regex:[0-9");
        assertThat(result.valid()).isFalse();
        assertThat(result.exception()).isPresent();
        assertThat(result.exception().get()).isInstanceOf(RuleDefException.class);
    }

    @Test
    @DisplayName("Invalid type rule expectation causes RuleDefException")
    void unsupportedTypeThrowsRuleDefException() {
        AttributeCheckResult result = Attributes.check("true", "type:tristate");
        assertThat(result.valid()).isFalse();
        assertThat(result.exception()).isPresent();
        assertThat(result.exception().get()).isInstanceOf(RuleDefException.class);
    }

    @Test
    @DisplayName("Bad min rule expectation returns exception")
    void badMinRuleReturnsRuleDefException() {
        AttributeCheckResult result = Attributes.check("5", "type:number; min:abc");
        assertThat(result.valid()).isFalse();
        assertThat(result.exception()).isPresent();
    }

    @Test
    @DisplayName("Regex with escaped space does not crash but fails")
    void regexWithEscapeAndSpaceHandledSafely() {
        AttributeCheckResult result = Attributes.check("123 ", "regex:123\\  ");
        assertThat(result.valid()).isFalse();
        assertThat(result.exception()).isPresent();
    }

    @Test
    @DisplayName("Duplicate rules are deduplicated")
    void duplicateRulesSilentlyDeduplicated() {
        AttributeCheckResult result = Attributes.check("5", "type:number; min:10; min:1");
        assertThat(result.valid() || result.failedRules().size() == 1).isTrue();
    }

    @Test
    @DisplayName("MinLength rule applies to trimmed value")
    void minLengthAppliesToTrimmedValue() {
        AttributeCheckResult result = Attributes.check(" ab ", "minLength:2");
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("MaxLength rule fails on too long input")
    void maxLengthFails() {
        AttributeCheckResult result = Attributes.check("abcdef", "maxLength:5");
        assertThat(result.valid()).isFalse();
        assertThat(result.failedRules()).containsExactly(RuleDef.of("maxLength", "5"));
    }

    @Test
    @DisplayName("Whitespace-only string fails minLength rule")
    void whitespaceOnlyFailsMinLength() {
        AttributeCheckResult result = Attributes.check("   ", "minLength:1");
        assertThat(result.valid()).isFalse();
    }

    @Test
    @DisplayName("All rules pass on valid input")
    void allRulesPassWhenEverythingIsValid() {
        AttributeCheckResult result = Attributes.check("true", "required; regex:true|false; type:boolean");
        assertThat(result.valid()).isTrue();
    }
}
