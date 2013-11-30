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

package com.kolich.curacao.handlers.requests.mappers;

import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ControllerMethodArgumentMapper<T> {

    /**
     * An object that represents a "request context" that spans across
     * controller method argument mappers.  This object is established once
     * and persists across the life of the request.  A controller argument
     * mapper can use the internal mutable property map in this class to
     * pass data objects from itself to another mapper if desired.
     */
    public final static class CuracaoRequestContext {

        private static final String REQUEST_BODY_MAP_KEY = "body";

        private final Map<String,String> pathVars_;
        private final HttpServletRequest request_;
        private final HttpServletResponse response_;

        /**
         * A set of mutable properties attached to this request context that
         * is passed from one controller method argument mapper to another.
         * This allows one argument mapper to attach properties that can then
         * be used/consumed by another argument mapper later in the processing
         * chain.
         */
        private final Map<String,Object> propertyMap_;

        public CuracaoRequestContext(@Nonnull final HttpServletRequest request,
            @Nonnull final HttpServletResponse response,
            @Nonnull final Map<String,String> pathVars) {
            request_ = checkNotNull(request, "Servlet request cannot be null.");
            response_ = checkNotNull(response, "Servlet response cannot be null.");
            pathVars_ = checkNotNull(pathVars, "Path variables cannot be null.");
            propertyMap_ = Maps.newConcurrentMap();
        }

        public final Map<String,String> getPathVars() {
            return pathVars_;
        }

        public final HttpServletRequest getRequest() {
            return request_;
        }

        public final HttpServletResponse getResponse() {
            return response_;
        }

        public final void setProperty(final String key, final Object value) {
            propertyMap_.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public final <T> T getProperty(final String key) {
            return (T)propertyMap_.get(key);
        }

        /**
         * Get the in-memory buffered copy of the request body, if it exists.
         * @return the byte[] in memory buffered body, or null if no body has
         * been buffered yet.
         */
        public final byte[] getBody() {
            return getProperty(REQUEST_BODY_MAP_KEY);
        }

        /**
         * Set the in-memory buffered copy of the request body.
         * @param body the byte[] in memory buffered body.
         */
        public final void setBody(final byte[] body) {
            setProperty(REQUEST_BODY_MAP_KEY, body);
        }

    }

    /**
     * Called when the mapper is asked to lookup the argument/parameter
     * from the request for the controller method invocation.  The returned
     * value will be passed into the controller method as an argument when
     * invoked via reflection by this library.
     * @return an object of type T if the mapper could extract a valid argument
     * from the incoming request.  Should return null if no argument could be
     * discovered or extracted.
     * @throws Exception in the event of an error or exception case.
     */
    @Nullable
	public abstract T resolve(@Nullable final Annotation annotation,
        final CuracaoRequestContext context) throws Exception;

}
