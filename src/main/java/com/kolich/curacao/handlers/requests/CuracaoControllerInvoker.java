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

import static com.kolich.curacao.handlers.requests.ControllerArgumentTypeMappingTable.getArgumentMappersForType;
import static com.kolich.curacao.handlers.requests.RequestRoutingTable.getRoutesByMethod;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.kolich.curacao.exceptions.routing.PathNotFoundException;
import com.kolich.curacao.handlers.requests.mappers.ControllerArgumentMapper;
import com.kolich.curacao.util.helpers.UrlPathHelper;
import com.kolich.curacao.util.matchers.AntPathMatcher;

public final class CuracaoControllerInvoker implements Callable<Object> {
	
	private static final UrlPathHelper pathHelper__ =
		UrlPathHelper.getInstance();
	private static final AntPathMatcher antMatcher__ =
		AntPathMatcher.getInstance();
		
	private final Logger logger_;
	
	private final AsyncContext context_;
	
	private final HttpServletRequest request_;
	private final HttpServletResponse response_;
	
	private final String method_;
	private final String requestUri_;
	//private final String contextPath_;
	
	private final String comment_;
	
	public CuracaoControllerInvoker(final Logger logger,
		final AsyncContext context) {
		logger_ = logger;
		context_ = context;
		// Derived properties below.
		request_ = (HttpServletRequest)context_.getRequest();
		response_ = (HttpServletResponse)context_.getResponse();
		method_ = request_.getMethod().toUpperCase();
		requestUri_ = request_.getRequestURI();
		//contextPath_ = request_.getContextPath();
		comment_ = String.format("%s:%s", method_, requestUri_);
	}
	
	@Override
	public final Object call() throws Exception {
		final String pathWithinApplication =
			pathHelper__.getPathWithinApplication(request_);
		logger_.debug("Computed path within application context (requestUri=" +
			comment_ + ", computedPath=" + pathWithinApplication + ")");
		final Map<String,CuracaoMethodInvokable> candidates =
			getRoutesByMethod(method_);
		logger_.debug("Found " + candidates.size() + " controller " +
			"candidates for request: " + comment_);
		// Check if we found any viable candidates for the incoming HTTP
		// request method.  If we didn't find any, immeaditely bail letting
		// the user know this incoming HTTP request method just isn't
		// supported by the implementation.
		if(candidates.isEmpty()) {
			throw new PathNotFoundException("Found " + candidates.size() +
				" controller candidates for request: " + comment_);
		}
		// For each viable option, need to compare the path provided
		// with the attached invokable method annotation to decide
		// if that path matches the request.
		CuracaoMethodInvokable invokable = null;
		Map<String,String> pathVars = null;
		for(final Map.Entry<String,CuracaoMethodInvokable> e :
			candidates.entrySet()) {
			if(logger_.isDebugEnabled()) {
				logger_.debug("Found invokable method candidate: " +
					e.getKey().toString());
			}
			pathVars = antMatcher__.extractUriTemplateVariables(e.getKey(),
				pathWithinApplication);
			if(pathVars != null) {
				if(logger_.isDebugEnabled()) {
					logger_.debug("Extracted path variables: " +
						pathVars.toString());
				}
				invokable = e.getValue();
				break;
			}
		}
		// If we found ~some~ method that supports the incoming HTTP request
		// type, but no proper annotated controller method that matches
		// the request path, that means we've got nothing.
		if(invokable == null) {
			throw new PathNotFoundException("Found no invokable controller " +
				"method worthy of servicing request: " + comment_);
		}
		// Invoke the filter attached to the controller method invokable.
		// This method may throw an exception, which is totally fair and will
		// be handled by the upper-layer.
		invokable.getFilter().getInstance().filter(request_);
		// Build the paramter list to be passed into the controller method
		// via reflection.
		final Object[] parameters = buildPopulatedParameterList(invokable,
			pathVars);
		// Reflection invoke the discovered "controller" method.
		final Object invokedResult = invokable.getMethod().invoke(
			invokable.getController().getInstance(),
			parameters);
		// A set of hard coded controller return type pre-processors. That is,
		// we take the type/object that the controller returned once invoked
		// and see if we need to do anything special with it in this request
		// context (using the thread that's handling the _REQUEST_).
		Object o = invokedResult;
		if(invokedResult instanceof Callable) {
			o = ((Callable<?>)invokedResult).call();
		} else if(invokedResult instanceof Future) {
			o = ((Future<?>)invokedResult).get();
		}
		return o;
	}
	
	private final Object[] buildPopulatedParameterList(
		final CuracaoMethodInvokable invokable,
		final Map<String,String> pathVars) throws Exception {
		final List<Object> params = Lists.newLinkedList();
		// The actual method argument/parameter types, in order.
		final List<Class<?>> methodParams = invokable.getParameterTypes();
		// A 2D array (ugh) that gives a list of all annotations 
		final Annotation[][] a = invokable.getParameterAnnotations();
		for(int i = 0; i < methodParams.size(); i++) {
			Object toAdd = null;
			// A list of all annotations attached to this method
			// argument/parameter, in order.  If the argument/parameter has
			// no annotations, this will be an _empty_ array of length zero.
			final Annotation[] annotations = a[i];
			// Yes, the developer can decorate a controller method param
			// with multiple annotations, but we're only going to ever
			// care about the first one.
			final Annotation first = getFirstAnnotation(annotations);
			// Get the type/class associated with the method argument/parameter
			// at the given index.
			final Class<?> o = methodParams.get(i);			
			if(o.isAssignableFrom(AsyncContext.class)) {
				toAdd = context_;
			} else {
				// Given a class type, find an argument mapper for it.  Note
				// that if no mappers exist for the given type, the method
				// below will ~not~ return null, but rather an empty collection.
				final Collection<ControllerArgumentMapper<?>> mappers =
					getArgumentMappersForType(o);
				for(final ControllerArgumentMapper<?> mapper : mappers) {
					// Ask each mapper, in order, to resolve the argument.
					// The first mapper to resolve (return non-null) wins.
					// User registered mappers are called first given that they
					// are inserted into the multi-map first before the "default"
					// mappers, which allows consumers of this library to register
					// and override default argument mappers for foundational
					// classes like "String", etc. if they wish.
					if((toAdd = mapper.resolve(first, pathVars, request_,
						response_)) != null) {
						break;
					}
				}
			}
			params.add(toAdd);
		}
		return params.toArray(new Object[]{});
	}
	
	private static final Annotation getFirstAnnotation(final Annotation[] as) {
		return getAnnotationSafely(as, 0);
	}
	
	private static final Annotation getAnnotationSafely(final Annotation[] as,
		final int index) {
		return (as.length > 0 && index < as.length) ? as[index] : null;
	}

}
