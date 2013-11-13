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

package com.kolich.curacao.exceptions;

import com.kolich.common.KolichCommonException;
import com.kolich.curacao.entities.CuracaoEntity;
import com.kolich.curacao.entities.empty.StatusCodeOnlyCuracaoEntity;

public class CuracaoException extends KolichCommonException {

	private static final long serialVersionUID = 1303687433405348365L;

	public CuracaoException(final String message,
		final Exception cause) {
		super(message, cause);
	}
	
	public CuracaoException(final String message) {
		super(message);
	}
	
	public CuracaoException(final Exception cause) {
		super(cause);
	}
	
	/**
	 * A {@link CuracaoException} with an attached entity that
	 * represents the exception case.  Typically, the enclosed entity
	 * will be written out to an error response.
	 */
	public static class WithEntity extends CuracaoException {

		private static final long serialVersionUID = -1913974898753262542L;
		
		protected final CuracaoEntity entity_;
		
		public WithEntity(final CuracaoEntity entity,
			final String message, final Exception cause) {
			super(message, cause);
			entity_ = entity;
		}
		
		public WithEntity(final CuracaoEntity entity,
			final Exception cause) {
			this(entity, null, cause);
		}
		
		public final CuracaoEntity getEntity() {
			return entity_;
		}
		
	}
	
	/**
	 * A {@link CuracaoException} with a corresponding HTTP
	 * status code, sometimes called an HTTP response code.  This
	 * exception class is to be used when only an HTTP response/status
	 * code is to be sent back to the consumer/client.  That is, no
	 * response body will be rendered (Content-Length will be zero).
	 */
	public static class WithStatus extends WithEntity {
		
		private static final long serialVersionUID = 8990984701759170150L;
		
		public WithStatus(final int status, final String message,
			final Exception cause) {
			super(new StatusCodeOnlyCuracaoEntity(status),
				message, cause);
		}
		
		public WithStatus(final int status, final Exception cause) {
			this(status, null, cause);
		}
		
	}

}
