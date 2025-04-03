package com.github.wingsofovnia.keycloak.organization;

import com.github.wingsofovnia.keycloak.organization.attribute.Attributes;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.Rule;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredAction.PROVIDER_ID;
import static com.github.wingsofovnia.keycloak.organization.util.RequiredActions.requiredActionConfigMapValueOf;
import static com.github.wingsofovnia.keycloak.organization.util.RequiredActions.requiredActionConfigValueOf;

public final class CreateOrganizationRequiredActionConfig {

    public static final String ATTRIBUTES_KEY = "attributes";

    public static final String SKIP_ROLE_KEY = "skip_role";
    public static final String SKIP_ROLE_DEFAULT_VALUE = "admin"; // realm admin

    public static final String GEN_DOMAIN_KEY = "generated_domain";
    public static final boolean GEN_DOMAIN_KEY_DEFAULT_VALUE = true;

    public static final String REDIRECT_QUERY_FLAG_KEY = "redirect_query_flag";
    public static final boolean REDIRECT_QUERY_FLAG_KEY_DEFAULT_VALUE = true;

    public static final String REDIRECT_QUERY_FLAG_NAME_KEY = "redirect_query_name_flag";
    public static final String REDIRECT_QUERY_FLAG_NAME_KEY_DEFAULT_VALUE = "newOrganization";

    public static final String ADD_AS_MANAGED_KEY = "add_as_managed";
    public static final String ADD_AS_MANAGED_OPT_MANAGED = "Managed";
    public static final String ADD_AS_MANAGED_OPT_UNMANAGED = "Unmanaged";

    public static final List<ProviderConfigProperty> PROVIDER_PROPERTIES = ProviderConfigurationBuilder.create()
            .property()
            .name(ATTRIBUTES_KEY)
            .label("Organization attributes")
            .helpText("""
                    Defines organization attributes to collect from the user during account creation.
                    Each entry maps an attribute name (key) to a semicolon-separated list of validation rules (value).
                    Attribute names will be shown as-is in the form unless a realm translation is set.
                    Supported rules:
                    required — not blank;
                    type:double|integer|boolean|string — asserts value is parsable to number or other types;
                    min/max — numeric boundaries (inclusive);
                    minLength/maxLength — string length constraints (inclusive);
                    regex — must match the given pattern.
                    All rules treat values as strings and parse when needed.
                    Example: "score" → "required; type:number; min:0; max:100"
                    """
            )
            .type(ProviderConfigProperty.MAP_TYPE)
            .add()
            .property()
            .name(SKIP_ROLE_KEY)
            .label("Do not require from users with role")
            .helpText("A role that should be skipped from requiring this action.")
            .type(ProviderConfigProperty.ROLE_TYPE)
            .defaultValue(SKIP_ROLE_DEFAULT_VALUE)
            .add()
            .property()
            .name(GEN_DOMAIN_KEY)
            .label("Generate random domain key")
            .helpText("Generates a unique random name for the domain, useful when domain name is not used.")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(GEN_DOMAIN_KEY_DEFAULT_VALUE)
            .add()
            .property()
            .name(REDIRECT_QUERY_FLAG_KEY)
            .label("Add a query flag set to true to the redirect URI")
            .helpText("If new organization is created by this required action, adds &{flag_name}=true to the redirect URL")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(REDIRECT_QUERY_FLAG_KEY_DEFAULT_VALUE)
            .add()
            .property()
            .name(REDIRECT_QUERY_FLAG_NAME_KEY)
            .label("Query param flag name to add to the redirect URI")
            .type(ProviderConfigProperty.STRING_TYPE)
            .defaultValue(REDIRECT_QUERY_FLAG_NAME_KEY_DEFAULT_VALUE)
            .add()
            .property()
            .name(ADD_AS_MANAGED_KEY)
            .label("Add a user to the created organization as")
            .helpText("""
                    Defines if a user should be added to the created organization as a managed or unmanaged member.
                    Managed, the lifecycle of the member is bound to the lifecycle of the organization and they can not exist without the organization they belong to.
                    Unmanaged, the lifecycle of the member is disassociated from the lifecycle of the organization and they can exist even though the organization does not exist.
                    """
            )
            .type(ProviderConfigProperty.LIST_TYPE)
            .options(List.of(ADD_AS_MANAGED_OPT_MANAGED, ADD_AS_MANAGED_OPT_UNMANAGED))
            .defaultValue(ADD_AS_MANAGED_OPT_MANAGED)
            .add()
            .build();

    private CreateOrganizationRequiredActionConfig() {
        throw new AssertionError();
    }

    public static Optional<String> getSkippedRole(KeycloakSession session) {
        return requiredActionConfigValueOf(SKIP_ROLE_KEY, PROVIDER_ID, session)
                .filter(role -> !role.isBlank());
    }

    public static boolean isDomainGenerationEnabled(KeycloakSession session) {
        return requiredActionConfigValueOf(GEN_DOMAIN_KEY, PROVIDER_ID, session)
                .map(Boolean::parseBoolean)
                .orElse(GEN_DOMAIN_KEY_DEFAULT_VALUE);
    }

    public static boolean isNewOrganizationQueryFlagEnabled(KeycloakSession session) {
        return requiredActionConfigValueOf(REDIRECT_QUERY_FLAG_KEY, PROVIDER_ID, session)
                .map(Boolean::parseBoolean)
                .orElse(REDIRECT_QUERY_FLAG_KEY_DEFAULT_VALUE);
    }

    public static String getNewOrganizationQueryFlagName(KeycloakSession session) {
        return requiredActionConfigValueOf(REDIRECT_QUERY_FLAG_NAME_KEY, PROVIDER_ID, session)
                .filter(name -> !name.isBlank())
                .orElse(REDIRECT_QUERY_FLAG_NAME_KEY_DEFAULT_VALUE);
    }

    public static boolean isAddAsManagedEnabled(KeycloakSession session) {
        final Optional<String> addAsManagedStr = requiredActionConfigValueOf(ADD_AS_MANAGED_KEY, PROVIDER_ID, session);
        return addAsManagedStr.map(ADD_AS_MANAGED_OPT_MANAGED::equals).orElse(true);
    }

    public static Optional<List<Rule>> getOrganizationAttributeRules(String attrName, KeycloakSession session) {
        return requiredActionConfigMapValueOf(ATTRIBUTES_KEY, PROVIDER_ID, session)
                .map(allAttrRules -> allAttrRules.get(attrName))
                .map(Attributes::parseRules);
    }

    public static Map<String, String> getOrganizationAttributeDefs(KeycloakSession session) {
        return requiredActionConfigMapValueOf(ATTRIBUTES_KEY, PROVIDER_ID, session).orElse(Map.of());
    }
}
