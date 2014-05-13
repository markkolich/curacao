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

package com.kolich.curacao.handlers.requests;

import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An object that represents a mutable "request context" that spans across
 * controller method argument mappers.  This object is established once
 * and persists across the life of the request.  A controller argument
 * mapper can use the internal mutable property map in this class to
 * pass data objects from itself to another mapper if desired.
 */
public final class CuracaoRequestContext {

    private static final String PATH_WITHIN_APPLICATION_KEY =
        "pathWithinApplication";
    private static final String REQUEST_BODY_MAP_KEY =
        "body";

    public final ServletContext sContext_;
    public final HttpServletRequest request_;
    public final HttpServletResponse response_;
    public final Map<String,String> pathVars_;

    /**
     * A set of mutable properties attached to this request context that
     * is passed from one controller method argument mapper to another.
     * This allows one argument mapper to attach properties that can then
     * be used/consumed by another argument mapper later in the processing
     * chain.
     */
    private final Map<String,Object> propertyMap_;

    public CuracaoRequestContext(@Nonnull final ServletContext sContext,
                                 @Nonnull final HttpServletRequest request,
                                 @Nonnull final HttpServletResponse response,
                                 @Nonnull final Map<String,String> pathVars) {
        sContext_ = checkNotNull(sContext, "Servlet context cannot be null.");
        request_ = checkNotNull(request, "Servlet request cannot be null.");
        response_ = checkNotNull(response, "Servlet response cannot be null.");
        pathVars_ = checkNotNull(pathVars, "Path variables cannot be null.");
        propertyMap_ = Maps.newConcurrentMap();
    }

    @SuppressWarnings("unchecked")
    public final <T> T getProperty(final String key) {
        return (T)propertyMap_.get(key);
    }
    public final void setProperty(final String key, final Object value) {
        propertyMap_.put(key, value);
    }

    /**
     * Returns the path to the request without the Servlet context,
     * if any.  For example, if the request is GET:/foobar/dog/cat and the
     * Servlet context is "foobar" then the path within application would
     * be GET:/dog/cat as extracted.
     */
    public final String getPathWithinApplication() {
        return getProperty(PATH_WITHIN_APPLICATION_KEY);
    }
    /**
     * Sets the path within the application as extracted.
     */
    public final void setPathWithinApplication(final String path) {
        setProperty(PATH_WITHIN_APPLICATION_KEY, path);
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
