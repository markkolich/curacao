/**
 * Copyright (c) 2016 Mark S. Kolich
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

package curacao.embedded.controllers;

import curacao.annotations.Controller;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.RequestUri;
import curacao.embedded.entities.SampleGsonEntity;
import curacao.mappers.request.matchers.CuracaoAntPathMatcher;

import java.io.File;

@Controller
public final class EmbeddedSampleController {

    @RequestMapping(value="/plaintext", matcher=CuracaoAntPathMatcher.class)
    public final String plainText() {
        return "Hello, World!";
    }

    @RequestMapping(value="/json", matcher=CuracaoAntPathMatcher.class)
    public final SampleGsonEntity json() {
        return new SampleGsonEntity("Hello, World!");
    }

    /**
     * Serves "static" content (files) from the classpath.  That is, stuff
     * packaged into src/main/resources/static are bundled into the JAR
     * and can be loaded as a resource from the classpath here.
     */
    @RequestMapping(value="/static/**", matcher=CuracaoAntPathMatcher.class)
    public final File staticFile(@RequestUri(includeContext=false) final String uri) {
        return new File(uri);
    }

}
