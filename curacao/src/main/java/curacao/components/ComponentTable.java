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

import com.google.common.base.Predicates;
import com.google.common.collect.*;
import curacao.annotations.Component;
import curacao.core.servlet.ServletContext;
import curacao.exceptions.CuracaoException;
import curacao.exceptions.reflection.ArgumentRequiredException;
import curacao.exceptions.reflection.ComponentInstantiationException;
import curacao.util.helpers.WildcardMatchHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.util.reflection.CuracaoReflectionUtils.getComponentsInBootPackage;
import static curacao.util.reflection.CuracaoReflectionUtils.getConstructorWithMostParameters;
import static curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructorForClass;
import static org.slf4j.LoggerFactory.getLogger;

public final class ComponentTable {

    private static final Logger LOG = getLogger(ComponentTable.class);

    private static final int UNINITIALIZED = 0;
    private static final int INITIALIZED = 1;

    /**
     * This table maps a set of known class instance types to their respective singleton objects.
     */
    private final Map<Class<?>, Object> componentTable_;

    /**
     * A local boot switch to ensure that components in this mapping table are only ever initialized if
     * uninitialized, and destroyed if initialized.
     */
    private final AtomicInteger bootSwitch_;

    /**
     * The underlying Servlet context of this application. Used only to inject the {@link ServletContext}
     * object into instantiated components as needed.
     */
    private final ServletContext context_;

    public ComponentTable(
            @Nonnull final ServletContext context) {
        context_ = checkNotNull(context, "Servlet context cannot be null.");
        bootSwitch_ = new AtomicInteger(UNINITIALIZED);
        // Scan for "components" inside of the declared boot package
        // looking for annotated Java classes that represent components.
        try {
            componentTable_ = buildComponentTable();
        } catch (final Exception e) {
            throw new ComponentInstantiationException("Failed to build component table.", e);
        }
        LOG.debug("Application component table: {}", componentTable_);
    }

    private Map<Class<?>, Object> buildComponentTable() throws Exception {
        // Linked hash map to preserve order.
        final Map<Class<?>, Object> componentMap = Maps.newLinkedHashMap();
        // Immediately add the Servlet context object to the component map such that components and controllers who
        // need access to the context via their Injectable constructor can get it w/o any trickery.
        componentMap.put(ServletContext.class, context_);

        final Set<Class<?>> scannedComponents = getComponentsInBootPackage();

        // A complete list of components to instantiate, in order.
        final List<Class<?>> componentsToInstantiate = Lists.newLinkedList();
        // A set of components to suppress, by the required suppression pattern.
        final Set<String> componentsToSuppress = Sets.newHashSet();

        // Walk the list of scanned components, identifying any component that has provided a
        // suppression hint in the annotation. If so, add the suppression pattern to the suppression
        // list and also add the component to the list of components to instantiate (giving preferential
        // treatment to components with a suppression list to be instantiated first).
        scannedComponents.stream()
                .filter(c -> c.getAnnotation(Component.class) != null)
                .map(c -> Pair.of(c, c.getAnnotation(Component.class)))
                .map(c -> (c.getRight().value().length > 0) ? Pair.of(c.getLeft(), c.getRight().value()) : null)
                .filter(Objects::nonNull)
                .forEach(c -> {
                    componentsToInstantiate.add(c.getLeft());
                    Arrays.stream(c.getRight()).filter(StringUtils::isNotBlank).forEach(componentsToSuppress::add);
                });

        // Walk the list of scanned components, carefully excluding any component that matches the
        // discovered suppression list.
        scannedComponents.stream()
                .filter(c -> !WildcardMatchHelper.matchesAny(componentsToSuppress, c.getCanonicalName()))
                .forEach(componentsToInstantiate::add);

        // For each discovered component...
        for (final Class<?> component : componentsToInstantiate) {
            LOG.debug("Found: {}", component.getCanonicalName());
            try {
                // If the component mapping table does not already contain an instance for component class type,
                // then attempt to instantiate one.
                if (!componentMap.containsKey(component)) {
                    // The "dep stack" is used to keep track of where we are as far as circular dependencies go.
                    final Set<Class<?>> depStack = Sets.newLinkedHashSet();
                    // Recursively instantiate components up-the-tree as needed, as defined based on their
                    // @Injectable annotated constructors. Note that this method does NOT initialize the
                    // component, that is done later after all components are instantiated.
                    final Object instance = instantiate(componentsToInstantiate, componentMap, component, depStack);
                    componentMap.put(component, instance);
                }
            } catch (final Exception e) {
                // The component could not be instantiated. There's no point in continuing, so give up in error.
                throw new ComponentInstantiationException("Failed to instantiate component instance: "
                        + component.getCanonicalName(), e);
            }
        }
        return ImmutableMap.copyOf(componentMap);
    }

    private Object instantiate(
            final List<Class<?>> allComponents,
            final Map<Class<?>, Object> componentMap,
            final Class<?> component,
            final Set<Class<?>> depStack) throws Exception {
        Object instance = null;
        // The component class is only "injectable" if it is annotated with the correct component
        // annotation at the class level.
        final boolean isInjectable = isInjectable(component);
        // Locate a single constructor worthy of injecting with ~other~ components, if any. May be null.
        final Constructor<?> injectableCtor = (isInjectable) ? getInjectableConstructorForClass(component) : null;
        if (injectableCtor == null) {
            final Constructor<?> plainCtor = getConstructorWithMostParameters(component);
            final int paramCount = plainCtor.getParameterTypes().length;
            // Class.newInstance() is evil, so we do the ~right~ thing here to instantiate a new instance of the
            // component using the preferred getConstructor() idiom. Note we don't have any arguments to pass to
            // the constructor because it was not annotated so we just pass an array of all "null" meaning every
            // argument into the constructor will be null.
            instance = plainCtor.newInstance(new Object[paramCount]);
        } else {
            final Class<?>[] types = injectableCtor.getParameterTypes();
            // Construct an array of Object's outright to avoid system array copying from a List/Collection to
            // a vanilla array later.
            final Object[] params = new Object[types.length];
            for (int i = 0, l = types.length; i < l; i++) {
                final Class<?> type = types[i];
                // https://github.com/markkolich/curacao/issues/7
                // If the dependency stack contains the type we're tasked with instantiating, but the component map
                // already contains an instance with this type, then it's ~not~ a real circular dependency -- we
                // already have what we need to fulfill the request.
                if (depStack.contains(type) && !componentMap.containsKey(type)) {
                    // Circular dependency detected, A -> B, but B -> A.
                    // Or, A -> B -> C, but C -> A. Can't do that, sorry!
                    throw new CuracaoException("CIRCULAR DEPENDENCY DETECTED! While trying to instantiate "
                            + "component: " + component.getCanonicalName() + " it depends on other components: "
                            + depStack);
                } else if (componentMap.containsKey(type)) {
                    // The component mapping table already contained an instance of the component type we're after.
                    // Simply grab it and add it to the constructor parameter list.
                    params[i] = componentMap.get(type);
                } else if (type.isInterface()) {
                    // Interfaces are handled differently. The logic here involves finding some component, if any,
                    // that implements the discovered interface type. If one is found, we attempt to instantiate it,
                    // if it hasn't been instantiated already.
                    final Class<?> found = Iterables.tryFind(allComponents, Predicates.subtypeOf(type)).orNull();
                    if (found != null) {
                        // We found some component that implements the discovered interface. Let's try to instantiate
                        // it. Add the ~interface~ class type ot the dependency stack.
                        depStack.add(type);
                        // Recursion!
                        final Object recursiveInstance =
                                instantiate(allComponents, componentMap, found, depStack);
                        // Add the freshly instantiated component instance to the new component mapping table as we go.
                        componentMap.put(found, recursiveInstance);
                        // Add the freshly instantiated component instance to the list of component constructor
                        // arguments/parameters.
                        params[i] = recursiveInstance;
                    } else {
                        // Found no component that implements the given interface.
                        params[i] = null;
                    }
                } else {
                    // The component mapping table does not contain a component for the given class. We might need to
                    // instantiate a fresh one and then inject, checking carefully for circular dependencies.
                    depStack.add(component);
                    // Recursion!
                    final Object recursiveInstance = instantiate(allComponents, componentMap, type, depStack);
                    // Add the freshly instantiated component instance to the new component mapping table as we go.
                    componentMap.put(type, recursiveInstance);
                    // Add the freshly instantiated component instance to the list of component constructor
                    // arguments/parameters.
                    params[i] = recursiveInstance;
                }
                // Check if null.
                if (params[i] == null) {
                    throw new ArgumentRequiredException("Could not resolve constructor argument '"
                            + type.getCanonicalName() + "' on class: " + component.getCanonicalName());
                }
            }
            instance = injectableCtor.newInstance(params);
        }
        // The freshly freshly instantiated component instance may implement a set of interfaces, and therefore,
        // can be used to inject other components that have specified it using only the interface and not a
        // concrete implementation. As such, add each implemented interface to the component map pointing directly
        // to the concrete implementation instance.
        Class<?> superClass = instance.getClass();
        while (superClass != null && !Object.class.equals(superClass)) {
            for (final Class<?> interfacz : superClass.getInterfaces()) {
                // If the component is decorated with 'CuracaoComponent' don't bother trying to add said interface to
                // the underlying component map (it's special, internal to the toolkit, unrelated to user defined
                // interfaces).
                if (!interfacz.isAssignableFrom(CuracaoComponent.class)) {
                    componentMap.put(interfacz, instance);
                }
            }
            // Will be null for "Object" (base class)
            superClass = superClass.getSuperclass();
        }
        return instance;
    }

    @Nullable
    public Object getComponentForType(
            @Nonnull final Class<?> clazz) {
        checkNotNull(clazz, "Class instance type cannot be null.");
        return componentTable_.get(clazz);
    }

    /**
     * Initializes all the components in this instance. Should only be called once on application context
     * startup. Is gated using an atomic boot switch, ensuring that this method will only initialize the
     * components if they haven't been initialized yet. This essentially guarantees that the components will
     * only ever be initialized once, calling this method multiple times (either intentionally or by mistake)
     * will have no effect.
     *
     * @return the underlying {@link ComponentTable}, this instance for convenience
     */
    public ComponentTable initializeAll() {
        // We use an AtomicInteger here to guard against consumers of this class from calling initializeAll() on
        // the set of components multiple times. This guarantees that the initialize() method of each component will
        // never be called more than once in the same application life-cycle.
        if (bootSwitch_.compareAndSet(UNINITIALIZED, INITIALIZED)) {
            final List<ComponentInitializable> initializables = componentTable_.values().stream()
                    .filter(c -> c instanceof ComponentInitializable)
                    .map(c -> (ComponentInitializable) c)
                    .collect(Collectors.toList());

            for (final ComponentInitializable initializable : initializables) {
                try {
                    LOG.debug("Initializing component: {}", initializable.getClass().getCanonicalName());
                    initializable.initialize();
                } catch (final Exception e) {
                    throw new CuracaoException("Failed to initialize component: "
                            + initializable.getClass().getCanonicalName(), e);
                }
            }
        }
        return this; // Convenience
    }

    /**
     * Destroys all of the components in this instance. Should only be called once on application context shutdown.
     * Is gated using an atomic boot switch, ensuring that this method will only destroy the components if they have
     * been initialized. This essentially guarantees that the components will only ever be destroyed once, calling this
     * method multiple times (either intentionally or by mistake) will have no effect.
     *
     * @return the underlying {@link ComponentTable}, this instance for convenience
     */
    public ComponentTable destroyAll() {
        // We use an AtomicInteger here to guard against consumers of this class from calling destroyAll() on the set
        // of components multiple times. This guarantees that the destroy() method of each component will never be
        // called more than once in the same application life-cycle.
        if (bootSwitch_.compareAndSet(INITIALIZED, UNINITIALIZED)) {
            final List<ComponentDestroyable> destroyables = componentTable_.values().stream()
                    .filter(c -> c instanceof ComponentDestroyable)
                    .map(c -> (ComponentDestroyable) c)
                    .collect(Collectors.toList());

            for (final ComponentDestroyable destroyable : destroyables) {
                try {
                    LOG.debug("Destroying component: {}", destroyable.getClass().getCanonicalName());
                    destroyable.destroy();
                } catch (final Exception e) {
                    throw new CuracaoException("Failed to destroy component: "
                            + destroyable.getClass().getCanonicalName(), e);
                }
            }
        }
        return this; // Convenience
    }

    private static boolean isInjectable(
            final Class<?> component) {
        return (null != component.getAnnotation(Component.class));
    }

}
