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

import org.apache.commons.lang3.StringUtils;

public final class CuracaoMethodInvokable {
	
	private final Class<?> clazz_;
	
	private final Method method_;
	private final List<Class<?>> parameterTypes_;
		
	public CuracaoMethodInvokable(final Class<?> clazz, final Method method) {
		clazz_ = checkNotNull(clazz, "Invokable method base class cannot " +
			"be null.");
		method_ = checkNotNull(method, "Invokable method cannot be null.");
		parameterTypes_ = Arrays.asList(method_.getParameterTypes());
	}
	
	public Class<?> getClazz() {
		return clazz_;
	}
	
	public Method getMethod() {
		return method_;
	}
	
	public List<Class<?>> getParameterTypes() {
		return parameterTypes_;
	}
	
	public Annotation[][] getParameterAnnotations() {
		return method_.getParameterAnnotations();
	}
	
	@Override
	public String toString() {
		return String.format("%s.%s(%s)",
			clazz_.getCanonicalName(),
			method_.getName(),
			StringUtils.join(parameterTypes_, ","));
	}

}
