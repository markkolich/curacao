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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.kolich.curacao.CuracaoContextListener.CuracaoCoreObjectMap;
import com.kolich.curacao.handlers.requests.CuracaoContext;
import com.kolich.curacao.handlers.requests.CuracaoControllerInvoker;
import com.kolich.curacao.handlers.responses.MappingResponseTypeCallbackHandler;

import javax.servlet.*;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.kolich.curacao.CuracaoContextListener.CuracaoCoreObjectMap.objectMapFromContext;

/*package private*/
abstract class AbstractCuracaoServletBase extends GenericServlet {

    private static final long serialVersionUID = -4453673037534924911L;

    /**
     * A non-final, locally cached copy of the contexts global core
     * object map.
     */
    private CuracaoCoreObjectMap coreObjectMap_;

	@Override
	public final void init(final ServletConfig config) throws ServletException {
        // Extract the core object map from the underlying context.  It cannot
        // be null.  If it is null, likely the consumer didn't add a proper
        // servlet context listener to their configuration, and as a result,
        // not core object map was bound to the context.
        coreObjectMap_ = checkNotNull(objectMapFromContext(
           config.getServletContext()), "No Curacao core object map was " +
            "attached to context. Curacao Servlet context listener not " +
            "defined in 'web.xml'?");
		myInit(config, coreObjectMap_.context_);
	}

	@Override
	public final void destroy() {
		myDestroy();
	}

	/**
	 * Called when this Servlet instance is being initialized.  This method
     * is called after the library has started and initialized its own internal
     * pools and resources.
	 * @param servletConfig the {@link ServletConfig} tied to this Servlet
	 * @param context the underlying {@link ServletContext} of the Servlet
	 */
	public abstract void myInit(final ServletConfig servletConfig,
                                final ServletContext context) throws ServletException;

    /**
     * Called when this Servlet instance is shutting down.  This method
     * is called after the library has shut down its internal pools and
     * other resources.
     */
	public abstract void myDestroy();

	@Override
	public final void service(final ServletRequest request,
                              final ServletResponse response) {
		// Establish a new async context for the incoming request.
		final AsyncContext asyncCtx = request.startAsync(request, response);
        // Establish a new curacao context for the incoming request.
        final CuracaoContext ctx = new CuracaoContext(
            // The Curacao internal core object map
            coreObjectMap_,
            // New async context from the container
            asyncCtx);
		// Instantiate a new callback handler for this request context.
        // NOTE: This has to come first before we submit the context to the
        // thread pool for processing, because in the init/constructor path
        // we attach an async listener to the async context, and we want that
        // to be attached before we start processing the request.
		final FutureCallback<Object> callback =
			new MappingResponseTypeCallbackHandler(ctx);
		// Instantiate a new controller invoker, which is a callable for our
        // master thread pool.
        final Callable<Object> callable = new CuracaoControllerInvoker(ctx);
        // Submit the request to the thread pool for processing.
		final ListenableFuture<Object> future =
            coreObjectMap_.threadPoolService_.submit(callable);
		// Bind a callback to the returned Future<?>, such that when it
		// completes the "callback handler" will be called to deal with the
		// result.  Note that the future may complete successfully, or in
		// failure, and both cases are handled here.  The response will be
		// processed using a thread from the thread pool.
		addCallback(future, callback, coreObjectMap_.threadPoolService_);
		// At this point, the Servlet container detaches and its container
		// thread that got us here is released to do additional work.
	}

}
