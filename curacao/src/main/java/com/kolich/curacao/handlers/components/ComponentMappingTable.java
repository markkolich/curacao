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

package com.kolich.curacao.handlers.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import com.google.common.collect.Maps;
import com.kolich.curacao.CuracaoConfigLoader;
import com.kolich.curacao.annotations.Component;

public final class ComponentMappingTable {
	
	private static final Logger logger__ = 
		getLogger(ComponentMappingTable.class);
	
	private static final String COMPONENT_ANNOTATION_SN =
		Component.class.getSimpleName();
	
	/**
	 * This table maps a set of known class instance types to their
	 * respective {@link CuracaoComponent}'s.
	 */
	private final Map<Class<?>, CuracaoComponent> table_;
	
	private final AtomicBoolean bootSwitch_;
	
	private ComponentMappingTable() {
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Loading component mappers from " +
			"declared boot-package: " + bootPackage);
		// Scan for "components" inside of the declared boot package
		// looking for annotated Java classes that represent components.
		table_ = buildMappingTable(bootPackage);
		if(logger__.isInfoEnabled()) {
			logger__.info("Application component mapping table: " +
				table_);
		}
		bootSwitch_ = new AtomicBoolean(false);
	}
	
	private static class LazyHolder {
		private static final ComponentMappingTable instance__ =
			new ComponentMappingTable();
	}
	private static final Map<Class<?>, CuracaoComponent> getTable() {
		return LazyHolder.instance__.table_;
	}
	private static final AtomicBoolean getBootSwitch() {
		return LazyHolder.instance__.bootSwitch_;
	}
		
	public static final CuracaoComponent getComponentForType(
		@Nonnull final Object result) {
		checkNotNull(result, "Result object cannot be null.");
		return getComponentForType(result.getClass());
	}
	
	@Nullable
	public static final CuracaoComponent getComponentForType(
		@Nonnull final Class<?> clazz) {
		checkNotNull(clazz, "Class instance type cannot be null.");
		return getTable().get(clazz);
	}
	
	private static final Map<Class<?>, CuracaoComponent>
		buildMappingTable(final String bootPackage) {
		final Map<Class<?>, CuracaoComponent> components =
			Maps.newConcurrentMap(); // Concurrent map.
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
		logger__.debug("Found " + componentClasses.size() + " mappers " +
			"annotated with @" + COMPONENT_ANNOTATION_SN);
		// For each discovered component...
		for(final Class<?> component : componentClasses) {
			logger__.debug("Found @" + COMPONENT_ANNOTATION_SN + ": " +
				component.getCanonicalName());
			if(!implementsComponentInterface(component)) {
				logger__.error("Class " + component.getCanonicalName() +
					" was annotated with @" + COMPONENT_ANNOTATION_SN +
					" but does not implement required interface " +
					CuracaoComponent.class.getCanonicalName());
				continue;
			}
			try {
				// Class.newInstance() is evil, so we do the ~right~ thing
				// here to instantiate a new instance of the mapper using
				// the preferred getConstructor() idiom.
				final Constructor<?> ctor = component.getConstructor();
				components.put(component, (CuracaoComponent)ctor.newInstance());
			} catch (NoSuchMethodException e) {
				logger__.error("Failed to instantiate component instance: " +
					component.getCanonicalName() + " -- This class is very " +
					"likely missing a nullary (no argument) constructor. " +
					"Please add one.", e);
			} catch (Exception e) {
				logger__.error("Failed to instantiate component instance: " +
					component.getCanonicalName(), e);
			}
		}
		return components;
	}
	
	private static final boolean implementsComponentInterface(
		final Class<?> component) {
		// If the class does not implement any interfaces, getInterfaces()
		// will return an empty array which translates into an empty list here.
		final List<Class<?>> interfaces = asList(component.getInterfaces());
		boolean implementz = false;
		for(final Class<?> clazz : interfaces) {
			if(clazz.equals(CuracaoComponent.class)) {
				implementz = true;
				break;
			}
		}
		return implementz;
	}
	
	public static final void initializeAll() {
		// We use an AtomicBoolean here to guard against consumers of this
		// class from calling initialize() on the set of components multiple
		// times.  This guarantees that the initialize() method of each
		// component will never be called more than once in the same
		// application life-cycle.
		if(getBootSwitch().compareAndSet(false, true)) {
			for(final Map.Entry<Class<?>,CuracaoComponent> entry :
				getTable().entrySet()) {
				final Class<?> clazz = entry.getKey();
				final CuracaoComponent component = entry.getValue();
				try {
					logger__.debug("Enabling @" + COMPONENT_ANNOTATION_SN +
						": " + clazz.getCanonicalName());
					component.initialize();
				} catch (Exception e) {
					logger__.error("Failed to initialize @" +
						COMPONENT_ANNOTATION_SN + ": " +
						clazz.getCanonicalName(), e);
				}
			}
		}
	}
	
	public static final void destroyAll() {
		// We use an AtomicBoolean here to guard against consumers of this
		// class from calling destroy() on the set of components multiple
		// times.  This guarantees that the destroy() method of each
		// component will never be called more than once in the same
		// application life-cycle.
		if(getBootSwitch().compareAndSet(true, false)) {
			for(final Map.Entry<Class<?>,CuracaoComponent> entry :
				getTable().entrySet()) {
				final Class<?> clazz = entry.getKey();
				final CuracaoComponent component = entry.getValue();
				try {
					logger__.debug("Destroying @" + COMPONENT_ANNOTATION_SN +
						": " + clazz.getCanonicalName());
					component.destroy();
				} catch (Exception e) {
					logger__.error("Failed to destroy (shutdown) @" +
						COMPONENT_ANNOTATION_SN + ": " +
						clazz.getCanonicalName(), e);
				}
			}
		}
	}

}
