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

package com.kolich.curacao.embedded.mappers;

import com.kolich.curacao.annotations.mappers.ControllerReturnTypeMapper;
import com.kolich.curacao.handlers.responses.mappers.RenderingResponseTypeMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import static org.slf4j.LoggerFactory.getLogger;

@ControllerReturnTypeMapper(File.class)
public final class ClasspathFileResponseMapper
    extends RenderingResponseTypeMapper<File> {

    private static final Logger logger__ =
        getLogger(ClasspathFileResponseMapper.class);

    // This is a really lame/poor attempt at serving static resources from
    // the classpath.  There are a number of known gaps here that would need
    // to be addressed in a real production worthy implementation:
    //  == The discovery of the right "Content-Type" header based on extension
    //  == The setting of the "Content-Length" header based on resource size

    @Override
    public final void render(final AsyncContext context,
                             final HttpServletResponse response,
                             final @Nonnull File entity) throws Exception {
        // Get the path, and remove the leading slash.
        final String path = entity.getAbsolutePath().substring(1);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final URL resource = loader.getResource(path);
        if(resource == null) {
            logger__.warn("Resource not found: " + path);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            logger__.debug("Serving resource from classpath: " + path);
            try(final InputStream in = loader.getResourceAsStream(path);
                final OutputStream out = response.getOutputStream();) {
                IOUtils.copyLarge(in, out);
            }
        }
    }

}
