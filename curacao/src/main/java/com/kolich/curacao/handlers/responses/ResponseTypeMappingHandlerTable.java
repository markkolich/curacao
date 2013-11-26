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

package com.kolich.curacao.handlers.responses;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.curacao.handlers.components.ComponentMappingTable.getComponentForType;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructor;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getTypesInPackageAnnotatedWith;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kolich.curacao.CuracaoConfigLoader;
import com.kolich.curacao.annotations.mappers.ControllerReturnTypeMapper;
import com.kolich.curacao.entities.CuracaoEntity;
import com.kolich.curacao.exceptions.CuracaoException;
import com.kolich.curacao.handlers.responses.mappers.RenderingResponseTypeMapper;
import com.kolich.curacao.handlers.responses.mappers.types.CuracaoEntityResponseMapper;
import com.kolich.curacao.handlers.responses.mappers.types.CuracaoExceptionWithEntityResponseMapper;
import com.kolich.curacao.handlers.responses.mappers.types.DefaultObjectResponseMapper;
import com.kolich.curacao.handlers.responses.mappers.types.DefaultThrowableResponseMapper;

public final class ResponseTypeMappingHandlerTable {
	
	private static final Logger logger__ = 
		getLogger(ResponseTypeMappingHandlerTable.class);
	
	private static final String CONTROLLER_RTN_TYPE_SN =
		ControllerReturnTypeMapper.class.getSimpleName();
	
	/**
	 * This table maps a set of known class instance types to their
	 * mapping response handlers.  Once a class type mapping response
	 * handler is discovered, its association with a known response handler
	 * is cached in the mapping cache providing O(1) constant lookup time.
	 */
	private final Map<Class<?>, RenderingResponseTypeMapper<?>> table_;
	
	/**
	 * This cache acts as a O(1) lookup helper which caches known class
	 * instance types to their mapping response handlers.
	 */
	private final Map<Class<?>, RenderingResponseTypeMapper<?>> cache_;
	
	private ResponseTypeMappingHandlerTable() {
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Loading response type mappers from " +
			"declared boot-package: " + bootPackage);
		// Scan the "mappers" inside of the declared boot package
		// looking for annotated Java classes that represent response type
		// mappers.
		table_ = buildMappingTable(bootPackage);
		cache_ = Maps.newConcurrentMap();
		if(logger__.isInfoEnabled()) {
			logger__.info("Application response type mapping table: " +
				table_);
		}
	}

    // This makes use of the "Initialization-on-demand holder idiom" which is
    // discussed in detail here:
    // http://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
    // As such, this is totally thread safe and performant.
	private static class LazyHolder {
		private static final ResponseTypeMappingHandlerTable instance__ =
			new ResponseTypeMappingHandlerTable();
	}
	private static final Map<Class<?>, RenderingResponseTypeMapper<?>> getTable() {
		return LazyHolder.instance__.table_;
	}
	private static final Map<Class<?>, RenderingResponseTypeMapper<?>> getCache() {
		return LazyHolder.instance__.cache_;
	}
	
	public static final void preload() {
		getTable();
	}
	
	public static final RenderingResponseTypeMapper<?>
		getHandlerForType(@Nonnull final Object result) {
		checkNotNull(result, "Result object cannot be null.");
		return getHandlerForType(result.getClass());
	}
	
	/**
	 * Examines the internal response type mapper cache and mapping table
	 * to find a suitable mapper that is capable of rending the object type
	 * represented by the given class.  Note that this method never returns
	 * null, and is guaranteed to always return some mapper.  That is even if
	 * the mapping table and cache contain no registered mappers for the given
	 * class type, this method will return a default generic mapper capable of
	 * serializing the object (which is really just equivalent to calling
	 * {@link Object#toString()}).
	 */
	public static final RenderingResponseTypeMapper<?>
		getHandlerForType(@Nonnull final Class<?> clazz) {
		checkNotNull(clazz, "Class instance type cannot be null.");
		final Map<Class<?>, RenderingResponseTypeMapper<?>>
			cache = getCache();
		RenderingResponseTypeMapper<?> handler = cache.get(clazz);
		if(handler == null) {
			final Map<Class<?>, RenderingResponseTypeMapper<?>>
				table = getTable();
			for(final Map.Entry<Class<?>, RenderingResponseTypeMapper<?>> entry :
				table.entrySet()) {
				final Class<?> type = entry.getKey();
				if(type.isAssignableFrom(clazz)) {
					handler = entry.getValue();
					cache.put(clazz, handler);
					break;
				}
			}
		}
		return handler;
	}
	
	private static final Map<Class<?>, RenderingResponseTypeMapper<?>>
		buildMappingTable(final String bootPackage) {
		// Using a LinkedHashMap internally because insertion order is
		// very important in this case.
		final Map<Class<?>, RenderingResponseTypeMapper<?>> mappers =
			Maps.newLinkedHashMap();  // Linked hash map to maintain order.
		// Find all "controller classes" in the specified boot package that
		// are annotated with our return type mapper annotation.
		final Set<Class<?>> mapperClasses =
			getTypesInPackageAnnotatedWith(bootPackage,
				ControllerReturnTypeMapper.class);
		logger__.debug("Found " + mapperClasses.size() + " mappers " +
			"annotated with @" + CONTROLLER_RTN_TYPE_SN);
		// For each discovered mapper class...
		for(final Class<?> mapper : mapperClasses) {
			logger__.debug("Found @" + CONTROLLER_RTN_TYPE_SN + ": " +
				mapper.getCanonicalName());
			final Class<?> superclazz = mapper.getSuperclass();
			if(!RenderingResponseTypeMapper.class.isAssignableFrom(superclazz)) {
				logger__.error("Class " + mapper.getCanonicalName() +
					" was annotated with @" + CONTROLLER_RTN_TYPE_SN +
					" but does not extend required superclass " +
					RenderingResponseTypeMapper.class.getSimpleName());
				continue;
			}
			try {
				final ControllerReturnTypeMapper ma = mapper.getAnnotation(
					ControllerReturnTypeMapper.class);
				// Locate a single constructor worthy of injecting with
				// components, if any.  May be null.
				final Constructor<?> ctor = getInjectableConstructor(mapper);
				RenderingResponseTypeMapper<?> instance = null;
				if(ctor == null) {
					// Class.newInstance() is evil, so we do the ~right~ thing
					// here to instantiate a new instance of the mapper using
					// the preferred getConstructor() idiom.
					instance = (RenderingResponseTypeMapper<?>)
						mapper.getConstructor().newInstance();					
				} else {
					final List<Class<?>> types = asList(ctor.getParameterTypes());
					final List<Object> params = Lists.newLinkedList();
					for(final Class<?> type : types) {
						params.add(getComponentForType(type));
					}
					instance = (RenderingResponseTypeMapper<?>)
						ctor.newInstance(params.toArray(new Object[]{}));
				}
				mappers.put(ma.value(), instance);
			} catch (Exception e) {
				logger__.error("Failed to instantiate response mapper " +
					"instance: " + mapper.getCanonicalName(), e);
			}
		}
		// Add the "default" mappers to the _end_ of the linked hash map.
		// Remember, linked hash map maintains order.
		mappers.putAll(getDefaultHandlers());
		return mappers;
	}
	
	private static final Map<Class<?>, RenderingResponseTypeMapper<?>>
		getDefaultHandlers() {
		final Map<Class<?>, RenderingResponseTypeMapper<?>> defaults =
			Maps.newLinkedHashMap(); // Linked hash map to maintain order.
		defaults.put(CuracaoEntity.class,
			new CuracaoEntityResponseMapper());
		defaults.put(CuracaoException.WithEntity.class,
			new CuracaoExceptionWithEntityResponseMapper());
		defaults.put(Throwable.class,
			new DefaultThrowableResponseMapper());		
		// Must be last since "Object" is the root of all types in Java.
		defaults.put(Object.class,
			new DefaultObjectResponseMapper());
		return defaults;
	}

}
