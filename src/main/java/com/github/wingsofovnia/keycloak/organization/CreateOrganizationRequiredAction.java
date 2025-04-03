package com.github.wingsofovnia.keycloak.organization;

import com.github.wingsofovnia.keycloak.organization.attribute.AttributeCheckResult;
import com.github.wingsofovnia.keycloak.organization.attribute.Attributes;
import com.github.wingsofovnia.keycloak.organization.attribute.rule.Rule;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionConfig.PROVIDER_PROPERTIES;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionConfig.getNewOrganizationQueryFlagName;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionConfig.getOrganizationAttributeRules;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionConfig.getSkippedRole;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionConfig.isAddAsManagedEnabled;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionConfig.isDomainGenerationEnabled;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionConfig.isNewOrganizationQueryFlagEnabled;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionForm.createBlankOrganizationDomainError;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionForm.createBlankOrganizationNameError;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionForm.createDuplicateOrganizationNameError;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionForm.createForm;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionForm.createInvalidOrganizationAttributesError;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionForm.createInvalidOrganizationDomainError;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionForm.getOrganizationAttributes;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionForm.getOrganizationDomain;
import static com.github.wingsofovnia.keycloak.organization.CreateOrganizationRequiredActionForm.getOrganizationName;
import static com.github.wingsofovnia.keycloak.organization.util.Maps.multivaluedMapOf;
import static com.github.wingsofovnia.keycloak.organization.util.Organizations.getInvitingOrganization;
import static com.github.wingsofovnia.keycloak.organization.util.Organizations.organizationAliasOf;
import static com.github.wingsofovnia.keycloak.organization.util.Organizations.randomDomainOf;

public class CreateOrganizationRequiredAction implements RequiredActionProvider, RequiredActionFactory {

    public static final String PROVIDER_ID = "create-organization-required-action";

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        final UserModel authenticatedUser = context.getUser();
        if (authenticatedUser == null) {
            return;
        }

        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            authenticatedUser.removeRequiredAction(PROVIDER_ID);
            return;
        }

        final OrganizationProvider organizationProvider = context.getSession()
                .getProvider(OrganizationProvider.class);

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
        context.challenge(createForm(context));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        final UserModel user = context.getUser();
        final MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        final OrganizationProvider organizationProvider = context.getSession().getProvider(OrganizationProvider.class);

        // Read form values
        final Optional<String> maybeOrganizationName = getOrganizationName(formData);
        if (maybeOrganizationName.isEmpty()) {
            context.challenge(createForm(context, formData, createBlankOrganizationNameError()));
            return;
        }
        final String organizationName = maybeOrganizationName.get();
        final String organizationAlias = organizationAliasOf(organizationName);

        final String organizationDomainName;
        if (isDomainGenerationEnabled(context.getSession())) {
            organizationDomainName = randomDomainOf(organizationName);
        } else {
            final Optional<String> maybeOrganizationDomainName = getOrganizationDomain(formData);
            if (maybeOrganizationDomainName.isEmpty()) {
                context.challenge(createForm(context, formData, createBlankOrganizationDomainError()));
                return;
            }
            organizationDomainName = maybeOrganizationDomainName.get();
        }

        final Map<String, String> organizationAttributes = getOrganizationAttributes(formData);
        final Map<String, AttributeCheckResult> attrFailedCheckResults = new HashMap<>();
        for (Map.Entry<String, String> attr : organizationAttributes.entrySet()) {
            final String attrName = attr.getKey();
            final String attrValue = attr.getValue();

            final Optional<List<Rule>> maybeAttrRules = getOrganizationAttributeRules(attrName, context.getSession());
            if (maybeAttrRules.isEmpty()) {
                context.challenge(createForm(context, formData, createInvalidOrganizationAttributesError()));
                return;
            }

            final AttributeCheckResult attributeCheckResult = Attributes.check(attrValue, maybeAttrRules.get());
            if (!attributeCheckResult.isValid()) {
                attrFailedCheckResults.put(attrName, attributeCheckResult);
            }
        }
        if (!attrFailedCheckResults.isEmpty()) {
            final List<FormMessage> formMessages = createInvalidOrganizationAttributesError(attrFailedCheckResults);
            context.challenge(createForm(context, formData, formMessages));
            return;
        }

        // Create organization
        OrganizationModel createdOrganization;
        try {
            createdOrganization = organizationProvider.create(organizationName, organizationAlias);
        } catch (ModelDuplicateException e) {
            context.challenge(createForm(context, formData, createDuplicateOrganizationNameError()));
            return;
        }

        // Set domain
        try {
            createdOrganization.setDomains(Set.of(new OrganizationDomainModel(organizationDomainName)));
        } catch (ModelValidationException e) {
            try {
                context.challenge(createForm(context, formData, createInvalidOrganizationDomainError()));
                return;
            } finally {
                organizationProvider.remove(createdOrganization);
            }
        }

        // Set attributes
        try {
            createdOrganization.setAttributes(multivaluedMapOf(organizationAttributes));
        } catch (ModelValidationException e) {
            try {
                context.challenge(createForm(context, formData, createInvalidOrganizationAttributesError()));
                return;
            } finally {
                organizationProvider.remove(createdOrganization);
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
        return PROVIDER_PROPERTIES;
    }

    @Override
    public void close() {

    }
}
