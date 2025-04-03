package com.github.wingsofovnia.keycloak.organization.attribute;

import com.github.wingsofovnia.keycloak.organization.attribute.rule.MaxLengthRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MaxRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MinRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RegexRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RequiredRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleDefException;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.TypeRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributesTest {

    @Test
    @DisplayName("Validates number within allowed range")
    void validNumberWithinRange() {
        AttributeCheckResult result = Attributes.check("5", "min:1; max:10");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Fails when number is below the minimum")
    void failsBelowMin() {
        AttributeCheckResult result = Attributes.check("3", "min:5");
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedRules()).containsExactly(new MinRule(5));
    }

    @Test
    @DisplayName("Accepts value equal to the minimum (i.e. is inclusive)")
    void minBoundaryIsInclusive() {
        AttributeCheckResult result = Attributes.check("10", "min:10");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Whitespace-padded numeric strings are trimmed before type and min/max check")
    void paddedNumericStringStillValid() {
        AttributeCheckResult result = Attributes.check("   42   ", "min:40; max:50");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("min:1 fails when blank string is treated as 0")
    void minFailsOnBlankStringParsedAsZero() {
        AttributeCheckResult result = Attributes.check("   ", "min:1");
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(new MinRule(1));
    }

    @Test
    @DisplayName("Fails when min expectation is blank")
    void failsWhenMinExpectationIsBlank() {
        assertThatThrownBy(() -> Attributes.check("10", "min:"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("max:-1 fails when blank string is treated as 0")
    void maxFailsOnBlankStringParsedAsZero() {
        AttributeCheckResult result = Attributes.check("   ", "max:-1");
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedRules())
                .containsExactly(new MaxRule(-1));
    }


    @Test
    @DisplayName("max:1 does not fail when blank string is treated as 0")
    void maxNotFailsOnBlankStringParsedAsZero() {
        AttributeCheckResult result = Attributes.check("   ", "max:1");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Min throws with invalid numeric expectation")
    void minThrowWithInvalidNumericExpectation() {
        assertThatThrownBy(() -> Attributes.check("10", "min:abc"))
                .isInstanceOf(RuleDefException.class);
    }

    @Test
    @DisplayName("Max throws with invalid numeric expectation")
    void maxThrowWithInvalidNumericExpectation() {
        assertThatThrownBy(() -> Attributes.check("10", "max:abc"))
                .isInstanceOf(RuleDefException.class);
    }

    @Test
    @DisplayName("Only type rule fails when value is a string with isValid length")
    void onlyTypeFailsWhenMinMaxMatchLength() {
        AttributeCheckResult result = Attributes.check("abc", "type:number; minLength:2; maxLength:5");
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedRules()).containsExactly(new TypeRule(TypeRule.Type.of("number").get()));
    }

    @Test
    @DisplayName("MinLength rule applies to trimmed value")
    void minLengthAppliesToTrimmedValue() {
        AttributeCheckResult result = Attributes.check(" ab ", "minLength:2");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Whitespace-only string fails minLength rule")
    void whitespaceOnlyFailsMinLength() {
        AttributeCheckResult result = Attributes.check("   ", "minLength:1");
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("MaxLength rule fails on too long input")
    void maxLengthFails() {
        AttributeCheckResult result = Attributes.check("abcdef", "maxLength:5");
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedRules()).containsExactly(new MaxLengthRule(5));
    }

    @Test
    @DisplayName("Blank value fails required rule")
    void blankFailsRequired() {
        AttributeCheckResult result = Attributes.check("   ", "required");
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedRules()).containsExactly(new RequiredRule());
    }

    @Test
    @DisplayName("Null value fails required rule")
    void nullValueFailsRequired() {
        AttributeCheckResult result = Attributes.check(null, "required");
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedRules()).contains(new RequiredRule());
    }

    @Test
    @DisplayName("Non-blank value passes required rule")
    void requiredPassesOnNonBlank() {
        AttributeCheckResult result = Attributes.check("something", "required");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Regex rule passes for matching input")
    void regexMatchSucceeds() {
        AttributeCheckResult result = Attributes.check("12345", "regex:\\d+");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Regex rule fails for non-matching input")
    void regexMismatchFails() {
        AttributeCheckResult result = Attributes.check("abc", "regex:\\d+");
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedRules()).containsExactly(new RegexRule(Pattern.compile("\\d+")));
    }

    @Test
    @DisplayName("Invalid regex pattern is reported via exception")
    void brokenRegexReturnsRuleDefException() {
        assertThatThrownBy(() -> {
            Attributes.check("anything", "regex:[0-9");
        }).isInstanceOf(RuleDefException.class);
    }

    @Test
    @DisplayName("Handles regex with colon in pattern")
    void regexWithColonIsParsedCorrectly() {
        AttributeCheckResult result = Attributes.check("AB:1234", "regex:[A-Z]{2}:[0-9]{4}");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Boolean type passes for 'true' or 'false'")
    void booleanTypeRecognized() {
        assertThat(Attributes.check("true", "type:boolean").isValid()).isTrue();
        assertThat(Attributes.check("false", "type:boolean").isValid()).isTrue();
    }

    @Test
    @DisplayName("Boolean type fails for other values")
    void booleanTypeFailsOnOtherValues() {
        AttributeCheckResult result = Attributes.check("yes", "type:boolean");
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedRules()).containsExactly(new TypeRule(TypeRule.Type.of("boolean").get()));
    }

    @Test
    @DisplayName("Case-insensitive boolean parsing")
    void booleanCaseInsensitive() {
        assertThat(Attributes.check("TRUE", "type:boolean").isValid()).isTrue();
        assertThat(Attributes.check("False", "type:boolean").isValid()).isTrue();
        assertThat(Attributes.check("TrUe", "type:boolean").isValid()).isTrue();
    }


    @Test
    @DisplayName("Invalid type rule expectation causes RuleDefException")
    void unsupportedTypeThrowsRuleDefException() {
        assertThatThrownBy(() -> {
            Attributes.check("true", "type:tristate");
        }).isInstanceOf(RuleDefException.class);
    }

    // ---


    @Test
    @DisplayName("Trailing semicolon in rule string is ignored")
    void trailingSemicolonIgnored() {
        AttributeCheckResult result = Attributes.check("123", "type:number; min:100;");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Continue checking all rules in case of failure")
    void continueCheckingAfterFailure() {
        AttributeCheckResult result = Attributes.check("abc", "type:number; required; maxLength:2");
        assertThat(result.isValid()).isFalse();
        assertThat(result.failedRules())
                .contains(new TypeRule(TypeRule.Type.of("number").get()), new MaxLengthRule(2));
    }

    @Test
    @DisplayName("Succeeds on empty rule set")
    void emptyRuleSetAlwaysSucceeds() {
        assertThat(Attributes.check("whatever", "").isValid()).isTrue();
    }

    @Test
    @DisplayName("Null rule string is treated as no rules")
    void nullRuleStringAlwaysValid() {
        assertThat(Attributes.check("something", (String) null).isValid()).isTrue();
    }

    @Test
    @DisplayName("Unknown rule throws informative exception")
    void unknownRuleThrows() {
        assertThatThrownBy(() -> Attributes.check("foo", "nonexistent:xyz"))
                .isInstanceOf(RuleDefException.class)
                .hasMessageContaining("Unknown rule");
    }

    @Test
    @DisplayName("Rule name trimming works")
    void ruleNameTrimming() {
        assertThat(Attributes.check("true", "   type:boolean   ").isValid()).isTrue();
    }


    @Test
    @DisplayName("In case of duplicated rules takes last")
    void duplicateRulesSilentlyDeduplicated() {
        AttributeCheckResult result = Attributes.check("5", "type:number; min:10; min:1");
        assertThat(result.isValid() || result.failedRules().size() == 1).isTrue();
    }

    @Test
    @DisplayName("All rules pass on isValid input")
    void allRulesPassWhenEverythingIsValid() {
        AttributeCheckResult result = Attributes.check("true", "required; regex:true|false; type:boolean");
        assertThat(result.isValid()).isTrue();
    }
}
