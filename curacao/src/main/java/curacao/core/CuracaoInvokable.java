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

package curacao.core;

import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableList;
import curacao.annotations.Injectable;
import curacao.components.ComponentTable;
import curacao.exceptions.CuracaoException;
import curacao.mappers.request.RouteSpecificity;
import curacao.mappers.request.filters.CuracaoRequestFilter;
import curacao.mappers.request.matchers.CuracaoPathMatcher;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.util.reflection.CuracaoReflectionUtils.getNoArgConstructorForClass;

public final class CuracaoInvokable {

    /**
     * The context's core component mapping table.
     */
    public final ComponentTable componentTable_;

    /**
     * The context's instances of controllers, path matchers and filters, by class, shared by all of its routes so
     * that each class is instantiated once per context.
     */
    private final Cache<Class<?>, Object> instances_;

    /**
     * The route pattern registered with this invokable, from its {@link curacao.annotations.Route#value()}.
     */
    public final String pattern_;

    /**
     * How specific the route pattern is, for choosing among several routes that match one request.
     */
    public final RouteSpecificity specificity_;

    /**
     * The controller class this invokable is attached to.
     */
    public final InvokableClassWithInstance<?> controller_;

    /**
     * The path matcher, that is attached to this invokable controller method.
     */
    public final InvokableClassWithInstance<? extends CuracaoPathMatcher> matcher_;

    /**
     * The filters that are attached to this invokable controller method.
     */
    public final List<InvokableClassWithInstance<? extends CuracaoRequestFilter>> filters_;

    /**
     * The controller Java method itself.
     */
    public final Method method_;

    /**
     * The arguments/parameters for the controller Java method. Will be
     * an array of length zero if the underlying Java method takes no
     * parameters.
     */
    public final Class<?>[] parameterTypes_;

    /**
     * The annotation collection associated with each argument/parameter of
     * the controller Java method. Will be an array of length zero if the
     * underlying Java method takes no parameters.
     */
    public final Annotation[][] parameterAnnotations_;

    public CuracaoInvokable(
            @Nonnull final ComponentTable componentTable,
            @Nonnull final Cache<Class<?>, Object> instances,
            @Nonnull final String pattern,
            @Nonnull final InjectableComponent<?> controller,
            @Nonnull final InjectableComponent<? extends CuracaoPathMatcher> matcher,
            @Nonnull final List<InjectableComponent<? extends CuracaoRequestFilter>> filters,
            @Nonnull final Method method) {
        componentTable_ = checkNotNull(componentTable, "Component table cannot be null.");
        instances_ = checkNotNull(instances, "Instance cache cannot be null.");
        pattern_ = checkNotNull(pattern, "Route pattern cannot be null.");
        checkNotNull(controller, "Controller base class cannot be null.");
        checkNotNull(matcher, "Path matcher injectable cannot be null.");
        specificity_ = RouteSpecificity.of(pattern_, matcher.clazz_);
        checkNotNull(filters, "Method filter injectable list cannot be null.");
        method_ = checkNotNull(method, "Controller method cannot be null.");

        // Instantiate a new instance of the controller class itself.
        try {
            controller_ = new InvokableClassWithInstance<>(controller.clazz_, controller.constructor_);
        } catch (final Exception e) {
            throw new CuracaoException("Failed to instantiate controller instance: "
                    + controller.clazz_.getCanonicalName(), e);
        }

        // Instantiate a new instance of the underlying path matcher class
        // attached to the controller method.
        try {
            matcher_ = new InvokableClassWithInstance<>(matcher.clazz_, matcher.constructor_);
        } catch (final Exception e) {
            throw new CuracaoException("Failed to instantiate method path matcher instance: "
                    + matcher.clazz_.getCanonicalName(), e);
        }

        // Instantiate a new instance of the filter classes attached to the controller method.
        try {
            final ImmutableList.Builder<InvokableClassWithInstance<? extends CuracaoRequestFilter>> builder =
                    ImmutableList.builder();
            for (final InjectableComponent<? extends CuracaoRequestFilter> filter : filters) {
                builder.add(new InvokableClassWithInstance<>(filter.clazz_, filter.constructor_));
            }
            filters_ = builder.build();
        } catch (final CuracaoException e) {
            throw e;
        } catch (final Exception e) {
            throw new CuracaoException("Failed to instantiate request filters.", e);
        }

        parameterTypes_ = method_.getParameterTypes();
        parameterAnnotations_ = method_.getParameterAnnotations();
    }

    @Override
    public String toString() {
        return String.format("%s.%s(%s)",
                controller_.clazz_.getCanonicalName(),
                method_.getName(),
                StringUtils.join(parameterTypes_, ", "));
    }

    public static final class InjectableComponent<T> {

        public final Class<? extends T> clazz_;
        public final Constructor<?> constructor_;

        public InjectableComponent(
                @Nonnull final Class<? extends T> clazz,
                @Nullable final Constructor<?> constructor) {
            clazz_ = checkNotNull(clazz, "Injectable class cannot be null.");
            constructor_ = constructor;
        }

    }

    public final class InvokableClassWithInstance<T> {

        /**
         * The class instance type of this invokable.
         */
        @Nonnull
        public final Class<T> clazz_;

        /**
         * A class constructor annotated with the {@link Injectable} constructor annotation.
         * May be null if the controller class has no {@link Injectable} annotated constructors.
         */
        @Nullable
        public final Constructor<?> injectable_;

        /**
         * An instantiated instance of the invokable class.
         */
        @Nonnull
        public final T instance_;

        @SuppressWarnings("unchecked")
        public InvokableClassWithInstance(
                @Nonnull final Class<T> clazz,
                @Nullable final Constructor<?> injectable) throws Exception {
            clazz_ = checkNotNull(clazz, "Class cannot be null.");
            injectable_ = injectable;
            // https://github.com/markkolich/curacao/issues/23
            // Only attempt to instantiate a new instance of the invokable if the context's cache does
            // not already contain an instance. This avoids instantiating multiple copies of the same
            // controller instance; one per route. The cache belongs to the context, not the JVM, so a
            // restarted context, or a second one in the same JVM, builds its own instances.
            instance_ = (T) instances_.get(clazz, () -> newInstance(clazz_));
        }

        @SuppressWarnings("unchecked")
        private T newInstance(
                final Class<?> clazz) throws Exception {
            T instance = null;
            // The injectable will be null if the class has no injectable annotated constructors.
            if (injectable_ == null) {
                // No @Injectable constructor: the public no-argument one, or a startup failure.
                instance = (T) getNoArgConstructorForClass(clazz).newInstance();
            } else {
                // The injectable here is a filter or controller class constructor, whose parameters are
                // components, resolved by the component table (which fails on an ambiguous or missing one).
                instance = (T) injectable_.newInstance(componentTable_.resolveArguments(injectable_, clazz_));
            }
            return instance;
        }

    }

}
