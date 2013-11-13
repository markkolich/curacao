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

package com.kolich.curacao.handlers;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.InvocationTargetException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.google.common.util.concurrent.FutureCallback;

public abstract class AbstractFutureCallbackHandler
	implements FutureCallback<Object> {
	
	private static final Logger logger__ =
		getLogger(AbstractFutureCallbackHandler.class);
		
	protected final AsyncContext context_;
	
	protected final HttpServletRequest request_;
	protected final HttpServletResponse response_;
	
	public AbstractFutureCallbackHandler(final AsyncContext context) {
		checkNotNull(context, "Async context cannot be null.");
		context_ = context;
		// Derived properties below. 
		request_ = (HttpServletRequest)context_.getRequest();
		response_ = (HttpServletResponse)context_.getResponse();
	}
	
	@Override
	public final void onSuccess(final Object result) {
		try {
			// Only attempt to lookup a response "handler" for the resulting
			// object if the invoked controller method returned an actual
			// non-null value.  In the case where the controller returned
			// null, the contract is that the controller is then responsible
			// for handling the entire response, including completing the
			// async context.
			if(result != null) {
				successAndComplete(result);
			}
		} catch (Exception e) {
			// There's very little that could be done at this point to
			// "salvage" the response going back to the client.  Even if we
			// did try and reset the HTTP response status code to indicate
			// error, that may fail given the renderer could have already set
			// the status and may have sent some data down to the client.
			// Based on the nature of HTTP, if an HTTP response code was sent
			// by the Servlet container followed by some data, it's impossible
			// for the renderer (or even this library) to go back and
			// reset/change the response code with the client once that status
			// has already been sent.
			logger__.error("Failed miserably to render 'success' " +
				"response. Abandoning...", e);
		}
	}
	
	@Override
	public final void onFailure(final Throwable t) {
		try {
			failureAndComplete(
				// In reflection land, when a reflection invoked method throws
				// an exception, it's inconveniently wrapped in a stupid
				// InvocationTargetException.  So, before we call the real
				// failure handler we unwrap the "real" exception from within
				// the passed throwable.
				(t != null && t instanceof InvocationTargetException) ?
					t.getCause() : t);
		} catch (Exception e) {
			// There's very little that could be done at this point to
			// "salvage" the response going back to the client.  Even if we
			// did try and reset the HTTP response status code to indicate
			// error, that may fail given the renderer could have already set
			// the status and may have sent some data down to the client.
			// Based on the nature of HTTP, if an HTTP response code was sent
			// by the Servlet container followed by some data, it's impossible
			// for the renderer (or even this library) to go back and
			// reset/change the response code with the client once that status
			// has already been sent.
			logger__.error("Failed miserably to render 'failure' " +
				"response. Abandoning...", e);
		}
	}
		
	public abstract void successAndComplete(final Object result)
		throws Exception;
	
	public abstract void failureAndComplete(final Throwable t)
		throws Exception;

}