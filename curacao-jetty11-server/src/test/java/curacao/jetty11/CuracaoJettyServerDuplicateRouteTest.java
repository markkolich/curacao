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
import org.eclipse.jetty.util.resource.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the application in {@code curacao.jetty11.fixtures.duplicateroute}: two controllers that map the same route,
 * so Curacao fails to start.
 */
public final class CuracaoJettyServerDuplicateRouteTest {

    @TempDir
    /* package */ static Path WEBAPP;

    @BeforeAll
    public static void bootTheBrokenApplication() {
        // Surefire runs each test class in a fresh JVM, so Curacao hasn't read its configuration yet.
        System.setProperty("curacao.boot-package", "curacao.jetty11.fixtures.duplicateroute");
        ConfigFactory.invalidateCaches();
    }

    @Test
    public void failsStartupTest() throws Exception {
        final Server server = new CuracaoJettyServer.Builder("/broken")
                .setPort(0)
                .setBaseResource(Resource.newResource(WEBAPP))
                .setStopAtShutdown(false)
                .setFailOnStartupError(true)
                .build();
        try {
            final Exception e = assertThrows(Exception.class, server::start);

            final int index = ExceptionUtils.indexOfType(e, ComponentConfigurationException.class);
            assertTrue(index >= 0, ExceptionUtils.getStackTrace(e));
            final String message = ExceptionUtils.getThrowableList(e).get(index).getMessage();
            assertTrue(message.contains("Ambiguous route"), message);
            assertTrue(message.contains("^/same$"), message);
        } finally {
            server.stop();
        }
    }

}
