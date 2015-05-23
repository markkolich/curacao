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

package com.kolich.curacao.components;

import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.kolich.curacao.CuracaoConfigLoader;
import com.kolich.curacao.annotations.Component;
import com.kolich.curacao.annotations.parameters.Required;
import com.kolich.curacao.exceptions.CuracaoException;
import com.kolich.curacao.exceptions.reflection.ComponentArgumentRequiredException;
import com.kolich.curacao.exceptions.reflection.ComponentInstantiationException;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.curacao.util.reflection.CuracaoAnnotationUtils.hasAnnotation;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructor;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getTypesInPackageAnnotatedWith;
import static org.slf4j.LoggerFactory.getLogger;

public final class ComponentTable {

	private static final Logger logger__ = getLogger(ComponentTable.class);

	private static final String COMPONENT_ANNOTATION_SN =
		Component.class.getSimpleName();

    private static final int UNINITIALIZED = 0, INITIALIZED = 1;

	/**
	 * This table maps a set of known class instance types to their
	 * respective singleton objects.
	 */
	private final ImmutableMap<Class<?>, Object> componentTable_;

    /**
     * A local boot switch to ensure that components in this mapping
     * table are only ever initialized if uninitialized, and destroyed
     * if initialized.
     */
    private final AtomicInteger bootSwitch_;

    /**
     * The underlying Servlet context of this application.  Used
     * only to inject the {@link ServletContext} object into instantiated
     * components as needed.
     */
    private final ServletContext context_;

	public ComponentTable(@Nonnull final ServletContext context) {
        context_ = checkNotNull(context, "Servlet context cannot be null.");
        bootSwitch_ = new AtomicInteger(UNINITIALIZED);
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Loading components from declared boot-package: {}",
            bootPackage);
		// Scan for "components" inside of the declared boot package
		// looking for annotated Java classes that represent components.
		componentTable_ = buildComponentTable(bootPackage);
        logger__.info("Application component table: {}", componentTable_);
	}

	private final ImmutableMap<Class<?>, Object> buildComponentTable(final String bootPackage) {
		final Map<Class<?>, Object> componentMap =
			Maps.newLinkedHashMap(); // Linked hash map to preserve order.
        // Immediately add the Servlet context object to the component map
        // such that components and controllers who need access to the context
        // via their Injectable constructor can get it w/o any trickery.
        componentMap.put(ServletContext.class, context_);
		// Use the reflections package scanner to scan the boot package looking
		// for all classes therein that contain "annotated" component classes.
        final ImmutableSet<Class<?>> components =
            getTypesInPackageAnnotatedWith(bootPackage, Component.class);
		logger__.debug("Found {} components annotated with @{}",
            components.size(), COMPONENT_ANNOTATION_SN);
		// For each discovered component...
		for (final Class<?> component : components) {
			logger__.debug("Found @{}: {}", COMPONENT_ANNOTATION_SN,
				component.getCanonicalName());
            try {
                // If the component mapping table does not already contain an
                // instance for component class type, then instantiate one.
                if (!componentMap.containsKey(component)) {
                    // The "dep stack" is used to keep track of where we are as
                    // far as circular dependencies go.
                    final Set<Class<?>> depStack = Sets.newLinkedHashSet();
                    componentMap.put(component,
                        // Recursively instantiate components up-the-tree
                        // as needed, as defined based on their @Injectable
                        // annotated constructors.  Note that this method does
                        // NOT initialize the component, that is done later
                        // after all components are instantiated.
                        instantiate(components, componentMap, component, depStack));
                }
            } catch (Exception e) {
                // The component could not be instantiated.  There's no point
                // in continuing, so give up in error.
                throw new ComponentInstantiationException("Failed to " +
                    "instantiate component instance: " +
                    component.getCanonicalName(), e);
            }
		}
		return ImmutableMap.copyOf(componentMap);
	}

    private final Object instantiate(final ImmutableSet<Class<?>> allComponents,
                                     final Map<Class<?>, Object> componentMap,
                                     final Class<?> component,
                                     final Set<Class<?>> depStack) throws Exception {
        // Locate a single constructor worthy of injecting with ~other~
        // components, if any.  May be null.
        final Constructor<?> ctor = getInjectableConstructor(component);
        Object instance = null;
        if (ctor == null) {
            // Class.newInstance() is evil, so we do the ~right~ thing
            // here to instantiate a new instance of the component
            // using the preferred getConstructor() idiom.
            instance = component.getConstructor().newInstance();
        } else {
            final Class<?>[] types = ctor.getParameterTypes();
            // Construct an array of Object's outright to avoid system array
            // copying from a List/Collection to a vanilla array later.
            final Object[] params = new Object[types.length];
            for (int i = 0, l = types.length; i < l; i++) {
                final Class<?> type = types[i];
                // <https://github.com/markkolich/curacao/issues/7>
                // If the dependency stack contains the type we're tasked with
                // instantiating, but the component map already contains an
                // instance with this type, then it's ~not~ a real circular
                // dependency -- we already have what we need to fulfill the
                // request.
                if (depStack.contains(type) && !componentMap.containsKey(type)) {
                    // Circular dependency detected, A -> B, but B -> A.
                    // Or, A -> B -> C, but C -> A.  Can't do that, sorry!
                    throw new CuracaoException("CIRCULAR DEPENDENCY DETECTED! " +
                        "While trying to instantiate @" + COMPONENT_ANNOTATION_SN +
                        ": " + component.getCanonicalName() + " it depends " +
                        "on the other components (" + depStack + ")");
                } else if (componentMap.containsKey(type)) {
                    // The component mapping table already contained an instance
                    // of the component type we're after.  Simply grab it and
                    // add it to the constructor parameter list.
                    params[i] = componentMap.get(type);
                } else if (type.isInterface()) {
                    // Interfaces are handled differently.  The logic here
                    // involves finding some component, if any, that implements
                    // the discovered interface type.  If one is found, we attempt
                    // to instantiate it, if it hasn't been instantiated already.
                    final Class<?> found = Iterables.tryFind(allComponents,
                        Predicates.assignableFrom(type)).orNull();
                    if (found != null) {
                        // We found some component that implements the discovered
                        // interface.  Let's try to instantiate it.  Add the
                        // ~interface~ class type ot the dependency stack.
                        depStack.add(type);
                        final Object recursiveInstance =
                            // Recursion!
                            instantiate(allComponents, componentMap, found,
                                depStack);
                        // Add the freshly instantiated component instance to the
                        // new component mapping table as we go.
                        componentMap.put(found, recursiveInstance);
                        // Add the freshly instantiated component instance to the
                        // list of component constructor arguments/parameters.
                        params[i] = recursiveInstance;
                    } else {
                        // Found no component that implements the given interface.
                        params[i] = null;
                    }
                } else {
                    // The component mapping table does not contain a
                    // component for the given class.  We might need to
                    // instantiate a fresh one and then inject, checking
                    // carefully for circular dependencies.
                    depStack.add(component);
                    final Object recursiveInstance =
                        // Recursion!
                        instantiate(allComponents, componentMap, type,
                            depStack);
                    // Add the freshly instantiated component instance to the
                    // new component mapping table as we go.
                    componentMap.put(type, recursiveInstance);
                    // Add the freshly instantiated component instance to the
                    // list of component constructor arguments/parameters.
                    params[i] = recursiveInstance;
                }
                // https://github.com/markkolich/curacao/issues/18
                // If the constructor argument parameter was null, this implies that we could not find a
                // suitable "component" or object to pass into the constructor. As such, we need to verify if
                // the parameter is annotated with @Required and if it is, fail hard.
                if (params[i] == null) {
                    // Get a list of parameters attached to this constructor argument.
                    final Annotation[] annotations = ctor.getParameterAnnotations()[i];
                    // Is any annotation on the argument annotated with @Required?
                    if (hasAnnotation(annotations, Required.class)) {
                        throw new ComponentArgumentRequiredException("Could not resolve " +
                            "@Required component constructor argument: " + type.getCanonicalName());
                    }
                }
            }
            instance = ctor.newInstance(params);
        }
        // The freshly freshly instantiated component instance may implement
        // a set of interfaces, and therefore, can be used to inject other
        // components that have specified it using only the interface and not
        // a concrete implementation.  As such, add each implemented interface
        // to the component map pointing directly to the concrete implementation
        // instance.
        for (final Class<?> interfacz : instance.getClass().getInterfaces()) {
            // If the component is decorated with 'CuracaoComponent' don't
            // bother trying to add said interfaces to the underlying component
            // map (they're special, internal to the toolkit, unrelated to
            // user defined interfaces).
            if (!interfacz.isAssignableFrom(CuracaoComponent.class)) {
                componentMap.put(interfacz, instance);
            }
        }
        return instance;
    }

    @Nullable
    public final Object getComponentForType(@Nonnull final Class<?> clazz) {
        checkNotNull(clazz, "Class instance type cannot be null.");
        return componentTable_.get(clazz);
    }

    /**
     * Initializes all of the components in this instance.  Should only be
     * called once on application context startup.  Is gated using an atomic
     * boot switch, ensuring that this method will only initialize the
     * components if they haven't been initialized yet.  This essentially
     * guarantees that the components will only ever be initialized once,
     * calling this method multiple times (either intentionally or by mistake)
     * will have no effect.
     * @return the underlying {@link ComponentTable}, this instance
     */
	public final ComponentTable initializeAll() {
        // We use an AtomicInteger here to guard against consumers of this
        // class from calling initializeAll() on the set of components multiple
        // times.  This guarantees that the initialize() method of each
        // component will never be called more than once in the same
        // application life-cycle.
		if (bootSwitch_.compareAndSet(UNINITIALIZED, INITIALIZED)) {
			for (final Map.Entry<Class<?>, Object> entry : componentTable_.entrySet()) {
				final Class<?> clazz = entry.getKey();
				final Object component = entry.getValue();
                // Only attempt to initialize the component if it implements
                // the component initializable interface.
                if (component instanceof ComponentInitializable) {
                    try {
                        logger__.debug("Initializing @{}: {}",
                            COMPONENT_ANNOTATION_SN, clazz.getCanonicalName());
                        ((ComponentInitializable)component).initialize();
                    } catch (Exception e) {
                        // If the component failed to initialize, should we
                        // keep going?  That's up for debate.  Currently if one
                        // component did the wrong thing, we log the error
                        // and move on.  However, it is acknowledged that this
                        // behavior may lead to other more obscure issues later.
                        logger__.error("Failed to initialize @{}: {}",
                            COMPONENT_ANNOTATION_SN, clazz.getCanonicalName(),
                            e);
                    }
                }
			}
		}
        return this; // Convenience
	}

    /**
     * Destroys all of the components in this instance.  Should only be
     * called once on application context shutdown.  Is gated using an atomic
     * boot switch, ensuring that this method will only destroy the
     * components if they have been initialized.  This essentially guarantees
     * that the components will only ever be destroyed once, calling this
     * method multiple times (either intentionally or by mistake) will have
     * no effect.
     * @return the underlying {@link ComponentTable}, this instance
     */
	public final ComponentTable destroyAll() {
        // We use an AtomicInteger here to guard against consumers of this
        // class from calling destroyAll() on the set of components multiple
        // times.  This guarantees that the destroy() method of each
        // component will never be called more than once in the same
        // application life-cycle.
		if (bootSwitch_.compareAndSet(INITIALIZED, UNINITIALIZED)) {
			for (final Map.Entry<Class<?>, Object> entry : componentTable_.entrySet()) {
				final Class<?> clazz = entry.getKey();
				final Object component = entry.getValue();
                // Only attempt to destroy the component if it implements
                // the component destroyable interface.
                if (component instanceof ComponentDestroyable) {
                    try {
                        logger__.debug("Destroying @{}: {}",
                            COMPONENT_ANNOTATION_SN, clazz.getCanonicalName());
                        ((ComponentDestroyable)component).destroy();
                    } catch (Exception e) {
                        logger__.error("Failed to destroy (shutdown) @{}: {}",
                            COMPONENT_ANNOTATION_SN, clazz.getCanonicalName(),
                            e);
                    }
                }
			}
		}
        return this; // Convenience
	}

}
