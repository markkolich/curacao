package com.kolich.curacao.embedded;

import com.kolich.curacao.CuracaoDispatcherServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

public final class ServerBootstrap {

    private static final String SERVLET_MAPPING = "/foobar/*";

    public static void main(final String[] args) throws Exception {

        final Server server = new Server(8080);

        final ServletHandler handler = new ServletHandler();
        handler.setServer(server);

        final ServletHolder holder = handler.addServletWithMapping(
            CuracaoDispatcherServlet.class,
            SERVLET_MAPPING);

        holder.setAsyncSupported(true); // Async supported = true
        holder.setInitOrder(1); // Load on startup = true

        final WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setResourceBase("src/main/webapp");
        context.addServlet(holder, SERVLET_MAPPING);

        server.setHandler(context);

        server.start();
        server.join();

    }

}
