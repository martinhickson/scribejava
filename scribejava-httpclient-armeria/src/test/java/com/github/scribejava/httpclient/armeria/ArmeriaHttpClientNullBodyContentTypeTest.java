package com.github.scribejava.httpclient.armeria;

import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.model.Verb;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ArmeriaHttpClientNullBodyContentTypeTest {

    @Test
    public void deleteWithNullStringBodyOmitsDefaultContentType() throws Exception {
        assertNoDefaultContentType(new Request() {
            @Override
            public void send(ArmeriaHttpClient client, String url) throws Exception {
                client.execute(null, Collections.<String, String>emptyMap(), Verb.DELETE, url, (String) null).close();
            }
        });
    }

    @Test
    public void deleteWithNullByteBodyOmitsDefaultContentType() throws Exception {
        assertNoDefaultContentType(new Request() {
            @Override
            public void send(ArmeriaHttpClient client, String url) throws Exception {
                client.execute(null, Collections.<String, String>emptyMap(), Verb.DELETE, url, (byte[]) null).close();
            }
        });
    }

    @Test
    public void deleteWithNullBodyKeepsExplicitContentType() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse());
        server.start();
        final ArmeriaHttpClient client = new ArmeriaHttpClient();
        try {
            final Map<String, String> headers = new HashMap<String, String>();
            headers.put(HttpClient.CONTENT_TYPE, "application/json");
            client.execute(null, headers, Verb.DELETE, server.url("/empty").toString(), (String) null).close();
            assertEquals("application/json", server.takeRequest().getHeader(HttpClient.CONTENT_TYPE));
        } finally {
            client.close();
            server.shutdown();
        }
    }

    @Test
    public void postWithStringBodyKeepsDefaultContentType() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse());
        server.start();
        final ArmeriaHttpClient client = new ArmeriaHttpClient();
        try {
            client.execute(null, Collections.<String, String>emptyMap(), Verb.POST, server.url("/body").toString(),
                    "grant_type=client_credentials").close();
            final RecordedRequest recorded = server.takeRequest();
            assertEquals(HttpClient.DEFAULT_CONTENT_TYPE, recorded.getHeader(HttpClient.CONTENT_TYPE));
            assertEquals("grant_type=client_credentials", recorded.getBody().readUtf8());
        } finally {
            client.close();
            server.shutdown();
        }
    }

    private static void assertNoDefaultContentType(Request request) throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse());
        server.start();
        final ArmeriaHttpClient client = new ArmeriaHttpClient();
        try {
            request.send(client, server.url("/empty").toString());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("DELETE", recorded.getMethod());
            assertNull(recorded.getHeader(HttpClient.CONTENT_TYPE));
        } finally {
            client.close();
            server.shutdown();
        }
    }

    private interface Request {

        void send(ArmeriaHttpClient client, String url) throws Exception;
    }
}
