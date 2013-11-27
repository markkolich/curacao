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

import com.kolich.curacao.annotations.Component;
import com.kolich.curacao.annotations.Injectable;

/**
 * A component is any singleton that can be injected into a controller class,
 * a filter, a response type handler, or a controller argument mapper.  They
 * are injected by defining a single constructor in each class, respectively,
 * that is annotated with the {@link Injectable} annotation.
 *
 * Any class annotated with {@link Component} must implement this interface.
 * This interface defines a set of methods, namely initialize() and destroy()
 * that are called within the application life-cycle.  When the application is
 * shutting down, the component must be ready to stop itself, when the destroy()
 * method is called.  Likewise, then the application is starting, it must be
 * ready to start or initialize itself when the initialize() method is
 * called.
 */
public interface CuracaoComponent {
	
	/**
	 * Called during application startup when this {@link Component}
	 * should initialize itself.  This method is guaranteed to never be
	 * called more than once within the application life-cycle.
	 * @throws Exception when the {@link Component} failed to initialize
	 */
	public void initialize() throws Exception;
	
	/**
	 * Called during application shutdown when this {@link Component}
	 * should destroy/shutdown itself.  This method is guaranteed to never
	 * be called more than once within the application life-cycle.
	 * @throws Exception when the {@link Component} failed to stop
	 */
	public void destroy() throws Exception;

}
