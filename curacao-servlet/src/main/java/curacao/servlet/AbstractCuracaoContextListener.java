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

package curacao.servlet;

import com.google.common.util.concurrent.ListeningExecutorService;
import curacao.components.ComponentTable;
import curacao.core.CuracaoCoreObjectMap;
import curacao.mappers.MapperTable;
import curacao.mappers.request.RequestMappingTable;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.CuracaoConfig.getThreadPoolNameFormat;
import static curacao.CuracaoConfig.getThreadPoolSize;
import static curacao.core.CuracaoCoreObjectMap.CONTEXT_KEY_CORE_OBJECT_MAP;
import static curacao.util.AsyncExecutorServiceFactory.createNewListeningExecutorService;

public abstract class AbstractCuracaoContextListener {

    /**
     * A non-final, locally cached copy of the context global core object map.
     */
    protected CuracaoCoreObjectMap coreObjectMap_;

    protected final void initializeCuracaoContext(
            final curacao.core.servlet.ServletContext curacaoServletContext) {
        checkNotNull(curacaoServletContext, "Curacao servlet context cannot be null.");

        // The context global thread pool.
        final int threadPoolSize = getThreadPoolSize();
        final String threadPoolNameFormat = getThreadPoolNameFormat();
        final ListeningExecutorService executorService =
                createNewListeningExecutorService(threadPoolSize, threadPoolNameFormat);

        // Core components: component mapping table, routing table, response
        // type mapping table, and method argument mapping table.
        final ComponentTable componentTable = new ComponentTable(curacaoServletContext).initializeAll();
        final RequestMappingTable requestMappingTable = new RequestMappingTable(componentTable);
        final MapperTable mapperTable = new MapperTable(componentTable);

        coreObjectMap_ = new CuracaoCoreObjectMap(
                // The Curacao servlet context.
                curacaoServletContext,
                // The thread pool that handles request and response processing.
                executorService,
                // Internal tables used for components, routing, request and response handling.
                componentTable,
                requestMappingTable,
                mapperTable);

        // Attach the core object map to the context. Will be consumed by any Curacao dispatcher servlets.
        curacaoServletContext.setAttribute(CONTEXT_KEY_CORE_OBJECT_MAP, coreObjectMap_);
    }

    protected final void destroyCuracaoContext(
            final curacao.core.servlet.ServletContext curacaoServletContext) {
        // https://github.com/markkolich/curacao/issues/6
        // Only attempt to shutdown the thread pool and destroy components
        // if said entities are already initialized and non-null.
        if (coreObjectMap_ != null) {
            if (coreObjectMap_.executorService_ != null) {
                coreObjectMap_.executorService_.shutdown();
            }
            if (coreObjectMap_.componentTable_ != null) {
                coreObjectMap_.componentTable_.destroyAll();
            }
        }
    }

}
