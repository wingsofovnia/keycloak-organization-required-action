package com.github.wingsofovnia.keycloak.organization;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.wingsofovnia.keycloak.organization.Organizations.organizationAliasOf;
import static com.github.wingsofovnia.keycloak.organization.Organizations.randomDomainOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class OrganizationsTest {

    @Test
    void randomDomainOfTest() {
        assertThat(randomDomainOf("My Organization")).endsWith("-my-organization");
        assertThat(randomDomainOf("  My   Organization ")).endsWith("-my-organization");
        assertThat(randomDomainOf("Org @ 123!")).endsWith("-org-123");
        assertThat(randomDomainOf("MyOrg2023")).endsWith("-myorg2023");

        assertThatCode(() -> UUID.fromString(randomDomainOf(null))).doesNotThrowAnyException();
        assertThatCode(() -> UUID.fromString(randomDomainOf(""))).doesNotThrowAnyException();
        assertThatCode(() -> UUID.fromString(randomDomainOf("   "))).doesNotThrowAnyException();
    }

    @Test
    void organizationAliasOfTest() {
        assertThat(organizationAliasOf("My Organization")).isEqualTo("my_organization");
        assertThat(organizationAliasOf("  My   Organization ")).isEqualTo("my_organization");
        assertThat(organizationAliasOf("Org @ 123!")).isEqualTo("org_123");
        assertThat(organizationAliasOf("MyOrg2023")).isEqualTo("myorg2023");
        assertThat(organizationAliasOf("SimilarAlias")).isEqualTo("similaralias");
        assertThat(organizationAliasOf("CaseSensitiveTEST")).isEqualTo("casesensitivetest");
        assertThat(organizationAliasOf("Special---Characters")).isEqualTo("special_characters");

        assertThatThrownBy(() -> organizationAliasOf(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> organizationAliasOf(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> organizationAliasOf("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
