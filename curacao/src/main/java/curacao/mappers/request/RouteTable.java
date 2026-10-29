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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import curacao.annotations.Controller;
import curacao.annotations.Route;
import curacao.annotations.Route.Method;
import curacao.components.ComponentTable;
import curacao.core.CuracaoInvokable;
import curacao.core.CuracaoInvokable.InjectableComponent;
import curacao.exceptions.reflection.AbstractConfigurationException;
import curacao.exceptions.reflection.ComponentConfigurationException;
import curacao.mappers.request.filters.CuracaoRequestFilter;
import curacao.mappers.request.matchers.CuracaoPathMatcher;
import curacao.mappers.request.matchers.CuracaoRegexPathMatcher;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.util.reflection.CuracaoReflectionUtils.getControllersInBootPackage;
import static curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructorForClass;
import static curacao.util.reflection.CuracaoReflectionUtils.getRoutes;
import static org.slf4j.LoggerFactory.getLogger;

public final class RouteTable {

    private static final Logger LOG = getLogger(RouteTable.class);

    /**
     * An {@link ImmutableListMultimap} which maps a request method to a list of {@link CuracaoInvokable}'s.
     * When a request is received, this map is used to look up the set of invokables that are tied to a given HTTP
     * request method in O(1) constant time. The toolkit then walks that set looking for a match for the given
     * request path/route.
     */
    private final ImmutableListMultimap<Method, CuracaoInvokable> map_;

    /**
     * This context's controllers, path matchers and filters, by class: one instance of each, shared by its routes.
     */
    private final Cache<Class<?>, Object> instances_;

    /**
     * The context's core component mapping table.
     */
    private final ComponentTable componentTable_;

    public RouteTable(
            @Nonnull final ComponentTable componentTable) {
        componentTable_ = checkNotNull(componentTable, "Component mapping table cannot be null.");
        instances_ = CacheBuilder.newBuilder().build();
        // Scan the "controllers" inside of the declared boot package looking for annotated Java methods
        // that will be called when a request is received.
        map_ = buildRoutingTable();
        LOG.info("Application routing table: {}", map_);
    }

    /**
     * If no routes were found for the given {@link Route.Method}, this method is guaranteed to return
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
        // What each mapping (HTTP method, matcher and pattern) is mapped to, to find two handlers for one.
        final Map<String, String> mapped = Maps.newHashMap();
        LOG.debug("Found {} controllers annotated with @{}", controllers.size(), Controller.class.getSimpleName());
        // For each discovered controller class, find all annotated methods inside of it
        // and add them to the routing table.
        for (final Class<?> controller : controllers) {
            LOG.debug("Found @{}: {}", Controller.class.getSimpleName(), controller.getCanonicalName());
            // Fetch a list of all @Route annotated controller methods in the controller itself, and any
            // super classes walking up the class hierarchy.
            final Set<java.lang.reflect.Method> methods = getAllRoutesInHierarchy(controller);
            for (final java.lang.reflect.Method method : methods) {
                final Route route = method.getAnnotation(Route.class);
                // The actual "path" that the controller method will be called on when a request is received.
                final String path = route.value();
                // Attempt to construct a new invokable for the route and add it to the local routing table.
                try {
                    final CuracaoInvokable invokable = getInvokableForRoute(controller, method, route);
                    // A controller method's @Route may map multiple HTTP request method types
                    // to a single Java method. So, for each supported/annotated method...
                    for (final Method httpMethod : route.methods()) {
                        final String mapping = httpMethod + " " + route.matcher().getName() + " " + path;
                        final String handler = controller.getName() + "." + method.getName()
                                + Arrays.toString(method.getParameterTypes());
                        final String existing = mapped.putIfAbsent(mapping, handler);
                        if (handler.equals(existing)) {
                            // The same handler again: a method overriding an annotated one in a superclass.
                            continue;
                        } else if (existing != null) {
                            throw new ComponentConfigurationException("Ambiguous route: " + httpMethod + " "
                                    + path + " is mapped to both " + existing + " and " + handler + ".");
                        }
                        builder.put(httpMethod, invokable);
                        LOG.info("Successfully added route to route table (route={}:{}, invokable={})",
                                httpMethod, path, invokable);
                    }
                } catch (final Exception e) {
                    // A configuration error, not a failure we can work around: rather than silently drop
                    // the route, fail startup so the misconfiguration can't go unnoticed.
                    AbstractConfigurationException.rethrowIfPresent(e);
                    LOG.error("Failed to add route to routing table (route={}, methods={})", path,
                            Arrays.toString(route.methods()), e);
                }
            }
        }
        return builder.build();
    }

    private CuracaoInvokable getInvokableForRoute(
            final Class<?> controller,
            final java.lang.reflect.Method method,
            final Route route) throws Exception {
        checkNotNull(controller, "Controller cannot be null.");
        checkNotNull(method, "Method cannot be null.");
        checkNotNull(route, "Route cannot be null.");

        final Class<? extends CuracaoPathMatcher> matcher = route.matcher();
        if (CuracaoRegexPathMatcher.class.equals(matcher)) {
            // Compile the regex now, so a malformed one fails startup instead of failing to match every request.
            try {
                Pattern.compile(route.value());
            } catch (final PatternSyntaxException e) {
                throw new ComponentConfigurationException("The route on " + controller.getName() + "."
                        + method.getName() + "() isn't a valid regular expression: " + e.getMessage());
            }
        }
        final Class<? extends CuracaoRequestFilter>[] filters = route.filters();
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
                // This context's controller, matcher and filter instances.
                instances_,
                // The route pattern for this invokable.
                route.value(),
                // Controller class and injectable constructor.
                new InjectableComponent<>(controller, getInjectableConstructorForClass(controller)),
                // Path matcher class and injectable constructor.
                new InjectableComponent<>(matcher, getInjectableConstructorForClass(matcher)),
                // Filter classes and injectable constructors.
                filterList,
                // Method in controller class.
                method);
    }

    private static Set<java.lang.reflect.Method> getAllRoutesInHierarchy(
            final Class<?> clazz) {
        final Multimap<Class<?>, java.lang.reflect.Method> routes = getRoutes();

        final ImmutableSet.Builder<java.lang.reflect.Method> builder = ImmutableSet.builder();
        Class<?> superClass = clazz;
        // https://github.com/markkolich/curacao/issues/15
        // The logic herein crawls up the class hierarchy of a given @Controller looking for
        // methods annotated with the @Route annotation. This is fine, except that when we
        // crawl up the class hierarchy and get to java.lang.Object we waste cycles looking through
        // java.lang.Object for any methods annotated with @Route. Good times, because there
        // won't be any in java.lang.Object; why spend cycles using reflection checking java.lang.Object
        // for any @Route annotated methods when there won't be any? We avoid that wasteful
        // check by checking if the super class is equal to java.lang.Object; if it is, we just skip it
        // because Object is the guaranteed root of objects in Java.
        while (superClass != null && !Object.class.equals(superClass)) {
            builder.addAll(routes.get(superClass));
            // Will be null for "Object" (base class)
            superClass = superClass.getSuperclass();
        }
        return builder.build();
    }

}
