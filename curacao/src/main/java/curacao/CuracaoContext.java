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

package curacao;

import com.google.common.collect.Maps;
import curacao.CuracaoContextListener.CuracaoCoreObjectMap;
import curacao.annotations.RequestMapping.Method;
import curacao.components.ComponentTable;
import curacao.mappers.MapperTable;
import curacao.mappers.request.RequestMappingTable;
import curacao.mappers.request.matchers.CuracaoPathMatcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An object that represents a mutable "request context" within a single thread.  There is only one of these
 * context instances per Curacao processing thread.
 *
 * This object is instantiated once when the underlying {@link AsyncContext} is created by the Servlet container,
 * and persists across the entire request and response lifecycle.  This is much better than a thread local, because if
 * a large object is attached to the internal property map herein, and the consumer forgets to "clean up" that object,
 * it will organically fall off and eventually be garbage-collected by the JVM.  A thread local, on the other hand
 * attaches objects directly to the thread itself which may persist for the life of the thread (which could very well
 * be for the life of the JVM) leading to awful memory leaks.  We use our own context object here so the consumer can
 * attach whatever they want for the lifetime of the request/response, and have some confidence that it will be GC'ed
 * organically when Curacao is done using it.
 */
public final class CuracaoContext {

    private static final String INVOKABLE_KEY = "curacao-invokable";
    private static final String PATH_WITHIN_APPLICATION_KEY = "curacao-pathWithinApplication";
    private static final String PATH_VARIABLES_KEY = "curacao-pathVariables";
    private static final String REQUEST_BODY_MAP_KEY = "curacao-body";

    public final ServletContext servletCtx_;

    public final AsyncContext asyncCtx_;
    public final HttpServletRequest request_;
    public final HttpServletResponse response_;

    public final ComponentTable componentTable_;
    public final RequestMappingTable requestMappingTable_;
    public final MapperTable mapperTable_;

    public final Method method_;
    public final String comment_;

    /**
     * When this context was created, in milliseconds.
     */
    public final long creationTime_;

    /**
     * A set of mutable properties attached to this request context that
     * is passed from one controller method argument mapper to another.
     * This allows one argument mapper to attach properties that can then
     * be used/consumed by another argument mapper later in the processing
     * chain.
     */
    private final Map<String,Object> propertyMap_;

    public CuracaoContext(@Nonnull final CuracaoCoreObjectMap coreObjectMap,
                          @Nonnull final AsyncContext asyncCtx) {
        checkNotNull(coreObjectMap, "Core object map cannot be null.");
        // Probably don't need to check for null here again, but just to be safe in case the Servlet container
        // somehow violated contract and returned a null async context.
        asyncCtx_ = checkNotNull(asyncCtx, "Async context cannot be null.");
        // The following extracted from the core object map are guaranteed to be non-null, by enforcement in the
        // core object map constructor itself.
        servletCtx_ = coreObjectMap.servletCtx_;
        componentTable_ = coreObjectMap.componentTable_;
        requestMappingTable_ = coreObjectMap.requestMappingTable_;
        mapperTable_ = coreObjectMap.mapperTable_;
        // Local properties
        request_ = (HttpServletRequest)asyncCtx_.getRequest();
        response_ = (HttpServletResponse)asyncCtx_.getResponse();
        method_ = Method.fromString(request_.getMethod());
        comment_ = requestToString(request_);
        creationTime_ = System.currentTimeMillis();
        // NOTE: Does not need to be a concurrent map because there is only ever one context per thread.
        // Therefore, this map should only ever be mutated by a single thread.
        propertyMap_ = Maps.newHashMap();
    }

    @SuppressWarnings("unchecked")
    public final <T> T getProperty(final String key) {
        return (T)propertyMap_.get(key);
    }
    public final void setProperty(final String key,
                                  final Object value) {
        propertyMap_.put(key, value);
    }

    /**
     * Get the {@link CuracaoInvokable} attached to this context.
     *
     * The invokable here represents a controller class and method, that will be "invoked" by
     * Curacao using reflection to service the request.
     *
     * @return the {@link CuracaoInvokable} attached to this context, or null if one does not exist
     */
    @Nullable
    public final CuracaoInvokable getInvokable() {
        return getProperty(INVOKABLE_KEY);
    }

    /**
     * Sets the {@link CuracaoInvokable} attached to this context.
     */
    public final void setInvokable(final CuracaoInvokable invokable) {
        setProperty(INVOKABLE_KEY, invokable);
    }

    /**
     * Get the map that represents the matched and extracted path variables from the request, if any. This map is
     * typically immutable, and is generated by the underlying {@link CuracaoPathMatcher} which was invoked
     * and matched the request.
     *
     * @return an immutable map containing any extracted path variables
     */
    @Nullable
    public final Map<String,String> getPathVariables() {
        return getProperty(PATH_VARIABLES_KEY);
    }

    /**
     * Sets the {@link CuracaoPathMatcher} matched path variables from this request.
     *
     * @param pathVars matcher extracted path variables
     */
    public final void setPathVariables(final Map<String,String> pathVars) {
        setProperty(PATH_VARIABLES_KEY, pathVars);
    }

    /**
     * Returns the path to the request without the Servlet context, if any.  For example, if the request is
     * GET:/foobar/dog/cat and the Servlet context is "foobar" then the path within application would
     * be GET:/dog/cat as extracted.
     */
    @Nullable
    public final String getPathWithinApplication() {
        return getProperty(PATH_WITHIN_APPLICATION_KEY);
    }

    /**
     * Sets the path within the application as extracted.
     *
     * @param path the path within the application
     */
    public final void setPathWithinApplication(final String path) {
        setProperty(PATH_WITHIN_APPLICATION_KEY, path);
    }

    /**
     * Get the in-memory buffered copy of the request body, if it exists.
     *
     * @return the byte[] in memory buffered body, or null if no body has been buffered yet.
     */
    @Nullable
    public final byte[] getBody() {
        return getProperty(REQUEST_BODY_MAP_KEY);
    }

    /**
     * Set the in-memory buffered copy of the request body.
     *
     * @param body the byte[] in memory buffered body.
     */
    public final void setBody(final byte[] body) {
        setProperty(REQUEST_BODY_MAP_KEY, body);
    }

    @Override
    public final String toString() {
        return comment_;
    }

    /**
     * Given a {@link HttpServletRequest} returns a String representing the HTTP request method, and full
     * request URI (including any query parameters... a.k.a., the "query string").
     */
    private static String requestToString(final HttpServletRequest request) {
        final StringBuffer requestUrl = request.getRequestURL();
        final String queryString = request.getQueryString();
        if (queryString != null) {
            requestUrl.append("?").append(queryString);
        }
        return "[" + request.getMethod() + " " + requestUrl + "]";
    }

}
