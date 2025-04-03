package com.github.wingsofovnia.keycloak.organization;

import com.github.wingsofovnia.keycloak.organization.attribute.AttributeCheckResult;
import com.github.wingsofovnia.keycloak.organization.attribute.Attributes;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MaxLengthRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MaxRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MinLengthRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.MinRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.RequiredRule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.Rule;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.TypeRule;
import jakarta.annotation.Nonnull;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.Profile;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.MapperTypeSerializer;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.wingsofovnia.keycloak.organization.Organizations.getInvitingOrganization;
import static com.github.wingsofovnia.keycloak.organization.Organizations.organizationAliasOf;
import static com.github.wingsofovnia.keycloak.organization.Organizations.randomDomainOf;
import static java.util.stream.Collectors.toMap;
import static org.keycloak.utils.RequiredActionHelper.getRequiredActionByProviderId;

public class CreateOrganizationRequiredAction implements RequiredActionProvider, RequiredActionFactory {

    private static final String PROVIDER_ID = "create-organization-required-action";

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

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property()
            .name(ATTRIBUTES_KEY)
            .label("Organization attributes")
            .helpText("""
                    Defines organization attributes to collect from the user during account creation.
                    Each entry maps an attribute name (key) to a semicolon-separated list of validation rules (value).
                    Attribute names will be shown as-is in the form unless a realm translation is set.
                    Supported rules: required — not blank;
                    type:number|boolean — asserts value is parsable to number or boolean;
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

    private static final String ORGANIZATION_NAME_FIELD = "orgName";
    private static final String ORGANIZATION_DOMAIN_FIELD = "orgDomain";
    private static final String ORGANIZATION_ATTR_FIELD_PREFIX = "orgAttr_";

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return;
        }
        final OrganizationProvider organizationProvider = context.getSession()
                .getProvider(OrganizationProvider.class);

        final UserModel authenticatedUser = context.getUser();
        if (authenticatedUser == null) {
            return;
        }

        // Invited users should not create any new org but join to the invited one.
        if (getInvitingOrganization(context.getSession()).isPresent()) {
            authenticatedUser.removeRequiredAction(PROVIDER_ID);
            return;
        }

        // Check if the user has a role which was requested to be skipped from this required action (e.g. realm admins).
        final Optional<String> maybeSkippedRole = getSkippedRole(context.getSession());
        if (maybeSkippedRole.isPresent()) {
            final String skippedRole = maybeSkippedRole.get();
            final Stream<String> authenticatedUserRoles = authenticatedUser
                    .getRoleMappingsStream()
                    .map(RoleModel::getName);

            if (authenticatedUserRoles.anyMatch(skippedRole::equals)) {
                authenticatedUser.removeRequiredAction(PROVIDER_ID);
                return;
            }
        }

        // If the user is a member of some other organization, skip.
        if (organizationProvider.getByMember(authenticatedUser).findAny().isEmpty()) {
            authenticatedUser.addRequiredAction(PROVIDER_ID);
        } else {
            authenticatedUser.removeRequiredAction(PROVIDER_ID);
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        context.challenge(challengeOf(context));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        final UserModel user = context.getUser();
        final MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        final OrganizationProvider organizationProvider = context.getSession().getProvider(OrganizationProvider.class);

        // Read form values
        final String organizationName = formData.getFirst(ORGANIZATION_NAME_FIELD);
        if (organizationName == null || organizationName.isBlank()) {
            final List<FormMessage> formMessages = List.of(new FormMessage(ORGANIZATION_NAME_FIELD, Messages.ORGANIZATION_NAME_IS_BLANK));
            context.challenge(challengeOf(context, formData, formMessages));
            return;
        }
        final String organizationAlias = organizationAliasOf(organizationName);
        final String organizationDomainName;
        if (isDomainGenerationEnabled(context.getSession())) {
            organizationDomainName = randomDomainOf(organizationName);
        } else {
            organizationDomainName = formData.getFirst(ORGANIZATION_DOMAIN_FIELD);
            if (organizationDomainName == null || organizationDomainName.isBlank()) {
                final List<FormMessage> formMessages = List.of(new FormMessage(ORGANIZATION_DOMAIN_FIELD, Messages.ORGANIZATION_DOMAIN_IS_BLANK));
                context.challenge(challengeOf(context, formData, formMessages));
                return;
            }
        }

        // Create organization
        OrganizationModel createdOrganization;
        try {
            createdOrganization = organizationProvider.create(organizationName, organizationAlias);
        } catch (ModelDuplicateException e) {
            final List<FormMessage> formMessages = List.of(new FormMessage(ORGANIZATION_NAME_FIELD, Messages.ORGANIZATION_EXISTS));
            context.challenge(challengeOf(context, formData, formMessages));
            return;
        }

        // Set domain
        try {
            createdOrganization.setDomains(Set.of(new OrganizationDomainModel(organizationDomainName)));
        } catch (ModelValidationException e) {
            try {
                final List<FormMessage> formMessages = List.of(new FormMessage(ORGANIZATION_DOMAIN_FIELD, Messages.ORGANIZATION_DOMAIN_IS_INVALID));
                context.challenge(challengeOf(context, formData, formMessages));
                return;
            } finally {
                organizationProvider.remove(createdOrganization);
            }
        }

        // Set attributes
        final Map<String, String> organizationAttributeDefs = getOrganizationAttributeDefs(context.getSession());
        final Map<String, List<String>> organizationAttributes = new HashMap<>();
        for (Map.Entry<String, String> attrDef : organizationAttributeDefs.entrySet()) {
            final String attrName = attrDef.getKey();
            final String attrRuleDefSetStr = attrDef.getValue();

            final String attrFiledName = ORGANIZATION_ATTR_FIELD_PREFIX + attrName;
            final String attrFieldValue = formData.getFirst(attrFiledName);

            final AttributeCheckResult attrCheckResult = Attributes.check(attrFieldValue, attrRuleDefSetStr);
            if (!attrCheckResult.valid()) {
                final List<FormMessage> formMessages = List.of(new FormMessage(attrFiledName, Messages.ORGANIZATION_ATTRIBUTE_IS_INVALID));
                context.challenge(challengeOf(context, formData, formMessages));
                organizationProvider.remove(createdOrganization);
                return;
            }

            organizationAttributes.put(attrName, List.of(attrFieldValue));
        }
        createdOrganization.setAttributes(organizationAttributes);

        // Add the user to the newly created org
        if (isAddAsManagedEnabled(context.getSession())) {
            organizationProvider.addManagedMember(createdOrganization, user);
        } else {
            organizationProvider.addMember(createdOrganization, user);
        }

        // Append new org flag to the redirect uri
        if (isNewOrganizationQueryFlagEnabled(context.getSession())) {
            final String origRedirectUriStr = context.getAuthenticationSession().getRedirectUri();
            if (origRedirectUriStr != null && !origRedirectUriStr.isBlank()) {
                final String flagName = getNewOrganizationQueryFlagName(context.getSession());

                final String redirectUriWithNewOrgFlag = UriBuilder
                        .fromUri(origRedirectUriStr)
                        .queryParam(flagName, true)
                        .build()
                        .toString();
                context.getAuthenticationSession().setRedirectUri(redirectUriWithNewOrgFlag);
            }
        }

        // Drop this required action from the user
        user.removeRequiredAction(PROVIDER_ID);
        context.getAuthenticationSession().removeRequiredAction(PROVIDER_ID);
        context.success();
    }

    @Override
    public String getDisplayText() {
        return "Require Create & Join Organization";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new CreateOrganizationRequiredAction();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public void close() {

    }

    private Response challengeOf(
            @Nonnull RequiredActionContext context,
            @Nonnull MultivaluedMap<String, String> formData,
            @Nonnull List<FormMessage> errors
    ) {
        final LoginFormsProvider loginFormsProvider = context.form()
                .setAttribute("isDomainGenerationEnabled", isDomainGenerationEnabled(context.getSession()))
                .setAttribute("formData", formData.entrySet().stream()
                        .filter(entry -> !entry.getValue().isEmpty())
                        .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().get(0))));

        if (!errors.isEmpty()) {
            errors.forEach(loginFormsProvider::addError);
        }

        final Map<String, String> organizationAttributeDefs = getOrganizationAttributeDefs(context.getSession());
        if (!organizationAttributeDefs.isEmpty()) {
            final Map<String, Map<String, String>> organizationAttributeAttribute = new HashMap<>();

            for (Map.Entry<String, String> attrDef : organizationAttributeDefs.entrySet()) {
                final String attrName = attrDef.getKey();
                final String attrRuleDefSetStr = attrDef.getValue();


                final List<Rule> attrRules = Attributes.parseRules(attrRuleDefSetStr);

                final String attrFieldType = attrRules.stream()
                        .filter(rule -> rule instanceof TypeRule)
                        .map(rule -> switch (((TypeRule) rule).expectation()) {
                            case DOUBLE, INTEGER, FLOAT -> "number";
                            default -> "text";
                        })
                        .findAny()
                        .orElse("text");


                final String attrFieldMax = attrRules.stream()
                        .filter(rule -> rule instanceof MaxRule)
                        .map(rule -> ((MaxRule) rule).expectation().toString())
                        .findAny()
                        .orElse("");
                final String attrFieldMin = attrRules.stream()
                        .filter(rule -> rule instanceof MinRule)
                        .map(rule -> ((MinRule) rule).expectation().toString())
                        .findAny()
                        .orElse("");


                final String attrFieldMaxLength = attrRules.stream()
                        .filter(rule -> rule instanceof MaxLengthRule)
                        .map(rule -> ((MaxLengthRule) rule).expectation().toString())
                        .findAny()
                        .orElse("");
                final String attrFieldMinLength = attrRules.stream()
                        .filter(rule -> rule instanceof MinLengthRule)
                        .map(rule -> ((MinLengthRule) rule).expectation().toString())
                        .findAny()
                        .orElse("");

                final String attrFieldRequired = attrRules.stream()
                        .filter(rule -> rule instanceof RequiredRule)
                        .map(def -> Boolean.TRUE.toString())
                        .findAny()
                        .orElse("");

                organizationAttributeAttribute.put(attrName, Map.of(
                        "type", attrFieldType,
                        "required", attrFieldRequired,
                        "min", attrFieldMin,
                        "max", attrFieldMax,
                        "minLength", attrFieldMinLength,
                        "maxLength", attrFieldMaxLength
                ));
            }

            loginFormsProvider.setAttribute("attributes", organizationAttributeAttribute);
        }

        return loginFormsProvider.createForm("new_organization.ftl");
    }

    private Response challengeOf(@Nonnull RequiredActionContext context) {
        return challengeOf(context, new MultivaluedHashMap<>(), List.of());
    }

    private Optional<String> getSkippedRole(KeycloakSession session) {
        return getConfigValue(SKIP_ROLE_KEY, session)
                .filter(role -> !role.isBlank());
    }

    private boolean isDomainGenerationEnabled(KeycloakSession session) {
        return getConfigValue(GEN_DOMAIN_KEY, session)
                .map(Boolean::parseBoolean)
                .orElse(GEN_DOMAIN_KEY_DEFAULT_VALUE);
    }

    private boolean isNewOrganizationQueryFlagEnabled(KeycloakSession session) {
        return getConfigValue(REDIRECT_QUERY_FLAG_KEY, session)
                .map(Boolean::parseBoolean)
                .orElse(REDIRECT_QUERY_FLAG_KEY_DEFAULT_VALUE);
    }

    private String getNewOrganizationQueryFlagName(KeycloakSession session) {
        return getConfigValue(REDIRECT_QUERY_FLAG_NAME_KEY, session)
                .filter(name -> !name.isBlank())
                .orElse(REDIRECT_QUERY_FLAG_NAME_KEY_DEFAULT_VALUE);
    }

    private boolean isAddAsManagedEnabled(KeycloakSession session) {
        final Optional<String> addAsManagedStr = getConfigValue(ADD_AS_MANAGED_KEY, session);
        return addAsManagedStr.map(ADD_AS_MANAGED_OPT_MANAGED::equals).orElse(true);
    }

    private Map<String, String> getOrganizationAttributeDefs(KeycloakSession session) {
        return getConfigMapValue(ATTRIBUTES_KEY, session).orElse(Map.of());
    }

    private Optional<String> getConfigValue(@Nonnull String key, @Nonnull KeycloakSession session) {
        return getConfigModel(session)
                .filter(model -> model.containsConfigKey(key))
                .map(model -> model.getConfigValue(key));
    }

    private Optional<Map<String, String>> getConfigMapValue(@Nonnull String key, @Nonnull KeycloakSession session) {
        return getConfigModel(session)
                .filter(model -> model.containsConfigKey(key))
                .map(model -> model.getConfigValue(key))
                .map(MapperTypeSerializer::deserialize)
                .map(mapWithMaybeMultipleValues -> mapWithMaybeMultipleValues
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().stream().anyMatch(value -> !value.isBlank()))
                        .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().get(entry.getValue().size() - 1)))
                );
    }

    private Optional<RequiredActionConfigModel> getConfigModel(@Nonnull KeycloakSession session) {
        if (session == null) {
            return Optional.empty();
        }

        final KeycloakContext keycloakContext = session.getContext();
        final RealmModel realm = keycloakContext.getRealm();
        final RequiredActionProviderModel requiredAction = getRequiredActionByProviderId(realm, PROVIDER_ID);
        if (requiredAction == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(realm.getRequiredActionConfigByAlias(requiredAction.getAlias()));
    }
}
