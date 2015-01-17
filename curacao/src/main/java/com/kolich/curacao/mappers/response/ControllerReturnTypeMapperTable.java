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

package com.kolich.curacao.mappers.response;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.kolich.curacao.CuracaoConfigLoader;
import com.kolich.curacao.annotations.mappers.ControllerReturnTypeMapper;
import com.kolich.curacao.components.ComponentMappingTable;
import com.kolich.curacao.entities.CuracaoEntity;
import com.kolich.curacao.exceptions.CuracaoException;
import com.kolich.curacao.mappers.response.types.CuracaoEntityReturnMapper;
import com.kolich.curacao.mappers.response.types.CuracaoExceptionWithEntityReturnMapper;
import com.kolich.curacao.mappers.response.types.DefaultObjectReturnMapper;
import com.kolich.curacao.mappers.response.types.DefaultThrowableReturnMapper;
import com.kolich.curacao.mappers.response.types.resources.AbstractETagAwareFileReturnMapper;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructor;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getTypesInPackageAnnotatedWith;
import static org.slf4j.LoggerFactory.getLogger;

public final class ControllerReturnTypeMapperTable {
	
	private static final Logger logger__ = 
		getLogger(ControllerReturnTypeMapperTable.class);
	
	private static final String CONTROLLER_RTN_TYPE_SN =
		ControllerReturnTypeMapper.class.getSimpleName();

    /**
     * A static set of library provided {@link AbstractRenderingReturnTypeMapper}'s
     * that are always injected into the response handler mapping table after
     * any user application provided response handlers.
     */
    private static final Map<Class<?>, AbstractRenderingReturnTypeMapper<?>> defaultMappers__;
    static {
        defaultMappers__ = Maps.newLinkedHashMap(); // Linked hash map to maintain order.
        defaultMappers__.put(File.class, new AbstractETagAwareFileReturnMapper(){});
        defaultMappers__.put(CuracaoEntity.class, new CuracaoEntityReturnMapper());
        defaultMappers__.put(CuracaoException.WithEntity.class,
            new CuracaoExceptionWithEntityReturnMapper());
        defaultMappers__.put(Throwable.class, new DefaultThrowableReturnMapper());
        // Must be last since "Object" is the root of all types in Java.
        defaultMappers__.put(Object.class, new DefaultObjectReturnMapper());
    }
	
	/**
	 * This table maps a set of known class instance types to their
	 * mapping response handlers.  Once a class type mapping response
	 * handler is discovered, its association with a known response handler
	 * is cached in the mapping cache providing O(1) constant lookup time.
	 */
	private final Map<Class<?>, AbstractRenderingReturnTypeMapper<?>> table_;
	
	/**
	 * This cache acts as a O(1) lookup helper which caches known class
	 * instance types to their mapping response handlers.
	 */
	private final Map<Class<?>, AbstractRenderingReturnTypeMapper<?>> cache_;

    /**
     * The context's core component mapping table.
     */
    private final ComponentMappingTable componentMappingTable_;
	
	public ControllerReturnTypeMapperTable(final ComponentMappingTable componentMappingTable) {
        componentMappingTable_ = checkNotNull(componentMappingTable,
            "Component mapping table cannot be null.");
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Loading response type mappers from " +
			"declared boot-package: {}", bootPackage);
		// Scan the "mappers" inside of the declared boot package
		// looking for annotated Java classes that represent response type
		// mappers.
		table_ = buildMappingTable(bootPackage);
		cache_ = Maps.newConcurrentMap();
		logger__.info("Application response type mapping table: {}", table_);
	}
	
	public final AbstractRenderingReturnTypeMapper<?>
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
	public final AbstractRenderingReturnTypeMapper<?>
		getHandlerForType(@Nonnull final Class<?> clazz) {
		checkNotNull(clazz, "Class instance type cannot be null.");
		AbstractRenderingReturnTypeMapper<?> handler = cache_.get(clazz);
		if (handler == null) {
			for (final Map.Entry<Class<?>, AbstractRenderingReturnTypeMapper<?>> entry :
				table_.entrySet()) {
				final Class<?> type = entry.getKey();
				if (type.isAssignableFrom(clazz)) {
					handler = entry.getValue();
					cache_.put(clazz, handler);
					break;
				}
			}
		}
		return handler;
	}
	
	private final ImmutableMap<Class<?>, AbstractRenderingReturnTypeMapper<?>>
		buildMappingTable(final String bootPackage) {
		// Using a LinkedHashMap internally because insertion order is
		// very important in this case.
		final Map<Class<?>, AbstractRenderingReturnTypeMapper<?>> mappers =
			Maps.newLinkedHashMap();  // Preserves insertion order.
		// Find all "controller classes" in the specified boot package that
		// are annotated with our return type mapper annotation.
		final Set<Class<?>> mapperClasses =
			getTypesInPackageAnnotatedWith(bootPackage,
				ControllerReturnTypeMapper.class);
		logger__.debug("Found {} mappers annotated with @{}",
			mapperClasses.size(), CONTROLLER_RTN_TYPE_SN);
		// For each discovered mapper class...
		for (final Class<?> mapper : mapperClasses) {
			logger__.debug("Found @{}: {}", CONTROLLER_RTN_TYPE_SN,
				mapper.getCanonicalName());
			final Class<?> superclazz = mapper.getSuperclass();
			if (!AbstractRenderingReturnTypeMapper.class.isAssignableFrom(superclazz)) {
				logger__.error("Class {} was annotated with @{} but does not " +
					"extend required superclass: {}",
					mapper.getCanonicalName(), CONTROLLER_RTN_TYPE_SN,
					AbstractRenderingReturnTypeMapper.class.getSimpleName());
				continue;
			}
			try {
				final ControllerReturnTypeMapper ma = mapper.getAnnotation(
					ControllerReturnTypeMapper.class);
				// Locate a single constructor worthy of injecting with
				// components, if any.  May be null.
				final Constructor<?> ctor = getInjectableConstructor(mapper);
				AbstractRenderingReturnTypeMapper<?> instance = null;
				if (ctor == null) {
					// Class.newInstance() is evil, so we do the ~right~ thing
					// here to instantiate a new instance of the mapper using
					// the preferred getConstructor() idiom.
					instance = (AbstractRenderingReturnTypeMapper<?>)
						mapper.getConstructor().newInstance();
				} else {
					final Class<?>[] types = ctor.getParameterTypes();
					final Object[] params = new Object[types.length];
                    for (int i = 0, l = types.length; i < l; i++) {
                        params[i] = componentMappingTable_.getComponentForType(types[i]);
                    }
					instance = (AbstractRenderingReturnTypeMapper<?>)ctor.newInstance(params);
				}
				mappers.put(ma.value(), instance);
			} catch (Exception e) {
				logger__.error("Failed to instantiate response mapper " +
					"instance: {}", mapper.getCanonicalName(), e);
			}
		}
		// Add the "default" mappers to the ~end~ of the linked hash map, being
        // careful not to overwrite any user-defined mappers.  That is, if a
        // user has declared their own mappers for one of our default types,
        // we should not blindly "putAll" and overwrite them.
        // <https://github.com/markkolich/curacao/issues/9>
        for (final Map.Entry<Class<?>, AbstractRenderingReturnTypeMapper<?>> entry :
            defaultMappers__.entrySet()) {
            // Only add the default mapper if a user-defined one does not exist.
            if (!mappers.containsKey(entry.getKey())) {
                mappers.put(entry.getKey(), entry.getValue());
            }
        }
		return ImmutableMap.copyOf(mappers);
	}

}
