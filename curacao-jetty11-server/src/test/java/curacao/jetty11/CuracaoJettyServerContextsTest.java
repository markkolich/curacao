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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Two applications in one JVM: each context builds its own controllers, wired to its own components.
 */
public final class CuracaoJettyServerContextsTest {

    @TempDir
    /* package */ static Path WEBAPP;

    @Test
    public void eachContextBuildsItsOwnControllersTest() throws Exception {
        final Server first = server();
        final Server second = server();
        try {
            first.start();
            second.start();

            final String fromFirst = instanceNumber(first);
            final String fromSecond = instanceNumber(second);
            // Each controller asks its own context for the component.
            assertNotEquals(fromFirst, fromSecond);
            // And keeps using it.
            assertEquals(fromFirst, instanceNumber(first));
        } finally {
            second.stop();
            first.stop();
        }
    }

    @Test
    public void restartedContextBuildsNewControllersTest() throws Exception {
        final Server first = server();
        final String beforeRestart;
        try {
            first.start();
            beforeRestart = instanceNumber(first);
        } finally {
            first.stop();
        }

        // The first context's components are destroyed; a new context must not reuse controllers wired to them.
        final Server second = server();
        try {
            second.start();
            assertNotEquals(beforeRestart, instanceNumber(second));
        } finally {
            second.stop();
        }
    }

    private static Server server() throws Exception {
        return new CuracaoJettyServer.Builder("/app")
                .setPort(0)
                .setBaseResource(Resource.newResource(WEBAPP))
                .setStopAtShutdown(false)
                .build();
    }

    private static String instanceNumber(
            final Server server) throws Exception {
        final int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        final HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + "/app/instance-number")).build();
        final HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        return response.body();
    }

}
