/*
 * Copyright (c) 2021 Mark S. Kolich
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import curacao.CuracaoContextListener;
import curacao.CuracaoDispatcherServlet;
import curacao.embedded.filters.TimingFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import javax.servlet.DispatcherType;
import java.net.URL;
import java.util.List;

import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

/**
 * Embedded Jetty server bootstrap. The default configuration below exposes a
 * Curacao managed endpoint at:
 *
 * http://localhost:8080/curacao
 */
public final class ServerBootstrap {

    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8080;

    private static final String CONTEXT_PATH = "/curacao";
    private static final String STATIC_SERVLET_MAPPING_UNDER_CONTEXT = "/static/*";
    private static final String CURACAO_MAPPING_UNDER_CONTEXT = "/*";

    public static void main(
            final String... args) throws Exception {
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (final Exception e) {
            port = DEFAULT_SERVER_PORT;
        }

        final Server server = new Server();
        final ServerConnector connector = new ServerConnector(server);
        connector.setHost(DEFAULT_SERVER_HOST);
        connector.setPort(port);
        connector.setIdleTimeout(20000L); // 20-seconds

        server.addConnector(connector);

        final ServletContextHandler context = new ServletContextHandler(NO_SECURITY | NO_SESSIONS);
        context.addEventListener(new CuracaoContextListener()); // Required
        context.setContextPath(CONTEXT_PATH);
        context.setBaseResource(getBaseResourceForRuntime());

        final ServletHolder defaultHolder = new ServletHolder("default", DefaultServlet.class);
        defaultHolder.setAsyncSupported(true); // Async supported = true
        defaultHolder.setInitOrder(1); // Load on startup = true
        defaultHolder.setInitParameter(DefaultServlet.CONTEXT_INIT + "dirAllowed", "true");
        defaultHolder.setInitParameter(DefaultServlet.CONTEXT_INIT + "acceptRanges", "true");
        defaultHolder.setInitParameter(DefaultServlet.CONTEXT_INIT + "cacheControl", "public,max-age=3600");
        context.addServlet(defaultHolder, STATIC_SERVLET_MAPPING_UNDER_CONTEXT);

        final ServletHolder holder = new ServletHolder("curacao", CuracaoDispatcherServlet.class);
        holder.setAsyncSupported(true); // Async supported = true
        holder.setInitOrder(1); // Load on startup = true
        context.addServlet(holder, CURACAO_MAPPING_UNDER_CONTEXT);

        final List<DispatcherType> dispatcherTypes = ImmutableList.of(DispatcherType.FORWARD, DispatcherType.REQUEST);
        context.addFilter(TimingFilter.class, "/*", Sets.newEnumSet(dispatcherTypes, DispatcherType.class));

        // Attach a Gzip handler to the context which Gzips responses.
        final GzipHandler gzip = new GzipHandler();
        gzip.addIncludedMimeTypes("application/json", "text/plain");
        gzip.setHandler(context);

        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {gzip, context});

        server.setHandler(handlers);

        server.start();
        server.join();
    }

    private static Resource getBaseResourceForRuntime() throws Exception {
        // In dev, the base resource will be something like "src/main/webapp".
        final Resource srcMainWebApp = Resource.newResource("curacao-embedded/src/main/webapp");
        if (srcMainWebApp.exists()) {
            return srcMainWebApp;
        }

        // In prod/deployment, the app runs within a single fat JAR and so the base resource
        // will be a "webapp" directory within the fat JAR.
        final URL webApp = Resources.getResource("webapp");
        return Resource.newResource(webApp);
    }

}
