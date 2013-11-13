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

import static com.google.common.io.ByteStreams.limit;
import static com.kolich.curacao.CuracaoConfigLoader.getMaxRequestBodySizeInBytes;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.toByteArray;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kolich.curacao.annotations.parameters.RequestBody;
import com.kolich.curacao.exceptions.RequestTooLargeException;
import com.kolich.curacao.handlers.requests.mappers.ControllerArgumentMapper;

public abstract class MemoryBufferingRequestBodyMapper<T>
	extends ControllerArgumentMapper<T> {
	
	private static final long MAX_REQUEST_BODY_SIZE_BYTES =
		getMaxRequestBodySizeInBytes();

	@Override
	public final T resolve(final Annotation annotation,
		final Map<String,String> pathVars, final HttpServletRequest request,
		final HttpServletResponse response) throws Exception {
		T result = null;
		if(annotation instanceof RequestBody) {
			InputStream is = null;
			try {
				is = request.getInputStream();
				long contentLength = request.getContentLength();
				if(contentLength != -1) {
					// Content-Length was specified, check to make sure it's not
					// larger than the maximum request body size supported.
					if(MAX_REQUEST_BODY_SIZE_BYTES > 0 &&
						contentLength > MAX_REQUEST_BODY_SIZE_BYTES) {
						throw new RequestTooLargeException("Incoming request " +
							"body was too large to buffer into memory: " +
							contentLength + "-bytes > " +
							MAX_REQUEST_BODY_SIZE_BYTES + "-bytes maximum.");				
					}
				} else {
					contentLength = (MAX_REQUEST_BODY_SIZE_BYTES == 0) ?
						// Default to copy as much as data as we can if the
						// config does not place a limit on the maximum size
						// of the request body to buffer in memory.
						Long.MAX_VALUE :
						// No Content-Length was specified.  Default the max
						// number of bytes to the max request body size.
						MAX_REQUEST_BODY_SIZE_BYTES;
				}
				final byte[] body = toByteArray(limit(is, contentLength));
				result = resolveSafe(annotation, pathVars, request,
					response, body);
			} finally {
				// Force close the ServletInputStream on success or failure.
				// This ensures that when the request body is larger than
				// what we're configured to allow, we gracefully cleanup
				// before bailing out with a request too large exception.
				// This also forcibly hangs up on the client sending data.
				closeQuietly(is);
			}
		}
		return result;
	}
	
	public abstract T resolveSafe(final Annotation annotation,
		final Map<String,String> pathVars, final HttpServletRequest request,
		final HttpServletResponse response, final byte[] body)
			throws Exception;

}
