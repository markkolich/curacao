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

package curacao;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import curacao.handlers.ReturnTypeMapperCallbackHandler;

import javax.annotation.Nonnull;
import javax.servlet.*;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.CuracaoContextListener.CuracaoCoreObjectMap.objectMapFromContext;

/**
 * The root Curacao dispatcher Servlet.
 *
 * This class is intentionally not declared final, and should be extended
 * if needed such that consumers can override the {@link #ready(ServletContext)} method herein.
 */
public class CuracaoDispatcherServlet extends GenericServlet {

	private static final long serialVersionUID = -3191215230966342034L;

    /**
     * A non-final, locally cached copy of the contexts global core object map.
     */
    private CuracaoContextListener.CuracaoCoreObjectMap coreObjectMap_;

    @Override
    public final void init(final ServletConfig config) throws ServletException {
        // Extract the core object map from the underlying context.  It cannot be null.
        // If it is null, likely the consumer didn't add a proper servlet context listener
        // to their configuration, and as a result, not core object map was bound to the context.
        coreObjectMap_ = checkNotNull(objectMapFromContext(config.getServletContext()),
            "No Curacao core object map was attached to context. Curacao Servlet context " +
            "listener not defined in `web.xml`?");
        // Invoke the ready method right before this servlet will put into service to handle requests.
        // This is essentially the last place custom handlers and other code can be invoked before the
        // servlet container starts sending traffic through this servlet.
        ready(coreObjectMap_.servletCtx_);
    }

    /**
     * Override if needed.
     *
     * This method is invoked immediately before the servlet container will start sending
     * traffic to this servlet.
     *
     * @param context the servlet context behind this web-application
     */
    public void ready(final ServletContext context) throws ServletException {
        // Noop
    }

    /**
     * Override if needed.
     *
     * Called by the servlet container to indicate to a servlet that the servlet is being taken
     * out of service.  See {@link Servlet#destroy}.
     */
    @Override
    public void destroy() {
        // Noop
    }

    @Override
    public final void service(final ServletRequest request,
                              final ServletResponse response) {
        // Establish a new async context for the incoming request.
        final AsyncContext asyncCtx = request.startAsync(request, response);
        // Establish a new curacao context for the incoming request.
        final CuracaoContext ctx = new CuracaoContext(coreObjectMap_, asyncCtx);
        // Instantiate a new callback handler for this request context. NOTE: This has to come first before
        // we submit the context to the thread pool for processing, because in the init/constructor path
        // we attach an async listener to the async context, and we want that to be attached before we start
        // processing the request.
        final FutureCallback<Object> callback = getCallbackHandlerForContext(ctx);
        // Instantiate a new controller invoker, which is a callable for our master thread pool.
        final Callable<Object> callable = new CuracaoControllerInvoker(ctx);
        // Submit the request to the thread pool for processing.
        final ListenableFuture<Object> future = coreObjectMap_.threadPoolService_.submit(callable);
        // Bind a callback to the returned Future<?>, such that when it completes the "callback handler" will be
        // called to deal with the result.  Note that the future may complete successfully, or in failure, and both
        // cases are handled here.  The response will be processed using a thread from the thread pool.
        Futures.addCallback(future, callback, coreObjectMap_.threadPoolService_);
        // At this point, the Servlet container detaches and its container thread that got us here is released
        // to do additional work.
    }

    /**
     * Override if needed to return a custom {@link FutureCallback}, which can be helpful/handy
     * for custom request/response lifecycle processing.  By default, this method returns the
     * Curacao global {@link ReturnTypeMapperCallbackHandler}.
     *
     * @param ctx the {@link CuracaoContext} tied to the request
     * @return a non-null {@link FutureCallback}
     */
    @Nonnull
    public FutureCallback<Object> getCallbackHandlerForContext(@Nonnull final CuracaoContext ctx) {
        return new ReturnTypeMapperCallbackHandler(ctx);
    }

}
