/**
 * Copyright (c) 2013 Mark S. Kolich
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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.kolich.curacao.handlers.components.ComponentMappingTable;
import com.kolich.curacao.handlers.requests.ControllerMethodArgumentMappingTable;
import com.kolich.curacao.handlers.requests.ControllerRoutingTable;
import com.kolich.curacao.handlers.requests.CuracaoControllerInvoker;
import com.kolich.curacao.handlers.responses.MappingResponseTypeCallbackHandler;
import com.kolich.curacao.handlers.responses.ResponseTypeMappingHandlerTable;
import org.slf4j.Logger;

import javax.servlet.*;

import static com.google.common.util.concurrent.Futures.addCallback;
import static com.kolich.curacao.CuracaoConfigLoader.*;
import static com.kolich.curacao.util.AsyncServletExecutorServiceFactory.createNewListeningService;
import static org.slf4j.LoggerFactory.getLogger;

/*package private*/
abstract class AbstractCuracaoServletBase extends GenericServlet {

    private static final long serialVersionUID = -4453673037534924911L;
	
	private static final Logger logger__ =
		getLogger(AbstractCuracaoServletBase.class);

	// Preloading goodness.  If the web-application has overridden these
	// properties and they are set to true, then we need to preload the routes
	// (controllers), mapping response handlers (mappers), and controller
    // argument mapping table.
	static {
		if(CuracaoConfigLoader.shouldPreloadRoutes()) {
			ControllerRoutingTable.preload();
		}
		if(CuracaoConfigLoader.shouldPreloadResponseMappingHandlers()) {
			ResponseTypeMappingHandlerTable.preload();
		}
		if(CuracaoConfigLoader.shouldPreloadControllerArgumentMappers()) {
			ControllerMethodArgumentMappingTable.preload();
		}
	}
	
	private interface RequestPool {
		public static final int SIZE = getRequestPoolSize();
		public static final String NAME_FORMAT = getRequestPoolNameFormat();
	}
	private interface ResponsePool {
		public static final int SIZE = getResponsePoolSize();
		public static final String NAME_FORMAT = getResponsePoolNameFormat();
	}
	
	// NOTE: Two separate thread pools are established, one for handling
	// incoming requests and another for handling (rendering) outgoing
	// responses.  This is by design.
	private ListeningExecutorService requestPool_;
	private ListeningExecutorService responsePool_;

    /**
     * A local cache of the Servlet context.
     */
    private ServletContext sContext_;
	
	@Override
	public final void init(final ServletConfig servletConfig)
		throws ServletException {
        // Establish a local cache of the Servlet context of this application
        // within the Servlet container.  This is to work around an asinine
        // bug in Jetty where a race condition prevents the container from
        // setting the Servlet "context" attached to the request until some
        // internal event fires.  That's bullshit, we need the context
        // immediately so we fetch it here when we know it's available and
        // cache it for the life of the application.
        sContext_ = servletConfig.getServletContext();
		requestPool_ = createNewListeningService(RequestPool.SIZE,
			RequestPool.NAME_FORMAT);
		responsePool_ = createNewListeningService(ResponsePool.SIZE,
			ResponsePool.NAME_FORMAT);
        // Build the component mapping table and initialize each reflection
        // discovered component in the boot package.  This is always done by
        // default and is not configurable via a config property.
        ComponentMappingTable.initializeAll(sContext_);
		myInit(servletConfig, sContext_);
	}
	
	@Override
	public final void destroy() {
		requestPool_.shutdown();
		responsePool_.shutdown();
		// Call destroy on each reflection discovered component.
		ComponentMappingTable.destroyAll(sContext_);
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
			new MappingResponseTypeCallbackHandler(context);
		// Submit the request to the request thread pool for processing.
		final ListenableFuture<Object> future = requestPool_.submit(
			new CuracaoControllerInvoker(logger__, sContext_, context));
		// Bind a callback to the returned Future<?>, such that when it
		// completes the "callback handler" will be called to deal with the
		// result.  Note that the future may complete successfully, or in
		// failure, and both cases are handled here.  The response will be
		// processed using a thread from the response thread pool.
		addCallback(future, callback, responsePool_);
		// At this point, the Servlet container detaches and is container
		// thread that got us here detaches.
	}
	
}
