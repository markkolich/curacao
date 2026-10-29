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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import curacao.CuracaoConfig;
import curacao.annotations.*;
import curacao.exceptions.reflection.ComponentConfigurationException;
import curacao.exceptions.reflection.MultipleInjectableConstructorsException;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@SuppressWarnings("rawtypes") // for Constructor vs. Constructor<?>
public final class CuracaoReflectionUtils {

    private final Reflections reflections_;

    private final Supplier<Set<Class<?>>> componentTypesSupplier_;
    private final Supplier<Set<Class<?>>> controllerTypesSupplier_;
    private final Supplier<Set<Class<?>>> mapperTypesSupplier_;

    private final Supplier<Multimap<Class<?>, Method>> routeMethodsSupplier_;

    private static final class LazyHolder {
        private static final CuracaoReflectionUtils INSTANCE = new CuracaoReflectionUtils();
    }

    public static CuracaoReflectionUtils getInstance() {
        return LazyHolder.INSTANCE;
    }

    private CuracaoReflectionUtils() {
        final String bootPackage = CuracaoConfig.getBootPackage();
        // forPackage() only finds the classpath roots (directories or JARs) that hold the boot package; without
        // the input filter, every class in those roots would be scanned, including every library in a fat JAR.
        reflections_ = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(bootPackage))
                .filterInputsBy(new FilterBuilder().includePattern(Pattern.quote(bootPackage + ".") + ".*"))
                .setScanners(Scanners.MethodsAnnotated,
                        Scanners.FieldsAnnotated, Scanners.TypesAnnotated,
                        Scanners.SubTypes));

        componentTypesSupplier_ =
                Suppliers.memoize(() -> reflections_.getTypesAnnotatedWith(Component.class));
        controllerTypesSupplier_ =
                Suppliers.memoize(() -> reflections_.getTypesAnnotatedWith(Controller.class));
        mapperTypesSupplier_ =
                Suppliers.memoize(() -> reflections_.getTypesAnnotatedWith(Mapper.class));

        routeMethodsSupplier_ =
                Suppliers.memoize(() -> reflections_.getMethodsAnnotatedWith(Route.class).stream()
                .collect(ImmutableSetMultimap.<Method, Class<?>, Method>
                toImmutableSetMultimap(Method::getDeclaringClass, m -> m)));
    }

    // Static helpers

    public static Set<Class<?>> getComponentsInBootPackage() {
        return getInstance().componentTypesSupplier_.get();
    }

    public static Set<Class<?>> getControllersInBootPackage() {
        return getInstance().controllerTypesSupplier_.get();
    }

    public static Set<Class<?>> getMappersInBootPackage() {
        return getInstance().mapperTypesSupplier_.get();
    }

    public static Multimap<Class<?>, Method> getRoutes() {
        return getInstance().routeMethodsSupplier_.get();
    }

    /**
     * Returns the constructor of the given class annotated with {@link Injectable}, or null if it has none.
     * <p>
     * As in JSR-330, at most one constructor per class may be annotated with {@link Injectable}. A class
     * with more than one is a configuration error: rather than guess which constructor was intended, this
     * method throws a {@link MultipleInjectableConstructorsException} naming the class and its annotated
     * constructors.
     */
    @Nullable
    public static Constructor<?> getInjectableConstructorForClass(
            final Class<?> clazz) throws Exception {
        // Find the one we're looking for on the exact class in question.
        final List<Constructor<?>> matchingCtors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Injectable.class))
                .collect(ImmutableList.toImmutableList());
        Constructor<?> result = null;
        if (matchingCtors.size() > 1) {
            // The user has (perhaps mistakenly) annotated multiple constructors with the @Injectable
            // annotation. There's no way to know which one they meant, so fail instead of guessing.
            throw new MultipleInjectableConstructorsException(String.format("Found %d constructors in class "
                    + "'%s' annotated with the @%s annotation, but at most one is allowed: %s",
                    matchingCtors.size(), clazz.getCanonicalName(), Injectable.class.getSimpleName(),
                    matchingCtors));
        } else if (matchingCtors.size() == 1) {
            // The controller has exactly one injectable annotated constructor.
            result = matchingCtors.iterator().next();
        }
        return result;
    }

    /**
     * The public no-argument constructor of a class that has no {@link Injectable} constructor. Curacao has
     * nothing to pass to any other constructor: a constructor whose parameters should be injected needs
     * {@link Injectable}, rather than being called with nulls.
     *
     * @throws ComponentConfigurationException if the class has no public no-argument constructor
     */
    @Nonnull
    public static Constructor<?> getNoArgConstructorForClass(
            final Class<?> clazz) {
        try {
            return clazz.getConstructor();
        } catch (final NoSuchMethodException e) {
            throw new ComponentConfigurationException(clazz.getName() + " has no @" + Injectable.class.getSimpleName()
                    + " constructor and no public no-argument constructor; annotate the constructor whose "
                    + "arguments Curacao should inject with @" + Injectable.class.getSimpleName() + ".");
        }
    }

}
