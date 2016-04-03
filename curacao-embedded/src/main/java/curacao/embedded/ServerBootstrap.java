/**
 * Copyright (c) 2015 Mark S. Kolich
 * http://mark.koli.ch
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import curacao.CuracaoContextListener;
import curacao.CuracaoDispatcherServlet;
import curacao.embedded.filters.FooFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.DispatcherType;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Embedded Jetty server bootstrap.  The default configuration below
 * exposes a Curacao managed endpoint at http://localhost:8080/curacao.
 */
@SuppressWarnings("deprecation")
public final class ServerBootstrap {

    private static final int DEFAULT_SERVER_PORT = 8080;

    private static final String CONTEXT_PATH = "/curacao";
    private static final String SERVLET_MAPPING_UNDER_CONTEXT = "/*";

    public static void main(final String... args) throws Exception {
        final File workingDir = getWorkingDir(); // Basically "user.dir"

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            port = DEFAULT_SERVER_PORT;
        }

        final Server server = new Server();
        final ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        connector.setIdleTimeout(20000L); // 20-seconds

        server.addConnector(connector);

        final ServletHolder holder = new ServletHolder(CuracaoDispatcherServlet.class);
        holder.setAsyncSupported(true); // Async supported = true
        holder.setInitOrder(1); // Load on startup = true

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SECURITY|ServletContextHandler.NO_SESSIONS);
        context.addEventListener(new CuracaoContextListener()); // Required
        context.setContextPath(CONTEXT_PATH);
        context.setResourceBase(workingDir.getAbsolutePath());
        context.addServlet(holder, SERVLET_MAPPING_UNDER_CONTEXT);

        // curl -v -X GET -H"Accept-Encoding: gzip,deflate" http://localhost:8080/curacao/json
        final List<DispatcherType> dispatcherTypes = ImmutableList.of(DispatcherType.FORWARD, DispatcherType.REQUEST);
        /*FilterHolder gzipFilterHolder = context.addFilter(GzipFilter.class, "/*", Sets.newEnumSet(dispatcherTypes, DispatcherType.class));
        gzipFilterHolder.setInitParameter("mimeTypes", "application/json");
        gzipFilterHolder.setInitParameter("minGzipSize", "0");*/
        context.addFilter(FooFilter.class, "/*", Sets.newEnumSet(dispatcherTypes, DispatcherType.class));

        // Attach a Gzip handler to the context which Gzips JSON responses.
        final GzipHandler gzip = new GzipHandler();
        gzip.setMinGzipSize(0);
        gzip.addIncludedMimeTypes("application/json");
        gzip.setHandler(context);

        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {gzip, context});

        server.setHandler(handlers);

        server.start();
        server.join();
    }

    private static final File getWorkingDir() {
        final Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toFile();
    }

}
