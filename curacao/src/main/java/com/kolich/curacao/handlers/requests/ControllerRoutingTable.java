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
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getInjectableConstructor;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getMethodReflectionInstanceForClass;
import static com.kolich.curacao.util.reflection.CuracaoReflectionUtils.getTypesInPackageAnnotatedWith;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;

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
import com.kolich.curacao.handlers.requests.filters.CuracaoRequestFilter;

public final class ControllerRoutingTable {
	
	private static final Logger logger__ = 
		getLogger(ControllerRoutingTable.class);
	
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

    /**
     * A Table&lt;R,C,V&gt; which is used to internally map the following:
     *   R -- The supported path of the request.
     *   C -- The HTTP request method.
     *   V -- The controller and reflection invokable method that will be called
     *        to handle a request at path R.
     */
	private final Table<String, String, CuracaoMethodInvokable> table_;
	
	private ControllerRoutingTable() {
		final String bootPackage = CuracaoConfigLoader.getBootPackage();
		logger__.info("Scanning for controllers in declared boot-package: " +
			bootPackage);
		// Scan the "controllers" inside of the declared boot package
		// looking for annotated Java methods that will be called
		// when a request is received.
		table_ = buildRoutingTable(bootPackage);
		if(logger__.isInfoEnabled()) {
			logger__.info("Application routing table: " + table_);
		}
	}

    // This makes use of the "Initialization-on-demand holder idiom" which is
    // discussed in detail here:
    // http://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
    // As such, this is totally thread safe and performant.
	private static class LazyHolder {
		private static final ControllerRoutingTable instance__ =
			new ControllerRoutingTable();
	}
	private static final Table<String,String,CuracaoMethodInvokable> getTable() {
		return LazyHolder.instance__.table_;
	}
	
	public static final void preload() {
		getTable();
	}
	
	public static final Map<String,CuracaoMethodInvokable> getRoutesByHttpMethod(
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
		// Find all "controller classes" in the specified boot package that
		// are annotated with our @Controller annotation.
		final Set<Class<?>> controllers =
			getTypesInPackageAnnotatedWith(bootPackage, Controller.class);
		logger__.debug("Found " + controllers.size() + " controllers " +
			"annotated with @" + Controller.class.getSimpleName());
		// For each discovered controller class, find all annotated methods
		// inside of it and add them to the routing table.
		for(final Class<?> controller : controllers) {
			logger__.debug("Found @" + Controller.class.getSimpleName() + ": " +
				controller.getCanonicalName());
			final Reflections methodReflection =
				getMethodReflectionInstanceForClass(controller);
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
					final Class<? extends CuracaoRequestFilter> filter;
					if(a instanceof TRACE) {
						path = ((TRACE)a).value();
						filter = ((TRACE)a).filter();
					} else if(a instanceof HEAD) {
						path = ((HEAD)a).value();
						filter = ((HEAD)a).filter();
					} else if(a instanceof GET) {
						path = ((GET)a).value();
						filter = ((GET)a).filter();
					} else if(a instanceof POST) {
						path = ((POST)a).value();
						filter = ((POST)a).filter();
					} else if(a instanceof PUT) {
						path = ((PUT)a).value();
						filter = ((PUT)a).filter();
					} else if(a instanceof DELETE) {
						path = ((DELETE)a).value();
						filter = ((DELETE)a).filter();
					} else {
						logger__.warn("Found unsupported annotation, " +
							"ignoring and continuing: " + a.toString());
						continue;
					}
					try {
						// Pull out any injectable annotated controller class
						// constructors that may be present.  If none are
						// found, it's OK to return null, the invokable will
						// handle that later and use the default nullary
						// constructor.
						final Constructor<?> injectableCntlrCtor =
							getInjectableConstructor(controller);
						// Pull out any injectable annotated filter class
						// constructors that may be present.  If none are
						// found, it's OK to return null, the invokable will
						// handle that later and use the default nullary
						// constructor.
						final Constructor<?> injectableFilterCtor =
							getInjectableConstructor(filter);
						// Attach the controller method, and any annotated
						// request filters, to the routing table.
						final CuracaoMethodInvokable invokable =
							new CuracaoMethodInvokable(
								// Controller class
								controller, injectableCntlrCtor,
								// Filter class
								filter, injectableFilterCtor,
								// Controller method
								method);
						table.put(path, httpMethod.method_, invokable);
					} catch (Exception e) {
						logger__.error("Failed to add reflection discovered " +
							"route to routing table.", e);
					}
				}
			}
		}
		// Immutable, so things don't change out from under us later once
		// the routing table is constructed.
		return ImmutableTable.copyOf(table);
	}

}
