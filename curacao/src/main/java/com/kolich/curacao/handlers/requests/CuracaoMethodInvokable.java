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

package com.kolich.curacao.handlers.requests;

import com.google.common.collect.Lists;
import com.kolich.curacao.annotations.Injectable;
import com.kolich.curacao.exceptions.CuracaoException;
import com.kolich.curacao.handlers.components.ComponentMappingTable;
import com.kolich.curacao.handlers.requests.filters.CuracaoRequestFilter;
import com.kolich.curacao.handlers.requests.matchers.CuracaoPathMatcher;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CuracaoMethodInvokable {
	
	public class InvokableClassWithInstance<T> {

        /**
         * The class instance type of this invokable.
         */
        @Nonnull
		public final Class<T> clazz_;

		/**
		 * A class constructor annotated with the {@link Injectable}
		 * constructor annotation.  May be null if the controller class
		 * has no {@link Injectable} annotated constructors.
		 */
        @Nullable
        public final Constructor<?> injectable_;

        /**
         * An instantiated instance of the invokable class.
         */
        @Nonnull
        public final T instance_;

		public InvokableClassWithInstance(final Class<T> clazz,
                                          @Nullable final Constructor<?> injectable) throws Exception {
			clazz_ = checkNotNull(clazz, "Class cannot be null.");
			injectable_ = injectable;
			instance_ = newInstance(clazz_);
		}

		@SuppressWarnings("unchecked")
		private final T newInstance(final Class<?> clazz) throws Exception {
            T instance = null;
			// The injectable will be null if the class has no injectable
			// annotated constructors.
			if(injectable_ == null) {
				// Class.newInstance() is evil, so we do the ~right~ thing
				// here to instantiate new instances using the preferred
				// getConstructor() idiom.			
				instance = (T)clazz.getConstructor().newInstance();
			} else {
				// The injectable here is a filter or controller class
				// constructor.
				final Class<?>[] types = injectable_.getParameterTypes();
                // Construct an ArrayList with a prescribed capacity. In theory,
                // this is more performant because we will subsequently morph
                // the List into an array via toArray() below.
				final List<Object> params =
                    Lists.newArrayListWithCapacity(types.length);
				for(final Class<?> type : types) {
                    // The instance constructor may define a set of components
                    // that should be "injected". For each of those types, look
                    // them up in the component mapping table. Note that the
                    // component mapping table is guaranteed to exist and contain
                    // components before we even get here.
					params.add(componentMappingTable_.getComponentForType(type));
				}
				instance = (T)injectable_.newInstance(params.toArray());
			}
            return instance;
		}

	}

    /**
     * The context's core component mapping table.
     */
    public final ComponentMappingTable componentMappingTable_;

    /**
     * The path mapping registered/associated with this invokable.
     */
    public final String mapping_;

    /**
     * The controller class this invokable is attached to.
     */
    public final InvokableClassWithInstance<?> controller_;

    /**
     * The path matcher, that is attached to this invokable controller method.
     */
    public final InvokableClassWithInstance<? extends CuracaoPathMatcher> matcher_;

    /**
     * The filter, that is attached to this invokable controller method.
     */
    public final InvokableClassWithInstance<? extends CuracaoRequestFilter> filter_;

    /**
     * The controller Java method itself.
     */
    public final Method method_;

    /**
     * The arguments/parameters for the controller Java method. Will be
     * an array of length zero if the underlying Java method takes no
     * parameters.
     */
    public final Class<?>[] parameterTypes_;

    /**
     * The annotation collection associated with each argument/parameter of
     * the controller Java method.  Will be an array of length zero if the
     * underlying Java method takes no parameters.
     */
    public final Annotation[][] parameterAnnotations_;

	public CuracaoMethodInvokable(
        @Nonnull final ComponentMappingTable componentMappingTable,
        @Nonnull final String mapping,
        @Nonnull final Class<?> controller,
        @Nullable final Constructor<?> injectableCntlrCtor,
        @Nonnull final Class<? extends CuracaoPathMatcher> matcher,
        @Nullable final Constructor<?> injectableMatcherCtor,
        @Nonnull final Class<? extends CuracaoRequestFilter> filter,
		@Nullable final Constructor<?> injectableFilterCtor,
        @Nonnull final Method method) {
        componentMappingTable_ = checkNotNull(componentMappingTable,
            "Component mapping table cannot be null.");
        mapping_ = checkNotNull(mapping, "Request mapping cannot be null.");
		checkNotNull(controller, "Controller base class cannot be null.");
        checkNotNull(matcher, "Path matcher class cannot be null.");
		checkNotNull(filter, "Method filter class cannot be null.");
        method_ = checkNotNull(method, "Controller method cannot be null.");
		// Instantiate a new instance of the controller class.
		try {
			controller_ = new InvokableClassWithInstance<>(controller,
				injectableCntlrCtor);
		} catch (NoSuchMethodException e) {
			throw new CuracaoException("Failed to instantiate controller " +
				"instance: " + controller.getCanonicalName() + " -- This " +
				"class is likely missing a nullary (no argument) " +
				"constructor. Please add one.", e);
		} catch (Exception e) {
			throw new CuracaoException("Failed to instantiate controller " +
				"instance.", e);
		}
        // Instantiate a new instance of the underlying path matcher class
        // attached to the controller method.
        try {
            matcher_ = new InvokableClassWithInstance<>(matcher,
                injectableMatcherCtor);
        } catch (NoSuchMethodException e) {
            throw new CuracaoException("Failed to instantiate method " +
                "path matcher class instance: " + matcher.getCanonicalName() +
                " -- This class is likely missing a nullary (no argument) " +
                "constructor. Please add one.", e);
        } catch (Exception e) {
            throw new CuracaoException("Failed to instantiate method " +
                "path matcher instance.", e);
        }
		// Instantiate a new instance of the filter class attached to
		// the controller method.
		try {
			filter_ = new InvokableClassWithInstance<>(filter,
				injectableFilterCtor);
		} catch (NoSuchMethodException e) {
			throw new CuracaoException("Failed to instantiate method " +
				"filter class instance: " + filter.getCanonicalName() +
				" -- This class is likely missing a nullary (no argument) " +
				"constructor. Please add one.", e);
		} catch (Exception e) {
			throw new CuracaoException("Failed to instantiate method " +
				"filter instance.", e);
		}
		parameterTypes_ = method_.getParameterTypes();
        parameterAnnotations_ = method_.getParameterAnnotations();
	}
	
	@Override
	public final String toString() {
		return String.format("%s.%s(%s)",
            controller_.clazz_.getCanonicalName(),
            method_.getName(),
            StringUtils.join(parameterTypes_, ", "));
	}

}
