/*
 * Copyright (c) 2026 Mark S. Kolich
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

package curacao.mappers.request;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import curacao.annotations.Controller;
import curacao.annotations.RequestMapping;
import curacao.annotations.RequestMapping.Method;
import curacao.components.ComponentTable;
import curacao.core.CuracaoInvokable;
import curacao.core.CuracaoInvokable.InjectableComponent;
import curacao.mappers.request.filters.CuracaoRequestFilter;
import curacao.mappers.request.matchers.CuracaoPathMatcher;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.util.reflection.CuracaoReflectionUtils.getControllersInBootPackage;
import static curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructorForClass;
import static curacao.util.reflection.CuracaoReflectionUtils.getRequestMappings;
import static org.slf4j.LoggerFactory.getLogger;

public final class RequestMappingTable {

    private static final Logger LOG = getLogger(RequestMappingTable.class);

    /**
     * An {@link ImmutableListMultimap} which maps a request method to a list of {@link CuracaoInvokable}'s.
     * When a request is received, this map is used to look up the set of invokables that are tied to a given HTTP
     * request method in O(1) constant time. The toolkit then walks that set looking for a match for the given
     * request path/route.
     */
    private final ImmutableListMultimap<Method, CuracaoInvokable> map_;

    /**
     * The context's core component mapping table.
     */
    private final ComponentTable componentTable_;

    public RequestMappingTable(
            @Nonnull final ComponentTable componentTable) {
        componentTable_ = checkNotNull(componentTable, "Component mapping table cannot be null.");
        // Scan the "controllers" inside of the declared boot package looking for annotated Java methods
        // that will be called when a request is received.
        map_ = buildRoutingTable();
        LOG.info("Application routing table: {}", map_);
    }

    /**
     * If no routes were found for the given {@link RequestMapping.Method}, this method is guaranteed to return
     * an empty list. That is, it will never return null.
     */
    @Nonnull
    public List<CuracaoInvokable> getRoutesByHttpMethod(
            @Nonnull final Method method) {
        checkNotNull(method, "HTTP method cannot be null.");

        return map_.get(method);
    }

    private ImmutableListMultimap<Method, CuracaoInvokable> buildRoutingTable() {
        final ImmutableListMultimap.Builder<Method, CuracaoInvokable> builder = ImmutableListMultimap.builder();
        // Find all "controller classes" in the specified boot package that are annotated
        // with our @Controller annotation.
        final Set<Class<?>> controllers = getControllersInBootPackage();
        LOG.debug("Found {} controllers annotated with @{}", controllers.size(), Controller.class.getSimpleName());
        // For each discovered controller class, find all annotated methods inside of it
        // and add them to the routing table.
        for (final Class<?> controller : controllers) {
            LOG.debug("Found @{}: {}", Controller.class.getSimpleName(), controller.getCanonicalName());
            // Fetch a list of all request mapping annotated controller methods in the controller itself, and any
            // super classes walking up the class hierarchy.
            final Set<java.lang.reflect.Method> methods = getAllRequestMappingsInHierarchy(controller);
            for (final java.lang.reflect.Method method : methods) {
                final RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                // The actual "path" that the controller method will be called on when a request is received.
                final String route = mapping.value();
                // Attempt to construct a new invokable for the route and add it to the local routing table.
                try {
                    final CuracaoInvokable invokable = getInvokableForRoute(controller, method, mapping);
                    // The controller method request mapping annotation may map multiple HTTP request method types
                    // to a single Java method. So, for each supported/annotated method...
                    for (final Method httpMethod : mapping.methods()) {
                        builder.put(httpMethod, invokable);
                        LOG.info("Successfully added route to mapping table (route={}:{}, invokable={})",
                                httpMethod, route, invokable);
                    }
                } catch (final Exception e) {
                    LOG.error("Failed to add route to routing table (route={}, methods={})", route,
                            Arrays.toString(mapping.methods()), e);
                }
            }
        }
        return builder.build();
    }

    private CuracaoInvokable getInvokableForRoute(
            final Class<?> controller,
            final java.lang.reflect.Method method,
            final RequestMapping mapping) throws Exception {
        checkNotNull(controller, "Controller cannot be null.");
        checkNotNull(method, "Method cannot be null.");
        checkNotNull(mapping, "Request mapping cannot be null.");

        final Class<? extends CuracaoPathMatcher> matcher = mapping.matcher();
        final Class<? extends CuracaoRequestFilter>[] filters = mapping.filters();
        // For each annotation defined filter, add itself and any injectable
        // annotated constructors to the injectable filter list.
        final List<InjectableComponent<? extends CuracaoRequestFilter>> filterList =
                Lists.newArrayListWithCapacity(filters.length);
        for (final Class<? extends CuracaoRequestFilter> filter : filters) {
            filterList.add(new InjectableComponent<>(filter, getInjectableConstructorForClass(filter)));
        }

        // Attach the controller method, path matcher, and any annotated request filters, to the routing table.
        return new CuracaoInvokable(
                // Component mapping table, used internally to fetch instantiated instances of a component.
                componentTable_,
                // The "path" mapping for this invokable.
                mapping.value(),
                // Controller class and injectable constructor.
                new InjectableComponent<>(controller, getInjectableConstructorForClass(controller)),
                // Path matcher class and injectable constructor.
                new InjectableComponent<>(matcher, getInjectableConstructorForClass(matcher)),
                // Filter classes and injectable constructors.
                filterList,
                // Method in controller class.
                method);
    }

    private static Set<java.lang.reflect.Method> getAllRequestMappingsInHierarchy(
            final Class<?> clazz) {
        final Multimap<Class<?>, java.lang.reflect.Method> requestMappings = getRequestMappings();

        final ImmutableSet.Builder<java.lang.reflect.Method> builder = ImmutableSet.builder();
        Class<?> superClass = clazz;
        // https://github.com/markkolich/curacao/issues/15
        // The logic herein crawls up the class hierarchy of a given @Controller looking for
        // methods annotated with the @RequestMapping annotation. This is fine, except that when we
        // crawl up the class hierarchy and get to java.lang.Object we waste cycles looking through
        // java.lang.Object for any methods annotated with @RequestMapping. Good times, because there
        // won't be any in java.lang.Object; why spend cycles using reflection checking java.lang.Object
        // for any @RequestMapping annotated methods when there won't be any? We avoid that wasteful
        // check by checking if the super class is equal to java.lang.Object; if it is, we just skip it
        // because Object is the guaranteed root of objects in Java.
        while (superClass != null && !Object.class.equals(superClass)) {
            builder.addAll(requestMappings.get(superClass));
            // Will be null for "Object" (base class)
            superClass = superClass.getSuperclass();
        }
        return builder.build();
    }

}
