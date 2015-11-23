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

package curacao.util.reflection;

import com.google.common.collect.ImmutableSet;
import curacao.annotations.Injectable;
import org.apache.commons.io.FilenameUtils;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public final class CuracaoReflectionUtils {
	
	private static final Logger logger__ = getLogger(CuracaoReflectionUtils.class);
	
	// Cannot instantiate.
	private CuracaoReflectionUtils() {}
	
	public static final Reflections getTypeReflectionInstanceForPackage(final String pkg) {
		return new Reflections(new ConfigurationBuilder()
			.setUrls(ClasspathHelper.forPackage(pkg))
			// TODO: figure out the right way to use a parallel executor.
			// If we're going to use a parallel executor, that's fine except that we need provide our
			// own executor service; the default one provided by org.reflections doesn't spawn daemon
			// threads which hangs the app on graceful shutdown.
			/*.useParallelExecutor()*/
			.setScanners(new TypeAnnotationsScanner(), new SubTypesScanner()));
	}
	
	public static final Reflections getMethodReflectionInstanceForClass(final Class<?> clazz) {
        final String clazzCanonicalName = clazz.getCanonicalName();
		return new Reflections(new ConfigurationBuilder()
			.setUrls(ClasspathHelper.forClass(clazz))
			// TODO: figure out the right way to use a parallel executor.
			// If we're going to use a parallel executor, that's fine except that we need provide our
			// own executor service; the default one provided by org.reflections doesn't spawn daemon
			// threads which hangs the app on graceful shutdown.
			/*.useParallelExecutor()*/
			.filterInputsBy((input) -> {
				if (input == null) {
					return false;
				}
				// Remove the ".class" extension from the input string first and locate the canonical class.
				final String inputFqn = FilenameUtils.removeExtension(input)
                    // https://github.com/markkolich/curacao/issues/21
                    // Compiled inner classes look like this on the classpath: Foo$Bar.class
                    // And so, if we're looking for Foo.Bar.class, we have to replace the inner class "$"
                    // with a proper canonical separator "." to find it.
                    .replaceAll("\\$", "\\.");
				return clazzCanonicalName.equals(inputFqn);
			})
			.setScanners(new MethodAnnotationsScanner()));
	}
	
	public static final ImmutableSet<Class<?>> getTypesInPackageAnnotatedWith(final String pkg,
																			  final Class<? extends Annotation> annotation) {
		return ImmutableSet.copyOf(getTypeReflectionInstanceForPackage(pkg).getTypesAnnotatedWith(annotation));
	}
	
	@Nullable
	@SuppressWarnings("rawtypes") // for Constructor vs. Constructor<?>
	public static final Constructor<?> getInjectableConstructor(final Class<?> clazz) throws Exception {
		final Reflections reflect = getMethodReflectionInstanceForClass(clazz);
		// Get all constructors annotated with the injectable annotation.
		final Set<Constructor> injectableCtors = reflect.getConstructorsAnnotatedWith(Injectable.class);
		Constructor<?> result = null;
		if (injectableCtors.size() > 1) {
			// Ok, so the user has (perhaps mistakenly) annotated multiple constructors with the @Injectable
			// annotation.  Find the constructor with the ~most~ arguments, and use that one.
			result = getConstructorWithMostParameters(clazz);
			logger__.warn("Found multiple constructors in class `{}` annotated with the @{} annotation. " +
				"Will auto-inject the constructor with the most arguments: {}", clazz.getCanonicalName(),
				Injectable.class.getSimpleName(), result);
		} else if (injectableCtors.size() == 1) {
			// The controller has exactly one injectable annotated constructor.
			result = injectableCtors.iterator().next();
		}
		return result;
	}

	/**
	 * Given a class, uses reflection to find the constructor in the class with the most arguments/parameters.
	 * If the class has no constructors, this method returns the default constructor that takes zero arguments.
	 *
	 * This method is guaranteed to never return null; even if a class no explicit constructors defined, Java
	 * guarantees that the class will have at least an empty (nullary) no-argument default constructor.
	 */
	@Nonnull
	public static final Constructor<?> getConstructorWithMostParameters(final Class<?> clazz) throws Exception {
		Constructor<?> result = null;
		// It seems that the call to get a list of constructors on a class never returns null.  Per the docs, an
		// array of length 0 is returned if the class has no public constructors, or if the class is an array class,
		// or if the class reflects a primitive type or void.
		Constructor<?>[] ctors = clazz.getConstructors();
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
