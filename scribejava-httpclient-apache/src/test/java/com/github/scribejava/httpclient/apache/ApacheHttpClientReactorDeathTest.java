package com.github.scribejava.httpclient.apache;

import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Issue 1065: when the Apache async I/O reactor dies, requests already accepted
 * stay in the client queue and {@code Future.get()} parks on the completion latch
 * until the JVM is restarted.
 */
public class ApacheHttpClientReactorDeathTest {

    @Test
    public void outstandingRequestFailsWhenIoReactorStops() throws Exception {
        final ReactorDiesClient raw = new ReactorDiesClient();
        final ApacheHttpClient http = new ApacheHttpClient(raw);
        final Future<Response> future = http.executeAsync(null, Collections.<String, String>emptyMap(),
                Verb.GET, "http://example.com/stuck", (byte[]) null, null, null);
        assertNotNull(raw.parked);
        raw.killReactor();
        try {
            future.get(2, TimeUnit.SECONDS);
            fail("request completed after the I/O reactor stopped");
        } catch (final TimeoutException e) {
            fail("request stayed queued after the I/O reactor stopped");
        } catch (final ExecutionException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertTrue(e.getCause().getMessage().contains("I/O reactor"));
        } finally {
            http.close();
        }
    }

    /**
     * Stands in for {@code InternalHttpAsyncClient} after {@code "I/O reactor terminated
     * abnormally"}. {@code execute} accepts the exchange and never calls the callback,
     * which is the stuck {@code requestQueue} state from issue 1065.
     */
    private static final class ReactorDiesClient extends CloseableHttpAsyncClient {

        private volatile boolean running = true;
        private FutureCallback<?> parked;

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public void start() {
            running = true;
        }

        @Override
        public void close() throws IOException {
            running = false;
        }

        void killReactor() {
            running = false;
        }

        @Override
        public <T> Future<T> execute(final HttpAsyncRequestProducer requestProducer,
                final HttpAsyncResponseConsumer<T> responseConsumer, final HttpContext context,
                final FutureCallback<T> callback) {
            parked = callback;
            try {
                requestProducer.close();
            } catch (final IOException ex) {
                // the reactor is gone; nothing will produce this request
            }
            return new BasicFuture<T>(callback);
        }

        @Override
        public Future<HttpResponse> execute(final HttpHost target, final HttpRequest request,
                final HttpContext context, final FutureCallback<HttpResponse> callback) {
            parked = callback;
            return new BasicFuture<HttpResponse>(callback);
        }
    }
}
