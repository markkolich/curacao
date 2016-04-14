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

package curacao.examples.mappers;

import curacao.annotations.Mapper;
import curacao.entities.CuracaoEntity;
import curacao.entities.mediatype.document.TextPlainCuracaoEntity;
import curacao.exceptions.async.AsyncContextTimeoutException;
import curacao.mappers.response.ControllerReturnTypeMapper;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

@Mapper
public final class AsyncContextTimeoutExceptionMapper extends ControllerReturnTypeMapper<AsyncContextTimeoutException> {
	
	private static final CuracaoEntity TIMEOUT_ERROR = new TextPlainCuracaoEntity(
		SC_INTERNAL_SERVER_ERROR, "Async context timeout.");

	@Override
	public final void render(final AsyncContext context,
							 final HttpServletResponse response,
							 @Nonnull final AsyncContextTimeoutException entity) throws Exception {
		renderEntity(response, TIMEOUT_ERROR);
	}
	
}