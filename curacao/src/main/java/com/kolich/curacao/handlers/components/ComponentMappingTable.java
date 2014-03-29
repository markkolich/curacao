/**
 * Copyright (c) 2014 Mark S. Kolich
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

package com.kolich.curacao.handlers.components;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kolich.curacao.CuracaoConfigLoader;
import com.kolich.curacao.annotations.Component;
import com.kolich.curacao.exceptions.CuracaoException;
import com.kolich.curacao.exceptions.reflection.ComponentInstantiationException;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructor;
import static org.slf4j.LoggerFactory.getLogger;

public final class ComponentMappingTable {

	private static final Logger logger__ =
		getLogger(ComponentMappingTable.class);

	private static final String COMPONENT_ANNOTATION_SN =
		Component.class.getSimpleName();

    private static final int UNINITIALIZED = 0, INITIALIZED = 1;

	/**
	 * This table maps a set of known class instance types to their
	 * respective singleton objects.
	 */
	private final ImmutableMap<Class<?>, Object> table_;

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

	public ComponentMappingTable(final ServletContext context) {
        context_ = context;
        bootSwitch_ = new AtomicInteger(UNINITIALIZED);
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Loading component mappers from declared " +
            "boot-package: " + bootPackage);
		// Scan for "components" inside of the declared boot package
		// looking for annotated Java classes that represent components.
		table_ = buildMappingTable(bootPackage);
		if(logger__.isInfoEnabled()) {
			logger__.info("Application component mapping table: " + table_);
		}
	}

	private final ImmutableMap<Class<?>, Object> buildMappingTable(
        final String bootPackage) {
		final Map<Class<?>, Object> components =
			Maps.newLinkedHashMap(); // Linked hash map to preserve order.
        // Immediately add the Servlet context object to the component map
        // such that components and controllers who need access to the context
        // via their Injectable constructor can get it w/o any trickery.
        components.put(ServletContext.class, context_);
		// Use the reflections package scanner to scan the boot package looking
		// for all classes therein that contain "annotated" mapper classes.
		final Reflections componentReflection = new Reflections(
			new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage(bootPackage))
				.setScanners(new TypeAnnotationsScanner()));
		// Find all "controller classes" in the specified boot package that
		// are annotated with our component mapper annotation.
		final Set<Class<?>> componentClasses =
			componentReflection.getTypesAnnotatedWith(Component.class);
		logger__.debug("Found " + componentClasses.size() + " components " +
            "annotated with @" + COMPONENT_ANNOTATION_SN);
		// For each discovered component...
		for(final Class<?> component : componentClasses) {
			logger__.debug("Found @" + COMPONENT_ANNOTATION_SN + ": " +
				component.getCanonicalName());
            try {
                // If the component mapping table does not already contain an
                // instance for component class type, then instantiate one.
                if(!components.containsKey(component)) {
                    // The "dep stack" is used to keep track of where we are as
                    // far as circular dependencies go.
                    final Set<Class<?>> depStack = Sets.newLinkedHashSet();
                    components.put(component,
                        // Recursively instantiate components up-the-tree
                        // as needed, as defined based on their @Injectable
                        // annotated constructors.  Note that this method does
                        // NOT initialize the component, that is done later
                        // after all components are instantiated.
                        instantiate(components, component, depStack));
                }
            } catch (Exception e) {
                // The component could not be instantiated.  There's no point
                // in continuing, so give up in error.
                throw new ComponentInstantiationException("Failed to " +
                    "instantiate component instance: " +
                    component.getCanonicalName(), e);
            }
		}
		return ImmutableMap.copyOf(components);
	}

    private final Object instantiate(final Map<Class<?>, Object> components,
                                     final Class<?> component,
                                     final Set<Class<?>> depStack) throws Exception {
        // Locate a single constructor worthy of injecting with ~other~
        // components, if any.  May be null.
        final Constructor<?> ctor = getInjectableConstructor(component);
        Object instance = null;
        if(ctor == null) {
            // Class.newInstance() is evil, so we do the ~right~ thing
            // here to instantiate a new instance of the component
            // using the preferred getConstructor() idiom.
            instance = component.getConstructor().newInstance();
        } else {
            final Class<?>[] types = ctor.getParameterTypes();
            // Construct an ArrayList<T> with a prescribed capacity. In theory,
            // this is more performant because we will subsequently morph
            // the List<T> into an array via toArray() below which will ideally
            // reduce array copying.
            final List<Object> params =
                Lists.newArrayListWithCapacity(types.length);
            for(final Class<?> type : types) {
                if(depStack.contains(type)) {
                    // Circular dependency detected, A -> B, but B -> A.
                    // Or, A -> B -> C, but C -> A.  Can't do that, sorry!
                    throw new CuracaoException("CIRCULAR DEPENDENCY DETECTED! " +
                        "While trying to instantiate @" + COMPONENT_ANNOTATION_SN +
                        ": " + component.getCanonicalName() + " it depends " +
                        "on the other components (" + depStack + ")");
                } else if(components.containsKey(type)) {
                    // The component mapping table already contained an instance
                    // of the component type we're after.  Simply grab it and
                    // add it to the constructor parameter list.
                    params.add(components.get(type));
                } else {
                    // The component mapping table does not contain a
                    // component for the given class.  We might need to
                    // instantiate a fresh one and then inject, checking
                    // carefully for circular dependencies.
                    depStack.add(component);
                    final Object recursiveInstance =
                        // Recursion!
                        instantiate(components, type, depStack);
                    // Add the freshly instantiated component instance to the
                    // new component mapping table as we go.
                    components.put(type, recursiveInstance);
                    // Add the freshly instantiated component instance to the
                    // list of component constructor arguments/parameters.
                    params.add(recursiveInstance);
                }
            }
            instance = ctor.newInstance(params.toArray());
        }
        return instance;
    }

    @Nullable
    public final Object getComponentForType(@Nonnull final Class<?> clazz) {
        checkNotNull(clazz, "Class instance type cannot be null.");
        return table_.get(clazz);
    }

    /**
     * Initializes all of the components in this instance.  Should only be
     * called once on application context startup.  Is gated using an atomic
     * boot switch, ensuring that this method will only initialize the
     * components if they haven't been initialized yet.  This essentially
     * guarantees that the components will only ever be initialized once,
     * calling this method multiple times (either intentionally or by mistake)
     * will have no effect.
     * @return the underlying {@link ComponentMappingTable}, this instance
     */
	public final ComponentMappingTable initializeAll() {
        // We use an AtomicInteger here to guard against consumers of this
        // class from calling initializeAll() on the set of components multiple
        // times.  This guarantees that the initialize() method of each
        // component will never be called more than once in the same
        // application life-cycle.
		if(bootSwitch_.compareAndSet(UNINITIALIZED, INITIALIZED)) {
			for(final Map.Entry<Class<?>, Object> entry : table_.entrySet()) {
				final Class<?> clazz = entry.getKey();
				final Object component = entry.getValue();
                // Only attempt to initialize the component if it implements
                // the component initializable interface.
                if(component instanceof ComponentInitializable) {
                    try {
                        logger__.debug("Enabling @" + COMPONENT_ANNOTATION_SN +
                            ": " + clazz.getCanonicalName());
                        ((ComponentInitializable)component).initialize();
                    } catch (Exception e) {
                        // If the component failed to initialize, should we
                        // keep going?  That's up for debate.  Currently if one
                        // component did the wrong thing, we log the error
                        // and move on.  However, it is acknowledged that this
                        // behavior may lead to other more obscure issues later.
                        logger__.error("Failed to initialize @" +
                            COMPONENT_ANNOTATION_SN + ": " +
                            clazz.getCanonicalName(), e);
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
     * @return the underlying {@link ComponentMappingTable}, this instance
     */
	public final ComponentMappingTable destroyAll() {
        // We use an AtomicInteger here to guard against consumers of this
        // class from calling destroyAll() on the set of components multiple
        // times.  This guarantees that the destroy() method of each
        // component will never be called more than once in the same
        // application life-cycle.
		if(bootSwitch_.compareAndSet(INITIALIZED, UNINITIALIZED)) {
			for(final Map.Entry<Class<?>, Object> entry : table_.entrySet()) {
				final Class<?> clazz = entry.getKey();
				final Object component = entry.getValue();
                // Only attempt to destroy the component if it implements
                // the component destroyable interface.
                if(component instanceof ComponentDestroyable) {
                    try {
                        logger__.debug("Destroying @" + COMPONENT_ANNOTATION_SN +
                            ": " + clazz.getCanonicalName());
                        ((ComponentDestroyable)component).destroy();
                    } catch (Exception e) {
                        logger__.error("Failed to destroy (shutdown) @" +
                            COMPONENT_ANNOTATION_SN + ": " +
                            clazz.getCanonicalName(), e);
                    }
                }
			}
		}
        return this; // Convenience
	}

}
