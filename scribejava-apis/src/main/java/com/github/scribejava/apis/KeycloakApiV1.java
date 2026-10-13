package com.github.scribejava.apis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Keycloak before 17, when the realm URLs still included the {@code /auth} prefix.
 * {@link KeycloakApi} is the current API and omits that prefix.
 */
public class KeycloakApiV1 extends KeycloakApi {

    private static final ConcurrentMap<String, KeycloakApiV1> INSTANCES = new ConcurrentHashMap<>();

    protected KeycloakApiV1(String baseUrlWithRealm) {
        super(baseUrlWithRealm);
    }

    public static KeycloakApiV1 instance() {
        return instance("http://localhost:8080/", "master");
    }

    public static KeycloakApiV1 instance(String baseUrl, String realm) {
        final String baseUrlWithRealm = composeBaseUrlWithRealm(baseUrl, realm);

        //java8: switch to ConcurrentMap::computeIfAbsent
        KeycloakApiV1 api = INSTANCES.get(baseUrlWithRealm);
        if (api == null) {
            api = new KeycloakApiV1(baseUrlWithRealm);
            final KeycloakApiV1 alreadyCreatedApi = INSTANCES.putIfAbsent(baseUrlWithRealm, api);
            if (alreadyCreatedApi != null) {
                return alreadyCreatedApi;
            }
        }
        return api;
    }

    protected static String composeBaseUrlWithRealm(String baseUrl, String realm) {
        return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "auth/realms/" + realm;
    }
}
