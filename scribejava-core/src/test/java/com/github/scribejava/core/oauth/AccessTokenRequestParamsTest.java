package com.github.scribejava.core.oauth;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Issue 1082: the first {@code addExtraParameters} call NPE'd because the new map was
 * stored in the method parameter instead of {@code this.extraParameters}.
 */
public class AccessTokenRequestParamsTest {

    @Test
    public void addExtraParametersInitializesTheField() {
        final Map<String, String> extras = new HashMap<String, String>();
        extras.put("audience", "api");

        final AccessTokenRequestParams params = AccessTokenRequestParams.create("code").addExtraParameters(extras);

        assertEquals("api", params.getExtraParameters().get("audience"));
    }

    @Test
    public void addExtraParametersAppendsToAnExistingMap() {
        final AccessTokenRequestParams params = AccessTokenRequestParams.create("code")
                .addExtraParameter("audience", "api");
        final Map<String, String> more = new HashMap<String, String>();
        more.put("resource", "https://example");

        params.addExtraParameters(more);

        assertEquals("api", params.getExtraParameters().get("audience"));
        assertEquals("https://example", params.getExtraParameters().get("resource"));
    }

    @Test
    public void addExtraParametersIgnoresNullAndEmpty() {
        final AccessTokenRequestParams params = AccessTokenRequestParams.create("code");

        assertSame(params, params.addExtraParameters(null));
        assertSame(params, params.addExtraParameters(new HashMap<String, String>()));
        assertNull(params.getExtraParameters());
    }
}
