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

package curacao.jetty12;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import curacao.servlet.jakarta.CuracaoJakartaContextListener;
import curacao.servlet.jakarta.CuracaoJakartaDispatcherServlet;
import jakarta.servlet.Servlet;
import org.eclipse.jetty.ee11.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.jetty.ee11.servlet.ServletContextHandler.NO_SECURITY;
import static org.eclipse.jetty.ee11.servlet.ServletContextHandler.NO_SESSIONS;

/**
 * Builds an embedded Jetty 12 {@link Server} that hosts a Curacao application: the Curacao dispatcher servlet
 * and its context listener under a context path, beside any servlets the application adds, such as one for
 * static files.
 *
 * <pre>{@code
 * final ServletHolder staticFiles = new ServletHolder(DefaultServlet.class);
 * staticFiles.setInitParameter("dirAllowed", "false");
 * staticFiles.setInitParameter("cacheControl", "public,max-age=3600");
 *
 * final Server server = new CuracaoJettyServer.Builder("/onyx")
 *     .setHost(host)
 *     .setPort(port)
 *     .addServlet(staticFiles, "/static/*")
 *     .setErrorPage("/forward-errors")
 *     .build();
 * server.start();
 * server.join();
 * }</pre>
 *
 * <p>The server is built, not started, so tests can boot the whole application on an ephemeral port
 * ({@code setPort(0)}). The context has no sessions, no security handler and no JSP engine, and the server
 * sends neither a {@code Server} nor an {@code X-Powered-By} header.
 *
 * <p>If Curacao fails to boot (a misconfigured component, say), Jetty marks the context unavailable and answers
 * every request under it with a 503, while {@link Server#start()} still succeeds; an HTTP health check sees the
 * application as down. {@link Builder#setFailOnStartupError(boolean)} makes {@link Server#start()} throw instead.
 *
 * <p>This is the Jetty 12 counterpart of {@code curacao-jetty11-server} and keeps the same builder API, so an
 * application moves over by changing the import. The servlet types it takes and returns come from Jetty's EE11
 * environment though, which is the difference that reaches callers: a {@link ServletHolder} handed to
 * {@link Builder#addServlet(ServletHolder, String)} is an {@code org.eclipse.jetty.ee11.servlet.ServletHolder},
 * and a {@link Resource} handed to {@link Builder#setBaseResource(Resource)} comes from a
 * {@link ResourceFactory} rather than from the {@code Resource.newResource} static that Jetty 12 removed.
 * EE11 implements Servlet 6.1, which is the {@code jakarta.servlet-api} version {@code curacao-servlet-jakarta}
 * is already built against.
 */
public final class CuracaoJettyServer {

    /**
     * The context attribute that holds the context's base {@link Resource}, its "webapp" directory, for
     * components that read templates or static files from it at runtime.
     */
    public static final String BASE_RESOURCE_CONTEXT_ATTRIBUTE = "curacao.jetty12.base-resource";

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8080;
    public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofSeconds(30L);

    public static final String DEFAULT_THREAD_NAME_FORMAT = "jetty-%d";

    public static final String DEFAULT_CURACAO_MAPPING = "/*";

    /**
     * Where the base resource is found when none is set: the project's webapp directory when running from
     * source, otherwise a "webapp" directory on the classpath, as packaged in a fat JAR.
     */
    private static final String SOURCE_MAIN_WEBAPP_DIRECTORY = "src/main/webapp";
    private static final String CLASSPATH_WEBAPP_DIRECTORY = "webapp";

    private static final String CURACAO_SERVLET_NAME = "curacao";

    /**
     * Jetty's own {@link QueuedThreadPool} defaults, which its constructor that takes a thread factory doesn't
     * fill in.
     */
    private static final int THREAD_POOL_MAX_THREADS = 200;
    private static final int THREAD_POOL_MIN_THREADS = 8;
    private static final int THREAD_POOL_IDLE_TIMEOUT_MS = 60000;
    private static final int THREAD_POOL_RESERVED_THREADS = -1;

    private CuracaoJettyServer() {
    }

    /**
     * The HTTP configuration of the default connector: no {@code Server} or {@code X-Powered-By} header. A
     * connector from {@link Builder#setConnectorFactory(Function)} can start from it so that it doesn't send
     * them either.
     */
    public static HttpConfiguration newHttpConfiguration() {
        final HttpConfiguration config = new HttpConfiguration();
        config.setSendXPoweredBy(false); // Hide X-Powered-By: Jetty
        config.setSendServerVersion(false); // Hide Server: Jetty-*

        return config;
    }

    public static final class Builder {

        private final String contextPath_;

        private String threadNameFormat_ = DEFAULT_THREAD_NAME_FORMAT;

        private String host_ = DEFAULT_HOST;
        private int port_ = DEFAULT_PORT;
        private Duration idleTimeout_ = DEFAULT_IDLE_TIMEOUT;

        private final List<Consumer<ServerConnector>> connectorCustomizers_ = Lists.newArrayList();
        private Function<Server, ? extends Connector> connectorFactory_;

        private Resource baseResource_;
        private final Map<String, Object> contextAttributes_ = Maps.newLinkedHashMap();

        private Class<? extends Servlet> dispatcherServlet_ = CuracaoJakartaDispatcherServlet.class;
        private String curacaoMapping_ = DEFAULT_CURACAO_MAPPING;

        private final List<Map.Entry<ServletHolder, String>> servlets_ = Lists.newArrayList();

        private String errorPage_;

        private boolean failOnStartupError_;
        private boolean stopAtShutdown_ = true;

        private final List<Consumer<WebAppContext>> contextCustomizers_ = Lists.newArrayList();
        private final List<Consumer<Server>> serverCustomizers_ = Lists.newArrayList();

        /**
         * A builder for an application under the given context path, such as {@code "/onyx"}, or {@code "/"}
         * for the root.
         */
        public Builder(
                @Nonnull final String contextPath) {
            checkNotNull(contextPath, "Context path cannot be null.");
            checkArgument(contextPath.startsWith("/"), "Context path must start with a '/': %s", contextPath);
            contextPath_ = contextPath;
        }

        /**
         * The name format of the server's threads, a {@link String#format(String, Object...)} format given the
         * thread's number: {@code "onyx-%d"} names them onyx-0, onyx-1 and so on. Defaults to
         * {@code "jetty-%d"}. As in Guava's {@link ThreadFactoryBuilder#setNameFormat(String)}, which it's
         * passed to.
         */
        public Builder setThreadNameFormat(
                @Nonnull final String threadNameFormat) {
            threadNameFormat_ = checkNotNull(threadNameFormat, "Thread name format cannot be null.");
            return this;
        }

        public Builder setHost(
                @Nonnull final String host) {
            host_ = checkNotNull(host, "Host cannot be null.");
            return this;
        }

        /**
         * The port to listen on; 0 picks a free one, which {@link ServerConnector#getLocalPort()} reports once
         * the server has started.
         */
        public Builder setPort(
                final int port) {
            checkArgument(port >= 0 && port <= 65535, "Port must be between 0 and 65535: %s", port);
            port_ = port;
            return this;
        }

        /**
         * How long a connection may sit with no bytes flowing before Jetty closes it.
         */
        public Builder setIdleTimeout(
                @Nonnull final Duration idleTimeout) {
            checkNotNull(idleTimeout, "Idle timeout cannot be null.");
            checkArgument(!idleTimeout.isNegative(), "Idle timeout cannot be negative: %s", idleTimeout);
            idleTimeout_ = idleTimeout;
            return this;
        }

        /**
         * Changes the default connector after this builder has configured it (host, port and idle timeout), such
         * as its accept queue size, or its {@link HttpConfiguration} through
         * {@code connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration()}.
         * Customizers run in the order they were added. Can't be combined with
         * {@link #setConnectorFactory(Function)}, which replaces the default connector.
         */
        public Builder customizeConnector(
                @Nonnull final Consumer<ServerConnector> customizer) {
            connectorCustomizers_.add(checkNotNull(customizer, "Connector customizer cannot be null."));
            return this;
        }

        /**
         * Replaces the default connector: the factory is given the server and returns the connector to add to
         * it, for HTTPS, HTTP/2 or anything else. The host, port and idle timeout set on this builder configure
         * only the default connector, so the factory sets its own; {@link CuracaoJettyServer#newHttpConfiguration()}
         * is the default connector's HTTP configuration, to start from.
         */
        public Builder setConnectorFactory(
                @Nonnull final Function<Server, ? extends Connector> connectorFactory) {
            connectorFactory_ = checkNotNull(connectorFactory, "Connector factory cannot be null.");
            return this;
        }

        /**
         * The context's base resource, its "webapp" directory. When not set, {@code src/main/webapp} is used if
         * it exists (running from source), otherwise the {@code webapp} directory on the classpath (a fat JAR).
         *
         * <p>Jetty 12 builds a {@link Resource} through a {@link ResourceFactory} rather than through the
         * {@code Resource.newResource} statics of earlier versions: {@code ResourceFactory.root()} returns one
         * whose resources live as long as the JVM, which is what a base resource wants.
         */
        public Builder setBaseResource(
                @Nonnull final Resource baseResource) {
            baseResource_ = checkNotNull(baseResource, "Base resource cannot be null.");
            return this;
        }

        public Builder setContextAttribute(
                @Nonnull final String name,
                @Nonnull final Object value) {
            checkNotNull(name, "Attribute name cannot be null.");
            checkNotNull(value, "Attribute value cannot be null.");
            contextAttributes_.put(name, value);
            return this;
        }

        /**
         * The Curacao dispatcher servlet; defaults to {@link CuracaoJakartaDispatcherServlet}.
         */
        public Builder setDispatcherServlet(
                @Nonnull final Class<? extends CuracaoJakartaDispatcherServlet> dispatcherServlet) {
            dispatcherServlet_ = checkNotNull(dispatcherServlet, "Dispatcher servlet cannot be null.");
            return this;
        }

        /**
         * The Curacao dispatcher's mapping under the context path; defaults to {@code "/*"}.
         */
        public Builder setCuracaoMapping(
                @Nonnull final String curacaoMapping) {
            curacaoMapping_ = checkNotNull(curacaoMapping, "Curacao mapping cannot be null.");
            return this;
        }

        /**
         * Adds a servlet, configured by the caller, to the context at the given path spec, such as Jetty's
         * {@code DefaultServlet} at {@code "/static/*"} to serve static files from the base resource. The Curacao
         * dispatcher answers every path no servlet claims more specifically. Servlets are added in the order
         * given.
         */
        public Builder addServlet(
                @Nonnull final ServletHolder servlet,
                @Nonnull final String pathSpec) {
            checkNotNull(servlet, "Servlet cannot be null.");
            checkNotNull(pathSpec, "Path spec cannot be null.");
            servlets_.add(Maps.immutableEntry(servlet, pathSpec));
            return this;
        }

        /**
         * A path under the context that every error the container sends (a {@code sendError}, such as a
         * static file servlet's 404) is forwarded to, so the application can render its own error pages.
         */
        public Builder setErrorPage(
                @Nonnull final String errorPage) {
            errorPage_ = checkNotNull(errorPage, "Error page cannot be null.");
            return this;
        }

        /**
         * When true, a failure booting the application, Curacao's included, makes {@link Server#start()} throw;
         * when false (the default), the server starts and the context answers every request with a 503.
         * Either way, the components Curacao already initialized are destroyed.
         */
        public Builder setFailOnStartupError(
                final boolean failOnStartupError) {
            failOnStartupError_ = failOnStartupError;
            return this;
        }

        /**
         * Whether a JVM shutdown hook stops the server; defaults to true.
         */
        public Builder setStopAtShutdown(
                final boolean stopAtShutdown) {
            stopAtShutdown_ = stopAtShutdown;
            return this;
        }

        /**
         * Changes the context after this builder has configured it, to add a filter, a servlet or a listener.
         * Customizers run in the order they were added.
         */
        public Builder customizeContext(
                @Nonnull final Consumer<WebAppContext> customizer) {
            contextCustomizers_.add(checkNotNull(customizer, "Context customizer cannot be null."));
            return this;
        }

        /**
         * Changes the server after this builder has configured it, its handler set to the context, to wrap
         * that handler (in a {@code GzipHandler}, say) or add a connector. Customizers run in the order they
         * were added, after the context's.
         */
        public Builder customizeServer(
                @Nonnull final Consumer<Server> customizer) {
            serverCustomizers_.add(checkNotNull(customizer, "Server customizer cannot be null."));
            return this;
        }

        /**
         * Builds the server, without starting it.
         *
         * @throws IOException if the base resource can't be read
         * @throws IllegalArgumentException if the thread name format is malformed
         * @throws IllegalStateException if no base resource was set and none can be found, or if connector
         *     customizers were added along with a connector factory
         */
        public Server build() throws IOException {
            checkState(connectorFactory_ == null || connectorCustomizers_.isEmpty(), "customizeConnector() changes "
                    + "the default connector, which setConnectorFactory() replaces; configure the connector in the "
                    + "factory instead.");

            final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setNameFormat(threadNameFormat_)
                    .build();
            // Jetty's default sizes and idle timeout, with this builder's thread names.
            final QueuedThreadPool pool = new QueuedThreadPool(THREAD_POOL_MAX_THREADS, THREAD_POOL_MIN_THREADS,
                    THREAD_POOL_IDLE_TIMEOUT_MS, THREAD_POOL_RESERVED_THREADS, null, null, threadFactory);
            final Server server = new Server(pool);

            final Connector connector = (connectorFactory_ != null)
                    ? checkNotNull(connectorFactory_.apply(server), "Connector factory returned null.")
                    : defaultConnector(server);
            server.addConnector(connector);

            // Jetty 12 dropped the constructor that took the server and attached the context to it, so the
            // context is built standalone and installed as the server's handler further down. NO_SESSIONS is
            // what keeps a session handler from being created at all, which is why the Jetty 11 version's
            // follow-up setSessionHandler(null) - a workaround for jetty.project#3963 - is gone.
            final WebAppContext context = new WebAppContext(contextPath_, null, null, null, null,
                    NO_SESSIONS | NO_SECURITY);
            context.setContextPath(contextPath_);
            // Without the defaults descriptor Jetty doesn't load the JSP engine, which also starts faster.
            context.setDefaultsDescriptor(null);
            context.setThrowUnavailableOnStartupException(failOnStartupError_);

            final Resource baseResource = (baseResource_ != null) ? baseResource_ : findBaseResource(server);
            context.setBaseResource(baseResource);
            context.setAttribute(BASE_RESOURCE_CONTEXT_ATTRIBUTE, baseResource);

            contextAttributes_.forEach(context::setAttribute);

            servlets_.forEach(servlet -> context.addServlet(servlet.getKey(), servlet.getValue()));

            final ServletHolder curacaoHolder = new ServletHolder(CURACAO_SERVLET_NAME, dispatcherServlet_);
            curacaoHolder.setAsyncSupported(true);
            // Initialize the dispatcher when the server starts, not on the first request.
            curacaoHolder.setInitOrder(1);
            context.addEventListener(new CuracaoJakartaContextListener());
            context.addServlet(curacaoHolder, curacaoMapping_);

            if (errorPage_ != null) {
                final ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
                errorHandler.addErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE, errorPage_);
                context.setErrorHandler(errorHandler);
            }

            contextCustomizers_.forEach(customizer -> customizer.accept(context));

            server.setHandler(context);
            server.setStopAtShutdown(stopAtShutdown_);

            serverCustomizers_.forEach(customizer -> customizer.accept(server));

            return server;
        }

        private ServerConnector defaultConnector(
                final Server server) {
            final ConnectionFactory connectionFactory = new HttpConnectionFactory(newHttpConfiguration());
            final ServerConnector connector = new ServerConnector(server, connectionFactory);
            connector.setHost(host_);
            connector.setPort(port_);
            connector.setIdleTimeout(idleTimeout_.toMillis());

            connectorCustomizers_.forEach(customizer -> customizer.accept(connector));

            return connector;
        }

    }

    /**
     * Finds the base resource through a {@link ResourceFactory} scoped to the server, so that whatever it
     * mounts is released when the server stops.
     *
     * <p>The scoping matters for a fat JAR, where the base resource is a {@code jar:file:} URL and reading it
     * means mounting the archive as a file system. {@link ResourceFactory#root()} never releases what it
     * hands out, so mounting through it leaves Jetty logging
     * {@code WARN ResourceFactory - Leaked <jar> for jar:file:...!/webapp/} at startup, and holding the mount
     * for the life of the JVM. A plain directory mounts nothing and so never showed it.
     */
    private static Resource findBaseResource(
            final Server server) throws IOException {
        final ResourceFactory resourceFactory = ResourceFactory.of(server);

        final Resource source = resourceFactory.newResource(SOURCE_MAIN_WEBAPP_DIRECTORY);
        if (source != null && source.exists()) {
            return source;
        }

        try {
            return resourceFactory.newResource(Resources.getResource(CLASSPATH_WEBAPP_DIRECTORY));
        } catch (final IllegalArgumentException e) {
            throw new IllegalStateException("Found no base resource: neither " + SOURCE_MAIN_WEBAPP_DIRECTORY
                    + " nor a \"" + CLASSPATH_WEBAPP_DIRECTORY + "\" directory on the classpath exists. Set one "
                    + "with setBaseResource().", e);
        }
    }

}
