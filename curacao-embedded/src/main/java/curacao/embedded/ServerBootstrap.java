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

package curacao.embedded;

import curacao.embedded.filters.TimingFilter;
import curacao.jetty11.CuracaoJettyServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import java.time.Duration;
import java.util.EnumSet;

import static jakarta.servlet.DispatcherType.FORWARD;
import static jakarta.servlet.DispatcherType.REQUEST;

/**
 * Embedded Jetty server bootstrap. The default configuration below exposes a
 * Curacao managed endpoint at:
 * <a href="http://localhost:8080/curacao">http://localhost:8080/curacao</a>
 */
public final class ServerBootstrap {

    private static final int DEFAULT_SERVER_PORT = 8080;

    private static final String CONTEXT_PATH = "/curacao";

    private static final String STATIC_PATH_SPEC = "/static/*";
    private static final String STATIC_CACHE_CONTROL = "public,max-age=3600";

    private static final String STATIC_SERVLET_DIR_ALLOWED = "dirAllowed";
    private static final String STATIC_SERVLET_ACCEPT_RANGES = "acceptRanges";
    private static final String STATIC_SERVLET_CACHE_CONTROL = "cacheControl";

    /**
     * The webapp directory when running from the root of the Curacao project, as an IDE does. Otherwise the
     * builder finds it: {@code src/main/webapp} when running from this module, or the fat JAR's
     * {@code webapp} directory.
     */
    private static final String PROJECT_ROOT_WEBAPP_DIRECTORY = "curacao-embedded/src/main/webapp";

    private ServerBootstrap() {
    }

    public static void main(
            final String... args) throws Exception {
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (final Exception e) {
            port = DEFAULT_SERVER_PORT;
        }

        // Static files from the base resource, with directory listings on for this example.
        final ServletHolder staticFiles = new ServletHolder(DefaultServlet.class);
        staticFiles.setAsyncSupported(true);
        staticFiles.setInitParameter(STATIC_SERVLET_DIR_ALLOWED, Boolean.TRUE.toString());
        staticFiles.setInitParameter(STATIC_SERVLET_ACCEPT_RANGES, Boolean.TRUE.toString());
        staticFiles.setInitParameter(STATIC_SERVLET_CACHE_CONTROL, STATIC_CACHE_CONTROL);

        final CuracaoJettyServer.Builder builder = new CuracaoJettyServer.Builder(CONTEXT_PATH)
                .setPort(port)
                .setIdleTimeout(Duration.ofSeconds(20L))
                .addServlet(staticFiles, STATIC_PATH_SPEC)
                .setThreadNameFormat("curacao-embedded-jetty-%d")
                // Time every request, including forwards.
                .customizeContext(context -> context.addFilter(TimingFilter.class, "/*", EnumSet.of(FORWARD, REQUEST)))
                // Gzip JSON and plain text responses.
                .customizeServer(server -> {
                    final GzipHandler gzip = new GzipHandler();
                    gzip.addIncludedMimeTypes("application/json", "text/plain");
                    gzip.setHandler(server.getHandler());
                    server.setHandler(gzip);
                });

        final Resource projectRootWebApp = Resource.newResource(PROJECT_ROOT_WEBAPP_DIRECTORY);
        if (projectRootWebApp.exists()) {
            builder.setBaseResource(projectRootWebApp);
        }

        final Server server = builder.build();
        server.start();
        server.join();
    }

}
