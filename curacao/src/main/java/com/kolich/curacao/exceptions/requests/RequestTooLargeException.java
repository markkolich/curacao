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

package com.kolich.curacao.exceptions.requests;

import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;

import com.kolich.curacao.exceptions.CuracaoException;

public final class RequestTooLargeException
	extends CuracaoException.WithStatus {

	private static final long serialVersionUID = -5156790077603958124L;

	public RequestTooLargeException(final String message,
		final Exception cause) {
		super(SC_REQUEST_ENTITY_TOO_LARGE, message, cause);
	}
	
	public RequestTooLargeException(final String message) {
		this(message, null);
	}
	
	public RequestTooLargeException() {
		this(null);
	}

}
