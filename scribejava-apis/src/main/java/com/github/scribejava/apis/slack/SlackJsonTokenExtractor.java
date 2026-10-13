package com.github.scribejava.apis.slack;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthConstants;
import java.io.IOException;

public class SlackJsonTokenExtractor extends OAuth2AccessTokenJsonExtractor {

    protected SlackJsonTokenExtractor() {
    }

    private static class InstanceHolder {

        private static final SlackJsonTokenExtractor INSTANCE = new SlackJsonTokenExtractor();
    }

    public static SlackJsonTokenExtractor instance() {
        return SlackJsonTokenExtractor.InstanceHolder.INSTANCE;
    }

    /**
     * Slack v2 user-scoped responses omit the bot {@code access_token} and put the token on
     * {@code authed_user.access_token} (issue 1023).
     */
    @Override
    protected OAuth2AccessToken createToken(String rawResponse) throws IOException {
        final JsonNode response = OBJECT_MAPPER.readTree(rawResponse);
        final JsonNode authedUser = response.get("authed_user");
        final JsonNode botTokenNode = response.get(OAuthConstants.ACCESS_TOKEN);
        final JsonNode userTokenNode = authedUser == null ? null : authedUser.get(OAuthConstants.ACCESS_TOKEN);
        final boolean hasBotToken = hasText(botTokenNode);
        final boolean hasUserToken = hasText(userTokenNode);
        if (!hasBotToken && !hasUserToken) {
            throw new OAuthException("Response body is incorrect. Can't extract a '" + OAuthConstants.ACCESS_TOKEN
                    + "' from this: '" + rawResponse + "'", null);
        }

        JsonNode tokenType = response.get("token_type");
        JsonNode scope = response.get(OAuthConstants.SCOPE);
        if (!hasBotToken) {
            if (tokenType == null) {
                tokenType = authedUser.get("token_type");
            }
            if (scope == null) {
                scope = authedUser.get(OAuthConstants.SCOPE);
            }
        }

        final JsonNode expiresInNode = response.get("expires_in");
        final JsonNode refreshToken = response.get(OAuthConstants.REFRESH_TOKEN);
        return createToken(hasBotToken ? botTokenNode.asText() : userTokenNode.asText(),
                textOrNull(tokenType),
                expiresInNode == null || expiresInNode.isNull() ? null : expiresInNode.asInt(),
                textOrNull(refreshToken), textOrNull(scope), response, rawResponse);
    }

    @Override
    protected SlackOAuth2AccessToken createToken(String accessToken, String tokenType, Integer expiresIn,
            String refreshToken, String scope, JsonNode response, String rawResponse) {
        final JsonNode authedUser = response.get("authed_user");
        final JsonNode userAccessTokenNode = authedUser == null ? null : authedUser.get(OAuthConstants.ACCESS_TOKEN);
        final String userAccessToken = hasText(userAccessTokenNode) ? userAccessTokenNode.asText() : "";
        return new SlackOAuth2AccessToken(accessToken, tokenType, expiresIn, refreshToken, scope, userAccessToken,
                rawResponse);
    }

    private static boolean hasText(JsonNode node) {
        return node != null && !node.isNull() && node.asText().length() > 0;
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
