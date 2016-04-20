/**
 * Copyright (c) 2016 Mark S. Kolich
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

package curacao.test;

import com.google.common.collect.Sets;
import curacao.CuracaoContextListener;
import curacao.annotations.Component;
import curacao.test.annotations.CuracaoJUnit4RunnerConfig;
import curacao.test.annotations.MockComponent;
import curacao.util.reflection.CuracaoReflectionUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.reflect.Field;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.CuracaoContextListener.CuracaoCoreObjectMap.CONTEXT_KEY_MOCK_COMPONENTS;

/**
 * A JUnit4 ready, Curacao enabled test runner.
 *
 * This runner formally extends JUnit's {@link Runner} class, meaning it can be used with
 * JUnit's {@link RunWith} annotation on any given test class.  This Curacao enabled runner is special
 * in that it spawns a Jetty server and starts the web-application or service found under a path of your
 * choice, defaulting to <code>src/main/webapp</code>.  The Jetty server is started, then tests in the
 * class are allowed to run.  This is especially useful for (integration-like?) tests that require a service
 * to be running where the {@link Test} annotated methods make real on-the-wire calls to the Jetty service
 * listening on <code>localhost</code>.
 *
 * The spawned Jetty server listens on a port as configured by the {@link CuracaoJUnit4RunnerConfig}
 * which is required on any test class using this runner.  Test classes using this runner that are
 * not annotated with {@link CuracaoJUnit4RunnerConfig} will fail hard at runtime with a legit
 * {@link IllegalStateException}.
 *
 * Mock components as instantiated by unit tests can be injected into Curacao's core component table
 * with the handy {@link MockComponent} annotation.  Any {@link MockComponent} annotated field in the
 * test class will be slurped up and injected into Curacao's component table, meaning you can run a
 * real service that supports requests on-the-wire, but is backed by mock {@link Component}'s provided
 * by unit tests.
 *
 * This class is explicitly not declared <code>final</code>, allowing implementers to extend and override
 * behavior herein as desired.
 *
 * @author Mark Kolich
 * @since 4.1.0
 */
public class CuracaoJUnit4Runner extends BlockJUnit4ClassRunner {

    private static final Logger log = LoggerFactory.getLogger(CuracaoJUnit4Runner.class);

    /**
     * The Curacao {@link org.junit.runner.RunWith} annotated test class.
     */
    private final Class<?> testClass_;

    /**
     * A singleton instance of the test class.
     */
    private final Object testInstance_;

    /**
     * The {@link CuracaoJUnit4RunnerConfig} configuration attached to the test class.
     */
    private final CuracaoJUnit4RunnerConfig runnerConfig_;

    public CuracaoJUnit4Runner(@Nonnull final Class<?> klass) throws Exception {
        super(klass);
        testClass_ = checkNotNull(klass, "Test class cannot be null.");
        // Attempt to pull the runner config annotation from the test class; this is where things like the
        // runner port number and unpacked WAR path (e.g., src/main/webapp) come from.
        if (!testClass_.isAnnotationPresent(CuracaoJUnit4RunnerConfig.class)) {
            throw new IllegalStateException("Test class missing required @" +
                CuracaoJUnit4RunnerConfig.class.getSimpleName() + " annotation: " +
                testClass_.getCanonicalName());
        }
        runnerConfig_ = testClass_.getAnnotation(CuracaoJUnit4RunnerConfig.class);
        // Create the test class instance singleton, and cache it away.
        testInstance_ = super.createTest();
    }

    @Override
    protected final Object createTest() throws Exception {
        return testInstance_;
    }

    @Override
    public final void run(@Nonnull final RunNotifier notifier) {
        Server server = null;
        try {
            final Set<Object> mockComponents = Sets.newLinkedHashSet();
            // Build out a set of pre-loaded mocks that will be injected into the servlet context as
            // a custom attribute; note this scans the test class and any parent (abstract or otherwise)
            // classes it extends.
            Class<?> classToScan = testInstance_.getClass();
            while (classToScan != null && !Object.class.equals(classToScan)) {
                final Reflections reflections = CuracaoReflectionUtils.getReflectionInstanceForClass(classToScan);
                // Scan the test class for fields annotated with the mock component annotation.
                final Set<Field> mockFields = reflections.getFieldsAnnotatedWith(MockComponent.class);
                mockFields.stream().map(f -> {
                    try {
                        // Required to make any "private" fields accessible via reflection.
                        if (!f.isAccessible()) {
                            f.setAccessible(true);
                        }
                        return f.get(testInstance_);
                    } catch (Exception e) {
                        log.warn("Failed to extract @{} field `{}` from class: {}",
                            MockComponent.class.getSimpleName(), f,
                            testInstance_.getClass().getCanonicalName(), e);
                        return null;
                    }
                }).filter(f -> f != null).forEach(mockComponents::add);
                classToScan = classToScan.getSuperclass();
            }
            // Build out a new Jetty server (and NIO connector) listening on the desired port.
            server = buildServer(runnerConfig_.port(), runnerConfig_.contextPath(), runnerConfig_.webAppPath());
            checkNotNull(server, "Runner provided Jetty server cannot be null.");
            // New up a fresh instance of the custom web-app context, passing the pre-loaded mocks.
            final CuracaoJUnit4WebAppContext context = new CuracaoJUnit4WebAppContext(mockComponents);
            context.setWar(runnerConfig_.webAppPath()); // A pointer to the dir that holds WEB-INF/web.xml
            // Make sure the context path starts with a "/" as required by Jetty.
            final String contextPath;
            if (!runnerConfig_.contextPath().startsWith("/")) {
                contextPath = "/" + runnerConfig_.contextPath();
            } else {
                contextPath = runnerConfig_.contextPath();
            }
            context.setContextPath(contextPath);
            server.setHandler(context);
            // Start Jetty!
            server.start();
            // Run the tests!
            super.run(notifier);
        } catch (Exception e) {
            throw new RuntimeException("Curacao JUnit runner failed to start test.", e);
        } finally {
            if (server != null) {
                try {
                    // Stop Jetty, swallow any exceptions.
                    server.stop();
                } catch (Exception e) { }
            }
        }
    }

    /**
     * Constructs a new Jetty server listening on the given port.
     *
     * @param port the Jetty server listen port
     * @param contextPath the context path of the application, e.g., "/foo"
     * @param webAppPath the path to the unpacked web-app on disk, e.g., "src/main/webapp"
     * @return a well configured, runnable Jetty {@link Server} instance
     */
    @Nonnull
    public Server buildServer(@Nonnegative final int port,
                              @Nonnull final String contextPath,
                              @Nonnull final String webAppPath) {
        final Server server = new Server(port);
        server.setStopAtShutdown(true);
        return server;
    }

    /**
     * A custom Jetty {@link WebAppContext} used to inject pre-loaded mock components into a
     * Servlet context.  The injected mocks are consumed in {@link curacao.components.ComponentTable}
     * and used to build out the dependency-injected component mapping table, hence, injecting mock
     * objects for unit tests into the Curacao core component table.
     */
    private static final class CuracaoJUnit4WebAppContext extends WebAppContext {

        private final Set<Object> mockComponents_;

        public CuracaoJUnit4WebAppContext(@Nonnull final Set<Object> mockComponents) {
            mockComponents_ = checkNotNull(mockComponents, "Pre-loaded mock components cannot be null.");
        }

        @Override
        public final void callContextInitialized(final ServletContextListener listener,
                                                 final ServletContextEvent event) {
            // For any instance of the Curacao context listener, attach the pre-loaded mock components to
            // the underlying Servlet context as an attribute.
            if (listener instanceof CuracaoContextListener) {
                event.getServletContext().setAttribute(CONTEXT_KEY_MOCK_COMPONENTS, mockComponents_);
            }
            super.callContextInitialized(listener, event);
        }

    }

}
