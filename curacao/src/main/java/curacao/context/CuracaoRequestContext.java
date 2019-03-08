/*
 * Copyright (c) 2019 Mark S. Kolich
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

package curacao.context;

import com.google.common.collect.Maps;
import curacao.CuracaoContextListener.CuracaoCoreObjectMap;
import curacao.annotations.RequestMapping.Method;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
public final class CuracaoRequestContext implements CuracaoContext {

    private final ServletContext servletCtx_;

    private final AsyncContext asyncCtx_;
    private final HttpServletRequest request_;
    private final HttpServletResponse response_;
    private final Method method_;

    /**
     * When this context was created, in milliseconds.
     */
    private final long creationTime_;

    /**
     * A set of mutable properties attached to this request context that
     * is passed from one controller method argument mapper to another.
     * This allows one argument mapper to attach properties that can then
     * be used/consumed by another argument mapper later in the processing
     * chain.
     */
    private final Map<String,Object> propertyMap_;

    public CuracaoRequestContext(@Nonnull final CuracaoCoreObjectMap coreObjectMap,
                                 @Nonnull final AsyncContext asyncCtx) {
        checkNotNull(coreObjectMap, "Core object map cannot be null.");
        // Probably don't need to check for null here again, but just to be safe in case the Servlet container
        // somehow violated contract and returned a null async context.
        asyncCtx_ = checkNotNull(asyncCtx, "Async context cannot be null.");
        // The following extracted from the core object map are guaranteed to be non-null, by enforcement in the
        // core object map constructor itself.
        servletCtx_ = coreObjectMap.servletCtx_;
        // Local properties
        request_ = (HttpServletRequest)asyncCtx_.getRequest();
        response_ = (HttpServletResponse)asyncCtx_.getResponse();
        method_ = Method.fromString(request_.getMethod());
        creationTime_ = System.currentTimeMillis();
        // NOTE: Does not need to be a concurrent map because there is only ever one context per thread.
        // Therefore, this map should only ever be mutated by a single thread.
        propertyMap_ = Maps.newHashMap();
        Extensions.setComponentTable(this, coreObjectMap.componentTable_);
        Extensions.setRequestMappingTable(this, coreObjectMap.requestMappingTable_);
        Extensions.setMapperTable(this, coreObjectMap.mapperTable_);
    }

    @Override
    public final long getCreationTime() {
        return creationTime_;
    }

    @Nonnull
    @Override
    public final ServletContext getServletContext() {
        return servletCtx_;
    }

    @Nonnull
    @Override
    public final AsyncContext getAsyncContext() {
        return asyncCtx_;
    }

    @Nonnull
    @Override
    public final Method getMethod() {
        return method_;
    }

    @Nonnull
    @Override
    public final HttpServletRequest getRequest() {
        return request_;
    }

    @Nonnull
    @Override
    public final HttpServletResponse getResponse() {
        return response_;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T getProperty(final String key) {
        return (T)propertyMap_.get(key);
    }

    @Override
    public final void setProperty(final String key,
            final Object value) {
        propertyMap_.put(key, value);
    }

    /**
     * Given a {@link HttpServletRequest} returns a String representing the HTTP request method, and full
     * request URI (including any query parameters... a.k.a., the "query string").
     */
    @Override
    public final String toString() {
        final StringBuffer requestUrl = request_.getRequestURL();
        final String queryString = request_.getQueryString();
        if (queryString != null) {
            requestUrl.append("?").append(queryString);
        }
        return "[" + method_ + " " + requestUrl + "]";
    }

    @Override
    public final void close() throws IOException {
        propertyMap_.clear();
    }

}
