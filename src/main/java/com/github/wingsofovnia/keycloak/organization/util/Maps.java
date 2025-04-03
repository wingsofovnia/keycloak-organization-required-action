package com.github.wingsofovnia.keycloak.organization.util;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Maps {

    private Maps() {
        throw new AssertionError();
    }

    /**
     * Converts a map with list values into a map with single values by taking the last element of each list.
     * <p>
     * If a list is {@code null} or empty, the resulting map will contain {@code null} as the value for that key.
     */
    public static <K, V> Map<K, V> singleValueMapOf(Map<K, List<V>> mapWithMultipleValues) {
        if (mapWithMultipleValues == null || mapWithMultipleValues.isEmpty()) {
            return Map.of();
        }

        final Map<K, V> result = new HashMap<>();
        mapWithMultipleValues.forEach((key, list) -> {
            if (list == null || list.isEmpty()) {
                result.put(key, null);
            } else {
                result.put(key, list.get(list.size() - 1));
            }
        });

        return result;
    }

    public static <K, V> MultivaluedMap<K, V> multivaluedMapOf(Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return new MultivaluedHashMap<>();
        }

        final MultivaluedMap<K, V> result = new MultivaluedHashMap<>();
        map.forEach(result::add);

        return result;
    }
}
