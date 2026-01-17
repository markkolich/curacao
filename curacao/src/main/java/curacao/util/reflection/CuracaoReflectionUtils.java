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

package curacao.util.reflection;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import curacao.CuracaoConfig;
import curacao.annotations.*;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("rawtypes") // for Constructor vs. Constructor<?>
public final class CuracaoReflectionUtils {

    private static final Logger LOG = getLogger(CuracaoReflectionUtils.class);

    private final Reflections reflections_;

    private final Supplier<Multimap<Class<?>, Constructor>> injectableConstructorsSupplier_;

    private final Supplier<Set<Class<?>>> componentTypesSupplier_;
    private final Supplier<Set<Class<?>>> controllerTypesSupplier_;
    private final Supplier<Set<Class<?>>> mapperTypesSupplier_;

    private final Supplier<Multimap<Class<?>, Method>> requestMappingMethodsSupplier_;

    private static final class LazyHolder {
        private static final CuracaoReflectionUtils INSTANCE = new CuracaoReflectionUtils();
    }

    public static CuracaoReflectionUtils getInstance() {
        return LazyHolder.INSTANCE;
    }

    private CuracaoReflectionUtils() {
        reflections_ = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(CuracaoConfig.getBootPackage()))
                .setScanners(Scanners.ConstructorsAnnotated, Scanners.MethodsAnnotated,
                        Scanners.FieldsAnnotated, Scanners.TypesAnnotated,
                        Scanners.SubTypes));

        injectableConstructorsSupplier_ =
                Suppliers.memoize(() -> reflections_.getConstructorsAnnotatedWith(Injectable.class).stream()
                .collect(ImmutableSetMultimap.<Constructor, Class<?>, Constructor>
                toImmutableSetMultimap(Constructor::getDeclaringClass, c -> c)));

        componentTypesSupplier_ =
                Suppliers.memoize(() -> reflections_.getTypesAnnotatedWith(Component.class));
        controllerTypesSupplier_ =
                Suppliers.memoize(() -> reflections_.getTypesAnnotatedWith(Controller.class));
        mapperTypesSupplier_ =
                Suppliers.memoize(() -> reflections_.getTypesAnnotatedWith(Mapper.class));

        requestMappingMethodsSupplier_ =
                Suppliers.memoize(() -> reflections_.getMethodsAnnotatedWith(RequestMapping.class).stream()
                .collect(ImmutableSetMultimap.<Method, Class<?>, Method>
                toImmutableSetMultimap(Method::getDeclaringClass, m -> m)));
    }

    // Static helpers

    public static Multimap<Class<?>, Constructor> getInjectableConstructors() {
        return getInstance().injectableConstructorsSupplier_.get();
    }

    public static Set<Class<?>> getComponentsInBootPackage() {
        return getInstance().componentTypesSupplier_.get();
    }

    public static Set<Class<?>> getControllersInBootPackage() {
        return getInstance().controllerTypesSupplier_.get();
    }

    public static Set<Class<?>> getMappersInBootPackage() {
        return getInstance().mapperTypesSupplier_.get();
    }

    public static Multimap<Class<?>, Method> getRequestMappings() {
        return getInstance().requestMappingMethodsSupplier_.get();
    }

    @Nullable
    @SuppressWarnings("rawtypes") // for Constructor vs. Constructor<?>
    public static Constructor<?> getInjectableConstructorForClass(
            final Class<?> clazz) throws Exception {
        // Find the one we're looking for on the exact class in question.
        final Collection<Constructor> matchingCtors = getInjectableConstructors().get(clazz);
        Constructor<?> result = null;
        if (matchingCtors.size() > 1) {
            // Ok, so the user has (perhaps mistakenly) annotated multiple constructors with the @Injectable
            // annotation. Find the constructor with the ~most~ arguments, and use that one.
            result = getConstructorWithMostParameters(clazz);
            LOG.warn("Found multiple constructors in class '{}' annotated with the @{} annotation. "
                    + "Will auto-inject the constructor with the most arguments: {}", clazz.getCanonicalName(),
                    Injectable.class.getSimpleName(), result);
        } else if (matchingCtors.size() == 1) {
            // The controller has exactly one injectable annotated constructor.
            result = matchingCtors.iterator().next();
        }
        return result;
    }

    /**
     * Given a class, uses reflection to find the constructor in the class with the most arguments/parameters.
     * If the class has no constructors, this method returns the default constructor that takes zero arguments.
     * <p>
     * This method is guaranteed to never return null; even if a class no explicit constructors defined, Java
     * guarantees that the class will have at least an empty (nullary) no-argument default constructor.
     */
    @Nonnull
    public static Constructor<?> getConstructorWithMostParameters(
            final Class<?> clazz) throws Exception {
        Constructor<?> result = null;
        // It seems that the call to get a list of constructors on a class never returns null. Per the docs, an
        // array of length 0 is returned if the class has no public constructors, or if the class is an array class,
        // or if the class reflects a primitive type or void.
        final Constructor<?>[] ctors = clazz.getConstructors();
        if (ctors.length == 0) {
            // The class has no constructors, so get the "default constructor".
            result = clazz.getConstructor();
        } else {
            // Iterate over the list of constructors in the class and find the one with the most arguments.
            for (final Constructor<?> c : ctors) {
                final int args = c.getParameterTypes().length;
                if (result == null || args > result.getParameterTypes().length) {
                    result = c;
                }
            }
        }
        return result;
    }

}
