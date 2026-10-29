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

import com.google.common.collect.ImmutableList;
import curacao.jetty11.fixtures.app.EchoController;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the application in {@code curacao.jetty11.fixtures.app} on an ephemeral port.
 */
public final class CuracaoJettyServerTest {

    private static final String CONTEXT_PATH = "/test";
    private static final String CACHE_CONTROL = "public,max-age=60";
    private static final String CUSTOMIZED_HEADER = "X-Customized";
    private static final String CONNECTOR_HEADER = "X-Connector";
    private static final String THREAD_NAME_FORMAT = "test-jetty-%d";

    @TempDir
    /* package */ static Path WEBAPP;

    private static Server SERVER;
    private static HttpClient CLIENT;

    @BeforeAll
    public static void startServer() throws Exception {
        final Path staticDirectory = Files.createDirectory(WEBAPP.resolve("static"));
        Files.writeString(staticDirectory.resolve("hello.txt"), "Hello, static", StandardCharsets.UTF_8);

        // A filter added through the context customizer.
        final Filter customized = (request, response, chain) -> {
            ((HttpServletResponse) response).setHeader(CUSTOMIZED_HEADER, "true");
            chain.doFilter(request, response);
        };

        // Static files, configured entirely by the application.
        final ServletHolder staticFiles = new ServletHolder(DefaultServlet.class);
        staticFiles.setInitParameter("dirAllowed", Boolean.FALSE.toString());
        staticFiles.setInitParameter("cacheControl", CACHE_CONTROL);

        // A servlet of the application's own, beside the static files and Curacao.
        final HttpServlet ping = new HttpServlet() {
            @Override
            protected void doGet(
                    final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException {
                response.getWriter().write("pong");
            }
        };

        SERVER = new CuracaoJettyServer.Builder(CONTEXT_PATH)
                .setThreadNameFormat(THREAD_NAME_FORMAT)
                .setPort(0)
                .setBaseResource(Resource.newResource(WEBAPP))
                .addServlet(staticFiles, "/static/*")
                .addServlet(new ServletHolder(ping), "/ping")
                .setErrorPage("/forward-errors")
                .setStopAtShutdown(false)
                .customizeContext(context -> context.addFilter(new FilterHolder(customized), "/*",
                        EnumSet.of(DispatcherType.REQUEST)))
                .build();
        SERVER.start();
        CLIENT = HttpClient.newHttpClient();
    }

    @AfterAll
    public static void stopServer() throws Exception {
        if (SERVER != null) {
            SERVER.stop();
        }
    }

    @Test
    public void routesRequestsToCuracaoTest() throws Exception {
        final HttpResponse<String> response = get("/hello");

        assertEquals(200, response.statusCode());
        assertEquals("Hello from Curacao", response.body());
    }

    @Test
    public void servesStaticFilesTest() throws Exception {
        final HttpResponse<String> response = get("/static/hello.txt");

        assertEquals(200, response.statusCode());
        assertEquals("Hello, static", response.body());
        assertEquals(CACHE_CONTROL, response.headers().firstValue("Cache-Control").orElse(null));
    }

    @Test
    public void readsARequestBodyWithinTheLimitTest() throws Exception {
        // A chunked body: no Content-Length.
        final HttpResponse<String> response = post("/echo", chunked("fits"));

        assertEquals(200, response.statusCode());
        assertEquals("fits", response.body());
    }

    @Test
    public void rejectsAChunkedBodyOverTheLimitTest() throws Exception {
        final String body = "x".repeat((int) EchoController.MAX_BODY_SIZE + 1);
        final HttpResponse<String> response = post("/echo", chunked(body));

        // Refused, rather than cut short to the limit and handed to the controller.
        assertEquals(413, response.statusCode());
    }

    @Test
    public void rejectsABodyOverTheLimitByItsContentLengthTest() throws Exception {
        final String body = "x".repeat((int) EchoController.MAX_BODY_SIZE + 1);
        final HttpResponse<String> response = post("/echo", HttpRequest.BodyPublishers.ofString(body));

        assertEquals(413, response.statusCode());
    }

    @Test
    public void rejectsAnUnsupportedCharsetTest() throws Exception {
        // A charset no JVM knows, and a malformed charset name: the client's mistake, so a 415 rather than a 500.
        for (final String charset : new String[]{"not-a-charset", "@@@"}) {
            final HttpResponse<String> response = post("/echo", "text/plain; charset=" + charset,
                    HttpRequest.BodyPublishers.ofString("hello"));

            assertEquals(415, response.statusCode(), charset);
        }
    }

    @Test
    public void theMostSpecificMatchingRouteWinsTest() throws Exception {
        // Both FilesController's catch-all and FilesExportController's literal route match; the literal wins.
        assertEquals("export", get("/files/export").body());
        // Only the catch-all matches.
        assertEquals("file:report", get("/files/report").body());
    }

    @Test
    public void equallySpecificMatchingRoutesFailTest() throws Exception {
        // Each route alone.
        assertEquals("a", get("/ambiguous/q/x").body());
        assertEquals("b", get("/ambiguous/y/z").body());
        // Both match, and neither is more specific: an error, not whichever was found first.
        assertEquals(500, get("/ambiguous/y/x").statusCode());
    }

    @Test
    public void routesToAddedServletsTest() throws Exception {
        final HttpResponse<String> response = get("/ping");

        assertEquals(200, response.statusCode());
        assertEquals("pong", response.body());
    }

    @Test
    public void servesNoStaticFilesUnlessAServletIsAddedTest() throws Exception {
        final Server server = new CuracaoJettyServer.Builder(CONTEXT_PATH)
                .setPort(0)
                .setBaseResource(Resource.newResource(WEBAPP))
                .setStopAtShutdown(false)
                .build();
        try {
            server.start();

            // Curacao answers the path instead, and has no route for it.
            final HttpResponse<String> response = get((ServerConnector) server.getConnectors()[0],
                    "/static/hello.txt");
            assertEquals(404, response.statusCode());
            assertFalse(response.body().contains("Hello, static"), response.body());
        } finally {
            server.stop();
        }
    }

    @Test
    public void forwardsErrorsToTheErrorPageTest() throws Exception {
        final HttpResponse<String> response = get("/static/missing.txt");

        assertEquals("Custom error page", response.body());
    }

    @Test
    public void forwardsARefusedDirectoryListingToTheErrorPageTest() throws Exception {
        // The static servlet, with dirAllowed off, refuses the listing; the refusal goes to the error page.
        final HttpResponse<String> response = get("/static/");

        assertFalse(response.body().contains("hello.txt"), response.body());
        assertEquals("Custom error page", response.body());
    }

    @Test
    public void hidesServerHeadersTest() throws Exception {
        final HttpResponse<String> response = get("/hello");

        assertTrue(response.headers().firstValue("Server").isEmpty());
        assertTrue(response.headers().firstValue("X-Powered-By").isEmpty());
    }

    @Test
    public void exposesTheBaseResourceTest() throws Exception {
        final HttpResponse<String> response = get("/base-resource");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains(WEBAPP.getFileName().toString()), response.body());
    }

    @Test
    public void appliesCustomizersTest() throws Exception {
        final HttpResponse<String> response = get("/hello");

        assertEquals("true", response.headers().firstValue(CUSTOMIZED_HEADER).orElse(null));
    }

    @Test
    public void namesThreadsWithTheThreadNameFormatTest() {
        final List<String> names = Thread.getAllStackTraces().keySet().stream()
                .map(Thread::getName)
                .filter(name -> name.matches("test-jetty-\\d+"))
                .collect(ImmutableList.toImmutableList());

        assertFalse(names.isEmpty(), "No thread is named by " + THREAD_NAME_FORMAT);
    }

    @Test
    public void rejectsAMalformedThreadNameFormatTest() {
        final CuracaoJettyServer.Builder builder = new CuracaoJettyServer.Builder(CONTEXT_PATH)
                .setThreadNameFormat("jetty-%z");

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void customizesTheDefaultConnectorTest() throws Exception {
        final Server server = new CuracaoJettyServer.Builder(CONTEXT_PATH)
                .setPort(0)
                .setBaseResource(Resource.newResource(WEBAPP))
                .customizeConnector(connector -> connector.setAcceptQueueSize(42))
                .build();

        assertEquals(1, server.getConnectors().length);
        final ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        // The customizer runs after the builder's own settings.
        assertEquals(CuracaoJettyServer.DEFAULT_HOST, connector.getHost());
        assertEquals(42, connector.getAcceptQueueSize());
    }

    @Test
    public void replacesTheDefaultConnectorWithAFactoryTest() throws Exception {
        final AtomicReference<ServerConnector> created = new AtomicReference<>();
        final Server server = new CuracaoJettyServer.Builder(CONTEXT_PATH)
                .setBaseResource(Resource.newResource(WEBAPP))
                .setStopAtShutdown(false)
                .setConnectorFactory(s -> {
                    final HttpConfiguration config = CuracaoJettyServer.newHttpConfiguration();
                    config.addCustomizer((connector, configuration, request) ->
                            request.getResponse().setHeader(CONNECTOR_HEADER, "custom"));
                    final ServerConnector connector = new ServerConnector(s, new HttpConnectionFactory(config));
                    connector.setHost("127.0.0.1");
                    connector.setPort(0);
                    created.set(connector);
                    return connector;
                })
                .build();
        try {
            server.start();

            assertEquals(1, server.getConnectors().length);
            assertSame(created.get(), server.getConnectors()[0]);
            final HttpResponse<String> response = get(created.get(), "/hello");
            assertEquals(200, response.statusCode());
            assertEquals("custom", response.headers().firstValue(CONNECTOR_HEADER).orElse(null));
            // Started from newHttpConfiguration(), so it hides the server headers too.
            assertTrue(response.headers().firstValue("Server").isEmpty());
        } finally {
            server.stop();
        }
    }

    @Test
    public void rejectsAConnectorCustomizerWithAConnectorFactoryTest() {
        final CuracaoJettyServer.Builder builder = new CuracaoJettyServer.Builder(CONTEXT_PATH)
                .customizeConnector(connector -> connector.setAcceptQueueSize(42))
                .setConnectorFactory(ServerConnector::new);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void rejectsBadSettingsTest() {
        assertThrows(IllegalArgumentException.class, () -> new CuracaoJettyServer.Builder("test"));
        assertThrows(IllegalArgumentException.class, () -> new CuracaoJettyServer.Builder("/").setPort(-1));
        assertThrows(IllegalArgumentException.class, () -> new CuracaoJettyServer.Builder("/").setPort(65536));
    }

    private static HttpResponse<String> post(
            final String path,
            final HttpRequest.BodyPublisher body) throws Exception {
        return post(path, "text/plain; charset=utf-8", body);
    }

    private static HttpResponse<String> post(
            final String path,
            final String contentType,
            final HttpRequest.BodyPublisher body) throws Exception {
        final int port = ((ServerConnector) SERVER.getConnectors()[0]).getLocalPort();
        final HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + CONTEXT_PATH + path))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", contentType)
                .POST(body)
                .build();

        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * A body of unknown length, which the client sends chunked, without a Content-Length.
     */
    private static HttpRequest.BodyPublisher chunked(
            final String body) {
        return HttpRequest.BodyPublishers.ofInputStream(
                () -> new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    private static HttpResponse<String> get(
            final String path) throws Exception {
        return get((ServerConnector) SERVER.getConnectors()[0], path);
    }

    private static HttpResponse<String> get(
            final ServerConnector connector,
            final String path) throws Exception {
        final int port = connector.getLocalPort();
        final HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + CONTEXT_PATH + path)).build();

        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
