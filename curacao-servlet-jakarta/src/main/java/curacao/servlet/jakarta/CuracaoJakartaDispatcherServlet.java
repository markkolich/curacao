/*
 * Copyright (c) 2023 Mark S. Kolich
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

package curacao.servlet.jakarta;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import curacao.CuracaoConfig;
import curacao.context.CuracaoContext;
import curacao.context.CuracaoRequestContext;
import curacao.core.CuracaoControllerInvoker;
import curacao.core.CuracaoCoreObjectMap;
import curacao.handlers.ReturnTypeMapperCallbackHandler;
import jakarta.servlet.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.core.CuracaoCoreObjectMap.getObjectMapFromContext;

public class CuracaoJakartaDispatcherServlet extends GenericServlet {

    private CuracaoCoreObjectMap coreObjectMap_;

    @Override
    public final void init(
            final ServletConfig config) throws ServletException {
        final ServletContext servletContext =
                checkNotNull(config.getServletContext(), "Servlet context cannot be null.");

        final curacao.core.servlet.ServletContext curacaoServletContext =
                new JakartaServletContext(servletContext);

        final CuracaoCoreObjectMap coreObjectMap = getObjectMapFromContext(curacaoServletContext);
        coreObjectMap_ = checkNotNull(coreObjectMap, "No Curacao core object map was "
                + "attached to context. Curacao Servlet context listener not defined in web.xml?");

        start(coreObjectMap_.servletCtx_);
    }

    @Override
    public final void destroy() {
        stop(coreObjectMap_.servletCtx_);
    }

    @Override
    public final void service(
            final ServletRequest request,
            final ServletResponse response) throws ServletException, IOException {
        final AsyncContext asyncContext = request.startAsync(request, response);

        final CuracaoContext ctx =
                new CuracaoRequestContext(coreObjectMap_, new JakartaAsyncContext(asyncContext));

        final Callable<Object> callable = getRequestCallableForContext(ctx);
        final FutureCallback<Object> callback = getResponseCallbackForContext(ctx);

        final long asyncContextTimeoutMs = CuracaoConfig.getAsyncContextTimeoutMs();
        asyncContext.setTimeout(asyncContextTimeoutMs);
        asyncContext.addListener(new CuracaoJakartaAsyncListener(ctx, callback));

        final ListenableFuture<Object> future = coreObjectMap_.executorService_.submit(callable);
        Futures.addCallback(future, callback, coreObjectMap_.executorService_);
    }

    /**
     * Override if needed.
     * <p>
     * This method is invoked immediately before the servlet container will start sending
     * traffic to this servlet, and after all Curacao specific initialization has finished.
     *
     * @param context the servlet context of this web-application
     */
    public void start(
            final curacao.core.servlet.ServletContext context) throws ServletException {
        // Noop
    }

    /**
     * Override if needed.
     * <p>
     * This method is invoked that the servlet is being taken out of service.
     *
     * @param context the servlet context of this web-application
     */
    public void stop(
            final curacao.core.servlet.ServletContext context) {
        // Noop
    }

    /**
     * Override if needed to return a custom {@link Callable} which is run in the context of
     * Curacao's thread pool. This can be useful for attaching or injecting thread locals and
     * other thread specific objects that can be accessed during the typical request/response
     * processing lifecycle.
     * <p>
     * By default, this method returns the Curacao global {@link CuracaoControllerInvoker}.
     * <p>
     * Note that this method is invoked in the context of a servlet container thread, not a thread
     * owned/managed by Curacao.
     *
     * @param ctx the {@link CuracaoContext} tied to the request
     * @return a non-null {@link Callable}
     */
    @Nonnull
    public Callable<Object> getRequestCallableForContext(
            @Nonnull final CuracaoContext ctx) {
        return new CuracaoControllerInvoker(ctx);
    }

    /**
     * Override if needed to return a custom {@link FutureCallback}, which can be helpful/handy
     * for custom request/response processing lifecycle.
     * <p>
     * By default, this method returns the Curacao global {@link ReturnTypeMapperCallbackHandler}.
     * <p>
     * Note that this method is invoked in the context of a servlet container thread, not a thread
     * owned/managed by Curacao.
     *
     * @param ctx the {@link CuracaoContext} tied to the request
     * @return a non-null {@link FutureCallback}
     */
    @Nonnull
    public FutureCallback<Object> getResponseCallbackForContext(
            @Nonnull final CuracaoContext ctx) {
        return new ReturnTypeMapperCallbackHandler(ctx);
    }

}
