package com.github.wingsofovnia.keycloak.organization;

import com.github.wingsofovnia.keycloak.organization.attribute.AttributeCheckResult;
import com.github.wingsofovnia.keycloak.organization.attribute.Attributes;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MaxLengthRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MaxRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MinLengthRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MinRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RequiredRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.Rule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RuleWithExpectation;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.TypeRule;
import com.github.wingsofovnia.keycloak.organization.util.Messages;
import jakarta.annotation.Nonnull;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.utils.FormMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionConfig.getOrganizationAttributeDefs;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionConfig.isDomainGenerationEnabled;
import static com.github.wingsofovnia.keycloak.organization.util.Maps.singleValueMapOf;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public final class CreateOrganizationRequiredActionForm {

    private static final String ORGANIZATION_NAME_FIELD = "orgName";
    private static final String ORGANIZATION_DOMAIN_FIELD = "orgDomain";
    private static final String ORGANIZATION_ATTR_FIELD_PREFIX = "orgAttr_";

    private CreateOrganizationRequiredActionForm() {
        throw new AssertionError();
    }

    public static Optional<String> getOrganizationName(MultivaluedMap<String, String> formData) {
        final String nameFieldValue = formData.getFirst(ORGANIZATION_NAME_FIELD);
        if (nameFieldValue == null || nameFieldValue.isBlank()) {
            return Optional.ofNullable(nameFieldValue);
        }
        return Optional.of(nameFieldValue.trim());
    }

    public static Optional<String> getOrganizationDomain(MultivaluedMap<String, String> formData) {
        final String domainFieldValue = formData.getFirst(ORGANIZATION_DOMAIN_FIELD);
        if (domainFieldValue == null || domainFieldValue.isBlank()) {
            return Optional.ofNullable(domainFieldValue);
        }
        return Optional.of(domainFieldValue.trim());
    }

    public static Map<String, String> getOrganizationAttributes(MultivaluedMap<String, String> formData) {
        final Map<String, String> attributeFieldValues = new HashMap<>();
        singleValueMapOf(formData).forEach((attrFieldName, attrValue) -> {
            if (attrFieldName.startsWith(ORGANIZATION_ATTR_FIELD_PREFIX)) {
                final String attrName = attrFieldName.substring(ORGANIZATION_ATTR_FIELD_PREFIX.length());
                attributeFieldValues.put(attrName, attrValue);
            }
        });
        return attributeFieldValues;
    }

    public static FormMessage createBlankOrganizationNameError() {
        return new FormMessage(ORGANIZATION_NAME_FIELD, Messages.ORGANIZATION_NAME_IS_BLANK);
    }

    public static FormMessage createDuplicateOrganizationNameError() {
        return new FormMessage(ORGANIZATION_NAME_FIELD, Messages.ORGANIZATION_EXISTS);
    }

    public static FormMessage createBlankOrganizationDomainError() {
        return new FormMessage(ORGANIZATION_DOMAIN_FIELD, Messages.ORGANIZATION_DOMAIN_IS_BLANK);
    }

    public static FormMessage createInvalidOrganizationDomainError() {
        return new FormMessage(ORGANIZATION_DOMAIN_FIELD, Messages.ORGANIZATION_DOMAIN_IS_INVALID);
    }

    public static FormMessage createInvalidOrganizationAttributesError() {
        return new FormMessage(Messages.ORGANIZATION_ATTRIBUTES_ARE_INVALID);
    }

    public static List<FormMessage> createInvalidOrganizationAttributesError(Map<String, AttributeCheckResult> checkResults) {
        return checkResults.entrySet().stream()
                .filter(entry -> !entry.getValue().isValid())
                .map(entry -> {
                    final String attrName = entry.getKey();
                    final List<Rule> failedRules = entry.getValue().failedRules();
                    final String failedRulesStr = failedRules.stream()
                            .map(rule -> {
                                if (rule instanceof RuleWithExpectation<?>) {
                                    return rule.name() + ": " + ((RuleWithExpectation<?>) rule).expectation();
                                } else {
                                    return rule.name();
                                }
                            })
                            .collect(joining(", "));
                    return new FormMessage(ORGANIZATION_ATTR_FIELD_PREFIX + attrName, Messages.ORGANIZATION_ATTRIBUTE_VALIDATION_ERROR, failedRulesStr);
                }).collect(Collectors.toList());
    }

    public static Response createForm(
            RequiredActionContext context,
            MultivaluedMap<String, String> formData,
            List<FormMessage> errors
    ) {
        final LoginFormsProvider loginFormsProvider = context.form()
                .setAttribute("isDomainGenerationEnabled", isDomainGenerationEnabled(context.getSession()))
                .setAttribute("formData", formData.entrySet().stream()
                        .filter(entry -> !entry.getValue().isEmpty())
                        .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().get(0))));

        if (!errors.isEmpty()) {
            errors.forEach(loginFormsProvider::addError);
        }

        final Map<String, Map<String, String>> attributes = new HashMap<>();
        for (Map.Entry<String, String> attrDef : getOrganizationAttributeDefs(context.getSession()).entrySet()) {
            final String attrName = attrDef.getKey();
            final String attrRuleDefSetStr = attrDef.getValue();

            final List<Rule> attrRules = Attributes.parseRules(attrRuleDefSetStr);
            attributes.put(attrName, fieldValidationAttributesOf(attrRules));
        }
        if (!attributes.isEmpty()) {
            loginFormsProvider.setAttribute("attributes", attributes);
        }

        return loginFormsProvider.createForm("new_organization.ftl");
    }

    public static Response createForm(
            RequiredActionContext context,
            MultivaluedMap<String, String> formData,
            FormMessage error
    ) {
        return createForm(context, formData, List.of(error));
    }

    public static Response createForm(RequiredActionContext context) {
        return createForm(context, new MultivaluedHashMap<>(), List.of());
    }

    private static Map<String, String> fieldValidationAttributesOf(List<Rule> rules) {
        final String attrFieldType = rules.stream()
                .filter(rule -> rule instanceof TypeRule)
                .map(rule -> switch (((TypeRule) rule).expectation()) {
                    case DOUBLE, INTEGER, FLOAT -> "number";
                    default -> "text";
                })
                .findAny()
                .orElse("text");


        final String attrFieldMax = rules.stream()
                .filter(rule -> rule instanceof MaxRule)
                .map(rule -> ((MaxRule) rule).expectation().toString())
                .findAny()
                .orElse("");
        final String attrFieldMin = rules.stream()
                .filter(rule -> rule instanceof MinRule)
                .map(rule -> ((MinRule) rule).expectation().toString())
                .findAny()
                .orElse("");


        final String attrFieldMaxLength = rules.stream()
                .filter(rule -> rule instanceof MaxLengthRule)
                .map(rule -> ((MaxLengthRule) rule).expectation().toString())
                .findAny()
                .orElse("");
        final String attrFieldMinLength = rules.stream()
                .filter(rule -> rule instanceof MinLengthRule)
                .map(rule -> ((MinLengthRule) rule).expectation().toString())
                .findAny()
                .orElse("");

        final String attrFieldRequired = rules.stream()
                .filter(rule -> rule instanceof RequiredRule)
                .map(def -> Boolean.TRUE.toString())
                .findAny()
                .orElse("");

        return Map.of(
                "type", attrFieldType,
                "required", attrFieldRequired,
                "min", attrFieldMin,
                "max", attrFieldMax,
                "minLength", attrFieldMinLength,
                "maxLength", attrFieldMaxLength
        );
    }

}
