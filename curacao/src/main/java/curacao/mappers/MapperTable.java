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

package curacao.mappers;

import com.google.common.collect.*;
import curacao.annotations.Mapper;
import curacao.annotations.parameters.RequestBody;
import curacao.components.ComponentTable;
import curacao.context.CuracaoContext;
import curacao.core.servlet.HttpRequest;
import curacao.core.servlet.HttpResponse;
import curacao.core.servlet.ServletContext;
import curacao.entities.CuracaoEntity;
import curacao.exceptions.CuracaoException;
import curacao.exceptions.reflection.AbstractConfigurationException;
import curacao.exceptions.reflection.ComponentConfigurationException;
import curacao.mappers.request.AbstractControllerArgumentMapper;
import curacao.mappers.request.types.*;
import curacao.mappers.request.types.body.*;
import curacao.mappers.response.AbstractControllerReturnTypeMapper;
import curacao.mappers.response.types.CuracaoEntityReturnMapper;
import curacao.mappers.response.types.CuracaoExceptionWithEntityReturnMapper;
import curacao.mappers.response.types.DefaultObjectReturnMapper;
import curacao.mappers.response.types.DefaultThrowableReturnMapper;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructorForClass;
import static curacao.util.reflection.CuracaoReflectionUtils.getMappersInBootPackage;
import static curacao.util.reflection.CuracaoReflectionUtils.getNoArgConstructorForClass;
import static org.slf4j.LoggerFactory.getLogger;

public final class MapperTable {

    private static final Logger LOG = getLogger(MapperTable.class);

    private static final String MAPPER_ANNOTATION_SN = Mapper.class.getSimpleName();

    /**
     * A static set of library provided {@link AbstractControllerArgumentMapper}'s that are always injected into the
     * argument mapper table *after* any user application provided mappers.
     */
    private static final Multimap<Class<?>, AbstractControllerArgumentMapper<?>> DEFAULT_ARG_MAPPERS;

    static {
        DEFAULT_ARG_MAPPERS = LinkedHashMultimap.create(); // Linked hash multimap to maintain order.
        DEFAULT_ARG_MAPPERS.put(String.class, new StringMapper());
        DEFAULT_ARG_MAPPERS.put(Integer.class, new IntegerArgumentMapper());
        DEFAULT_ARG_MAPPERS.put(Long.class, new LongArgumentMapper());
        DEFAULT_ARG_MAPPERS.put(Float.class, new FloatArgumentMapper());
        DEFAULT_ARG_MAPPERS.put(Double.class, new DoubleArgumentMapper());
        DEFAULT_ARG_MAPPERS.put(Boolean.class, new BooleanArgumentMapper());
        DEFAULT_ARG_MAPPERS.put(Character.class, new CharacterArgumentMapper());
        DEFAULT_ARG_MAPPERS.put(ServletContext.class, new ServletContextMapper());
        DEFAULT_ARG_MAPPERS.put(InputStream.class, new InputStreamMapper());
        DEFAULT_ARG_MAPPERS.put(OutputStream.class, new OutputStreamMapper());
        DEFAULT_ARG_MAPPERS.put(HttpRequest.class, new HttpRequestMapper());
        DEFAULT_ARG_MAPPERS.put(HttpResponse.class, new HttpResponseMapper());
        // Request body helpers; safely buffers the request body into memory.
        DEFAULT_ARG_MAPPERS.put(byte[].class,
                new AbstractMemoryBufferingRequestBodyMapper<byte[]>() {
                    @Override
                    public byte[] resolveWithBody(
                            final RequestBody annotation,
                            final CuracaoContext context,
                            final byte[] body) throws Exception {
                        return body;
                    }
                });
        DEFAULT_ARG_MAPPERS.put(ByteBuffer.class,
                new AbstractByteBufferRequestBodyMapper<ByteBuffer>() {
                    @Override
                    public ByteBuffer resolveWithBuffer(
                            final ByteBuffer buffer) throws Exception {
                        return buffer;
                    }
                });
        DEFAULT_ARG_MAPPERS.put(ByteArrayInputStream.class,
                new AbstractByteArrayInputStreamRequestBodyMapper<InputStream>() {
                    @Override
                    public InputStream resolveWithInputStream(
                            final InputStream stream) throws Exception {
                        return stream;
                    }
                });
        DEFAULT_ARG_MAPPERS.put(InputStreamReader.class,
                new AbstractInputStreamReaderRequestBodyMapper<Reader>() {
                    @Override
                    public Reader resolveWithReader(
                            final InputStreamReader reader) throws Exception {
                        return reader;
                    }
                });
        DEFAULT_ARG_MAPPERS.put(String.class,
                new AbstractRequestBodyAsCharsetAwareStringMapper<String>() {
                    @Override
                    public String resolveWithStringAndEncoding(
                            final RequestBody annotation,
                            final String s,
                            final Charset encoding) throws Exception {
                        // If the request body annotation value is "" (empty string) then there's no body parameter
                        // to extract. We just return the entire body as a String.
                        return ("".equals(annotation.value())) ? s : null;
                    }
                });
        DEFAULT_ARG_MAPPERS.put(String.class, new RequestBodyParameterMapper());
        // For "application/x-www-form-urlencoded" encoded bodies (usually attached to POST and PUT requests).
        DEFAULT_ARG_MAPPERS.put(Multimap.class, new RequestBodyMultimapMapper());
        // Object must be last, acts as a "catch all".
        DEFAULT_ARG_MAPPERS.put(Object.class, new ObjectMapper());
    }

    /**
     * A static set of library provided {@link AbstractControllerReturnTypeMapper}'s
     * that are always injected into the response handler mapping table after
     * any user application provided response handlers.
     */
    private static final Map<Class<?>, AbstractControllerReturnTypeMapper<?>> DEFAULT_RETURN_TYPE_MAPPERS;

    static {
        DEFAULT_RETURN_TYPE_MAPPERS = Maps.newLinkedHashMap(); // Linked hash map to maintain order.
        DEFAULT_RETURN_TYPE_MAPPERS.put(CuracaoEntity.class, new CuracaoEntityReturnMapper());
        DEFAULT_RETURN_TYPE_MAPPERS.put(CuracaoException.WithEntity.class,
                new CuracaoExceptionWithEntityReturnMapper());
        DEFAULT_RETURN_TYPE_MAPPERS.put(Throwable.class, new DefaultThrowableReturnMapper());
        // Must be last since "Object" is the root of all types in Java.
        DEFAULT_RETURN_TYPE_MAPPERS.put(Object.class, new DefaultObjectReturnMapper());
    }

    /**
     * This table maps a set of known class instance types to their
     * argument mappers. A Multimap allows multiple argument mappers to
     * be registered for a single class type.
     */
    private final Multimap<Class<?>, AbstractControllerArgumentMapper<?>> argMapperTable_;

    /**
     * The application's return type mappers, by the type each renders, in mapper class name order. Once a mapper
     * is found for a type, the association is cached in the mapper cache, for O(1) lookups on later requests.
     */
    private final Map<Class<?>, AbstractControllerReturnTypeMapper<?>> appReturnTypeMappers_;
    private final Map<Class<?>, AbstractControllerReturnTypeMapper<?>> returnTypeMapperCache_;

    /**
     * The context's core component mapping table.
     */
    private final ComponentTable componentTable_;

    public MapperTable(
            @Nonnull final ComponentTable componentTable) {
        // Scan the boot package and find all "mapper classes" that are
        // annotated with our mapper annotation. We do this reflection scan
        // of the boot package once at the front door for performance reasons.
        this(componentTable, getMappersInBootPackage());
    }

    /**
     * Builds a table from the given mapper classes, without scanning; for tests.
     */
    /* package */ MapperTable(
            final ComponentTable componentTable,
            final Set<Class<?>> mappers) {
        componentTable_ = checkNotNull(componentTable, "Component table cannot be null.");
        // Build the argument mapper table.
        argMapperTable_ = buildArgumentMapperTable(mappers);
        // Build the return type mapper table and its cache.
        appReturnTypeMappers_ = buildReturnTypeMapperTable(mappers);
        returnTypeMapperCache_ = Maps.newConcurrentMap();
        LOG.info("Application argument mapper table: {}", argMapperTable_);
        LOG.info("Application return type mapper table: {}", appReturnTypeMappers_);
    }

    /**
     * Examines the internal argument mapper cache and mapper table
     * to find a suitable set of mappers that are capable of extracting the
     * arg type represented by the given class. Note that this method never
     * returns null. Even if no mappers exists for the given class type,
     * an empty collection is returned.
     */
    @Nonnull
    public Collection<AbstractControllerArgumentMapper<?>> getArgumentMappersForClass(
            final Class<?> clazz) {
        checkNotNull(clazz, "Class instance type cannot be null.");
        return Collections.unmodifiableCollection(argMapperTable_.get(clazz));
    }

    /**
     * Finds the mapper that renders an object of the given class. The application's mappers come first: if any of
     * them renders a type the class is assignable to, the one for the most specific such type wins, so a mapper
     * for {@code ResourceNotFoundException} beats one for {@code Exception}. Otherwise the most specific of
     * Curacao's built-in mappers renders it. Note that this method never returns null in practice: the built-in
     * mapper for {@link Object} renders anything else, with {@link Object#toString()}.
     */
    @Nullable
    public AbstractControllerReturnTypeMapper<?> getReturnTypeMapperForClass(
            @Nonnull final Class<?> clazz) {
        checkNotNull(clazz, "Class instance type cannot be null.");
        final AbstractControllerReturnTypeMapper<?> cached = returnTypeMapperCache_.get(clazz);
        if (cached != null) {
            return cached;
        }

        final AbstractControllerReturnTypeMapper<?> app = mostSpecific(appReturnTypeMappers_, clazz);
        final AbstractControllerReturnTypeMapper<?> handler = (app != null)
                ? app : mostSpecific(DEFAULT_RETURN_TYPE_MAPPERS, clazz);
        if (handler != null) {
            returnTypeMapperCache_.put(clazz, handler);
        }

        return handler;
    }

    /**
     * The mapper for the most specific of the mapped types the class is assignable to: one that isn't a supertype
     * of another such type. When neither of two types is more specific (an interface and a class, say), the one
     * first in the map's order wins, so the choice is the same on every run.
     */
    @Nullable
    private static AbstractControllerReturnTypeMapper<?> mostSpecific(
            final Map<Class<?>, AbstractControllerReturnTypeMapper<?>> mappers,
            final Class<?> clazz) {
        Class<?> best = null;
        for (final Class<?> type : mappers.keySet()) {
            if (type.isAssignableFrom(clazz) && (best == null || best.isAssignableFrom(type))) {
                best = type;
            }
        }

        return (best == null) ? null : mappers.get(best);
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractControllerArgumentMapper<?>> ImmutableMultimap<Class<?>,
            AbstractControllerArgumentMapper<?>> buildArgumentMapperTable(
            final Set<Class<?>> mapperSet) {
        // Using a LinkedHashMultimap internally because insertion order is very important in this case: the
        // mappers for a type are asked in order, and the first to resolve the argument wins. The application's are
        // in mapper class name order, so that order is the same on every run.
        final Multimap<Class<?>, AbstractControllerArgumentMapper<?>> mappers = LinkedHashMultimap.create();
        // Filter the incoming mapper set to only argument mappers.
        final List<Class<T>> filtered = Lists.newArrayList();
        for (final Class<?> mapper : mapperSet) {
            if (AbstractControllerArgumentMapper.class.isAssignableFrom(mapper)) {
                filtered.add((Class<T>) mapper);
            }
        }
        filtered.sort(Comparator.comparing(Class::getName));
        LOG.debug("Found {} argument mappers annotated with @{}", filtered.size(), MAPPER_ANNOTATION_SN);
        // For each discovered mapper class...
        for (final Class<T> mapper : filtered) {
            LOG.debug("Found @{}: argument mapper {}", MAPPER_ANNOTATION_SN, mapper.getCanonicalName());
            try {
                AbstractControllerArgumentMapper<?> instance = null;
                // The mapper class is only "injectable" if it is annotated with the correct mapper
                // annotation at the class level.
                final boolean isInjectable = (null != mapper.getAnnotation(Mapper.class));
                // Locate a single constructor worthy of injecting with components, if any. May be null.
                final Constructor<?> injectableCtor =
                        (isInjectable) ? getInjectableConstructorForClass(mapper) : null;
                if (injectableCtor == null) {
                    // No @Injectable constructor: the public no-argument one, or a startup failure.
                    instance = (AbstractControllerArgumentMapper<?>) getNoArgConstructorForClass(mapper).newInstance();
                } else {
                    instance = (AbstractControllerArgumentMapper<?>) injectableCtor.newInstance(
                            componentTable_.resolveArguments(injectableCtor, mapper));
                }

                final List<Class<?>> genericsClasses =
                        getTypeArguments(AbstractControllerArgumentMapper.class, mapper);
                if (!genericsClasses.isEmpty()) {
                    final Class<?> mapperGenericType = Iterables.getFirst(genericsClasses, null);
                    if (mapperGenericType == null) {
                        throw new CuracaoException("Could not resolve mapper generic type arguments on controller "
                                + "argument mapper: " + mapper.getCanonicalName());
                    }
                    // Note the key in the map is the parameterized generic type hanging off the mapper.
                    mappers.put(mapperGenericType, instance);
                } else {
                    throw new CuracaoException("Failed to identify the generic type arguments on controller "
                            + "argument mapper: " + mapper.getCanonicalName());
                }
            } catch (final Exception e) {
                // A configuration error, not a failure we can work around: rather than silently drop
                // the mapper, fail startup so the misconfiguration can't go unnoticed.
                AbstractConfigurationException.rethrowIfPresent(e);
                LOG.error("Failed to instantiate mapper instance: {}", mapper.getCanonicalName(), e);
            }
        }
        // Add the "default" mappers to the ~end~ of the immutable hash multi map. This essentially means that default
        // argument mappers (the ones provided by this library) are found & called after any user registered mappers.
        mappers.putAll(DEFAULT_ARG_MAPPERS);
        return ImmutableMultimap.copyOf(mappers);
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractControllerReturnTypeMapper<?>> Map<Class<?>,
            AbstractControllerReturnTypeMapper<?>> buildReturnTypeMapperTable(
            final Set<Class<?>> mapperSet) {
        // A LinkedHashMap, in mapper class name order, so that the table is the same on every run.
        final Map<Class<?>, AbstractControllerReturnTypeMapper<?>> mappers = Maps.newLinkedHashMap();
        // Filter the incoming mapper set to only return type mappers.
        final List<Class<T>> filtered = Lists.newArrayList();
        for (final Class<?> mapper : mapperSet) {
            if (AbstractControllerReturnTypeMapper.class.isAssignableFrom(mapper)) {
                filtered.add((Class<T>) mapper);
            }
        }
        filtered.sort(Comparator.comparing(Class::getName));

        LOG.debug("Found {} return type mappers annotated with @{}", filtered.size(), MAPPER_ANNOTATION_SN);
        // For each discovered mapper class...
        for (final Class<T> mapper : filtered) {
            LOG.debug("Found @{}: return type mapper {}", MAPPER_ANNOTATION_SN, mapper.getCanonicalName());
            try {
                AbstractControllerReturnTypeMapper<?> instance = null;
                // The mapper class is only "injectable" if it is annotated with the correct mapper
                // annotation at the class level.
                final boolean isInjectable = (null != mapper.getAnnotation(Mapper.class));
                // Locate a single constructor worthy of injecting with components, if any. May be null.
                final Constructor<?> injectableCtor =
                        (isInjectable) ? getInjectableConstructorForClass(mapper) : null;
                if (injectableCtor == null) {
                    // No @Injectable constructor: the public no-argument one, or a startup failure.
                    instance = (AbstractControllerReturnTypeMapper<?>)
                            getNoArgConstructorForClass(mapper).newInstance();
                } else {
                    instance = (AbstractControllerReturnTypeMapper<?>) injectableCtor.newInstance(
                            componentTable_.resolveArguments(injectableCtor, mapper));
                }

                final List<Class<?>> genericsClasses =
                        getTypeArguments(AbstractControllerReturnTypeMapper.class, mapper);
                if (!genericsClasses.isEmpty()) {
                    final Class<?> mapperGenericType = Iterables.getFirst(genericsClasses, null);
                    if (mapperGenericType == null) {
                        throw new CuracaoException("Could not resolve mapper generic type arguments on return "
                                + "type mapper: " + mapper.getCanonicalName());
                    }
                    final AbstractControllerReturnTypeMapper<?> existing = mappers.get(mapperGenericType);
                    if (existing != null) {
                        throw new ComponentConfigurationException("Return type mappers "
                                + existing.getClass().getName() + " and " + mapper.getName() + " both render "
                                + mapperGenericType.getName() + "; keep one.");
                    }
                    // Note the key in the map is the parameterized generic type hanging off the mapper.
                    mappers.put(mapperGenericType, instance);
                } else {
                    throw new CuracaoException("Failed to identify the generic type arguments on return "
                            + "type mapper: " + mapper.getCanonicalName());
                }
            } catch (final Exception e) {
                // A configuration error, not a failure we can work around: rather than silently drop
                // the mapper, fail startup so the misconfiguration can't go unnoticed.
                AbstractConfigurationException.rethrowIfPresent(e);
                LOG.error("Failed to instantiate mapper instance: {}", mapper.getCanonicalName(), e);
            }
        }
        // https://github.com/markkolich/curacao/issues/9
        // Curacao's built-in mappers are kept apart, and consulted only when none of these renders a type, so an
        // application's mapper always wins over a built-in one.
        return ImmutableMap.copyOf(mappers);
    }

    /**
     * Get the actual type arguments a child class has used to extend a generic
     * base class.
     *
     * @param baseClass  the base class
     * @param childClass the child class
     * @return a list of the raw classes for the actual type arguments.
     */
    private static <T> List<Class<?>> getTypeArguments(
            final Class<T> baseClass,
            final Class<? extends T> childClass) {
        final Map<Type, Type> resolvedTypes = Maps.newHashMap();

        Type type = childClass;
        // Start walking up the inheritance hierarchy until we hit baseClass
        while (!baseClass.equals(getClass(type))) {
            if (type instanceof Class) {
                // There is no useful information for us in raw types, so just keep going.
                type = ((Class<?>) type).getGenericSuperclass();
            } else {
                final ParameterizedType parameterizedType = (ParameterizedType) type;
                final Class<?> rawType = (Class<?>) parameterizedType.getRawType();

                final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                final TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
                for (int i = 0, l = actualTypeArguments.length; i < l; i++) {
                    resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
                }

                if (!rawType.equals(baseClass)) {
                    type = rawType.getGenericSuperclass();
                }
            }
        }

        // Finally, for each actual type argument provided to baseClass, determine (if possible)
        // the raw class for that type argument.
        final Type[] actualTypeArguments;
        if (type instanceof Class) {
            actualTypeArguments = ((Class<?>) type).getTypeParameters();
        } else {
            actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
        }

        final List<Class<?>> typeArgumentsAsClasses = Lists.newArrayList();
        // resolve types by chasing down type variables.
        for (final Type argumentType : actualTypeArguments) {
            Type baseType = argumentType;
            while (resolvedTypes.containsKey(baseType)) {
                baseType = resolvedTypes.get(baseType);
            }
            typeArgumentsAsClasses.add(getClass(baseType));
        }

        return typeArgumentsAsClasses;
    }

    /**
     * Get the underlying class for a type, or <code>null</code> if the type is a variable
     * type.
     *
     * @param type the type
     * @return the underlying class
     */
    private static Class<?> getClass(final Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            final Type componentType = ((GenericArrayType) type).getGenericComponentType();
            final Class<?> componentClass = getClass(componentType);
            if (componentClass != null) {
                return Array.newInstance(componentClass, 0).getClass();
            }
        }

        return null;
    }

}
