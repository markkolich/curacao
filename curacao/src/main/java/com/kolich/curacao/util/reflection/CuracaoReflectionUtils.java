/**
 * Copyright (c) 2013 Mark S. Kolich
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

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Set;

import javax.annotation.Nullable;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import com.google.common.base.Predicate;
import com.kolich.curacao.annotations.Injectable;
import com.kolich.curacao.handlers.requests.ControllerRoutingTable;

public final class CuracaoReflectionUtils {
	
	private static final Logger logger__ = 
		getLogger(ControllerRoutingTable.class);
	
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
		return new Reflections(
			new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forClass(clazz))
				.filterInputsBy(new Predicate<String>() {
		            @Override
					public boolean apply(final String input) {
		                return input != null &&
		                	// Intentionally limits the scanner to find
		                	// methods only inside of the discovered
		                	// controller class.
		                	input.startsWith(clazz.getCanonicalName());
		            }})
				.setScanners(new MethodAnnotationsScanner()));
	}
	
	public static final Set<Class<?>> getTypesInPackageAnnotatedWith(
		final String pkg, final Class<? extends Annotation> annotation) {
		return getTypeReflectionInstanceForPackage(pkg)
			.getTypesAnnotatedWith(annotation);
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
		if(ctors.size() > 1) {
			// Find the constructor with the ~most~ arguments, and we'll use
			// that one going forward.
			for(final Constructor<?> c : ctors) {
				final int args = c.getParameterTypes().length;
				if(result == null || args > result.getParameterTypes().length) {
					result = c;
				}
			}
			logger__.warn("Found multiple constructors in class " +
				clazz.getCanonicalName() + " annotated with the @" +
				Injectable.class.getSimpleName() + " annotation.  Will " +
				"auto-inject the constructor with the most arguments: " +
				result);
		} else if(ctors.size() == 1) {
			// The controller has exactly one injectable annotated constructor.
			result = ctors.iterator().next();
		}
		return result;
	}

}
