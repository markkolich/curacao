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

package com.kolich.curacao.mappers.request;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.kolich.curacao.CuracaoConfigLoader;
import com.kolich.curacao.annotations.mappers.ArgumentTypeMapper;
import com.kolich.curacao.annotations.parameters.RequestBody;
import com.kolich.curacao.components.ComponentMappingTable;
import com.kolich.curacao.mappers.request.types.*;
import com.kolich.curacao.mappers.request.types.body.*;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
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
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructor;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getTypesInPackageAnnotatedWith;
import static org.slf4j.LoggerFactory.getLogger;

public final class ControllerArgumentMapperTable {
	
	private static final Logger logger__ = 
		getLogger(ControllerArgumentMapperTable.class);
	
	private static final String ARG_MAPPER_SN =
		ArgumentTypeMapper.class.getSimpleName();

    /**
     * A static set of library provided {@link ControllerArgumentMapper}'s
     * that are always injected into the argument mapper table ~after~ any user
     * application provided mappers.
     */
    private static final Multimap<Class<?>, ControllerArgumentMapper<?>> defaultArgMappers__;
    static {
        defaultArgMappers__ = LinkedHashMultimap.create(); // Linked hash multimap to maintain order.
        defaultArgMappers__.put(String.class, new StringMapper());
        defaultArgMappers__.put(Integer.class, new IntegerArgumentMapper());
        defaultArgMappers__.put(Long.class, new LongArgumentMapper());
        defaultArgMappers__.put(ServletContext.class, new ServletContextMapper());
        defaultArgMappers__.put(ServletInputStream.class, new ServletInputStreamMapper());
        defaultArgMappers__.put(ServletOutputStream.class, new ServletOutputStreamMapper());
        defaultArgMappers__.put(HttpServletRequest.class, new HttpServletRequestMapper());
        defaultArgMappers__.put(HttpServletResponse.class, new HttpServletResponseMapper());
        // Request body helpers; safely buffers the request body into memory.
        defaultArgMappers__.put(byte[].class,
                new MemoryBufferingRequestBodyMapper<byte[]>() {
                    @Override
                    public byte[] resolveWithBody(final RequestBody annotation,
                                                  final CuracaoContext context, final byte[] body) throws Exception {
                        return body;
                    }
                });
        defaultArgMappers__.put(ByteBuffer.class,
                new ByteBufferRequestBodyMapper<ByteBuffer>() {
                    @Override
                    public final ByteBuffer resolveWithBuffer(final ByteBuffer buffer) throws Exception {
                        return buffer;
                    }
                });
        defaultArgMappers__.put(ByteArrayInputStream.class,
                new ByteArrayInputStreamRequestBodyMapper<InputStream>() {
                    @Override
                    public final InputStream resolveWithInputStream(final InputStream stream) throws Exception {
                        return stream;
                    }
                });
        defaultArgMappers__.put(InputStreamReader.class,
                new InputStreamReaderRequestBodyMapper<Reader>() {
                    @Override
                    public final Reader resolveWithReader(final InputStreamReader reader) throws Exception {
                        return reader;
                    }
                });
        defaultArgMappers__.put(String.class,
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
        defaultArgMappers__.put(String.class, new RequestBodyParameterMapper());
        // For "application/x-www-form-urlencoded" encoded bodies (usually
        // attached to POST and PUT requests).
        defaultArgMappers__.put(Multimap.class, new RequestBodyMultimapMapper());
        // Object must be last, acts as a "catch all".
        defaultArgMappers__.put(Object.class, new ObjectMapper());
    }
	
	/**
	 * This table maps a set of known class instance types to their
	 * argument mappers.  A multi-map allows multiple argument mappers to
	 * be registered for a single class type.
	 */
	private final Multimap<Class<?>, ControllerArgumentMapper<?>> table_;

    /**
     * The context's core component mapping table.
     */
    private final ComponentMappingTable components_;
		
	public ControllerArgumentMapperTable(@Nonnull final ComponentMappingTable components) {
        components_ = checkNotNull(components,
            "Component mapping table cannot be null.");
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Loading controller argument mappers from " +
			"declared boot-package: {}", bootPackage);
		// Scan the "mappers" inside of the declared boot package
		// looking for annotated Java classes that represent argument
		// mappers.
		table_ = buildArgumentMappingTable(bootPackage);
        logger__.info("Application controller argument mapper table: {}",
            table_);
	}
	
	/**
	 * Examines the internal argument mapper cache and mapper table
	 * to find a suitable set of mappers that are capable of extracting the
	 * arg type represented by the given class.  Note that this method never
	 * returns null.  Even if no mappers exists for the given class type,
	 * an empty collection is returned.
	 */
	public final Collection<ControllerArgumentMapper<?>>
		getArgumentMappersForType(final Class<?> clazz) {
		checkNotNull(clazz, "Class instance type cannot be null.");
		return Collections.unmodifiableCollection(table_.get(clazz));
	}
	
	private final ImmutableMultimap<Class<?>, ControllerArgumentMapper<?>>
		buildArgumentMappingTable(final String bootPackage) {
		// Using a LinkedHashMultimap internally because insertion order is
		// very important in this case.
		final Multimap<Class<?>, ControllerArgumentMapper<?>> mappers =
			LinkedHashMultimap.create(); // Preserves order
		// Find all "controller classes" in the specified boot package that
		// are annotated with our return type mapper annotation.
		final Set<Class<?>> mapperClasses =
			getTypesInPackageAnnotatedWith(bootPackage,
				ArgumentTypeMapper.class);
		logger__.debug("Found {} mappers " + "annotated with @{}",
            mapperClasses.size(), ARG_MAPPER_SN);
		// For each discovered mapper class...
		for (final Class<?> mapper : mapperClasses) {
			logger__.debug("Found @{}: {}", ARG_MAPPER_SN,
				mapper.getCanonicalName());
			final Class<?> superclazz = mapper.getSuperclass();
			if (!ControllerArgumentMapper.class.isAssignableFrom(superclazz)) {
				logger__.error("Class `{}` was annotated with @{}" +
					" but does not extend required superclass: {}",
                    mapper.getCanonicalName(), ARG_MAPPER_SN,
					ControllerArgumentMapper.class.getSimpleName());
				continue;
			}
			try {
				// Locate a single constructor worthy of injecting with
				// components, if any.  May be null.
				final Constructor<?> ctor = getInjectableConstructor(mapper);
				ControllerArgumentMapper<?> instance = null;
				if (ctor == null) {
					// Class.newInstance() is evil, so we do the ~right~ thing
					// here to instantiate a new instance of the mapper using
					// the preferred getConstructor() idiom.
					instance = (ControllerArgumentMapper<?>)
						mapper.getConstructor().newInstance();
				} else {
					final Class<?>[] types = ctor.getParameterTypes();
                    final Object[] params = new Object[types.length];
                    for (int i = 0, l = types.length; i < l; i++) {
                        params[i] = components_.getComponentForType(types[i]);
                    }
					instance = (ControllerArgumentMapper<?>)ctor.newInstance(params);
				}
                // This feels a bit convoluted, but works safely.  From the type
                // token, we're pulling its "raw" type then fetching its
                // associated class.  This is guaranteed to exist because of the
                // convenient isAssignableFrom() check above that guarantees this
                // class "extends" the right abstract parent.  From there, we can
                // safely pull off the type argument (generics) tied to the parent
                // abstract class of generic type T.  Type erasure sucks.
                final TypeToken<?> token = TypeToken.of(mapper);
                final Class<?> genericType = (Class<?>)
                    ((ParameterizedType)token.getRawType().getGenericSuperclass())
                        .getActualTypeArguments()[0];
                mappers.put(genericType, instance);
			} catch (Exception e) {
				logger__.error("Failed to instantiate controller argument " +
					"mapper instance: {}", mapper.getCanonicalName(), e);
			}
		}
		// Add the "default" mappers to the ~end~ of the immutable hash multi map.
		// This essentially means that default argument mappers (the ones
		// provided by this library) are found & called after any user registered
		// mappers.
		mappers.putAll(defaultArgMappers__);
		return ImmutableMultimap.copyOf(mappers);
	}

}
