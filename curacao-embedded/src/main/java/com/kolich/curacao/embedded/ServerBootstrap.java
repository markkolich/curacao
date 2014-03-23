/**
 * Copyright (c) 2014 Mark S. Kolich
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

package com.kolich.curacao.embedded;

import com.kolich.curacao.CuracaoContextListener;
import com.kolich.curacao.CuracaoDispatcherServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Embedded Jetty server bootstrap.  The default configuration below
 * exposes a Curacao managed endpoint at http://localhost:8080/curacao.
 */
public final class ServerBootstrap {

    private static final int DEFAULT_SERVER_PORT = 8080;

    private static final String CONTEXT_PATH = "/curacao";
    private static final String SERVLET_MAPPING_UNDER_CONTEXT = "/*";

    public static void main(final String[] args) throws Exception {

        final File workingDir = getWorkingDir(); // Basically "user.dir"

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            port = DEFAULT_SERVER_PORT;
        }

        final Server server = new Server(port);

        final ServletHolder holder = new ServletHolder(CuracaoDispatcherServlet.class);
        holder.setAsyncSupported(true); // Async supported = true
        holder.setInitOrder(1); // Load on startup = true

        final WebAppContext context = new WebAppContext();
        context.addEventListener(new CuracaoContextListener()); // Required
        context.setContextPath(CONTEXT_PATH);
        context.setResourceBase(workingDir.getAbsolutePath());
        context.addServlet(holder, SERVLET_MAPPING_UNDER_CONTEXT);

        server.setHandler(context);

        server.start();
        server.join();

    }

    private static final File getWorkingDir() {
        final Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toFile();
    }

}
