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

import static com.kolich.curacao.CuracaoConfigLoader.getRequestPoolNameFormat;
import static com.kolich.curacao.CuracaoConfigLoader.getRequestPoolSize;
import static com.kolich.curacao.CuracaoConfigLoader.getResponsePoolNameFormat;
import static com.kolich.curacao.CuracaoConfigLoader.getResponsePoolSize;
import static com.kolich.curacao.util.AsyncServletThreadPoolFactory.createNewThreadPool;
import static org.slf4j.LoggerFactory.getLogger;

import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;

public abstract class CuracaoDispatcherServlet
	extends AbstractCuracaoServletBase {

	private static final long serialVersionUID = -3191215230966342034L;
	
	private static final Logger logger__ =
		getLogger(CuracaoDispatcherServlet.class);
	
	private interface RequestPool {
		public static final int SIZE = getRequestPoolSize();
		public static final String NAME_FORMAT = getRequestPoolNameFormat();
	}
	private interface ResponsePool {
		public static final int SIZE = getResponsePoolSize();
		public static final String NAME_FORMAT = getResponsePoolNameFormat();
	}
	
	/**
	 * Create a new instance that will build a default thread pool
	 * that will service a fixed maximum number of concurrent
	 * requests.
	 * @param maxConcurrentRequests
	 */
	public CuracaoDispatcherServlet(final Logger logger) {
		// Creates a thread pool that builds new threads as needed, but will
		// reuse previously constructed threads when they are available.
		super(logger,
			// Separate thread pools for requests and responses.
			createNewThreadPool(RequestPool.SIZE, RequestPool.NAME_FORMAT),
			createNewThreadPool(ResponsePool.SIZE, ResponsePool.NAME_FORMAT));
	}
	
	public CuracaoDispatcherServlet() {
		this(logger__);
	}
	
	@Override
	public void myInit(final ServletConfig servletConfig,
		final ServletContext context) throws ServletException {
		// Nothing, intentional.
	}

	@Override
	public void myDestroy() {
		// Nothing, intentional.
	}
	
	@Override
	public AsyncListener getAsyncListener() {
		return null;
	}

}
