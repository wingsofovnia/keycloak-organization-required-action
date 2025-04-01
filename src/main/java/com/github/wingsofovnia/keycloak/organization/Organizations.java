package com.github.wingsofovnia.keycloak.organization;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.authentication.actiontoken.inviteorg.InviteOrgActionToken;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.OrganizationProvider;

import java.util.Optional;
import java.util.UUID;

public class Organizations {

    private Organizations() {
        throw new AssertionError();
    }

    @Nonnull
    public static String organizationAliasOf(@Nonnull String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Organization name cannot be null or blank");
        }

        return safeNameOf(name, "_");
    }

    @Nonnull
    public static String randomDomainOf(@Nonnull String name) {
        if (name == null || name.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return UUID.randomUUID() + "-" + safeNameOf(name, "-");
    }

    @Nonnull
    private static String safeNameOf(@Nonnull String name, @Nonnull String delimiter) {
        if (name == null || name.isBlank()) {
            return "";
        }

        return name
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9-_\\s]", "")
                .replaceAll("[\\s-]+", delimiter);
    }

    @Nonnull
    public static Optional<OrganizationModel> getInvitingOrganization(@Nonnull KeycloakSession session) {
        if (session == null) {
            return Optional.empty();
        }

        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return Optional.empty();
        }

        final InviteOrgActionToken token = (InviteOrgActionToken) session
                .getAttribute(InviteOrgActionToken.class.getName());
        if (token == null) {
            return Optional.empty();
        }

        final OrganizationProvider organizationProvider = session.getProvider(OrganizationProvider.class);
        return Optional.ofNullable(organizationProvider.getById(token.getOrgId()));
    }

    @Nullable
    public static OrganizationModel getInvitingOrganizationOrNull(@Nonnull KeycloakSession session) {
        if (session == null) {
            return null;
        }

        return getInvitingOrganization(session).orElse(null);
    }
}
