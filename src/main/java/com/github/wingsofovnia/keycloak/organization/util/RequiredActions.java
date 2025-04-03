package com.github.wingsofovnia.keycloak.organization.util;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.utils.MapperTypeSerializer;

import java.util.Map;
import java.util.Optional;

import static org.keycloak.utils.RequiredActionHelper.getRequiredActionByProviderId;

public final class RequiredActions {

    private RequiredActions() {
        throw new AssertionError();
    }

    public static Optional<String> requiredActionConfigValueOf(String key, String providerId, KeycloakSession session) {
        return requiredActionConfigOf(providerId, session)
                .filter(model -> model.containsConfigKey(key))
                .map(model -> model.getConfigValue(key));
    }

    public static Optional<Map<String, String>> requiredActionConfigMapValueOf(String key, String providerId, KeycloakSession session) {
        return requiredActionConfigOf(providerId, session)
                .filter(model -> model.containsConfigKey(key))
                .map(model -> model.getConfigValue(key))
                .map(MapperTypeSerializer::deserialize)
                .map(Maps::singleValueMapOf);
    }

    public static Optional<RequiredActionConfigModel> requiredActionConfigOf(String providerId, KeycloakSession session) {
        if (providerId == null || providerId.isBlank()) {
            return Optional.empty();
        }

        if (session == null) {
            return Optional.empty();
        }

        final KeycloakContext keycloakContext = session.getContext();
        final RealmModel realm = keycloakContext.getRealm();
        final RequiredActionProviderModel requiredAction = getRequiredActionByProviderId(realm, providerId);
        if (requiredAction == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(realm.getRequiredActionConfigByAlias(requiredAction.getAlias()));
    }
}
