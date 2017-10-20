/*
 * Copyright (c) 2017 Mark S. Kolich
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

package curacao.examples.controllers;

import curacao.annotations.Controller;
import curacao.annotations.RequestMapping;
import curacao.entities.CuracaoEntity;
import curacao.entities.mediatype.document.TextPlainCuracaoEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.WWW_AUTHENTICATE;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Controller
public final class BasicAuthExampleController {
	
	private static final String REALM = "BasicAuthExampleController";
	
	private static abstract class BasicAuthClosure<T extends CuracaoEntity> {
		
		public abstract T authorized();
		public abstract T unauthorized();
		
		private final HttpServletRequest request_;
		private final HttpServletResponse response_;
		private final String realm_;
		
		public BasicAuthClosure(final HttpServletRequest request,
			final HttpServletResponse response, final String realm) {
			request_ = request;
			response_ = response;
			realm_ = realm;
		}
		
		public final T execute() {
			final String authorization = request_.getHeader(AUTHORIZATION);
			if (authorization == null) {
				response_.addHeader(WWW_AUTHENTICATE, String.format("Basic realm=\"%s\"", realm_));
				return unauthorized();
			} else {
				return authorized();
			}
		}
		
	}

	@RequestMapping("^\\/api\\/secure$")
	public final CuracaoEntity basicAuth(final HttpServletRequest request,
		final HttpServletResponse response) {
		return new BasicAuthClosure<CuracaoEntity>(request, response, REALM) {
			@Override
			public final CuracaoEntity authorized() {
				return new TextPlainCuracaoEntity("It worked!");
			}
			@Override
			public final CuracaoEntity unauthorized() {
				return new TextPlainCuracaoEntity(SC_UNAUTHORIZED,
					"Oops, unauthorized.");
			}
		}.execute();
	}

}
