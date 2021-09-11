/*
 * Copyright (c) 2021 Mark S. Kolich
 * https://mark.koli.ch
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

import curacao.CuracaoInvokable.InvokableClassWithInstance;
import curacao.context.CuracaoContext;
import curacao.exceptions.routing.PathNotFoundException;
import curacao.mappers.request.AbstractControllerArgumentMapper;
import curacao.mappers.request.filters.CuracaoRequestFilter;
import curacao.mappers.request.matchers.CuracaoPathMatcher;
import curacao.util.helpers.UrlPathHelper;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.util.reflection.CuracaoAnnotationUtils.getFirstAnnotation;
import static org.slf4j.LoggerFactory.getLogger;

public final class CuracaoControllerInvoker implements Callable<Object> {

    private static final Logger LOG = getLogger(CuracaoControllerInvoker.class);

    private final CuracaoContext ctx_;

    public CuracaoControllerInvoker(
            @Nonnull final CuracaoContext ctx) {
        ctx_ = checkNotNull(ctx, "Curacao context cannot be null.");
    }

    @Override
    public Object call() throws Exception {
        // The path within the application represents the part of the URI
        // without the Servlet context, if any. For example, if the Servlet
        // content is "/foobar" and the incoming request was GET:/foobar/baz,
        // then this method will return just "/baz".
        final String pathWithinApplication = UrlPathHelper.getPathWithinApplication(ctx_);
        LOG.debug("Computed path within application context (requestUri={}, computedPath={})",
                ctx_, pathWithinApplication);
        // Attach the path within the application to the mutable context.
        CuracaoContext.Extensions.setPathWithinApplication(ctx_, pathWithinApplication);
        // Get a list of all supported application routes based on the incoming HTTP request method.
        final List<CuracaoInvokable> candidates =
                CuracaoContext.Extensions.getRequestMappingTable(ctx_).getRoutesByHttpMethod(ctx_.getMethod());
        LOG.debug("Found {} controller candidates for request: {}:{}", candidates.size(), ctx_.getMethod(),
                pathWithinApplication);
        // Check if we found any viable candidates for the incoming HTTP request method.
        if (candidates.isEmpty()) {
            // If we didn't find any, immediately bail letting the user know this incoming HTTP request method
            // just isn't supported by the implementation.
            throw new PathNotFoundException("Found 0 (zero) controller candidates for request: " + ctx_);
        }
        Pair<CuracaoInvokable, Map<String, String>> invokablePair = null;
        for (final CuracaoInvokable i : candidates) { // O(n)
            LOG.debug("Checking invokable method candidate: {}", i);
            // Get the matcher instance from the invokable.
            final CuracaoPathMatcher matcher = i.matcher_.instance_;
            // The matcher will return 'null' if the provided pattern did not match the path within application.
            final Map<String, String> pathVars = matcher.match(ctx_, i.mapping_, pathWithinApplication);
            if (pathVars != null) {
                // Matched!
                LOG.debug("Extracted path variables: {}", pathVars);
                invokablePair = ImmutablePair.of(i, pathVars);
                break;
            }
        }
        // If we found ~some~ method that supports the incoming HTTP request type, but no proper annotated
        // controller method that matches the request path, that means we've got nothing.
        if (invokablePair == null) {
            throw new PathNotFoundException("Found no invokable controller method worthy of "
                    + "servicing request.");
        }
        // Attach the discovered invokable to the mutable context.
        final CuracaoInvokable invokable = invokablePair.getLeft();
        CuracaoContext.Extensions.setInvokable(ctx_, invokable);
        // Attach extracted path variables from the matcher to the mutable context.
        final Map<String, String> pathVars = invokablePair.getRight();
        CuracaoContext.Extensions.setPathVariables(ctx_, pathVars);
        // Invoke each of the request filters attached to the controller method invokable, in order. Any filter
        // may throw an exception, which is totally fair and will be handled by the upper-layer.
        for (final InvokableClassWithInstance<? extends CuracaoRequestFilter> filter : invokable.filters_) {
            filter.instance_.filter(ctx_);
        }
        // Build the parameter list to be passed into the controller method via reflection.
        final Object[] parameters = buildParameterList(invokable);
        // Reflection invoke the discovered "controller" method.
        return invokable.method_.invoke(
                // The controller class.
                invokable.controller_.instance_,
                // Method arguments/parameters.
                parameters);
    }

    /**
     * Given an invokable, builds an array of Objects that correspond to the list of arguments (parameters)
     * to be passed into the invokable.
     */
    private Object[] buildParameterList(
            final CuracaoInvokable invokable) throws Exception {
        // The actual method argument/parameter types, in order.
        final Class<?>[] methodParams = invokable.parameterTypes_;
        // Create a new array list with capacity to reduce unnecessary copies,
        // given we're converting this list to an array later.
        final Object[] params = new Object[methodParams.length];
        // A 2D array (ugh) that gives a list of all annotations.
        final Annotation[][] a = invokable.parameterAnnotations_;
        for (int i = 0, l = methodParams.length; i < l; i++) {
            Object toAdd = null;
            // A list of all annotations attached to this method argument/parameter, in order. If the
            // argument/parameter has no annotations, this will be an ~empty~ array of length zero.
            final Annotation[] annotations = a[i];
            // Yes, the developer can decorate a controller method param with multiple annotations, but we're
            // only going to ever care about the first one.
            final Annotation first = getFirstAnnotation(annotations);
            // Get the type/class associated with the method argument/parameter at the given index.
            final Class<?> o = methodParams[i];
            // Validate that this parameter is not a "raw object". That is, is it literally a "java.lang.Object".
            // If so, we don't want to bother asking any of the argument mappers, just assign, keep calm, and carry on.
            final boolean isRawObject = o.isInstance(Object.class);
            if (!isRawObject && o.isAssignableFrom(AsyncContext.class)) {
                // Special cased here because we don't pass the AsyncContext into the controller
                // argument mappers.
                toAdd = ctx_.getAsyncContext();
            } else if (!isRawObject && o.isAssignableFrom(CuracaoContext.class)) {
                // Special cased here because we don't pass the mutable request context into the controller
                // argument mappers.
                toAdd = ctx_;
            } else if (!isRawObject && o.isAssignableFrom(CuracaoInvokable.class)) {
                toAdd = invokable;
            } else {
                // Given a class type, find an argument mapper for it. Note that if no mappers exist for the given
                // type, the method below will ~not~ return null, but rather an empty collection.
                final Collection<AbstractControllerArgumentMapper<?>> mappers =
                        CuracaoContext.Extensions.getMapperTable(ctx_).getArgumentMappersForClass(o);
                for (final AbstractControllerArgumentMapper<?> mapper : mappers) {
                    // Ask each mapper, in order, to resolve the argument. The first mapper to resolve (return
                    // non-null) wins. User registered mappers are called first given that they are inserted into the
                    // multi-map first before the "default" mappers, which allows consumers of this toolkit to register
                    // and override default argument mappers for foundational classes like "String", etc. if they wish.
                    if ((toAdd = mapper.resolve(first, ctx_)) != null) {
                        break;
                    }
                }
            }
            params[i] = toAdd;
        }
        return params;
    }

}
