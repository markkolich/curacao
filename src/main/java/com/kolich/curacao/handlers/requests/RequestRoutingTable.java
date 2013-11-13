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

package com.kolich.curacao.handlers.requests;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.kolich.curacao.CuracaoConfigLoader;
import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.methods.DELETE;
import com.kolich.curacao.annotations.methods.GET;
import com.kolich.curacao.annotations.methods.HEAD;
import com.kolich.curacao.annotations.methods.POST;
import com.kolich.curacao.annotations.methods.PUT;
import com.kolich.curacao.annotations.methods.TRACE;

public final class RequestRoutingTable {
	
	private static final Logger logger__ = getLogger(RequestRoutingTable.class);
	
	/**
	 * A list of all supported HTTP methods supported by this library,
	 * listed here using their Java annotation/reflection representation.
	 */
	public static enum SupportedMethods {
		TRACE(TRACE.class, "TRACE"),
		HEAD(HEAD.class, "HEAD"),
		GET(GET.class, "GET"),
		POST(POST.class, "POST"),
		PUT(PUT.class, "PUT"),
		DELETE(DELETE.class, "DELETE");
		public final Class<? extends Annotation> annotation_;
		public final String method_;
		private SupportedMethods(final Class<? extends Annotation> annotation,
			final String method) {
			annotation_ = annotation;
			method_ = method;
		}
	}
	
	private final Table<String,String,CuracaoMethodInvokable> table_;
	
	private RequestRoutingTable() {
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Scanning for controllers in declared boot-package: " +
			bootPackage);
		// Scan the "controllers" inside of the declared boot package
		// looking for annotated Java methods that will be called
		// when a request is received.
		table_ = buildRoutingTable(bootPackage);
	}
	
	private static class LazyHolder {
		private static final RequestRoutingTable instance__ =
			new RequestRoutingTable();
	}
	private static final Table<String,String,CuracaoMethodInvokable> getTable() {
		return LazyHolder.instance__.table_;
	}
	
	public static final void preload() {
		getTable();
	}
	
	public static final Map<String,CuracaoMethodInvokable> getRoutesByMethod(
		final String method) {
		checkNotNull(method, "HTTP method cannot be null.");
		return getTable().column(method);
	}
	
	private static final Table<String,String,CuracaoMethodInvokable>
		buildRoutingTable(final String bootPackage) {
		// Create a new routing table to hold the discovered methods.
		// Note that this table is mutable, but we will create a new
		// "immutable" table from it later on the return.
		final Table<String,String,CuracaoMethodInvokable> table =
			HashBasedTable.create();
		// Use the reflections package scanner to scan the boot package looking
		// for all methods therein that contain "annotated" methods.
		final Reflections controllerReflection = new Reflections(
			new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage(bootPackage))
				.setScanners(new TypeAnnotationsScanner()));
		// Find all "controller classes" in the specified boot package that
		// are annotated with our @Controller annotation.
		final Set<Class<?>> controllers =
			controllerReflection.getTypesAnnotatedWith(Controller.class);
		logger__.debug("Found " + controllers.size() + " controllers " +
			"annotated with @" + Controller.class.getSimpleName());
		// For each discovered controller class, find all annotated methods
		// inside of it and add them to the routing table.
		for(final Class<?> controller : controllers) {
			logger__.debug("Found @" + Controller.class.getSimpleName() + ": " +
				controller.getCanonicalName());
			// Using the Reflections library to make searching the classpath
			// easier to discover annotated classes and methods therein.
			final Reflections methodReflection = new Reflections(
				new ConfigurationBuilder()
					.setUrls(ClasspathHelper.forClass(controller))
					.filterInputsBy(new Predicate<String>() {
			            @Override
						public boolean apply(final String input) {
			                return input != null &&
			                	// Intentionally limits the scanner to find
			                	// methods only inside of the discovered
			                	// controller class.
			                	input.startsWith(controller.getCanonicalName());
			            }})
					.setScanners(new MethodAnnotationsScanner()));
			// For each annotation type we care about (all supported HTTP
			// methods), fetch the list of Java methods that correspond to each,
			// if any.
			for(final SupportedMethods httpMethod : SupportedMethods.values()) {
				final Set<Method> methods =
					methodReflection.getMethodsAnnotatedWith(
						httpMethod.annotation_);
				// For each discovered Java method, get its annotation and insert
				// the discovered annotation, HTTP type and Java method into
				// the routing table.
				for(final Method method : methods) {
					final Annotation a = method.getAnnotation(
						httpMethod.annotation_);
					final String path;
					if(a instanceof TRACE) {
						path = ((TRACE)a).value();
					} else if(a instanceof HEAD) {
						path = ((HEAD)a).value();
					} else if(a instanceof GET) {
						path = ((GET)a).value();
					} else if(a instanceof POST) {
						path = ((POST)a).value();
					} else if(a instanceof PUT) {
						path = ((PUT)a).value();
					} else if(a instanceof DELETE) {
						path = ((DELETE)a).value();
					} else {
						logger__.warn("Found unsupported annotation, " +
							"ignoring and continuing: " + a.toString());
						continue;
					}
					// Attach the controller method to the routing table.
					table.put(path,
						// HTTP request method
						httpMethod.method_,
						// Invokable.
						new CuracaoMethodInvokable(controller, method));
				}
			}
		}
		// Immutable, so things don't change out from under us later once
		// the routing table is constructed.
		return ImmutableTable.copyOf(table);
	}

}
