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

package com.kolich.curacao;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.kolich.curacao.handlers.components.ComponentMappingTable;
import com.kolich.curacao.handlers.requests.ControllerMethodArgumentMappingTable;
import com.kolich.curacao.handlers.requests.ControllerRoutingTable;
import com.kolich.curacao.handlers.responses.ResponseTypeMappingHandlerTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.curacao.CuracaoConfigLoader.getThreadPoolNameFormat;
import static com.kolich.curacao.CuracaoConfigLoader.getThreadPoolSize;
import static com.kolich.curacao.CuracaoContextListener.CuracaoCoreObjectMap.CONTEXT_KEY_CORE_OBJECT_MAP;
import static com.kolich.curacao.util.AsyncServletExecutorServiceFactory.createNewListeningService;

public final class CuracaoContextListener implements ServletContextListener {

    private interface ThreadPool {
        public static final int SIZE = getThreadPoolSize();
        public static final String NAME_FORMAT = getThreadPoolNameFormat();
    }

    /**
     * The core object map is an object that acts as a context global holder
     * of core objects used by Curacao.  This includes the routing table,
     * request and response thread pools and the component mapping table, among
     * others.  On context startup/initialization, the core object map is
     * attached to the context as an attribute such that these immutable and
     * global objects can be consumed/shared by Servlets in the context.
     */
    public static final class CuracaoCoreObjectMap {

        public static final String CONTEXT_KEY_CORE_OBJECT_MAP =
            "curacao.core-object-map";

        public final ServletContext context_;

        public final ListeningExecutorService threadPoolService_;

        public final ComponentMappingTable componentMappingTable_;
        public final ControllerRoutingTable routingTable_;
        public final ResponseTypeMappingHandlerTable responseHandlerTable_;
        public final ControllerMethodArgumentMappingTable methodArgTable_;

        public CuracaoCoreObjectMap(final ServletContext context,
                                    final ListeningExecutorService threadPoolService,
                                    final ComponentMappingTable componentMappingTable,
                                    final ControllerRoutingTable routingTable,
                                    final ResponseTypeMappingHandlerTable responseHandlerTable,
                                    final ControllerMethodArgumentMappingTable methodArgTable) {
            context_ = checkNotNull(context,
                "Servlet context cannot be null.");
            threadPoolService_ = checkNotNull(threadPoolService,
                "Thread pool service cannot be null.");
            componentMappingTable_ = checkNotNull(componentMappingTable,
                "Component mapping table cannot be null.");
            routingTable_ = checkNotNull(routingTable,
                "Controller routing table cannot be null.");
            responseHandlerTable_ = checkNotNull(responseHandlerTable,
                "Response type handler mapping table cannot be null.");
            methodArgTable_ = checkNotNull(methodArgTable,
                "Controller method argument mapping table cannot be null.");
        }

        @Nullable
        public static final CuracaoCoreObjectMap objectMapFromContext(
            @Nonnull final ServletContext context) {
            checkNotNull(context, "Servlet context cannot be null.");
            return (CuracaoCoreObjectMap)context.getAttribute(
                CONTEXT_KEY_CORE_OBJECT_MAP);
        }

    }

    /**
     * A non-final, locally cached copy of the contexts global core
     * object map.
     */
    private CuracaoCoreObjectMap coreObjectMap_;

    @Override
    public final void contextInitialized(final ServletContextEvent e) {
        // Ensure that the Servlet container is being honest, and returns
        // a valid non-null context attached to the event.
        final ServletContext context = checkNotNull(e.getServletContext(),
            "Servlet context cannot be null, but it was null -- your Servlet " +
            "container seems to have broken a well established contract.");
        // The context global thread pool.
        final ListeningExecutorService threadPoolService =
            createNewListeningService(ThreadPool.SIZE, ThreadPool.NAME_FORMAT);
        // Core components: component mapping table, routing table, response
        // type mapping table, and method argument mapping table.
        final ComponentMappingTable mappingTable =
            new ComponentMappingTable(context).initializeAll();
        final ControllerRoutingTable routingTable =
            new ControllerRoutingTable(mappingTable);
        final ResponseTypeMappingHandlerTable handlerTable =
            new ResponseTypeMappingHandlerTable(mappingTable);
        final ControllerMethodArgumentMappingTable argumentTable =
            new ControllerMethodArgumentMappingTable(mappingTable);
        coreObjectMap_ = new CuracaoCoreObjectMap(
            // The Servlet context
            context,
            // The thread pool that handles requests and responses.
            threadPoolService,
            // Internal tables used for components, routing, request
            // and response handling.
            mappingTable, routingTable, handlerTable, argumentTable);
        // Attach the core object map to the context.  Will be consumed by
        // any Curacao dispatcher servlets.
        context.setAttribute(CONTEXT_KEY_CORE_OBJECT_MAP, coreObjectMap_);
    }

    @Override
    public final void contextDestroyed(final ServletContextEvent e) {
        // <https://github.com/markkolich/curacao/issues/6>
        // Only attempt to shutdown the thread pool and destroy components
        // if said entities are already initialized and non-null.
        if(coreObjectMap_.threadPoolService_ != null) {
            coreObjectMap_.threadPoolService_.shutdown();
        }
        if(coreObjectMap_.componentMappingTable_ != null) {
            coreObjectMap_.componentMappingTable_.destroyAll();
        }
    }

}
