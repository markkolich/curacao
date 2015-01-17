/**
 * Copyright (c) 2015 Mark S. Kolich
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

package com.kolich.curacao.mappers.request;

import com.google.common.collect.ImmutableList;
import com.kolich.curacao.exceptions.routing.PathNotFoundException;
import com.kolich.curacao.mappers.request.CuracaoInvokable.InvokableClassWithInstance;
import com.kolich.curacao.mappers.request.filters.CuracaoRequestFilter;
import com.kolich.curacao.mappers.request.matchers.CuracaoPathMatcher;
import com.kolich.curacao.util.helpers.UrlPathHelper;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.AsyncContext;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public final class CuracaoControllerInvoker implements Callable<Object> {

    private static final Logger logger__ =
        getLogger(CuracaoControllerInvoker.class);
	
	private static final UrlPathHelper pathHelper__ =
        UrlPathHelper.getInstance();

    private final CuracaoContext ctx_;

	public CuracaoControllerInvoker(@Nonnull final CuracaoContext ctx) {
        ctx_ = checkNotNull(ctx, "Curacao context cannot be null.");
	}
	
	@Override
	public final Object call() throws Exception {
        // The path within the application represents the part of the URI
        // without the Servlet context, if any.  For example, if the Servlet
        // content is "/foobar" and the incoming request was GET:/foobar/baz,
        // then this method will return just "/baz".
		final String pathWithinApplication =
			pathHelper__.getPathWithinApplication(ctx_);
		logger__.debug("Computed path within application context " +
			"(requestUri={}, computedPath={})", ctx_.comment_,
			pathWithinApplication);
        // Attach the path within the application to the mutable context.
        ctx_.setPathWithinApplication(pathWithinApplication);
        // Get a list of all supported application routes based on the
        // incoming HTTP request method.
		final ImmutableList<CuracaoInvokable> candidates =
            ctx_.routingTable_.getRoutesByHttpMethod(ctx_.method_);
        logger__.debug("Found {} controller candidates for request: {}:{}",
			candidates.size(), ctx_.method_, pathWithinApplication);
		// Check if we found any viable candidates for the incoming HTTP
		// request method.  If we didn't find any, immediately bail letting
		// the user know this incoming HTTP request method just isn't
		// supported by the implementation.
		if (candidates.isEmpty()) {
			throw new PathNotFoundException("Found " + candidates.size() +
				" controller candidates for request: " + ctx_.comment_);
		}
		// For each viable option, need to compare the path provided
		// with the attached invokable method annotation to decide
		// if that path matches the request.
		CuracaoInvokable invokable = null;
		Map<String,String> pathVars = null;
		for (final CuracaoInvokable i : candidates) { // O(n)
			logger__.debug("Checking invokable method candidate: {}", i);
            // Get the matcher instance from the invokable.
            final CuracaoPathMatcher matcher = i.matcher_.instance_;
            // The matcher will return 'null' if the provided pattern did not
            // match the path within application.
            pathVars = matcher.match(ctx_,
                // The path mapping registered with the invokable.
                i.mapping_,
                // The path within the application.
                pathWithinApplication);
            if (pathVars != null) {
                // Matched!
				logger__.debug("Extracted path variables: {}", pathVars);
                invokable = i;
                break;
            }
		}
		// If we found ~some~ method that supports the incoming HTTP request
		// type, but no proper annotated controller method that matches
		// the request path, that means we've got nothing.
		if (invokable == null) {
			throw new PathNotFoundException("Found no invokable controller " +
				"method worthy of servicing request: " + ctx_.comment_);
		}
        // Attach extracted path variables from the matcher onto the mutable context.
        ctx_.setPathVariables(pathVars);
		// Invoke each of the request filters attached to the controller
		// method invokable, in order.  Any filter may throw an exception,
		// which is totally fair and will be handled by the upper-layer.
        for (final InvokableClassWithInstance<? extends CuracaoRequestFilter> filter :
            invokable.filters_) {
            filter.instance_.filter(ctx_);
        }
		// Build the parameter list to be passed into the controller method
		// via reflection.
		final Object[] parameters = buildPopulatedParameterList(invokable);
		// Reflection invoke the discovered "controller" method.
		final Object invokedResult = invokable.method_.invoke(
            // The controller class.
            invokable.controller_.instance_,
            // Method arguments/parameters.
            parameters);
		// A set of hard coded controller return type pre-processors. That is,
		// we take the type/object that the controller returned once invoked
		// and see if we need to do anything special with it in this request
		// context (using the thread that's handling the _REQUEST_).
		Object o = invokedResult;
		if (invokedResult instanceof Callable) {
			o = ((Callable<?>)invokedResult).call();
		} else if (invokedResult instanceof Future) {
			o = ((Future<?>)invokedResult).get();
		}
		return o;
	}

    /**
     * Given an invokable, builds an array of Objects that correspond to
     * the list of arguments (parameters) to be passed into the invokable.
     */
	private final Object[] buildPopulatedParameterList(
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
			// A list of all annotations attached to this method
			// argument/parameter, in order.  If the argument/parameter has
			// no annotations, this will be an ~empty~ array of length zero.
			final Annotation[] annotations = a[i];
			// Yes, the developer can decorate a controller method param
			// with multiple annotations, but we're only going to ever
			// care about the first one.
			final Annotation first = getFirstAnnotation(annotations);
			// Get the type/class associated with the method argument/parameter
			// at the given index.
			final Class<?> o = methodParams[i];
			// Validate that this parameter is not a "raw object".  That is,
			// is it literally a "java.lang.Object".  If so, we don't want
			// to bother asking any of the argument mappers, just assign,
			// keep calm, and carry on.
			final boolean isRawObject = o.isInstance(Object.class);
			if (!isRawObject && o.isAssignableFrom(AsyncContext.class)) {
				// Special cased here because we don't pass the AsyncContext
				// into the controller argument mappers.
				toAdd = ctx_.asyncCtx_;
			} else if (!isRawObject && o.isAssignableFrom(CuracaoContext.class)) {
                // Special cased here because we don't pass the mutable request
                // context into the controller argument mappers.
                toAdd = ctx_;
            } else {
				// Given a class type, find an argument mapper for it.  Note
				// that if no mappers exist for the given type, the method
				// below will ~not~ return null, but rather an empty collection.
				final Collection<ControllerArgumentMapper<?>> mappers =
                    ctx_.argMappingTable_.getArgumentMappersForType(o);
				for (final ControllerArgumentMapper<?> mapper : mappers) {
					// Ask each mapper, in order, to resolve the argument.
					// The first mapper to resolve (return non-null) wins.
					// User registered mappers are called first given that they
					// are inserted into the multi-map first before the "default"
					// mappers, which allows consumers of this toolkit to register
					// and override default argument mappers for foundational
					// classes like "String", etc. if they wish.
					if ((toAdd = mapper.resolve(first, ctx_)) != null) {
						break;
					}
				}
			}
			params[i] = toAdd;
		}
		return params;
	}

    @Nullable
	private static final Annotation getFirstAnnotation(final Annotation[] as) {
		return getAnnotationSafely(as, 0);
	}

    @Nullable
	private static final Annotation getAnnotationSafely(final Annotation[] as,
                                                        final int index) {
		return (as.length > 0 && index < as.length) ? as[index] : null;
	}

}
