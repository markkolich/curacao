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

package com.kolich.curacao.examples.controllers;

import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.Injectable;
import com.kolich.curacao.annotations.RequestMapping;
import com.kolich.curacao.annotations.parameters.Path;
import com.kolich.curacao.exceptions.routing.ResourceNotFoundException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

@Controller
public final class JspExampleController {

    private final ServletContext context_;

    private final File resourceDir_;

    @Injectable
    public JspExampleController(final ServletContext context) {
        context_ = checkNotNull(context, "Servlet context cannot be null.");
        resourceDir_ = new File(context_.getRealPath("/WEB-INF/static"));
    }

    @RequestMapping("^\\/api\\/jsp$")
	public final void dispatchToJsp(final AsyncContext context) {
		context.dispatch("/WEB-INF/jsp/demo.jsp");
	}

    @RequestMapping("^\\/api\\/lanyon$")
    public final void lanyon(final AsyncContext context) {
        context.dispatch("/WEB-INF/jsp/lanyon.jsp");
    }

    @RequestMapping("^\\/api\\/static\\/(?<resource>[a-zA-Z_0-9\\-\\.\\/]+)$")
    public final File staticFile(@Path("resource") final String resource) throws IOException {
        final File resourceFile = new File(resourceDir_, resource);
        if (!resourceFile.exists()) {
            throw new ResourceNotFoundException("Static resource not found: " +
                resourceFile.getCanonicalPath());
        }
        return resourceFile;
    }

}
