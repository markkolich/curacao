/*
 * Copyright (c) 2026 Mark S. Kolich
 * https://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package curacao.jetty11;

import com.typesafe.config.ConfigFactory;
import curacao.exceptions.reflection.ComponentConfigurationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the application in {@code curacao.jetty11.fixtures.broken}, which Curacao can't wire: a controller asks
 * for one {@code Animal} and two components are animals.
 */
public final class CuracaoJettyServerFailedBootTest {

    private static final String CONTEXT_PATH = "/broken";

    @TempDir
    /* package */ static Path WEBAPP;

    @BeforeAll
    public static void bootTheBrokenApplication() {
        // Surefire runs each test class in a fresh JVM, so Curacao hasn't read its configuration yet.
        System.setProperty("curacao.boot-package", "curacao.jetty11.fixtures.broken");
        ConfigFactory.invalidateCaches();
    }

    @Test
    public void answersEveryRequestWithA503Test() throws Exception {
        final Server server = builder().build();
        try {
            server.start();

            final int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            final HttpClient client = HttpClient.newHttpClient();
            for (final String path : new String[]{"/animal", "/static/anything.txt", "/"}) {
                final HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + CONTEXT_PATH + path)).build();
                final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(503, response.statusCode(), path);
            }
        } finally {
            server.stop();
        }
    }

    @Test
    public void failOnStartupErrorMakesStartThrowTest() throws Exception {
        final Server server = builder().setFailOnStartupError(true).build();
        try {
            final Exception e = assertThrows(Exception.class, server::start);
            assertTrue(ExceptionUtils.indexOfType(e, ComponentConfigurationException.class) >= 0,
                    ExceptionUtils.getStackTrace(e));
        } finally {
            server.stop();
        }
    }

    private static CuracaoJettyServer.Builder builder() throws Exception {
        return new CuracaoJettyServer.Builder(CONTEXT_PATH)
                .setPort(0)
                .setBaseResource(Resource.newResource(WEBAPP))
                .setStopAtShutdown(false);
    }

}
