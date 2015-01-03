/**
 * Copyright (c) 2015 Mark S. Kolich
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

package com.kolich.curacao.examples;

import com.kolich.curacao.CuracaoDispatcherServlet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This servlet demonstrates how one can extend Curacao's default
 * {@link CuracaoDispatcherServlet} so that a method (in this case ready)
 * is invoked just before the servlet container starts sending traffic
 * to the servlet.
 */
public final class ExampleCustomDispatcherServlet
    extends CuracaoDispatcherServlet {

    private static final long serialVersionUID = -5101315231855241013L;

    private static final Logger logger__ =
        getLogger(ExampleCustomDispatcherServlet.class);

    private static final Config config__ = ConfigFactory.load();

    /**
     * This method is invoked immediately before the servlet will start
     * receiving traffic from the container.
     *
     * @param context the servlet context behind this web-application
     * @throws ServletException
     */
    public void ready(final ServletContext context) throws ServletException {
        // Fun times, a MOTD: Message of the day
        logger__.info(config__.getString("examples.motd"));
    }

}
