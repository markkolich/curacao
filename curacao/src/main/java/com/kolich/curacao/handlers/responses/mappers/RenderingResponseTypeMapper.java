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

package com.kolich.curacao.handlers.responses.mappers;

import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import com.kolich.curacao.entities.CuracaoEntity;
import com.kolich.curacao.entities.empty.StatusCodeOnlyCuracaoEntity;

public abstract class RenderingResponseTypeMapper<T> {
	
	@SuppressWarnings("unchecked")
	public final void renderObject(final AsyncContext context,
		final HttpServletResponse response, @Nonnull final Object obj)
		throws Exception {
		// Meh, total shim to coerce the incoming 'obj' of type Object
		// into an object of type T.
		render(context, response, (T)obj);
	}
	
	public abstract void render(final AsyncContext context,
		final HttpServletResponse response, @Nonnull final T entity)
			throws Exception;
	
	protected static final void renderEntity(final HttpServletResponse response,
		final int statusCode) throws Exception {
		renderEntity(response,
			// Renders an empty response body with the right HTTP
			// response (status) code.
			new StatusCodeOnlyCuracaoEntity(statusCode));
	}
	
	protected static final void renderEntity(final HttpServletResponse response,
		final CuracaoEntity entity) throws Exception {
		try(final OutputStream os = response.getOutputStream()) {
			response.setStatus(entity.getStatus());
			final String contentType;
			if((contentType = entity.getContentType()) != null) {
				response.setContentType(contentType);
			}
			entity.write(os);
		}
	}

}
