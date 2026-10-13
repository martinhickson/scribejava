package com.github.scribejava.apis.slack;

import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.Response;
import java.io.IOException;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SlackJsonTokenExtractorTest {

    private final SlackJsonTokenExtractor extractor = SlackJsonTokenExtractor.instance();

    @Test
    public void extractsUserTokenWhenBotTokenIsAbsent() throws IOException {
        final String body = "{\"ok\":true,\"authed_user\":{\"id\":\"U1234\",\"scope\":\"chat:write\","
                + "\"access_token\":\"xoxp-user\",\"token_type\":\"user\"}}";
        final SlackOAuth2AccessToken token = (SlackOAuth2AccessToken) extractor.extract(ok(body));
        assertEquals("xoxp-user", token.getAccessToken());
        assertEquals("xoxp-user", token.getUserAccessToken());
        assertEquals("user", token.getTokenType());
        assertEquals("chat:write", token.getScope());
    }

    @Test
    public void keepsBotAndUserTokensWhenBothArePresent() throws IOException {
        final String body = "{\"ok\":true,\"access_token\":\"xoxb-bot\",\"token_type\":\"bot\","
                + "\"scope\":\"commands\",\"authed_user\":{\"id\":\"U1234\",\"scope\":\"chat:write\","
                + "\"access_token\":\"xoxp-user\",\"token_type\":\"user\"}}";
        final SlackOAuth2AccessToken token = (SlackOAuth2AccessToken) extractor.extract(ok(body));
        assertEquals("xoxb-bot", token.getAccessToken());
        assertEquals("xoxp-user", token.getUserAccessToken());
        assertEquals("bot", token.getTokenType());
        assertEquals("commands", token.getScope());
    }

    @Test
    public void allowsBotTokenWithoutAuthedUser() throws IOException {
        final String body = "{\"ok\":true,\"access_token\":\"xoxb-bot\",\"token_type\":\"bot\"}";
        final SlackOAuth2AccessToken token = (SlackOAuth2AccessToken) extractor.extract(ok(body));
        assertEquals("xoxb-bot", token.getAccessToken());
        assertEquals("", token.getUserAccessToken());
    }

    @Test
    public void rejectsResponseWithNoToken() throws IOException {
        final Response response = ok("{\"ok\":true}");
        assertThrows(OAuthException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                extractor.extract(response);
            }
        });
    }

    private static Response ok(String body) {
        return new Response(200, null, null, body);
    }
}
