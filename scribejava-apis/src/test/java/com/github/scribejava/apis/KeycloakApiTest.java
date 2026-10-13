package com.github.scribejava.apis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class KeycloakApiTest {

    @Test
    public void currentApiOmitsLegacyAuthPrefix() {
        final KeycloakApi api = KeycloakApi.instance("https://login.example.com", "myrealm");
        assertEquals("https://login.example.com/realms/myrealm/protocol/openid-connect/token",
                api.getAccessTokenEndpoint());
        assertEquals("https://login.example.com/realms/myrealm/protocol/openid-connect/auth",
                api.getAuthorizationBaseUrl());
        assertSame(api, KeycloakApi.instance("https://login.example.com", "myrealm"));
    }

    @Test
    public void v1KeepsLegacyAuthPrefix() {
        final KeycloakApiV1 api = KeycloakApiV1.instance("https://login.example.com/", "myrealm");
        assertEquals("https://login.example.com/auth/realms/myrealm/protocol/openid-connect/token",
                api.getAccessTokenEndpoint());
        assertEquals("https://login.example.com/auth/realms/myrealm/protocol/openid-connect/auth",
                api.getAuthorizationBaseUrl());
        assertSame(api, KeycloakApiV1.instance("https://login.example.com/", "myrealm"));
    }
}
