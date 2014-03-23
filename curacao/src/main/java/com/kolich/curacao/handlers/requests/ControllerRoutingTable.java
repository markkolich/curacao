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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.kolich.curacao.CuracaoConfigLoader;
import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.methods.RequestMapping;
import com.kolich.curacao.annotations.methods.RequestMapping.RequestMethod;
import com.kolich.curacao.handlers.components.ComponentMappingTable;
import com.kolich.curacao.handlers.requests.filters.CuracaoRequestFilter;
import com.kolich.curacao.handlers.requests.matchers.CuracaoPathMatcher;
import org.reflections.Reflections;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

public final class ControllerRoutingTable {
	
	private static final Logger logger__ = 
		getLogger(ControllerRoutingTable.class);

    /**
     * An {@link ImmutableListMultimap} which maps a request method to a
     * list of {@link CuracaoMethodInvokable}'s.
     */
	private final ImmutableListMultimap<RequestMethod, CuracaoMethodInvokable> map_;

    /**
     * The context's core component mapping table.
     */
    private final ComponentMappingTable componentMappingTable_;
	
	public ControllerRoutingTable(final ComponentMappingTable componentMappingTable) {
        componentMappingTable_ = componentMappingTable;
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Scanning for controllers in declared boot-package: " +
			bootPackage);
		// Scan the "controllers" inside of the declared boot package
		// looking for annotated Java methods that will be called
		// when a request is received.
        map_ = buildRoutingTable(bootPackage);
		if(logger__.isInfoEnabled()) {
			logger__.info("Application routing table: " + map_);
		}
	}

	public final ImmutableList<CuracaoMethodInvokable> getRoutesByHttpMethod(
        @Nonnull final RequestMethod method) {
		checkNotNull(method, "HTTP method cannot be null.");
		return map_.get(method);
	}
	
	private final ImmutableListMultimap<RequestMethod, CuracaoMethodInvokable>
		buildRoutingTable(final String bootPackage) {
        final ImmutableListMultimap.Builder<RequestMethod, CuracaoMethodInvokable> builder =
            ImmutableListMultimap.builder();
		// Find all "controller classes" in the specified boot package that
		// are annotated with our @Controller annotation.
		final Set<Class<?>> controllers =
			getTypesInPackageAnnotatedWith(bootPackage, Controller.class);
		logger__.debug("Found " + controllers.size() + " controllers " +
			"annotated with @" + Controller.class.getSimpleName());
		// For each discovered controller class, find all annotated methods
		// inside of it and add them to the routing table.
		for(final Class<?> controller : controllers) {
			logger__.debug("Found @" + Controller.class.getSimpleName() + ": " +
				controller.getCanonicalName());
			final Reflections methodReflection =
				getMethodReflectionInstanceForClass(controller);
            // Fetch a list of all request mapping annotated controller methods.
            final Set<Method> methods =
                methodReflection.getMethodsAnnotatedWith(RequestMapping.class);
            for(final Method method : methods) {
                final RequestMapping mapping =
                    method.getAnnotation(RequestMapping.class);
                // The actual "path" that the controller method will be called
                // on when a request is received.
                final String route = mapping.value();
                // Attempt to construct a new invokable for the route and add
                // it to the local routing table.
                try {
                    final CuracaoMethodInvokable invokable =
                        getInvokableForRoute(controller, method, mapping);
                    // The controller method request mapping annotation may
                    // map multiple HTTP request method types to a single
                    // Java method. So, for each supported/annotated method...
                    for(final RequestMethod httpMethod : mapping.methods()) {
                        builder.put(httpMethod, invokable);
                        if(logger__.isInfoEnabled()) {
                            logger__.info("Successfully added route to " +
                                "mapping table (route=" + httpMethod + ":" +
                                route + ", invokable=" + invokable);
                        }
                    }
                } catch (Exception e) {
                    logger__.error("Failed to add route to routing table " +
                        "(route=" + route + ", methods=" +
                        Arrays.toString(mapping.methods()) + ")", e);
                }
			}
		}
		return builder.build();
	}

    private final CuracaoMethodInvokable getInvokableForRoute(
        final Class<?> controller, final Method method,
        final RequestMapping mapping) {
        checkNotNull(controller, "Controller cannot be null.");
        checkNotNull(method, "Method cannot be null.");
        checkNotNull(mapping, "Request mapping cannot be null.");
        final Class<? extends CuracaoPathMatcher> matcher = mapping.matcher();
        final Class<? extends CuracaoRequestFilter> filter = mapping.filter();
        // Pull out any injectable annotated controller class
        // constructors that may be present.  If none are
        // found, it's OK to return null, the invokable will
        // handle that later and use the default nullary
        // constructor.
        final Constructor<?> injectableCntlrCtor =
            getInjectableConstructor(controller);
        // Pull out any injectable annotated patch matcher class
        // constructors that may be present.  If none are
        // found, it's OK to return null, the invokable will
        // handle that later and use the default nullary
        // constructor.
        final Constructor<?> injectableMatcherCtor =
            getInjectableConstructor(matcher);
        // Pull out any injectable annotated filter class
        // constructors that may be present.  If none are
        // found, it's OK to return null, the invokable will
        // handle that later and use the default nullary
        // constructor.
        final Constructor<?> injectableFilterCtor =
            getInjectableConstructor(filter);
        // Attach the controller method, and any annotated
        // request filters, to the routing table.
        return new CuracaoMethodInvokable(
            // Component mapping table, used internally to fetch instantiated
            // instances of a component.
            componentMappingTable_,
            // The "path" mapping for this invokable.
            mapping.value(),
            // Controller class and injectable constructor.
            controller, injectableCntlrCtor,
            // Path matcher class and injectable constructor.
            matcher, injectableMatcherCtor,
            // Filter class and injectable constructor.
            filter, injectableFilterCtor,
            // Method in controller class.
            method);
    }

}
