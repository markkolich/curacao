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

package curacao.components;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import curacao.annotations.Component;
import curacao.core.servlet.ServletContext;
import curacao.exceptions.CuracaoException;
import curacao.exceptions.reflection.ArgumentRequiredException;
import curacao.exceptions.reflection.ComponentConfigurationException;
import curacao.exceptions.reflection.ComponentInstantiationException;
import curacao.util.helpers.WildcardMatchHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.util.reflection.CuracaoReflectionUtils.getComponentsInBootPackage;
import static curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructorForClass;
import static curacao.util.reflection.CuracaoReflectionUtils.getNoArgConstructorForClass;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Creates every {@link Component} in the boot package, once, and resolves the {@link curacao.annotations.Injectable}
 * constructor parameters of components, controllers, filters and mappers. The rules are documented on
 * {@link Component}; in short, an injection is never guessed: an ambiguous or unresolvable parameter fails startup.
 */
public final class ComponentTable {

    private static final Logger LOG = getLogger(ComponentTable.class);

    private static final int UNINITIALIZED = 0;
    private static final int INITIALIZED = 1;

    private static final String ARROW = " -> ";
    private static final String NAME_SEPARATOR = ", ";

    /**
     * Lifecycle interfaces, which any component may implement, so they're never an injection type.
     */
    private static final Set<Class<?>> LIFECYCLE_TYPES = ImmutableSet.of(CuracaoComponent.class,
            ComponentInitializable.class, ComponentDestroyable.class);

    /**
     * Components in a stable order, for creation, lists and error messages.
     */
    private static final Comparator<Class<?>> BY_NAME = Comparator.comparing(Class::getName);

    /**
     * Every component, by its class, in creation order: a component's dependencies come before it.
     */
    private final Map<Class<?>, Object> components_;

    /**
     * For each type a component can be injected as (its class, superclasses and interfaces), the components
     * assignable to it, sorted by name.
     */
    private final ListMultimap<Class<?>, Class<?>> candidates_;

    /**
     * Components that were replaced or suppressed, and why.
     */
    private final Map<Class<?>, String> removed_;

    /**
     * A local boot switch to ensure that components in this mapping table are only ever initialized if
     * uninitialized, and destroyed if initialized.
     */
    private final AtomicInteger bootSwitch_;

    /**
     * The underlying Servlet context of this application, injectable into any component.
     */
    private final ServletContext context_;

    public ComponentTable(
            @Nonnull final ServletContext context) {
        this(context, getComponentsInBootPackage());
    }

    /**
     * Builds the table from the given component classes, instead of scanning the boot package.
     */
    /* package */ ComponentTable(
            @Nonnull final ServletContext context,
            @Nonnull final Set<Class<?>> scanned) {
        checkNotNull(scanned, "Scanned components cannot be null.");

        context_ = checkNotNull(context, "Servlet context cannot be null.");
        bootSwitch_ = new AtomicInteger(UNINITIALIZED);
        final List<Class<?>> all = scanned.stream()
                .filter(c -> c.getAnnotation(Component.class) != null)
                .sorted(BY_NAME)
                .collect(ImmutableList.toImmutableList());
        removed_ = removedComponents(all);
        final List<Class<?>> active = all.stream()
                .filter(c -> !removed_.containsKey(c))
                .collect(ImmutableList.toImmutableList());
        candidates_ = candidates(active);
        try {
            components_ = createAll(active);
        } catch (final CuracaoException e) {
            throw e;
        } catch (final Exception e) {
            throw new ComponentInstantiationException("Failed to build component table.", e);
        }
        LOG.debug("Application component table: {}", components_.keySet());
    }

    /**
     * Resolves the arguments of an {@link curacao.annotations.Injectable} constructor of a component,
     * controller, filter or mapper.
     *
     * @param consumer the class that declares the constructor, for error messages
     * @throws ComponentConfigurationException if a parameter matches several components and none is primary
     * @throws ArgumentRequiredException if a parameter matches no component
     */
    public Object[] resolveArguments(
            @Nonnull final Constructor<?> constructor,
            @Nonnull final Class<?> consumer) {
        checkNotNull(constructor, "Constructor cannot be null.");
        checkNotNull(consumer, "Consumer cannot be null.");

        return resolveArgumentsWith(constructor, consumer, components_::get);
    }

    /**
     * The component that a parameter of the given type gets, or null when no component matches it.
     *
     * @throws ComponentConfigurationException if several components match and none is primary
     */
    @Nullable
    public Object getComponentForType(
            @Nonnull final Class<?> clazz) {
        checkNotNull(clazz, "Class instance type cannot be null.");

        if (ServletContext.class.equals(clazz)) {
            return context_;
        } else if (candidates_.get(clazz).isEmpty()) {
            return null;
        }

        return components_.get(select(clazz, clazz));
    }

    /**
     * Initializes all the components in this instance, in creation order, so a component's dependencies are
     * initialized before it. Should only be called once on application context startup; it's gated by an atomic
     * boot switch, so calling it again has no effect.
     *
     * <p>If a component fails to initialize, the components before it are destroyed, in reverse order, before
     * the failure is thrown, so a failed startup leaves nothing running.
     *
     * @return the underlying {@link ComponentTable}, this instance for convenience
     */
    public ComponentTable initializeAll() {
        if (bootSwitch_.compareAndSet(UNINITIALIZED, INITIALIZED)) {
            final List<Object> initialized = Lists.newArrayList();
            for (final Object component : components_.values()) {
                if (component instanceof ComponentInitializable) {
                    try {
                        LOG.debug("Initializing component: {}", component.getClass().getCanonicalName());
                        ((ComponentInitializable) component).initialize();
                    } catch (final Exception e) {
                        final CuracaoException failure = new CuracaoException("Failed to initialize component: "
                                + component.getClass().getCanonicalName(), e);
                        bootSwitch_.set(UNINITIALIZED);
                        destroy(initialized).forEach(failure::addSuppressed);
                        throw failure;
                    }
                }
                initialized.add(component);
            }
        }
        return this; // Convenience
    }

    /**
     * Destroys all the components in this instance, in reverse creation order, so a component is destroyed
     * before its dependencies. Should only be called once on application context shutdown; it's gated by an
     * atomic boot switch, so calling it again, or before {@link #initializeAll()}, has no effect.
     *
     * <p>Every component is destroyed even when one fails; the first failure is thrown afterwards, with any
     * others attached as suppressed exceptions.
     *
     * @return the underlying {@link ComponentTable}, this instance for convenience
     */
    public ComponentTable destroyAll() {
        if (bootSwitch_.compareAndSet(INITIALIZED, UNINITIALIZED)) {
            final List<CuracaoException> failures = destroy(components_.values());
            if (!failures.isEmpty()) {
                final CuracaoException first = failures.get(0);
                failures.subList(1, failures.size()).forEach(first::addSuppressed);
                throw first;
            }
        }
        return this; // Convenience
    }

    /**
     * Destroys the given components in reverse order, carrying on past failures; returns the failures.
     */
    private static List<CuracaoException> destroy(
            final Collection<Object> components) {
        final List<CuracaoException> failures = Lists.newArrayList();
        for (final Object component : ImmutableList.copyOf(components).reverse()) {
            if (component instanceof ComponentDestroyable) {
                try {
                    LOG.debug("Destroying component: {}", component.getClass().getCanonicalName());
                    ((ComponentDestroyable) component).destroy();
                } catch (final Exception e) {
                    failures.add(new CuracaoException("Failed to destroy component: "
                            + component.getClass().getCanonicalName(), e));
                }
            }
        }

        return failures;
    }

    /**
     * The components that other components replace or suppress, and why. Fails on a replacement that isn't a
     * component, a component that removes itself, and replacements that form a cycle.
     */
    private static Map<Class<?>, String> removedComponents(
            final List<Class<?>> all) {
        final Set<Class<?>> known = ImmutableSet.copyOf(all);
        final Map<Class<?>, String> removed = Maps.newLinkedHashMap();
        // Who removes whom, to find cycles.
        final ListMultimap<Class<?>, Class<?>> removes = ArrayListMultimap.create();
        for (final Class<?> component : all) {
            final Component annotation = component.getAnnotation(Component.class);
            for (final Class<?> replaced : annotation.replaces()) {
                if (replaced.equals(component)) {
                    throw new ComponentConfigurationException("Component " + component.getName()
                            + " can't replace itself.");
                } else if (!known.contains(replaced)) {
                    throw new ComponentConfigurationException("Component " + component.getName() + " replaces "
                            + replaced.getName() + ", which isn't a @Component in the boot package.");
                }
                removed.putIfAbsent(replaced, "replaced by " + component.getName());
                removes.put(component, replaced);
            }
            for (final String pattern : annotation.suppress()) {
                if (StringUtils.isBlank(pattern)) {
                    continue;
                }
                final List<Class<?>> matched = all.stream()
                        .filter(c -> WildcardMatchHelper.matchesAny(new String[]{pattern}, nameOf(c)))
                        .collect(ImmutableList.toImmutableList());
                if (matched.isEmpty()) {
                    LOG.warn("Component {} suppresses \"{}\", which matches no component.", component.getName(),
                            pattern);
                }
                for (final Class<?> suppressed : matched) {
                    if (suppressed.equals(component)) {
                        throw new ComponentConfigurationException("Component " + component.getName()
                                + " suppresses itself with \"" + pattern + "\".");
                    }
                    removed.putIfAbsent(suppressed, "suppressed by " + component.getName() + " (\"" + pattern
                            + "\")");
                    removes.put(component, suppressed);
                }
            }
        }
        final Set<Class<?>> done = Sets.newHashSet();
        for (final Class<?> component : all) {
            findCycle(component, removes, new ArrayDeque<>(), done);
        }
        removed.forEach((component, reason) -> LOG.debug("Not creating component {}: {}", component.getName(),
                reason));

        return ImmutableMap.copyOf(removed);
    }

    /**
     * Fails when components replace or suppress each other in a cycle (A replaces B, B replaces A), which
     * would leave none of them.
     */
    private static void findCycle(
            final Class<?> component,
            final ListMultimap<Class<?>, Class<?>> removes,
            final Deque<Class<?>> path,
            final Set<Class<?>> done) {
        if (done.contains(component)) {
            return;
        } else if (path.contains(component)) {
            final List<Class<?>> walked = ImmutableList.copyOf(path);
            throw new ComponentConfigurationException("Components replace each other in a cycle, which would "
                    + "leave none of them: " + cycle(walked.subList(walked.indexOf(component), walked.size()),
                    component));
        }

        path.addLast(component);
        for (final Class<?> removed : removes.get(component)) {
            findCycle(removed, removes, path, done);
        }
        path.removeLast();
        done.add(component);
    }

    /**
     * Each type the active components can be injected as, with the components assignable to it.
     */
    private static ListMultimap<Class<?>, Class<?>> candidates(
            final List<Class<?>> active) {
        final ImmutableListMultimap.Builder<Class<?>, Class<?>> candidates = ImmutableListMultimap.builder();
        for (final Class<?> component : active) {
            for (final Class<?> type : injectableTypes(component)) {
                candidates.put(type, component);
            }
        }

        return candidates.build();
    }

    /**
     * The class itself, its superclasses (except {@link Object}) and every interface it implements, directly or
     * not, except the lifecycle interfaces.
     */
    private static Set<Class<?>> injectableTypes(
            final Class<?> component) {
        final Set<Class<?>> types = Sets.newLinkedHashSet();
        final Deque<Class<?>> pending = new ArrayDeque<>();
        for (Class<?> c = component; c != null && !Object.class.equals(c); c = c.getSuperclass()) {
            types.add(c);
            pending.addAll(ImmutableList.copyOf(c.getInterfaces()));
        }
        while (!pending.isEmpty()) {
            final Class<?> interfacz = pending.removeFirst();
            if (!LIFECYCLE_TYPES.contains(interfacz) && types.add(interfacz)) {
                pending.addAll(ImmutableList.copyOf(interfacz.getInterfaces()));
            }
        }

        return types;
    }

    private Map<Class<?>, Object> createAll(
            final List<Class<?>> active) throws Exception {
        final Map<Class<?>, Object> instances = Maps.newLinkedHashMap();
        for (final Class<?> component : active) {
            create(component, instances, new ArrayDeque<>());
        }

        return ImmutableMap.copyOf(instances);
    }

    /**
     * The component's instance, creating it, and first its dependencies, if it doesn't exist yet.
     */
    private Object create(
            final Class<?> component,
            final Map<Class<?>, Object> instances,
            final Deque<Class<?>> creating) throws Exception {
        final Object existing = instances.get(component);
        if (existing != null) {
            return existing;
        } else if (creating.contains(component)) {
            throw new ComponentConfigurationException("Circular dependency between components: "
                    + cycle(ImmutableList.copyOf(creating), component));
        }

        creating.addLast(component);
        final Constructor<?> injectable = getInjectableConstructorForClass(component);
        final Object instance;
        try {
            if (injectable == null) {
                // No @Injectable constructor: the public no-argument one, or a startup failure.
                instance = getNoArgConstructorForClass(component).newInstance();
            } else {
                instance = injectable.newInstance(resolveArgumentsWith(injectable, component,
                        dependency -> createDependency(dependency, instances, creating)));
            }
        } catch (final InvocationTargetException e) {
            throw new ComponentInstantiationException("Failed to instantiate component: "
                    + component.getCanonicalName(), (e.getCause() instanceof Exception)
                    ? (Exception) e.getCause() : e);
        }
        creating.removeLast();
        instances.put(component, instance);

        return instance;
    }

    private Object createDependency(
            final Class<?> dependency,
            final Map<Class<?>, Object> instances,
            final Deque<Class<?>> creating) {
        try {
            return create(dependency, instances, creating);
        } catch (final CuracaoException e) {
            throw e;
        } catch (final Exception e) {
            throw new ComponentInstantiationException("Failed to instantiate component: "
                    + dependency.getCanonicalName(), e);
        }
    }

    private Object[] resolveArgumentsWith(
            final Constructor<?> constructor,
            final Class<?> consumer,
            final Function<Class<?>, Object> instances) {
        final Type[] generic = constructor.getGenericParameterTypes();
        // An inner class's implicit outer parameter can make the two differ; fall back to the raw types.
        final Type[] types = (generic.length == constructor.getParameterCount())
                ? generic : constructor.getParameterTypes();
        final Object[] arguments = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            arguments[i] = resolve(types[i], consumer, instances);
        }

        return arguments;
    }

    private Object resolve(
            final Type type,
            final Class<?> consumer,
            final Function<Class<?>, Object> instances) {
        final Class<?> raw = rawClass(type, consumer);
        if (List.class.equals(raw)) {
            final Class<?> element = elementType(type, consumer);

            return candidates_.get(element).stream()
                    .map(instances)
                    .collect(ImmutableList.toImmutableList());
        } else if (ServletContext.class.equals(raw)) {
            return context_;
        }

        return instances.apply(select(raw, consumer));
    }

    /**
     * The one component a parameter of the given type gets.
     */
    private Class<?> select(
            final Class<?> type,
            final Class<?> consumer) {
        final List<Class<?>> matches = candidates_.get(type);
        if (matches.contains(type)) {
            // A component asked for by its own class.
            return type;
        } else if (matches.size() == 1) {
            return matches.get(0);
        } else if (matches.isEmpty()) {
            final String reason = removed_.get(type);
            if (reason != null) {
                throw new ArgumentRequiredException(consumer.getName() + " needs " + type.getName()
                        + ", which was " + reason + "; ask for its replacement or an interface it implements.");
            }
            throw new ArgumentRequiredException(consumer.getName() + " needs " + type.getName()
                    + ", but no @Component in the boot package is assignable to it.");
        }

        final List<Class<?>> primaries = matches.stream()
                .filter(c -> c.getAnnotation(Component.class).primary())
                .collect(ImmutableList.toImmutableList());
        if (primaries.size() == 1) {
            return primaries.get(0);
        } else if (primaries.size() > 1) {
            throw new ComponentConfigurationException(consumer.getName() + " needs " + type.getName()
                    + ", and more than one matching component is primary: " + names(primaries)
                    + ". Mark at most one with @Component(primary = true).");
        }
        throw new ComponentConfigurationException(consumer.getName() + " needs one " + type.getName()
                + ", but several components match: " + names(matches) + ". Mark one with "
                + "@Component(primary = true), remove the others with @Component(replaces = ...), or ask for "
                + "List<" + type.getSimpleName() + "> to get them all.");
    }

    private static Class<?> rawClass(
            final Type type,
            final Class<?> consumer) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        throw new ArgumentRequiredException(consumer.getName() + " has a constructor parameter of type " + type
                + ", which can't be injected.");
    }

    /**
     * The element type of a {@code List<T>} or {@code List<? extends T>} parameter.
     */
    private static Class<?> elementType(
            final Type type,
            final Class<?> consumer) {
        if (type instanceof ParameterizedType) {
            Type element = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (element instanceof WildcardType) {
                element = ((WildcardType) element).getUpperBounds()[0];
            }
            if (element instanceof Class && !Object.class.equals(element)) {
                return (Class<?>) element;
            } else if (element instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) element).getRawType();
            }
        }
        throw new ArgumentRequiredException(consumer.getName() + " asks for a List without a component element "
                + "type; declare it as List<SomeType>.");
    }

    private static String nameOf(
            final Class<?> component) {
        final String canonical = component.getCanonicalName();

        return (canonical == null) ? component.getName() : canonical;
    }

    /**
     * A cycle for an error message: {@code A -> B -> A}.
     */
    private static String cycle(
            final List<Class<?>> path,
            final Class<?> repeated) {
        return Stream.concat(path.stream(), Stream.of(repeated))
                .map(Class::getName)
                .collect(Collectors.joining(ARROW));
    }

    private static String names(
            final List<Class<?>> components) {
        return components.stream()
                .map(Class::getName)
                .collect(Collectors.joining(NAME_SEPARATOR));
    }

}
