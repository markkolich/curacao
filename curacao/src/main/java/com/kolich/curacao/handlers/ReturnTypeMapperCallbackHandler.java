/**
 * Copyright (c) 2015 Mark S. Kolich
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

import com.kolich.curacao.CuracaoContext;
import com.kolich.curacao.mappers.response.ControllerReturnTypeMapper;
import org.slf4j.Logger;

import javax.annotation.Nonnull;

import static org.slf4j.LoggerFactory.getLogger;

public final class ReturnTypeMapperCallbackHandler
	extends AbstractContextCompletingCallbackHandler {
	
	private static final Logger logger__ =
		getLogger(ReturnTypeMapperCallbackHandler.class);
	
	public ReturnTypeMapperCallbackHandler(@Nonnull final CuracaoContext ctx) {
		super(ctx);
	}

	@Override
	public final void renderSuccess(@Nonnull final Object result) throws Exception {
		if (logger__.isDebugEnabled()) {
			logger__.debug("In 'renderSuccess' handler callback, ready " +
				"to lookup response handler for type: {}",
				result.getClass().getCanonicalName());
		}
		lookupAndRender(result);
	}

	@Override
	public final void renderFailure(@Nonnull final Throwable t) throws Exception {
		if (logger__.isDebugEnabled()) {
			logger__.debug("In 'renderFailure' handler callback, ready " +
				"to lookup response handler for throwable type: {}",
				t.getClass().getCanonicalName());
		}
		logger__.warn("Failure occurred, handling exception.", t);
		lookupAndRender(t);
	}
	
	private final void lookupAndRender(@Nonnull final Object result) throws Exception {
		final ControllerReturnTypeMapper<?> handler;
		if ((handler = ctx_.mapperTable_.getHandlerForType(result.getClass())) != null) {
			handler.renderObject(ctx_.asyncCtx_, ctx_.response_, result);
		} else {
			// This should never happen!  The contract of the response
			// type mapper table is that even if the mapper table doesn't
			// contain an exact match for a given class it should return
			// ~something~ non-null (usually just a vanilla/generic response
			// handler that will take the response object and simply call
			// Object.toString() on it).
			logger__.error("Cannot render response, failed to find a type " +
				"specific callback handler for type: {}",
				result.getClass().getCanonicalName());
		}
	}

}
