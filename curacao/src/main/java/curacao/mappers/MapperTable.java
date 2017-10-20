/*
 * Copyright (c) 2017 Mark S. Kolich
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

package curacao.mappers;

import com.google.common.base.Predicates;
import com.google.common.collect.*;
import curacao.CuracaoConfigLoader;
import curacao.CuracaoContext;
import curacao.annotations.Controller;
import curacao.annotations.Mapper;
import curacao.annotations.parameters.RequestBody;
import curacao.components.ComponentTable;
import curacao.entities.CuracaoEntity;
import curacao.exceptions.CuracaoException;
import curacao.mappers.request.ControllerArgumentMapper;
import curacao.mappers.request.types.*;
import curacao.mappers.request.types.body.*;
import curacao.mappers.response.ControllerReturnTypeMapper;
import curacao.mappers.response.types.CuracaoEntityReturnMapper;
import curacao.mappers.response.types.CuracaoExceptionWithEntityReturnMapper;
import curacao.mappers.response.types.DefaultObjectReturnMapper;
import curacao.mappers.response.types.DefaultThrowableReturnMapper;
import curacao.mappers.response.types.resources.AbstractETagAwareFileReturnMapper;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.util.reflection.CuracaoReflectionUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

public final class MapperTable {
	
	private static final Logger log = getLogger(MapperTable.class);
	
	private static final String MAPPER_ANNOTATION_SN = Mapper.class.getSimpleName();

    /**
     * A static set of library provided {@link ControllerArgumentMapper}'s that are always injected into the
     * argument mapper table *after* any user application provided mappers.
     */
    private static final Multimap<Class<?>, ControllerArgumentMapper<?>> defaultArgMappers;
    static {
        defaultArgMappers = LinkedHashMultimap.create(); // Linked hash multimap to maintain order.
        defaultArgMappers.put(String.class, new StringMapper());
        defaultArgMappers.put(Integer.class, new IntegerArgumentMapper());
        defaultArgMappers.put(Long.class, new LongArgumentMapper());
        defaultArgMappers.put(Float.class, new FloatArgumentMapper());
        defaultArgMappers.put(Double.class, new DoubleArgumentMapper());
        defaultArgMappers.put(Boolean.class, new BooleanArgumentMapper());
        defaultArgMappers.put(Character.class, new CharacterArgumentMapper());
        defaultArgMappers.put(ServletContext.class, new ServletContextMapper());
        defaultArgMappers.put(ServletInputStream.class, new ServletInputStreamMapper());
        defaultArgMappers.put(ServletOutputStream.class, new ServletOutputStreamMapper());
        defaultArgMappers.put(HttpServletRequest.class, new HttpServletRequestMapper());
        defaultArgMappers.put(HttpServletResponse.class, new HttpServletResponseMapper());
        // Request body helpers; safely buffers the request body into memory.
        defaultArgMappers.put(byte[].class,
            new MemoryBufferingRequestBodyMapper<byte[]>() {
                @Override
                public byte[] resolveWithBody(final RequestBody annotation,
                                              final CuracaoContext context,
                                              final byte[] body) throws Exception {
                    return body;
                }
            });
        defaultArgMappers.put(ByteBuffer.class,
            new ByteBufferRequestBodyMapper<ByteBuffer>() {
                @Override
                public final ByteBuffer resolveWithBuffer(final ByteBuffer buffer) throws Exception {
                    return buffer;
                }
            });
        defaultArgMappers.put(ByteArrayInputStream.class,
            new ByteArrayInputStreamRequestBodyMapper<InputStream>() {
                @Override
                public final InputStream resolveWithInputStream(final InputStream stream) throws Exception {
                    return stream;
                }
            });
        defaultArgMappers.put(InputStreamReader.class,
            new InputStreamReaderRequestBodyMapper<Reader>() {
                @Override
                public final Reader resolveWithReader(final InputStreamReader reader) throws Exception {
                    return reader;
                }
            });
        defaultArgMappers.put(String.class,
            new RequestBodyAsCharsetAwareStringMapper<String>() {
                @Override
                public final String resolveWithStringAndEncoding(final RequestBody annotation,
                                                                 final String s,
                                                                 final Charset encoding) throws Exception {
                    // If the request body annotation value is "" (empty string) then there's no body parameter
                    // to extract.  We just return the entire body as a String.
                    return ("".equals(annotation.value())) ? s : null;
                }
            });
        defaultArgMappers.put(String.class, new RequestBodyParameterMapper());
        // For "application/x-www-form-urlencoded" encoded bodies (usually attached to POST and PUT requests).
        defaultArgMappers.put(Multimap.class, new RequestBodyMultimapMapper());
        // Object must be last, acts as a "catch all".
        defaultArgMappers.put(Object.class, new ObjectMapper());
    }

    /**
     * A static set of library provided {@link ControllerReturnTypeMapper}'s
     * that are always injected into the response handler mapping table after
     * any user application provided response handlers.
     */
    private static final Map<Class<?>, ControllerReturnTypeMapper<?>> defaultReturnTypeMappers;
    static {
        defaultReturnTypeMappers = Maps.newLinkedHashMap(); // Linked hash map to maintain order.
        defaultReturnTypeMappers.put(File.class, new AbstractETagAwareFileReturnMapper(){});
        defaultReturnTypeMappers.put(CuracaoEntity.class, new CuracaoEntityReturnMapper());
        defaultReturnTypeMappers.put(CuracaoException.WithEntity.class,
            new CuracaoExceptionWithEntityReturnMapper());
        defaultReturnTypeMappers.put(Throwable.class, new DefaultThrowableReturnMapper());
        // Must be last since "Object" is the root of all types in Java.
        defaultReturnTypeMappers.put(Object.class, new DefaultObjectReturnMapper());
    }
	
	/**
	 * This table maps a set of known class instance types to their
	 * argument mappers.  A Multimap allows multiple argument mappers to
	 * be registered for a single class type.
	 */
	private final Multimap<Class<?>, ControllerArgumentMapper<?>> argMapperTable_;

    /**
     * This table maps a set of known class instance types to their
     * return type mappers.  Once a return type mapper is discovered,
     * its association with a known response handler is cached in the mapper
     * cache providing O(1) constant lookup time on subsequent requests.
     */
    private final Map<Class<?>, ControllerReturnTypeMapper<?>> returnTypeMapperTable_;
    private final Map<Class<?>, ControllerReturnTypeMapper<?>> returnTypeMapperCache_;

    /**
     * The context's core component mapping table.
     */
    private final ComponentTable componentTable_;
		
	public MapperTable(@Nonnull final ComponentTable componentTable) {
        componentTable_ = checkNotNull(componentTable, "Component table cannot be null.");
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		log.info("Loading mappers from declared boot-package: {}", bootPackage);
        // Scan the boot package and find all "mapper classes" that are
        // annotated with our mapper annotation.  We do this reflection scan
        // of the boot package once at the front door for performance reasons.
        final Set<Class<?>> mappers = getTypesInPackageAnnotatedWith(bootPackage, Mapper.class);
        // Build the argument mapper table.
		argMapperTable_ = buildArgumentMapperTable(mappers);
        // Build the return return type mapper table and its cache.
        returnTypeMapperTable_ = buildReturnTypeMapperTable(mappers);
        returnTypeMapperCache_ = Maps.newConcurrentMap();
        log.info("Application argument mapper table: {}", argMapperTable_);
        log.info("Application return type mapper table: {}", returnTypeMapperTable_);
	}
	
	/**
	 * Examines the internal argument mapper cache and mapper table
	 * to find a suitable set of mappers that are capable of extracting the
	 * arg type represented by the given class.  Note that this method never
	 * returns null.  Even if no mappers exists for the given class type,
	 * an empty collection is returned.
	 */
	@Nonnull
	public final Collection<ControllerArgumentMapper<?>> getArgumentMappersForClass(final Class<?> clazz) {
		checkNotNull(clazz, "Class instance type cannot be null.");
		return Collections.unmodifiableCollection(argMapperTable_.get(clazz));
	}

    /**
     * Returns an exact argument mapper by its class type, if one exists. Returns null if no
     * {@link ControllerArgumentMapper} was found for the provided class.
     */
	@Nullable
	public final ControllerArgumentMapper<?> getArgumentMapperByType(@Nonnull final Class<?> clazz) {
        checkNotNull(clazz, "Class instance type cannot be null.");
	    return argMapperTable_.values().stream()
            .filter(m -> m.getClass().equals(clazz))
            .findFirst()
            .orElse(null);
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
    @Nullable
    public final ControllerReturnTypeMapper<?> getReturnTypeMapperForClass(@Nonnull final Class<?> clazz) {
        checkNotNull(clazz, "Class instance type cannot be null.");
        ControllerReturnTypeMapper<?> handler = returnTypeMapperCache_.get(clazz);
        if (handler == null) {
            for (final Map.Entry<Class<?>, ControllerReturnTypeMapper<?>> entry : returnTypeMapperTable_.entrySet()) {
                final Class<?> type = entry.getKey();
                if (type.isAssignableFrom(clazz)) {
                    handler = entry.getValue();
                    returnTypeMapperCache_.put(clazz, handler);
                    break;
                }
            }
        }
        return handler;
    }

    /**
     * Returns an exact return type mapper by its class type, if one exists. Returns null if no
     * {@link ControllerReturnTypeMapper} was found for the provided class.
     */
    @Nullable
    public final ControllerReturnTypeMapper<?> getReturnTypeMapperByType(@Nonnull final Class<?> clazz) {
        return returnTypeMapperTable_.values().stream()
            .filter(m -> m.getClass().equals(clazz))
            .findFirst()
            .orElse(null);
    }
	
	private final ImmutableMultimap<Class<?>, ControllerArgumentMapper<?>> buildArgumentMapperTable(final Set<Class<?>> mapperSet) {
		// Using a LinkedHashMultimap internally because insertion order is
		// very important in this case.
		final Multimap<Class<?>, ControllerArgumentMapper<?>> mappers = LinkedHashMultimap.create();
        // Filter the incoming mapper set to only argument mappers.
        final Set<Class<?>> filtered = Sets.filter(mapperSet, Predicates.subtypeOf(ControllerArgumentMapper.class));
        log.debug("Found {} argument mappers annotated with @{}", filtered.size(), MAPPER_ANNOTATION_SN);
		// For each discovered mapper class...
		for (final Class<?> mapper : filtered) {
			log.debug("Found @{}: argument mapper {}", MAPPER_ANNOTATION_SN, mapper.getCanonicalName());
			try {
                ControllerArgumentMapper<?> instance = null;
                // The mapper class is only "injectable" if it is annotated with the correct mapper
                // annotation at the class level.
                final boolean isInjectable = (null != mapper.getAnnotation(Mapper.class));
				// Locate a single constructor worthy of injecting with components, if any.  May be null.
				final Constructor<?> injectableCtor = (isInjectable) ? getInjectableConstructor(mapper) : null;
				if (injectableCtor == null) {
                    final Constructor<?> plainCtor = getConstructorWithMostParameters(mapper);
                    final int paramCount = plainCtor.getParameterTypes().length;
                    // Class.newInstance() is evil, so we do the ~right~ thing here to instantiate a new instance of the
                    // mapper using the preferred getConstructor() idiom.  Note we don't have any arguments to pass to
                    // the constructor because it was not annotated so we just pass an array of all "null" meaning every
                    // argument into the constructor will be null.
                    instance = (ControllerArgumentMapper<?>)plainCtor.newInstance(new Object[paramCount]);
				} else {
					final Class<?>[] types = injectableCtor.getParameterTypes();
                    final Object[] params = new Object[types.length];
                    for (int i = 0, l = types.length; i < l; i++) {
                        params[i] = componentTable_.getComponentForType(types[i]);
                    }
					instance = (ControllerArgumentMapper<?>)injectableCtor.newInstance(params);
				}
                // Note the key in the map is the parameterized generic type hanging off the mapper.
                mappers.put(getGenericType(mapper), instance);
			} catch (Exception e) {
				log.error("Failed to instantiate mapper instance: {}", mapper.getCanonicalName(), e);
			}
		}
		// Add the "default" mappers to the ~end~ of the immutable hash multi map. This essentially means that default
		// argument mappers (the ones provided by this library) are found & called after any user registered mappers.
		mappers.putAll(defaultArgMappers);
		return ImmutableMultimap.copyOf(mappers);
	}

    private final ImmutableMap<Class<?>, ControllerReturnTypeMapper<?>> buildReturnTypeMapperTable(final Set<Class<?>> mapperSet) {
        // Using a LinkedHashMap internally because insertion order is very important in this case.
        final Map<Class<?>, ControllerReturnTypeMapper<?>> mappers = Maps.newLinkedHashMap();
        // Filter the incoming mapper set to only return type mappers.
        final Set<Class<?>> filtered = Sets.filter(mapperSet, Predicates.subtypeOf(ControllerReturnTypeMapper.class));
        log.debug("Found {} return type mappers annotated with @{}", filtered.size(), MAPPER_ANNOTATION_SN);
        // For each discovered mapper class...
        for (final Class<?> mapper : filtered) {
            log.debug("Found @{}: return type mapper {}", MAPPER_ANNOTATION_SN, mapper.getCanonicalName());
            try {
                ControllerReturnTypeMapper<?> instance = null;
                // The mapper class is only "injectable" if it is annotated with the correct mapper
                // annotation at the class level.
                final boolean isInjectable = (null != mapper.getAnnotation(Mapper.class));
                // Locate a single constructor worthy of injecting with components, if any.  May be null.
                final Constructor<?> injectableCtor = (isInjectable) ? getInjectableConstructor(mapper) : null;
                if (injectableCtor == null) {
                    final Constructor<?> plainCtor = getConstructorWithMostParameters(mapper);
                    final int paramCount = plainCtor.getParameterTypes().length;
                    // Class.newInstance() is evil, so we do the ~right~ thing here to instantiate a new instance of the
                    // mapper using the preferred getConstructor() idiom.  Note we don't have any arguments to pass to
                    // the constructor because it was not annotated so we just pass an array of all "null" meaning every
                    // argument into the constructor will be null.
                    instance = (ControllerReturnTypeMapper<?>)plainCtor.newInstance(new Object[paramCount]);
                } else {
                    final Class<?>[] types = injectableCtor.getParameterTypes();
                    final Object[] params = new Object[types.length];
                    for (int i = 0, l = types.length; i < l; i++) {
                        params[i] = componentTable_.getComponentForType(types[i]);
                    }
                    instance = (ControllerReturnTypeMapper<?>)injectableCtor.newInstance(params);
                }
                // Note the key in the map is the parameterized generic type hanging off the mapper.
                mappers.put(getGenericType(mapper), instance);
            } catch (Exception e) {
                log.error("Failed to instantiate mapper instance: {}", mapper.getCanonicalName(), e);
            }
        }
        // https://github.com/markkolich/curacao/issues/9
        // Add the "default" mappers to the ~end~ of the linked hash map, being careful not to overwrite any
        // user-defined mappers.  That is, if a user has declared their own mappers for one of our default types,
        // we should not blindly "putAll" and overwrite them.
        for (final Map.Entry<Class<?>, ControllerReturnTypeMapper<?>> entry : defaultReturnTypeMappers.entrySet()) {
            // Only add the default mapper if a user-defined one does not exist.
            if (!mappers.containsKey(entry.getKey())) {
                mappers.put(entry.getKey(), entry.getValue());
            }
        }
        return ImmutableMap.copyOf(mappers);
    }

    private static final Class<?> getGenericType(@Nonnull final Class<?> mapper) {
        // This feels a bit convoluted, but works safely.  From the type token, we're pulling its "raw" type then
        // fetching its associated class.  This is guaranteed to exist because of the convenient isAssignableFrom()
        // check that happened earlier which guarantees this class "extends" the right abstract parent.  From
        // there, we can safely pull off the type argument (generics) tied to the parent abstract class of generic
        // type T. In layman's terms given a class of type Foo<T>, this method returns T, the type inside
        // of the generics < and >.
        return (Class<?>)((ParameterizedType)mapper.getGenericSuperclass()).getActualTypeArguments()[0];
    }

}
