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

package curacao.servlet.jakarta;

import curacao.servlet.AbstractCuracaoContextListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CuracaoJakartaContextListener extends AbstractCuracaoContextListener
        implements ServletContextListener {

    private curacao.core.servlet.ServletContext servletContext_;

    @Override
    public void contextInitialized(
            final ServletContextEvent e) {
        // Ensure that the Servlet container is being honest, and returns
        // a valid non-null context attached to the event.
        final ServletContext servletContext = checkNotNull(e.getServletContext(),
                "Servlet context cannot be null, but it was null -- your servlet "
                + "container appears to have broken a well established contract.");

        servletContext_ = new JakartaServletContext(servletContext);
        initializeCuracaoContext(servletContext_);
    }

    @Override
    public void contextDestroyed(
            final ServletContextEvent e) {
        destroyCuracaoContext(servletContext_);
    }

}
