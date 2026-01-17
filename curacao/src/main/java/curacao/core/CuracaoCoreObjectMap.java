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

package curacao.core;

import com.google.common.util.concurrent.ListeningExecutorService;
import curacao.annotations.RequestMapping;
import curacao.components.ComponentTable;
import curacao.core.servlet.ServletContext;
import curacao.mappers.MapperTable;
import curacao.mappers.request.AbstractControllerArgumentMapper;
import curacao.mappers.request.RequestMappingTable;
import curacao.mappers.response.AbstractControllerReturnTypeMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The core object map is an object that acts as a context global holder
 * of core objects used by Curacao internally. This includes the routing
 * table, request and response thread pools and the component mapping table,
 * among others. On servlet context startup/initialization, the core object
 * map is attached to the context as an attribute such that these immutable
 * and global objects can be consumed/shared by Servlets in the context as
 * needed.
 */
public final class CuracaoCoreObjectMap {

    public static final String CONTEXT_KEY_CORE_OBJECT_MAP = "curacao.core.object-map";

    public final ServletContext servletCtx_;

    public final ListeningExecutorService executorService_;

    public final ComponentTable componentTable_;
    public final RequestMappingTable requestMappingTable_;
    public final MapperTable mapperTable_;

    public CuracaoCoreObjectMap(
            final ServletContext servletCtx,
            final ListeningExecutorService executorService,
            final ComponentTable componentTable,
            final RequestMappingTable requestMappingTable,
            final MapperTable mapperTable) {
        servletCtx_ = checkNotNull(servletCtx, "Servlet context cannot be null.");
        executorService_ = checkNotNull(executorService, "Executor service cannot be null.");
        componentTable_ = checkNotNull(componentTable, "Mapper table cannot be null.");
        requestMappingTable_ = checkNotNull(requestMappingTable, "Request mapping routing table cannot be null.");
        mapperTable_ = checkNotNull(mapperTable, "Mapper table cannot be null.");
    }

    // Helpers

    @Nullable
    public static CuracaoCoreObjectMap getObjectMapFromContext(
            @Nonnull final ServletContext context) {
        checkNotNull(context, "Servlet context cannot be null.");
        return context.getAttribute(CONTEXT_KEY_CORE_OBJECT_MAP);
    }

    @Nullable
    @SuppressWarnings("unchecked") // intentional & safe
    public static <T> T getComponentFromContext(
            @Nonnull final ServletContext servletContext,
            @Nonnull final Class<T> clazz) {
        final CuracaoCoreObjectMap coreObjectMap = getObjectMapFromContext(servletContext);
        checkNotNull(coreObjectMap, "Curacao core object map should not be null; context not initialized?");

        return (T) coreObjectMap.componentTable_.getComponentForType(clazz);
    }

    @Nonnull
    public static List<CuracaoInvokable> getRoutesFromContext(
            @Nonnull final ServletContext servletContext,
            @Nonnull final RequestMapping.Method method) {
        final CuracaoCoreObjectMap coreObjectMap = getObjectMapFromContext(servletContext);
        checkNotNull(coreObjectMap, "Curacao core object map should not be null; context not initialized?");

        return coreObjectMap.requestMappingTable_.getRoutesByHttpMethod(method);
    }

    @Nonnull
    public static Collection<AbstractControllerArgumentMapper<?>> getArgumentMappersFromContext(
            @Nonnull final ServletContext servletContext,
            @Nonnull final Class<?> clazz) {
        final CuracaoCoreObjectMap coreObjectMap = getObjectMapFromContext(servletContext);
        checkNotNull(coreObjectMap, "Curacao core object map should not be null; context not initialized?");

        return coreObjectMap.mapperTable_.getArgumentMappersForClass(clazz);
    }

    @Nullable
    public static AbstractControllerReturnTypeMapper<?> getReturnTypeMappersFromContext(
            @Nonnull final ServletContext servletContext,
            @Nonnull final Class<?> clazz) {
        final CuracaoCoreObjectMap coreObjectMap = getObjectMapFromContext(servletContext);
        checkNotNull(coreObjectMap, "Curacao core object map should not be null; context not initialized?");

        return coreObjectMap.mapperTable_.getReturnTypeMapperForClass(clazz);
    }

}
