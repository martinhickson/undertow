/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.undertow.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.undertow.UndertowOptions;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.testutils.DefaultServer;
import io.undertow.testutils.HttpClientUtils;
import io.undertow.testutils.TestHttpClient;
import io.undertow.util.StatusCodes;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * Sonata turns both limits off: a request body larger than the 2MB default, and a blocking
 * read that waits with no timeout. {@code 0} means unlimited for the read and idle timeouts.
 * {@code -1} means unlimited for the entity size.
 */
@RunWith(DefaultServer.class)
public class UnlimitedRequestLimitsTestCase {

    private static final int BODY_SIZE = (int) UndertowOptions.DEFAULT_MAX_ENTITY_SIZE + 1024;

    @BeforeClass
    public static void setup() {
        DefaultServer.setRootHandler(new BlockingHandler(new HttpHandler() {
            @Override
            public void handleRequest(final HttpServerExchange exchange) throws Exception {
                if (exchange.getMaxEntitySize() > 0) {
                    throw new IllegalStateException("entity size is limited to " + exchange.getMaxEntitySize());
                }
                final InputStream inputStream = exchange.getInputStream();
                final byte[] buffer = new byte[8192];
                int total = 0;
                int read;
                while ((read = inputStream.read(buffer)) > 0) {
                    for (int i = 0; i < read; ++i) {
                        if (buffer[i] != '+') {
                            throw new IllegalStateException("unexpected body byte");
                        }
                    }
                    total += read;
                }
                final OutputStream outputStream = exchange.getOutputStream();
                outputStream.write(Integer.toString(total).getBytes("UTF-8"));
                outputStream.close();
            }
        }));
    }

    @DefaultServer.BeforeServerStarts
    public static void unlimitedReadTimeout() {
        DefaultServer.setServerOptions(OptionMap.create(Options.READ_TIMEOUT, 0));
    }

    @DefaultServer.AfterServerStops
    public static void restoreServerOptions() {
        DefaultServer.setServerOptions(OptionMap.EMPTY);
    }

    @Test
    public void testUnlimitedBodyAndReadTimeout() throws IOException {
        final OptionMap existing = DefaultServer.getUndertowOptions();
        final TestHttpClient client = new TestHttpClient();
        try {
            DefaultServer.setUndertowOptions(OptionMap.builder()
                    .addAll(existing)
                    .set(UndertowOptions.MAX_ENTITY_SIZE, -1L)
                    .set(UndertowOptions.MULTIPART_MAX_ENTITY_SIZE, -1L)
                    .set(UndertowOptions.IDLE_TIMEOUT, 0)
                    .getMap());

            final HttpPost post = new HttpPost(DefaultServer.getDefaultServerURL() + "/path");
            post.setEntity(new AbstractHttpEntity() {
                @Override
                public boolean isRepeatable() {
                    return true;
                }

                @Override
                public long getContentLength() {
                    return BODY_SIZE;
                }

                @Override
                public InputStream getContent() {
                    return null;
                }

                @Override
                public void writeTo(final OutputStream outstream) throws IOException {
                    outstream.write('+');
                    outstream.flush();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException(e);
                    }
                    final byte[] rest = new byte[BODY_SIZE - 1];
                    for (int i = 0; i < rest.length; ++i) {
                        rest[i] = '+';
                    }
                    outstream.write(rest);
                }

                @Override
                public boolean isStreaming() {
                    return false;
                }
            });
            final HttpResponse result = client.execute(post);
            Assert.assertEquals(StatusCodes.OK, result.getStatusLine().getStatusCode());
            Assert.assertEquals(Integer.toString(BODY_SIZE), HttpClientUtils.readResponse(result));
        } finally {
            DefaultServer.setUndertowOptions(existing);
            client.getConnectionManager().shutdown();
        }
    }
}
