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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import curacao.annotations.Injectable;
import curacao.components.ComponentTable;
import curacao.exceptions.CuracaoException;
import curacao.exceptions.reflection.ArgumentRequiredException;
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
import static curacao.util.reflection.CuracaoAnnotationUtils.hasAnnotation;
import static curacao.util.reflection.CuracaoReflectionUtils.getConstructorWithMostParameters;

public final class CuracaoInvokable {

    public static final class InjectableComponent<T> {
        public final Class<? extends T> clazz_;
        public final Constructor<?> constructor_;
        public InjectableComponent(@Nonnull final Class<? extends T> clazz,
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
        public InvokableClassWithInstance(@Nonnull final Class<T> clazz,
                                          @Nullable final Constructor<?> injectable) throws Exception {
            clazz_ = checkNotNull(clazz, "Class cannot be null.");
            injectable_ = injectable;
            // https://github.com/markkolich/curacao/issues/23
            // Only attempt to instantiate a new instance of the invokable if the internal cache does
            // not already contain an instance.  This avoids instantiating multiple copies of the same
            // controller instance; one per route.
            instance_ = (T)registeredInvokables.get(clazz, () -> newInstance(clazz_));
        }

        @SuppressWarnings("unchecked")
        private T newInstance(final Class<?> clazz) throws Exception {
            T instance = null;
            // The injectable will be null if the class has no injectable annotated constructors.
            if (injectable_ == null) {
                // Class.newInstance() is evil, so we do the ~right~ thing here to instantiate new instances
                // using the preferred getConstructor() idiom.
                final Constructor<?> plainCtor = getConstructorWithMostParameters(clazz);
                final int paramCount = plainCtor.getParameterTypes().length;
                instance = (T)plainCtor.newInstance(new Object[paramCount]);
            } else {
                // The injectable here is a filter or controller class constructor.
                final Class<?>[] types = injectable_.getParameterTypes();
                final Object[] params = new Object[types.length];
                for (int i = 0, l = types.length; i < l; i++) {
                    // The instance constructor may define a set of components that should be "injected". For each
                    // of those types, look them up in the component mapping table. Note that the component mapping
                    // table is guaranteed to exist and contain components before we even get here.
                    params[i] = componentTable_.getComponentForType(types[i]);
                    // https://github.com/markkolich/curacao/issues/18
                    // If the constructor argument parameter was null, this implies that we could not find a
                    // suitable "component" or object to provide for this constructor argument. As such, we need
                    // to verify if the parameter is annotated with @Nonnull and if it is, fail hard.
                    if (params[i] == null) {
                        // Get a list of annotations attached to this constructor argument.
                        final Annotation[] annotations = injectable_.getParameterAnnotations()[i];
                        // Is any annotation on the argument annotated with @Nonnull?
                        if (hasAnnotation(annotations, Nonnull.class)) {
                            throw new ArgumentRequiredException("Could not resolve " +
                                "@" + Nonnull.class.getSimpleName() + " constructor argument `" +
                                types[i].getCanonicalName() + "` on class: " + clazz_.getCanonicalName());
                        }
                    }
                }
                instance = (T)injectable_.newInstance(params);
            }
            return instance;
        }

    }

    /**
     * An in-heap cache, mapping a class to a singleton instance of an invokable.
     */
    private static final Cache<Class<?>, Object> registeredInvokables = CacheBuilder.newBuilder().build();

    /**
     * The context's core component mapping table.
     */
    public final ComponentTable componentTable_;

    /**
     * The path mapping registered/associated with this invokable.
     */
    public final String mapping_;

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
     * the controller Java method.  Will be an array of length zero if the
     * underlying Java method takes no parameters.
     */
    public final Annotation[][] parameterAnnotations_;

    public CuracaoInvokable(@Nonnull final ComponentTable componentTable,
                            @Nonnull final String mapping,
                            @Nonnull final InjectableComponent<?> controller,
                            @Nonnull final InjectableComponent<? extends CuracaoPathMatcher> matcher,
                            @Nonnull final List<InjectableComponent<? extends CuracaoRequestFilter>> filters,
                            @Nonnull final Method method) {
        componentTable_ = checkNotNull(componentTable, "Component table cannot be null.");
        mapping_ = checkNotNull(mapping, "Request mapping cannot be null.");
        checkNotNull(controller, "Controller base class cannot be null.");
        checkNotNull(matcher, "Path matcher injectable cannot be null.");
        checkNotNull(filters, "Method filter injectable list cannot be null.");
        method_ = checkNotNull(method, "Controller method cannot be null.");
        // Instantiate a new instance of the controller class.
        try {
            controller_ = new InvokableClassWithInstance<>(controller.clazz_, controller.constructor_);
        } catch (NoSuchMethodException e) {
            throw new CuracaoException("Failed to instantiate controller instance: " +
                controller.clazz_.getCanonicalName() + " -- This class is likely missing a nullary (no argument) " +
                "constructor. Please add one.", e);
        } catch (Exception e) {
            throw new CuracaoException("Failed to instantiate controller instance.", e);
        }
        // Instantiate a new instance of the underlying path matcher class
        // attached to the controller method.
        try {
            matcher_ = new InvokableClassWithInstance<>(matcher.clazz_, matcher.constructor_);
        } catch (NoSuchMethodException e) {
            throw new CuracaoException("Failed to instantiate method path matcher class instance: " +
                matcher.clazz_.getCanonicalName() + " -- This class is likely missing a nullary (no argument) " +
                "constructor. Please add one.", e);
        } catch (Exception e) {
            throw new CuracaoException("Failed to instantiate method path matcher instance.", e);
        }
        // Instantiate a new instance of the filter classes attached to the controller method.
        try {
            ImmutableList.Builder<InvokableClassWithInstance<? extends CuracaoRequestFilter>> builder =
                ImmutableList.builder();
            for (final InjectableComponent<? extends CuracaoRequestFilter> filter : filters) {
                try {
                    builder.add(new InvokableClassWithInstance<>(filter.clazz_, filter.constructor_));
                } catch (NoSuchMethodException e) {
                    throw new CuracaoException("Failed to instantiate method filter class instance: " +
                        filter.clazz_.getCanonicalName() + " -- This class is likely missing a nullary " +
                        "(no argument) constructor. Please add one.", e);
                }
            }
            filters_ = builder.build();
        } catch (CuracaoException e) {
           throw e;
        } catch (Exception e) {
            throw new CuracaoException("Failed to instantiate request filters.", e);
        }
        parameterTypes_ = method_.getParameterTypes();
        parameterAnnotations_ = method_.getParameterAnnotations();
    }
    
    @Override
    public final String toString() {
        return String.format("%s.%s(%s)",
            controller_.clazz_.getCanonicalName(),
            method_.getName(),
            StringUtils.join(parameterTypes_, ", "));
    }

}
