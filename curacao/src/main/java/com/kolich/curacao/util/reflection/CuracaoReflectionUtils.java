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

package com.kolich.curacao.util.reflection;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.kolich.curacao.annotations.Injectable;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public final class CuracaoReflectionUtils {
	
	private static final Logger logger__ = 
		getLogger(CuracaoReflectionUtils.class);

    private static final String CLASS_EXTENSION = ".class";
	
	// Cannot instantiate.
	private CuracaoReflectionUtils() {}
	
	public static final Reflections getTypeReflectionInstanceForPackage(
		final String pkg) {
		return new Reflections(
			new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage(pkg))
				.setScanners(new TypeAnnotationsScanner()));
	}
	
	public static final Reflections getMethodReflectionInstanceForClass(
		final Class<?> clazz) {
        final String clazzCanonicalName = clazz.getCanonicalName();
		return new Reflections(
			new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forClass(clazz))
				.filterInputsBy(new Predicate<String>() {
		            @Override
					public boolean apply(final String input) {
                        if (input == null) {
                            return false;
                        } else {
                            final String cNinputFqn =
                                getCanonicalClassFromInputFqn(input);
                            return clazzCanonicalName.equals(cNinputFqn);
                        }
		            }})
				.setScanners(new MethodAnnotationsScanner()));
	}
	
	public static final ImmutableSet<Class<?>> getTypesInPackageAnnotatedWith(
		final String pkg, final Class<? extends Annotation> annotation) {
		return ImmutableSet.copyOf(getTypeReflectionInstanceForPackage(pkg)
			.getTypesAnnotatedWith(annotation));
	}
	
	@Nullable
	@SuppressWarnings("rawtypes") // for Constructor vs. Constructor<?>
	public static final Constructor<?> getInjectableConstructor(
		final Class<?> clazz) {
		final Reflections reflect = getMethodReflectionInstanceForClass(clazz);
		// Get all constructors annotated with the injectable annotation.
		final Set<Constructor> ctors =
			reflect.getConstructorsAnnotatedWith(Injectable.class);
		Constructor<?> result = null;
		if (ctors.size() > 1) {
			// Find the constructor with the ~most~ arguments, and we'll use
			// that one going forward.
			for (final Constructor<?> c : ctors) {
				final int args = c.getParameterTypes().length;
				if (result == null || args > result.getParameterTypes().length) {
					result = c;
				}
			}
			logger__.warn("Found multiple constructors in class `{}`" +
				"annotated with the @{} annotation.  Will auto-inject the " +
				"constructor with the most arguments: ",
				clazz.getCanonicalName(), Injectable.class.getSimpleName(),
				result);
		} else if (ctors.size() == 1) {
			// The controller has exactly one injectable annotated constructor.
			result = ctors.iterator().next();
		}
		return result;
	}

    private static final String getCanonicalClassFromInputFqn(final String input) {
        // Split on "$" for FQN's that look like:
        //   com.a.b.c.D$Foobar.class
        // Even if there's no "$" to split on, index zero of the resulting
        // split should just be the original input string, and never null.
        String s = input.split("\\$")[0];
        // If the the splitted string ends with ".class" remove the suffix.
        //   com.a.b.c.Foobar.class
        // So we'll be left with just com.a.b.c.Foobar as desired.
        if (s.endsWith(CLASS_EXTENSION)) {
            s = s.substring(0, s.length() - 6);
        }
        return s;
    }

}
