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

package com.kolich.curacao.handlers.requests.mappers.types.body;

import static com.google.common.base.Charsets.ISO_8859_1;
import static com.google.common.io.ByteStreams.limit;
import static com.kolich.curacao.CuracaoConfigLoader.getDefaultMaxRequestBodySizeInBytes;
import static org.apache.commons.io.IOUtils.toByteArray;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kolich.curacao.annotations.parameters.RequestBody;
import com.kolich.curacao.exceptions.requests.RequestTooLargeException;
import com.kolich.curacao.handlers.requests.mappers.ControllerMethodArgumentMapper;

public abstract class MemoryBufferingRequestBodyMapper<T>
	extends ControllerMethodArgumentMapper<T> {
	
	private static final String DEFAULT_HTTP_REQUEST_CHARSET =
		ISO_8859_1.toString();
	
	private static final long DEFAULT_MAX_REQUEST_BODY_SIZE_BYTES =
		getDefaultMaxRequestBodySizeInBytes();

	@Override
	public final T resolve(final Annotation annotation,
		final Map<String,String> pathVars, final HttpServletRequest request,
		final HttpServletResponse response) throws Exception {
		T result = null;
		if(annotation instanceof RequestBody) {
			final RequestBody rb = (RequestBody)annotation;
			final long maxLength = (rb.maxSizeInBytes() > 0L) ?
				// If the RequestBody annotation specified a maximum body
				// size in bytes, then we should honor that here.
				rb.maxSizeInBytes() :
				// Otherwise, default to what's been globally configured in
				// the app configuration.
				DEFAULT_MAX_REQUEST_BODY_SIZE_BYTES;
			// Sigh, blocking.
			try(final InputStream is = request.getInputStream()) {
				//long contentLength = request.getContentLengthLong(); // Only available in Servlet 3.1
				long contentLength = Long.valueOf(request.getContentLength());
				if(contentLength != -1) {
					// Content-Length was specified, check to make sure it's not
					// larger than the maximum request body size supported.
					if(maxLength > 0 && contentLength > maxLength) {
						throw new RequestTooLargeException("Incoming request " +
							"body was too large to buffer into memory: " +
							contentLength + "-bytes > " + maxLength +
							"-bytes maximum.");
					}
				} else {
					contentLength = (maxLength <= 0) ?
						// Default to copy as much as data as we can if the
						// config does not place a limit on the maximum size
						// of the request body to buffer in memory.
						Long.MAX_VALUE :
						// No Content-Length was specified.  Default the max
						// number of bytes to the max request body size.
						maxLength;
				}
				final byte[] body = toByteArray(limit(is, contentLength));
				result = resolveSafely((RequestBody)annotation, pathVars,
					request, body);
			}
		}
		return result;
	}
	
	public abstract T resolveSafely(final RequestBody annotation,
		final Map<String,String> pathVars, final HttpServletRequest request,
		final byte[] body) throws Exception;
	
	protected static final String getRequestEncoding(
		final HttpServletRequest request) {
		String encoding = null;
		if((encoding = request.getCharacterEncoding()) == null) {
			// HTTP/1.1 says that the default charset is ISO-8859-1 if
			// not specified so we honor that here.
			encoding = DEFAULT_HTTP_REQUEST_CHARSET;
		}
		return encoding;
	}

}
