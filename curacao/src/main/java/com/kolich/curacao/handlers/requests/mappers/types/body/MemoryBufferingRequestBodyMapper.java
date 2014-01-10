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

package com.kolich.curacao.handlers.requests.mappers.types.body;

import com.kolich.curacao.annotations.parameters.RequestBody;
import com.kolich.curacao.exceptions.requests.RequestTooLargeException;
import com.kolich.curacao.handlers.requests.mappers.ControllerMethodArgumentMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.lang.annotation.Annotation;

import static com.google.common.io.ByteStreams.limit;
import static com.kolich.curacao.CuracaoConfigLoader.getDefaultCharEncodingIfNotSpecified;
import static com.kolich.curacao.CuracaoConfigLoader.getDefaultMaxRequestBodySizeInBytes;
import static org.apache.commons.io.IOUtils.toByteArray;

public abstract class MemoryBufferingRequestBodyMapper<T>
	extends ControllerMethodArgumentMapper<T> {
		
	private static final String DEFAULT_HTTP_REQUEST_CHARSET =
		getDefaultCharEncodingIfNotSpecified();	
	private static final long DEFAULT_MAX_REQUEST_BODY_SIZE_BYTES =
		getDefaultMaxRequestBodySizeInBytes();

	@Override
	public final T resolve(@Nullable final Annotation annotation,
        final CuracaoRequestContext context) throws Exception {
        // This mapper, and all of its children, only handles parameters
        // annotated with request body. If it's anything else, immediately
        // jump out and continue returning null.
		if(!(annotation instanceof RequestBody)) {
            return null;
        }
        T result = null;
        // If this request context already has a copy of the request body
        // in memory, then we should not attempt to "re-buffer" it.  We can
        // use what's already been fetched over the wire from the client.
        byte[] body = context.getBody();
        if(body == null) {
            // No pre-buffered body was attached to the request context.  We
            // should attempt to buffer one.
            final HttpServletRequest request = context.getRequest();
            final RequestBody rb = (RequestBody)annotation;
            final long maxLength = (rb.maxSizeInBytes() > 0L) ?
                // If the RequestBody annotation specified a maximum body
                // size in bytes, then we should honor that here.
                rb.maxSizeInBytes() :
                // Otherwise, default to what's been globally configured in
                // the app configuration.
                DEFAULT_MAX_REQUEST_BODY_SIZE_BYTES;
            // Sigh, blocking I/O (for now).
            try(final InputStream is = request.getInputStream()) {
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
                    // Content-Length was not specified on the request.
                    contentLength = (maxLength <= 0) ?
                        // Default to copy as much as data as we can if the
                        // config does not place a limit on the maximum size
                        // of the request body to buffer in memory.
                        Long.MAX_VALUE :
                        // No Content-Length was specified.  Default the max
                        // number of bytes to the max request body size.
                        maxLength;
                }
                // Note we're using a limited input stream that intentionally
                // limits the number of bytes read by reading N-bytes, then
                // stops.  This prevents us from filling up too many buffers
                // which could lead to bringing down the JVM with too many
                // requests and not enough memory.
                body = toByteArray(limit(is, contentLength));
                // Cache the freshly buffered body byte[] array to the request
                // context for other mappers to pick up if needed.
                context.setBody(body);
            }
        }
		return resolveWithBody((RequestBody) annotation, context, body);
	}

    /**
     * Called when the request body has been buffered into memory safely,
     * and is ready to be processed.
     * @param body a byte[] array representing the buffered request body.
     * @return an object of type T once resolved and constructed.
     * @throws Exception if anything went wrong during the mapper resolution.
     */
	@Nullable
	public abstract T resolveWithBody(final RequestBody annotation,
        final CuracaoRequestContext context, final byte[] body)
            throws Exception;
	
	@Nonnull
	protected static final String getRequestEncoding(
        final CuracaoRequestContext context) {
		String encoding = null;
		if((encoding = context.getRequest().getCharacterEncoding()) == null) {
            // The "default charset" is configurable although HTTP/1.1 says the
            // default, if not specified, is ISO-8859-1.  We acknowledge that's
            // incredibly lame, so we default to "UTF-8" via configuration and
            // let the app override that if it wants to use something else.
			encoding = DEFAULT_HTTP_REQUEST_CHARSET;
		}
		return encoding;
	}

}
