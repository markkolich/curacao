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
import com.kolich.curacao.handlers.requests.CuracaoControllerInvoker;
import com.kolich.curacao.handlers.responses.MappingResponseTypeCallbackHandler;
import org.slf4j.Logger;

import javax.servlet.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.kolich.curacao.CuracaoContextListener.CuracaoCoreObjectMap.objectMapFromContext;
import static org.slf4j.LoggerFactory.getLogger;

/*package private*/
abstract class AbstractCuracaoServletBase extends GenericServlet {

    private static final long serialVersionUID = -4453673037534924911L;

	private static final Logger logger__ =
		getLogger(AbstractCuracaoServletBase.class);

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
		final AsyncContext context = request.startAsync(request, response);
		// Instantiate a new callback handler for this request context.
		final FutureCallback<Object> callback =
			new MappingResponseTypeCallbackHandler(context,
                coreObjectMap_.responseHandlerTable_);
		// Submit the request to the request thread pool for processing.
		final ListenableFuture<Object> future =
            coreObjectMap_.requestPool_.submit(
			    new CuracaoControllerInvoker(logger__,
                    // New async context of the request
                    context,
                    // Global Servlet context
                    coreObjectMap_.context_,
                    // Controller routing table
                    coreObjectMap_.routingTable_,
                    // Controller method argument mapping table
                    coreObjectMap_.methodArgTable_));
		// Bind a callback to the returned Future<?>, such that when it
		// completes the "callback handler" will be called to deal with the
		// result.  Note that the future may complete successfully, or in
		// failure, and both cases are handled here.  The response will be
		// processed using a thread from the response thread pool.
		addCallback(future, callback, coreObjectMap_.responsePool_);
		// At this point, the Servlet container detaches and is container
		// thread that got us here detaches.
	}

}
