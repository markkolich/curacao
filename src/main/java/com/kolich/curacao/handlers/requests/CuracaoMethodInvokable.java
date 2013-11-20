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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import com.kolich.curacao.exceptions.CuracaoException;
import com.kolich.curacao.handlers.requests.filters.CuracaoRequestFilter;

public final class CuracaoMethodInvokable {
	
	public static class InvokableClassWithInstance<T> {
		private final Class<T> clazz_;
		private final T instance_;
		public InvokableClassWithInstance(final Class<T> clazz)
			throws NoSuchMethodException, Exception {
			clazz_ = checkNotNull(clazz, "Class cannot be null.");
			instance_ = newInstance(clazz_);
		}
		public Class<T> getClazz() {
			return clazz_;
		}
		public T getInstance() {
			return instance_;
		}
		@SuppressWarnings("unchecked")
		private final T newInstance(final Class<?> clazz)
			throws NoSuchMethodException, Exception {
			// Class.newInstance() is evil, so we do the ~right~ thing
			// here to instantiate new instances using the preferred
			// getConstructor() idiom.
			return (T)clazz.getConstructor().newInstance();
		}
	}
	
	private final InvokableClassWithInstance<?> controller_;
	private final InvokableClassWithInstance<? extends CuracaoRequestFilter> filter_;
	
	private final Method method_;
	private final List<Class<?>> parameterTypes_;
			
	public CuracaoMethodInvokable(final Class<?> controller,
		final Class<? extends CuracaoRequestFilter> filter,
		final Method method) {
		checkNotNull(controller, "Controller base class cannot be null.");
		checkNotNull(filter, "Controller method filter class cannot be null.");
		// Instantiate a new instance of the controller class.
		try {
			controller_ = new InvokableClassWithInstance<>(controller);
		} catch (NoSuchMethodException e) {
			throw new CuracaoException("Failed to instantiate controller " +
				"instance: " + controller.getCanonicalName() + " -- This " +
				"class is likely missing a nullary (no argument) " +
				"constructor. Please add one.", e);
		} catch (Exception e) {
			throw new CuracaoException("Failed to instantiate controller " +
				"instance.", e);
		}
		// Instantiate a new instance of the filter class attached to
		// the controller method.
		try {
			filter_ = new InvokableClassWithInstance<>(filter);
		} catch (NoSuchMethodException e) {
			throw new CuracaoException("Failed to instantiate method " +
				"filer class instance: " + filter.getCanonicalName() +
				" -- This class is likely missing a nullary (no argument) " +
				"constructor. Please add one.", e);
		} catch (Exception e) {
			throw new CuracaoException("Failed to instantiate method " +
				"filter instance.", e);
		}
		method_ = checkNotNull(method, "Controller method cannot be null.");
		parameterTypes_ = Arrays.asList(method_.getParameterTypes());
	}
	
	@Nonnull
	public InvokableClassWithInstance<?> getController() {
		return controller_;
	}
	
	@Nonnull
	public InvokableClassWithInstance<? extends CuracaoRequestFilter> getFilter() {
		return filter_;
	}
	
	@Nonnull
	public Method getMethod() {
		return method_;
	}
	
	@Nonnull
	public List<Class<?>> getParameterTypes() {
		return parameterTypes_;
	}
	
	/**
	 * Returns an array of length zero if the underlying method is
	 * parameterless.
	 */
	@Nonnull
	public Annotation[][] getParameterAnnotations() {
		return method_.getParameterAnnotations();
	}
	
	@Override
	public String toString() {
		return String.format("%s.%s(%s)",
			controller_.getClazz().getCanonicalName(),
			method_.getName(),
			StringUtils.join(parameterTypes_, ", "));
	}

}
