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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableList;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Finds and reads the default base resource when it lives inside a JAR on the classpath, which is how a fat
 * JAR deployment is laid out and the only arrangement in which Jetty has to mount an archive to read it.
 *
 * <p>Every other test here points the builder at a directory, and a directory mounts nothing - so the whole
 * mounting path, and the resource lifecycle that goes with it, went unexercised until an application was
 * actually deployed as a fat JAR. That is where it showed: a base resource built from a factory that never
 * releases anything leaves Jetty logging {@code Leaked <jar> for jar:file:...!/webapp/} on every startup,
 * and holding the mount open for the life of the JVM.
 */
public final class CuracaoJettyServerJarBaseResourceTest {

    private static final String WEBAPP_ENTRY = "webapp/hello.txt";
    private static final String WEBAPP_CONTENT = "Hello from inside a jar";

    @TempDir
    /* package */ static Path TEMP;

    /**
     * The default lookup falls back to a {@code webapp} directory on the classpath. Putting one inside a JAR
     * and making that JAR the context class loader is what a fat JAR looks like to this code.
     */
    @Test
    public void findsAndReadsABaseResourceInsideAJarTest() throws Exception {
        final Path jar = TEMP.resolve("app-runnable.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("webapp/"));
            out.closeEntry();
            out.putNextEntry(new JarEntry(WEBAPP_ENTRY));
            out.write(WEBAPP_CONTENT.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        final Logger resourceFactoryLogger = (Logger) LoggerFactory.getLogger(ResourceFactory.class);
        final Level originalLevel = resourceFactoryLogger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        resourceFactoryLogger.setLevel(Level.WARN);
        resourceFactoryLogger.addAppender(appender);
        final List<ILoggingEvent> events = appender.list;

        final ClassLoader original = Thread.currentThread().getContextClassLoader();
        Server server = null;
        try (URLClassLoader jarLoader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, original)) {
            Thread.currentThread().setContextClassLoader(jarLoader);

            // No setBaseResource, so the builder runs its own lookup and has to resolve into the JAR. It is
            // resolved during build(), so this asserts against the context without starting the server.
            server = new CuracaoJettyServer.Builder("/curacao")
                    .setPort(0)
                    .setStopAtShutdown(false)
                    .build();
            server.start();

            // Read it back the way an application does, through the documented context attribute.
            final WebAppContext context = (WebAppContext) server.getHandler();
            final Resource baseResource =
                    (Resource) context.getAttribute(CuracaoJettyServer.BASE_RESOURCE_CONTEXT_ATTRIBUTE);

            assertNotNull(baseResource, "found no base resource inside the jar");
            assertTrue(baseResource.getURI().toString().startsWith("jar:"),
                    "base resource did not resolve into the jar: " + baseResource.getURI());

            final Resource helloFile = baseResource.resolve("hello.txt");
            assertNotNull(helloFile);
            assertTrue(helloFile.exists(), "cannot read a file inside the mounted jar");
            try (var in = helloFile.newInputStream()) {
                assertEquals(WEBAPP_CONTENT, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            if (server != null) {
                server.stop();
            }
            resourceFactoryLogger.detachAppender(appender);
            resourceFactoryLogger.setLevel(originalLevel);
        }

        // Jetty reports a mount that nothing will ever release by logging it, so that is what gets
        // asserted: the mount has an owner. Reaching into Jetty's own mount registry would be the
        // more direct check, but FileSystemPool is deprecated for removal.
        final List<String> leaked = events.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("Leaked"))
                .collect(ImmutableList.toImmutableList());
        assertTrue(leaked.isEmpty(), "Jetty reported a leaked mount for the base resource: " + leaked);
    }

}
