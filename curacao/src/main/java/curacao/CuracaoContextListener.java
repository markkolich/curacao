/*
 * Copyright (c) 2019 Mark S. Kolich
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

package curacao;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import curacao.annotations.RequestMapping;
import curacao.components.ComponentTable;
import curacao.mappers.MapperTable;
import curacao.mappers.request.ControllerArgumentMapper;
import curacao.mappers.request.RequestMappingTable;
import curacao.mappers.response.ControllerReturnTypeMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.CuracaoConfigLoader.getThreadPoolNameFormat;
import static curacao.CuracaoConfigLoader.getThreadPoolSize;
import static curacao.CuracaoContextListener.CuracaoCoreObjectMap.CONTEXT_KEY_CORE_OBJECT_MAP;
import static curacao.util.AsyncServletExecutorServiceFactory.createNewListeningService;

public final class CuracaoContextListener implements ServletContextListener {

    private interface ThreadPool {
        int SIZE = getThreadPoolSize();
        String NAME_FORMAT = getThreadPoolNameFormat();
    }

    /**
     * The core object map is an object that acts as a context global holder
     * of core objects used by Curacao internally.  This includes the routing
     * table, request and response thread pools and the component mapping table,
     * among others.  On context startup/initialization, the core object map is
     * attached to the context as an attribute such that these immutable and
     * global objects can be consumed/shared by Servlets in the context as
     * needed.
     */
    public static final class CuracaoCoreObjectMap {

        public static final String CONTEXT_KEY_CORE_OBJECT_MAP = "curacao.core.object-map";

        public final ServletContext servletCtx_;

        public final ListeningExecutorService threadPoolService_;

        public final ComponentTable componentTable_;
        public final RequestMappingTable requestMappingTable_;
        public final MapperTable mapperTable_;

        public CuracaoCoreObjectMap(final ServletContext servletCtx,
                                    final ListeningExecutorService threadPoolService,
                                    final ComponentTable componentTable,
                                    final RequestMappingTable requestMappingTable,
                                    final MapperTable mapperTable) {
            servletCtx_ = checkNotNull(servletCtx, "Servlet context cannot be null.");
            threadPoolService_ = checkNotNull(threadPoolService, "Thread pool service cannot be null.");
            componentTable_ = checkNotNull(componentTable, "Mapper table cannot be null.");
            requestMappingTable_ = checkNotNull(requestMappingTable, "Request mapping routing table cannot be null.");
            mapperTable_ = checkNotNull(mapperTable, "Mapper table cannot be null.");
        }

        @Nullable
        public static final CuracaoCoreObjectMap objectMapFromContext(@Nonnull final ServletContext context) {
            checkNotNull(context, "Servlet context cannot be null.");
            return (CuracaoCoreObjectMap)context.getAttribute(CONTEXT_KEY_CORE_OBJECT_MAP);
        }

        @Nullable
        @SuppressWarnings("unchecked") // intentional & safe
        public static final <T> T componentFromContext(
                @Nonnull final ServletContext servletContext,
                @Nonnull final Class<T> clazz) {
            final CuracaoCoreObjectMap coreObjectMap = objectMapFromContext(servletContext);
            checkNotNull(coreObjectMap, "Curacao core object map should not be null; context not initialized?");

            return (T) coreObjectMap.componentTable_.getComponentForType(clazz);
        }

        @Nonnull
        public static final ImmutableList<CuracaoInvokable> routesFromContext(
                @Nonnull final ServletContext servletContext,
                @Nonnull final RequestMapping.Method method) {
            final CuracaoCoreObjectMap coreObjectMap = objectMapFromContext(servletContext);
            checkNotNull(coreObjectMap, "Curacao core object map should not be null; context not initialized?");

            return coreObjectMap.requestMappingTable_.getRoutesByHttpMethod(method);
        }

        @Nonnull
        public static final Collection<ControllerArgumentMapper<?>> argumentMappersFromContext(
                @Nonnull final ServletContext servletContext,
                @Nonnull final Class<?> clazz) {
            final CuracaoCoreObjectMap coreObjectMap = objectMapFromContext(servletContext);
            checkNotNull(coreObjectMap, "Curacao core object map should not be null; context not initialized?");

            return coreObjectMap.mapperTable_.getArgumentMappersForClass(clazz);
        }

        @Nullable
        public static final ControllerReturnTypeMapper<?> returnTypeMappersFromContext(
                @Nonnull final ServletContext servletContext,
                @Nonnull final Class<?> clazz) {
            final CuracaoCoreObjectMap coreObjectMap = objectMapFromContext(servletContext);
            checkNotNull(coreObjectMap, "Curacao core object map should not be null; context not initialized?");

            return coreObjectMap.mapperTable_.getReturnTypeMapperForClass(clazz);
        }

    }

    /**
     * A non-final, locally cached copy of the contexts global core object map.
     */
    private CuracaoCoreObjectMap coreObjectMap_;

    @Override
    public final void contextInitialized(final ServletContextEvent e) {
        // Ensure that the Servlet container is being honest, and returns
        // a valid non-null context attached to the event.
        final ServletContext context = checkNotNull(e.getServletContext(),
            "Servlet context cannot be null, but it was null -- your Servlet " +
            "container appears to have broken a well established contract.");
        // The context global thread pool.
        final ListeningExecutorService threadPoolService =
            createNewListeningService(ThreadPool.SIZE, ThreadPool.NAME_FORMAT);
        // Core components: component mapping table, routing table, response
        // type mapping table, and method argument mapping table.
        final ComponentTable componentTable = new ComponentTable(context).initializeAll();
        final RequestMappingTable requestMappingTable = new RequestMappingTable(componentTable);
        final MapperTable mapperTable = new MapperTable(componentTable);
        coreObjectMap_ = new CuracaoCoreObjectMap(
            // The servlet context.
            context,
            // The thread pool that handles requests and responses.
            threadPoolService,
            // Internal tables used for components, routing, request and response handling.
            componentTable,
            requestMappingTable,
            mapperTable);
        // Attach the core object map to the context.  Will be consumed by any Curacao dispatcher servlets.
        context.setAttribute(CONTEXT_KEY_CORE_OBJECT_MAP, coreObjectMap_);
    }

    @Override
    public final void contextDestroyed(final ServletContextEvent e) {
        // https://github.com/markkolich/curacao/issues/6
        // Only attempt to shutdown the thread pool and destroy components
        // if said entities are already initialized and non-null.
        if (coreObjectMap_ != null) {
            if (coreObjectMap_.threadPoolService_ != null) {
                coreObjectMap_.threadPoolService_.shutdown();
            }
            if (coreObjectMap_.componentTable_ != null) {
                coreObjectMap_.componentTable_.destroyAll();
            }
        }
    }

}
