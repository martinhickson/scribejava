package com.github.scribejava.httpclient.apache;

import com.github.scribejava.core.httpclient.AbstractAsyncOnlyHttpClient;
import com.github.scribejava.core.httpclient.multipart.MultipartPayload;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

public class ApacheHttpClient extends AbstractAsyncOnlyHttpClient {

    private final CloseableHttpAsyncClient client;
    private final Set<OAuthAsyncCompletionHandler<?>> inFlight
            = Collections.synchronizedSet(new HashSet<OAuthAsyncCompletionHandler<?>>());

    public ApacheHttpClient() {
        this(ApacheHttpClientConfig.defaultConfig());
    }

    public ApacheHttpClient(ApacheHttpClientConfig config) {
        this(config.getHttpAsyncClientBuilder());
    }

    public ApacheHttpClient(HttpAsyncClientBuilder builder) {
        this(builder.build());
    }

    public ApacheHttpClient(CloseableHttpAsyncClient client) {
        this.client = client;
        this.client.start();
        // httpasyncclient 4.1 leaves exchanges in requestQueue when the I/O reactor
        // thread dies (issue 1065). Nothing completes their callbacks, so Future.get()
        // waits until the process is restarted. Fail those exchanges when the client
        // stops running. A stopped client cannot be started again.
        watchReactor();
    }

    private void watchReactor() {
        final Thread watcher = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (client.isRunning()) {
                        Thread.sleep(50L);
                    }
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
                failInFlight(new IOException("I/O reactor terminated"));
            }
        }, "scribejava-apache-reactor-watch");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void failInFlight(final IOException cause) {
        final List<OAuthAsyncCompletionHandler<?>> pending;
        synchronized (inFlight) {
            pending = new ArrayList<OAuthAsyncCompletionHandler<?>>(inFlight);
            inFlight.clear();
        }
        for (OAuthAsyncCompletionHandler<?> handler : pending) {
            handler.failed(cause);
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    @Override
    public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
            byte[] bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {
        final HttpEntity entity = bodyContents == null ? null : new ByteArrayEntity(bodyContents);
        return doExecuteAsync(userAgent, headers, httpVerb, completeUrl, entity, callback, converter);
    }

    @Override
    public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
            MultipartPayload bodyContents, OAuthAsyncRequestCallback<T> callback,
            OAuthRequest.ResponseConverter<T> converter) {

        throw new UnsupportedOperationException("ApacheHttpClient does not support MultipartPayload yet.");
    }

    @Override
    public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
            String bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {
        final HttpEntity entity = bodyContents == null ? null : new StringEntity(bodyContents, StandardCharsets.UTF_8);
        return doExecuteAsync(userAgent, headers, httpVerb, completeUrl, entity, callback, converter);
    }

    @Override
    public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl,
            File bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {
        final HttpEntity entity = bodyContents == null ? null : new FileEntity(bodyContents);
        return doExecuteAsync(userAgent, headers, httpVerb, completeUrl, entity, callback, converter);
    }

    private <T> Future<T> doExecuteAsync(String userAgent, Map<String, String> headers, Verb httpVerb,
            String completeUrl, HttpEntity entity, OAuthAsyncRequestCallback<T> callback,
            OAuthRequest.ResponseConverter<T> converter) {
        final RequestBuilder builder = getRequestBuilder(httpVerb);
        builder.setUri(completeUrl);

        if (entity != null && httpVerb.isPermitBody()) {
            if (!headers.containsKey(CONTENT_TYPE)) {
                builder.addHeader(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
            }
            builder.setEntity(entity);
        }

        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.addHeader(header.getKey(), header.getValue());
        }

        if (userAgent != null) {
            builder.setHeader(OAuthConstants.USER_AGENT_HEADER_NAME, userAgent);
        }
        final OAuthAsyncCompletionHandler<T> handler = new OAuthAsyncCompletionHandler<T>(callback, converter) {
            @Override
            public void completed(final HttpResponse httpResponse) {
                inFlight.remove(this);
                super.completed(httpResponse);
            }

            @Override
            public void failed(final Exception ex) {
                inFlight.remove(this);
                super.failed(ex);
            }

            @Override
            public void cancelled() {
                inFlight.remove(this);
                super.cancelled();
            }
        };
        inFlight.add(handler);
        try {
            final Future<HttpResponse> future = client.execute(builder.build(), handler);
            return new ApacheHttpFuture<T>(future, handler);
        } catch (final RuntimeException ex) {
            inFlight.remove(handler);
            handler.failed(ex);
            throw ex;
        }
    }

    private static RequestBuilder getRequestBuilder(Verb httpVerb) {
        switch (httpVerb) {
            case GET:
                return RequestBuilder.get();
            case PUT:
                return RequestBuilder.put();
            case DELETE:
                return RequestBuilder.delete();
            case HEAD:
                return RequestBuilder.head();
            case POST:
                return RequestBuilder.post();
            case PATCH:
                return RequestBuilder.patch();
            case TRACE:
                return RequestBuilder.trace();
            case OPTIONS:
                return RequestBuilder.options();
            default:
                throw new IllegalArgumentException("message build error: unknown verb type");
        }
    }
}
