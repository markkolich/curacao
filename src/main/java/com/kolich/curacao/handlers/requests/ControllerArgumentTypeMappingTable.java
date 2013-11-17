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

package com.kolich.curacao.handlers.requests;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.kolich.curacao.CuracaoConfigLoader;
import com.kolich.curacao.annotations.mappers.ControllerArgumentTypeMapper;
import com.kolich.curacao.handlers.requests.mappers.ControllerArgumentMapper;
import com.kolich.curacao.handlers.requests.mappers.types.HttpServletRequestMapper;
import com.kolich.curacao.handlers.requests.mappers.types.HttpServletResponseMapper;
import com.kolich.curacao.handlers.requests.mappers.types.IntegerArgumentMapper;
import com.kolich.curacao.handlers.requests.mappers.types.LongArgumentMapper;
import com.kolich.curacao.handlers.requests.mappers.types.ServletInputStreamMapper;
import com.kolich.curacao.handlers.requests.mappers.types.ServletOutputStreamMapper;
import com.kolich.curacao.handlers.requests.mappers.types.StringMapper;
import com.kolich.curacao.handlers.requests.mappers.types.body.ByteArrayInputStreamRequestMapper;
import com.kolich.curacao.handlers.requests.mappers.types.body.ByteBufferRequestMapper;
import com.kolich.curacao.handlers.requests.mappers.types.body.EncodedPostBodyMultiMapMapper;
import com.kolich.curacao.handlers.requests.mappers.types.body.InputStreamReaderRequestMapper;
import com.kolich.curacao.handlers.requests.mappers.types.body.RequestBodyAsCharsetAwareStringMapper;

public final class ControllerArgumentTypeMappingTable {
	
	private static final Logger logger__ = 
		getLogger(ControllerArgumentTypeMappingTable.class);
	
	/**
	 * This table maps a set of known class instance types to their
	 * argument mappers.  A multi-map allows multiple argument mappers to
	 * be registered for a single Class type.
	 */
	private final Multimap<Class<?>, ControllerArgumentMapper<?>> table_;
		
	private ControllerArgumentTypeMappingTable() {
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Loading controller argument mappers from " +
			"declared boot-package: " + bootPackage);
		// Scan the "mappers" inside of the declared boot package
		// looking for annotated Java classes that represent argument
		// mappers.
		table_ = buildArgumentMappingTable(bootPackage);
	}
	
	private static class LazyHolder {
		private static final ControllerArgumentTypeMappingTable instance__ =
			new ControllerArgumentTypeMappingTable();
	}
	private static final Multimap<Class<?>, ControllerArgumentMapper<?>>
		getTable() {
		return LazyHolder.instance__.table_;
	}
	
	public static final void preload() {
		getTable();
	}
	
	/**
	 * Examines the internal argument mapper cache and mapping table
	 * to find a suitable set of mappers that are capable of extracting the
	 * arg type represented by the given class.  Note that this method never
	 * returns null.  Even if no mappers exists for the given class type,
	 * an empty collection is returned.
	 */
	public static final Collection<ControllerArgumentMapper<?>>
		getArgumentMappersForType(final Class<?> clazz) {
		checkNotNull(clazz, "Class instance type cannot be null.");
		return Collections.unmodifiableCollection(getTable().get(clazz));
	}
	
	private static final Multimap<Class<?>, ControllerArgumentMapper<?>>
		buildArgumentMappingTable(final String bootPackage) {
		// Using a LinkedHashMultimap internally because insertion order is
		// very important in this case.
		final Multimap<Class<?>, ControllerArgumentMapper<?>> mappers =
			LinkedHashMultimap.create();
		// Use the reflections package scanner to scan the boot package looking
		// for all classes therein that contain "annotated" mapper classes.
		final Reflections mapperReflection = new Reflections(
			new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage(bootPackage))
				.setScanners(new TypeAnnotationsScanner()));
		// Find all "controller classes" in the specified boot package that
		// are annotated with our return type mapper annotation.
		final Set<Class<?>> mapperClasses =
			mapperReflection.getTypesAnnotatedWith(ControllerArgumentTypeMapper.class);
		logger__.debug("Found " + mapperClasses.size() + " mappers " +
			"annotated with @" + ControllerArgumentTypeMapper.class.getSimpleName());
		// For each discovered mapper class...
		for(final Class<?> mapper : mapperClasses) {
			logger__.debug("Found @" + ControllerArgumentTypeMapper.class.getSimpleName() +
				": " + mapper.getCanonicalName());
			final Class<?> superclazz = mapper.getSuperclass();
			if(!ControllerArgumentMapper.class.isAssignableFrom(superclazz)) {
				logger__.error("Class " + mapper.getCanonicalName() +
					" was annotated with @" + ControllerArgumentTypeMapper.class.getSimpleName() +
					" but does not extend required superclass " +
					ControllerArgumentMapper.class.getSimpleName());
				continue;
			}
			try {
				final ControllerArgumentTypeMapper ma = mapper.getAnnotation(
					ControllerArgumentTypeMapper.class);
				// Class.newInstance() is evil, so we do the ~right~ thing
				// here to instantiate a new instance of the mapper using
				// the preferred getConstructor() idiom.
				final Constructor<?> ctor = mapper.getConstructor();
				mappers.put(ma.value(),
					(ControllerArgumentMapper<?>)
						ctor.newInstance());
			} catch (NoSuchMethodException e) {
				logger__.error("Failed to instantiate controller argument " +
					"mapper instance: " + mapper.getCanonicalName() +
					" -- This class is very likely missing a nullary (no " +
					"argument) constructor. Please add one.", e);
			} catch (Exception e) {
				logger__.error("Failed to instantiate controller argument " +
					"mapper instance: " + mapper.getCanonicalName(), e);
			}
		}
		// Add the "default" mappers to the ~end~ of the linked hash multi map.
		// This essentially means that default argument mappers (the ones
		// provided by this library) are found & called after any user registered
		// mappers.
		mappers.putAll(getDefaultMappers());
		return mappers;
	}
	
	private static final Multimap<Class<?>, ControllerArgumentMapper<?>>
		getDefaultMappers() {
		final Multimap<Class<?>, ControllerArgumentMapper<?>> defaults =
			LinkedHashMultimap.create(); // Linked hash multimap to maintain order.
		defaults.put(String.class, new StringMapper());
		defaults.put(Integer.class, new IntegerArgumentMapper());
		defaults.put(Long.class, new LongArgumentMapper());
		defaults.put(ServletInputStream.class, new ServletInputStreamMapper());
		defaults.put(ServletOutputStream.class, new ServletOutputStreamMapper());
		defaults.put(HttpServletRequest.class, new HttpServletRequestMapper());
		defaults.put(HttpServletResponse.class, new HttpServletResponseMapper());
		// Request body helpers; safely buffers the requesty body into
		// buffers in memory.
		defaults.put(ByteBuffer.class, new ByteBufferRequestMapper());
		defaults.put(ByteArrayInputStream.class, new ByteArrayInputStreamRequestMapper());
		defaults.put(InputStreamReader.class, new InputStreamReaderRequestMapper());
		defaults.put(String.class, new RequestBodyAsCharsetAwareStringMapper());
		defaults.put(Multimap.class, new EncodedPostBodyMultiMapMapper());
		return defaults;
	}

}
