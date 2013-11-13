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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.addCallback;

import java.util.concurrent.ExecutorService;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.kolich.curacao.handlers.requests.ControllerArgumentTypeMappingTable;
import com.kolich.curacao.handlers.requests.CuracaoControllerInvoker;
import com.kolich.curacao.handlers.requests.RequestRoutingTable;
import com.kolich.curacao.handlers.responses.MappingResponseTypeCallbackHandler;
import com.kolich.curacao.handlers.responses.ResponseTypeMappingHandlerTable;

/*package private*/
abstract class AbstractCuracaoServletBase extends GenericServlet {

	private static final long serialVersionUID = 8388599956708926598L;
	
	// Preloading goodness.  If the web-application has overridden these
	// properties and they are set to true, then we need to preload the routes
	// (controllers) and mapping response handlers (mappers).
	static {
		if(CuracaoConfigLoader.shouldPreloadRoutes()) {
			RequestRoutingTable.preload();
		}
		if(CuracaoConfigLoader.shouldPreloadResponseMappingHandlers()) {
			ResponseTypeMappingHandlerTable.preload();
		}
		if(CuracaoConfigLoader.shouldPreloadControllerArgumentMappers()) {
			ControllerArgumentTypeMappingTable.preload();
		}
	}
	
	private final Logger logger_;
	
	// NOTE: Two separate thread pools are established, one for handling
	// incoming requests and another for handling (rendering) outgoing
	// responses.  This is by design.  Of course, these are just "pointers"
	// so there's nothing stopping an extending implementation from using
	// a single thread pool for both.
	private final ListeningExecutorService requestPool_;
	private final ListeningExecutorService responsePool_;
		
	public AbstractCuracaoServletBase(final Logger logger,
		final ExecutorService requestPool, final ExecutorService responsePool) {
		checkNotNull(requestPool, "Executor service request thread pool " +
			"cannot be null.");
		checkNotNull(responsePool, "Executor service response thread pool " +
			"cannot be null.");
		logger_ = logger;
		requestPool_ = MoreExecutors.listeningDecorator(requestPool);
		responsePool_ = MoreExecutors.listeningDecorator(responsePool);
	}
	
	@Override
	public final void init(final ServletConfig servletConfig)
		throws ServletException {
		myInit(servletConfig, servletConfig.getServletContext());
	}
	
	@Override
	public final void destroy() {
		// Was a tough call placing the call to 'shutdown' here.  That is,
		// should we blindly shutdown the passed executor services when this
		// Servlet is being "destroyed"?  The alternative is to do nothing and
		// let the extending (child) class remember to shut them down in their
		// 'myDestroy' method implementation.  That felt risky.  So, in the
		// end, I opted to call 'shutdown' here on behalf of the extending
		// class.. was cleaner and just made more sense.  Ideally, prevents
		// leaks in the case where a Servlet is started+stopped repeatedly but
		// the extending class failed to remember call to 'shutdown' on their
		// executor services.  Note that calling shutdown() on a pool that is
		// already "shutdown" has no effect, and is basically a NO-OP.
		requestPool_.shutdown();
		responsePool_.shutdown();
		myDestroy();
	}
	
	/**
	 * Called when this Servlet instance is being initialized.
	 * @param servletConfig the {@link ServletConfig} tied to this Servlet
	 * @param context the underlying {@link ServletContext}
	 */
	public abstract void myInit(final ServletConfig servletConfig,
		final ServletContext context) throws ServletException;
	
	/**
	 * Do your cleanup and shutdown related business here.
	 */
	public abstract void myDestroy();
	
	@Override
	public final void service(final ServletRequest request,
		final ServletResponse response) {
		// Establish a new async context for the incoming request.
		final AsyncContext context = request.startAsync(request, response);
		// Instantiate a new callback handler for this request context.
		final FutureCallback<Object> callback = 
			new MappingResponseTypeCallbackHandler(context, getAsyncListener());
		// Submit the request to the request thread pool for processing.
		final ListenableFuture<Object> future = requestPool_.submit(
			new CuracaoControllerInvoker(logger_, context));
		// Bind a callback to the returned Future<?>, such that when it
		// completes the "callback handler" will be called to deal with the
		// result.  Note that the future may complete successfully, or in
		// failure, and both cases are handled here.  The response will be
		// processed using a thread from the response thread pool.  Could be
		// the same pool as the request thread pool, if desired.
		addCallback(future, callback, responsePool_);
		// At this point, the Servlet container detaches and is container
		// thread that got us here detaches.
	}
		
	public abstract AsyncListener getAsyncListener();
	
}
