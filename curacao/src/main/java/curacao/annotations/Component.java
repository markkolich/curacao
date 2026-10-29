/*
 * Copyright (c) 2026 Mark S. Kolich
 * https://mark.koli.ch
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

package curacao.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class in the boot package as a component: a singleton, created once at startup and injected by type
 * into {@link Injectable} constructors of components, controllers, filters and mappers.
 *
 * <p>A constructor parameter is resolved like this:</p>
 *
 * <ul>
 *     <li>{@code List<T>} gets every component assignable to {@code T}, sorted by class name; it may be
 *     empty.</li>
 *     <li>A component class asked for by its own type gets that component.</li>
 *     <li>Otherwise the parameter gets the one component assignable to its type (a class, superclass or
 *     interface). With several, the one marked {@link #primary()} wins; with several and no single primary,
 *     startup fails, listing the candidates. With none, startup fails too: nothing is ever injected as
 *     null.</li>
 * </ul>
 *
 * <p>To swap one component for another, give the new one {@link #replaces()}: the replaced component is never
 * created, and asking for it by its own type fails at startup naming its replacement.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {

    /**
     * Components this one replaces. They're never created, and nothing can be injected with them. Each must be
     * a component in the boot package; a component can't replace itself, and replacements can't form a cycle.
     */
    Class<?>[] replaces() default {};

    /**
     * Components to remove by canonical class name, for classes that may not be on the classpath (a class
     * literal in {@link #replaces()} wouldn't compile). {@code *} matches any run of characters, e.g.
     * {@code "com.example.legacy.*"}. A pattern that matches no component is logged as a warning.
     */
    String[] suppress() default {};

    /**
     * Whether this component is the one to inject when several components match a constructor parameter's
     * type. At most one matching component may be primary.
     */
    boolean primary() default false;

}
