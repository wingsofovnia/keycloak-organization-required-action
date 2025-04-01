package com.github.wingsofovnia.keycloak.organization;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.ValidationException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.wingsofovnia.keycloak.organization.Organizations.getInvitingOrganization;
import static com.github.wingsofovnia.keycloak.organization.Organizations.organizationAliasOf;
import static com.github.wingsofovnia.keycloak.organization.Organizations.getInvitingOrganizationOrNull;
import static com.github.wingsofovnia.keycloak.organization.Organizations.randomDomainOf;
import static org.keycloak.utils.RequiredActionHelper.getRequiredActionByProviderId;

public class CreateOrganizationRequiredAction implements RequiredActionProvider, RequiredActionFactory {

    private static final String PROVIDER_ID = "create-organization-required-action";

    public static final String CREATE_ORG_HELP_TEXT_KEY = "create_organization_help_text";

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
            .name(CREATE_ORG_HELP_TEXT_KEY)
            .label("Custom help text on the organization creation page")
            .helpText("If not set, the default localized message will be displayed")
            .type(ProviderConfigProperty.STRING_TYPE)
            .defaultValue(null)
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
        final UserModel user = context.getUser();
        final OrganizationModel maybeInvitingOrganization = getInvitingOrganizationOrNull(context.getSession());

        context.challenge(challengeOf(context, user, maybeInvitingOrganization));
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
            context.challenge(challengeOf(context, user, null, formMessages));
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
                context.challenge(challengeOf(context, user, null, formMessages));
                return;
            }
        }

        // Create organization
        OrganizationModel createdOrganization = null;
        try {
            createdOrganization = organizationProvider.create(organizationName, organizationAlias);
            createdOrganization.setDomains(Set.of(new OrganizationDomainModel(organizationDomainName)));
        } catch (ModelDuplicateException e) {
            final List<FormMessage> formMessages = List.of(new FormMessage(ORGANIZATION_NAME_FIELD, Messages.ORGANIZATION_EXISTS));
            context.challenge(challengeOf(context, user, null, formMessages));
            return;
        } catch (ModelValidationException e) {
            try {
                final List<FormMessage> formMessages = List.of(new FormMessage(ORGANIZATION_DOMAIN_FIELD, Messages.ORGANIZATION_DOMAIN_IS_INVALID));
                context.challenge(challengeOf(context, user, null, formMessages));
                return;
            } finally {
                if (createdOrganization != null) {
                    organizationProvider.remove(createdOrganization);
                }
            }
        }

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
            @Nonnull UserModel authenticatedUser,
            @Nullable OrganizationModel invitingOrganization,
            @Nonnull List<FormMessage> errors
    ) {
        final LoginFormsProvider loginFormsProvider = context.form()
                .setAttribute("user", authenticatedUser)
                .setAttribute("helpText", getCustomHelpText(context.getSession()).orElse(null))
                .setAttribute("isDomainGenerationEnabled", isDomainGenerationEnabled(context.getSession()));

        if (!errors.isEmpty()) {
            errors.forEach(loginFormsProvider::addError);
        }
        return loginFormsProvider.createForm("new_organization.ftl");
    }

    private Response challengeOf(
            @Nonnull RequiredActionContext context,
            @Nonnull UserModel authenticatedUser,
            @Nullable OrganizationModel invitingOrganization
    ) {
        return challengeOf(context, authenticatedUser, invitingOrganization, List.of());
    }

    private Optional<String> getCustomHelpText(KeycloakSession session) {
        final String helpText = getConfigValue(CREATE_ORG_HELP_TEXT_KEY, session);
        if (helpText == null || helpText.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(helpText);
    }

    private Optional<String> getSkippedRole(KeycloakSession session) {
        final String skippedRole = getConfigValue(SKIP_ROLE_KEY, session);
        if (skippedRole == null || skippedRole.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(skippedRole);
    }

    private boolean isDomainGenerationEnabled(KeycloakSession session) {
        final String genDomainStr = getConfigValue(GEN_DOMAIN_KEY, session);
        if (genDomainStr == null) {
            return GEN_DOMAIN_KEY_DEFAULT_VALUE;
        }

        return Boolean.parseBoolean(genDomainStr);
    }

    private boolean isNewOrganizationQueryFlagEnabled(KeycloakSession session) {
        final String redirectQueryFlagStr = getConfigValue(REDIRECT_QUERY_FLAG_KEY, session);
        if (redirectQueryFlagStr == null) {
            return REDIRECT_QUERY_FLAG_KEY_DEFAULT_VALUE;
        }

        return Boolean.parseBoolean(redirectQueryFlagStr);
    }

    private String getNewOrganizationQueryFlagName(KeycloakSession session) {
        final String flagName = getConfigValue(REDIRECT_QUERY_FLAG_NAME_KEY, session);
        if (flagName == null || flagName.isBlank()) {
            return REDIRECT_QUERY_FLAG_NAME_KEY_DEFAULT_VALUE;
        }
        return flagName;
    }

    private boolean isAddAsManagedEnabled(KeycloakSession session) {
        final String addAsManagedStr = getConfigValue(ADD_AS_MANAGED_KEY, session);
        if (addAsManagedStr == null) {
            return true;
        }

        return ADD_AS_MANAGED_OPT_MANAGED.equals(addAsManagedStr);
    }

    private String getConfigValue(@Nonnull String key, @Nonnull KeycloakSession session) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (session == null) {
            return null;
        }

        final KeycloakContext keycloakContext = session.getContext();
        final RealmModel realm = keycloakContext.getRealm();
        final RequiredActionProviderModel requiredAction = getRequiredActionByProviderId(realm, PROVIDER_ID);
        if (requiredAction == null) {
            return null;
        }

        final RequiredActionConfigModel configModel = realm.getRequiredActionConfigByAlias(requiredAction.getAlias());
        if (configModel == null || !configModel.containsConfigKey(key)) {
            return null;
        }

        return configModel.getConfigValue(key);
    }
}
