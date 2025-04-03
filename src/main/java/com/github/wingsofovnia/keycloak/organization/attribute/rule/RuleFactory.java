package com.github.wingsofovnia.keycloak.organization.attribute.rule;

public interface RuleFactory<T extends Rule> {

    String ruleName();

    T create(Object... params) throws RuleDefException;
}
