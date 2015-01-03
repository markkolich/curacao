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

package com.kolich.curacao.handlers.requests;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.kolich.curacao.CuracaoConfigLoader;
import com.kolich.curacao.annotations.mappers.ControllerArgumentTypeMapper;
import com.kolich.curacao.annotations.parameters.RequestBody;
import com.kolich.curacao.handlers.components.ComponentMappingTable;
import com.kolich.curacao.handlers.requests.mappers.ControllerMethodArgumentMapper;
import com.kolich.curacao.handlers.requests.mappers.types.*;
import com.kolich.curacao.handlers.requests.mappers.types.body.*;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructor;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getTypesInPackageAnnotatedWith;
import static org.slf4j.LoggerFactory.getLogger;

public final class ControllerArgumentMappingTable {
	
	private static final Logger logger__ = 
		getLogger(ControllerArgumentMappingTable.class);
	
	private static final String CONTROLLER_ARG_MAPPER_SN =
		ControllerArgumentTypeMapper.class.getSimpleName();

    /**
     * A static set of library provided {@link ControllerMethodArgumentMapper}'s
     * that are always injected into the argument mapping table ~after~ any user
     * application provided mappers.
     */
    private static final Multimap<Class<?>, ControllerMethodArgumentMapper<?>> defaultMappers__;
    static {
        defaultMappers__ = LinkedHashMultimap.create(); // Linked hash multimap to maintain order.
        defaultMappers__.put(String.class, new StringMapper());
        defaultMappers__.put(Integer.class, new IntegerArgumentMapper());
        defaultMappers__.put(Long.class, new LongArgumentMapper());
        defaultMappers__.put(ServletContext.class, new ServletContextMapper());
        defaultMappers__.put(ServletInputStream.class, new ServletInputStreamMapper());
        defaultMappers__.put(ServletOutputStream.class, new ServletOutputStreamMapper());
        defaultMappers__.put(HttpServletRequest.class, new HttpServletRequestMapper());
        defaultMappers__.put(HttpServletResponse.class, new HttpServletResponseMapper());
        // Request body helpers; safely buffers the request body into memory.
        defaultMappers__.put(byte[].class,
            new MemoryBufferingRequestBodyMapper<byte[]>() {
                @Override
                public byte[] resolveWithBody(final RequestBody annotation,
                    final CuracaoContext context, final byte[] body)
                    throws Exception {
                    return body;
                }
            });
        defaultMappers__.put(ByteBuffer.class,
            new ByteBufferRequestMapper<ByteBuffer>() {
                @Override
                public final ByteBuffer resolveWithBuffer(final ByteBuffer buffer)
                    throws Exception {
                    return buffer;
                }
            });
        defaultMappers__.put(ByteArrayInputStream.class,
            new ByteArrayInputStreamRequestMapper<InputStream>() {
                @Override
                public final InputStream resolveWithInputStream(final InputStream stream)
                    throws Exception {
                    return stream;
                }
            });
        defaultMappers__.put(InputStreamReader.class,
            new InputStreamReaderRequestMapper<Reader>() {
                @Override
                public final Reader resolveWithReader(final InputStreamReader reader)
                    throws Exception {
                    return reader;
                }
            });
        defaultMappers__.put(String.class,
            new RequestBodyAsCharsetAwareStringMapper<String>() {
                @Override
                public final String resolveWithStringAndEncoding(
                    final RequestBody annotation, final String s,
                    final String encoding) throws Exception {
                    // If the request body annotation value is "" (empty
                    // string) then there's no body parameter to extract.  We
                    // just return the entire body as a String.
                    return ("".equals(annotation.value())) ? s : null;
                }
            });
        defaultMappers__.put(String.class, new RequestBodyParameterMapper());
        // For "application/x-www-form-urlencoded" encoded bodies (usually
        // attached to POST and PUT requests).
        defaultMappers__.put(Multimap.class, new RequestBodyMultimapMapper());
        // Object must be last, acts as a "catch all".
        defaultMappers__.put(Object.class, new ObjectMapper());
    }
	
	/**
	 * This table maps a set of known class instance types to their
	 * argument mappers.  A multi-map allows multiple argument mappers to
	 * be registered for a single class type.
	 */
	private final Multimap<Class<?>, ControllerMethodArgumentMapper<?>> table_;

    /**
     * The context's core component mapping table.
     */
    private final ComponentMappingTable components_;
		
	public ControllerArgumentMappingTable(final ComponentMappingTable components) {
        components_ = components;
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Loading controller argument mappers from " +
			"declared boot-package: {}", bootPackage);
		// Scan the "mappers" inside of the declared boot package
		// looking for annotated Java classes that represent argument
		// mappers.
		table_ = buildArgumentMappingTable(bootPackage);
        logger__.info("Application controller argument mapping table: {}",
            table_);
	}
	
	/**
	 * Examines the internal argument mapper cache and mapping table
	 * to find a suitable set of mappers that are capable of extracting the
	 * arg type represented by the given class.  Note that this method never
	 * returns null.  Even if no mappers exists for the given class type,
	 * an empty collection is returned.
	 */
	public final Collection<ControllerMethodArgumentMapper<?>>
		getArgumentMappersForType(final Class<?> clazz) {
		checkNotNull(clazz, "Class instance type cannot be null.");
		return Collections.unmodifiableCollection(table_.get(clazz));
	}
	
	private final ImmutableMultimap<Class<?>, ControllerMethodArgumentMapper<?>>
		buildArgumentMappingTable(final String bootPackage) {
		// Using a LinkedHashMultimap internally because insertion order is
		// very important in this case.
		final Multimap<Class<?>, ControllerMethodArgumentMapper<?>> mappers =
			LinkedHashMultimap.create(); // Preserves order
		// Find all "controller classes" in the specified boot package that
		// are annotated with our return type mapper annotation.
		final Set<Class<?>> mapperClasses =
			getTypesInPackageAnnotatedWith(bootPackage,
				ControllerArgumentTypeMapper.class);
		logger__.debug("Found {} mappers " + "annotated with @{}",
            mapperClasses.size(), CONTROLLER_ARG_MAPPER_SN);
		// For each discovered mapper class...
		for (final Class<?> mapper : mapperClasses) {
			logger__.debug("Found @{}: {}", CONTROLLER_ARG_MAPPER_SN,
				mapper.getCanonicalName());
			final Class<?> superclazz = mapper.getSuperclass();
			if (!ControllerMethodArgumentMapper.class.isAssignableFrom(superclazz)) {
				logger__.error("Class `{}` was annotated with @{}" +
					" but does not extend required superclass: {}",
                    mapper.getCanonicalName(), CONTROLLER_ARG_MAPPER_SN,
					ControllerMethodArgumentMapper.class.getSimpleName());
				continue;
			}
			try {
				final ControllerArgumentTypeMapper ma = mapper.getAnnotation(
					ControllerArgumentTypeMapper.class);
				// Locate a single constructor worthy of injecting with
				// components, if any.  May be null.
				final Constructor<?> ctor = getInjectableConstructor(mapper);
				ControllerMethodArgumentMapper<?> instance = null;
				if (ctor == null) {
					// Class.newInstance() is evil, so we do the ~right~ thing
					// here to instantiate a new instance of the mapper using
					// the preferred getConstructor() idiom.
					instance = (ControllerMethodArgumentMapper<?>)
						mapper.getConstructor().newInstance();
				} else {
					final Class<?>[] types = ctor.getParameterTypes();
                    final Object[] params = new Object[types.length];
                    for (int i = 0, l = types.length; i < l; i++) {
                        params[i] = components_.getComponentForType(types[i]);
                    }
					instance = (ControllerMethodArgumentMapper<?>)ctor.newInstance(params);
				}
				mappers.put(ma.value(), instance);
			} catch (Exception e) {
				logger__.error("Failed to instantiate controller argument " +
					"mapper instance: {}", mapper.getCanonicalName(), e);
			}
		}
		// Add the "default" mappers to the ~end~ of the immutable hash multi map.
		// This essentially means that default argument mappers (the ones
		// provided by this library) are found & called after any user registered
		// mappers.
		mappers.putAll(defaultMappers__);
		return ImmutableMultimap.copyOf(mappers);
	}

}
