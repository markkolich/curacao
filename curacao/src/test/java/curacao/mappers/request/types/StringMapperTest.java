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

package curacao.mappers.request.types;

import curacao.annotations.parameters.Extension;
import curacao.context.CuracaoContext;
import curacao.core.servlet.HttpRequest;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class StringMapperTest {

    private static final StringMapper MAPPER = new StringMapper();

    @Test
    public void readsTheExtensionOfTheLastPathSegmentTest() throws Exception {
        assertEquals("pdf", extension("/files/report.pdf"));
        assertEquals("gz", extension("/files/archive.tar.gz"));
        // A dot earlier in the path isn't an extension.
        assertNull(extension("/v1.2/report"));
        assertNull(extension("/files/report"));
    }

    @Test
    public void ignoresPathParametersTest() throws Exception {
        // The path within the application has ";" parameters removed; the raw request URI doesn't.
        assertEquals("pdf", extension("/files/report.pdf;jsessionid=abc", "/files/report.pdf"));
    }

    private static String extension(
            final String path) throws Exception {
        return extension(path, path);
    }

    private static String extension(
            final String requestUri,
            final String pathWithinApplication) throws Exception {
        final HttpRequest request = mock(HttpRequest.class);
        when(request.getRequestURI()).thenReturn(requestUri);
        final CuracaoContext context = mock(CuracaoContext.class);
        when(context.getRequest()).thenReturn(request);
        when(context.getProperty(anyString())).thenReturn(pathWithinApplication);

        return MAPPER.resolve(extensionAnnotation(), context);
    }

    private static Annotation extensionAnnotation() throws Exception {
        return Fixture.class.getMethod("handle", String.class).getParameterAnnotations()[0][0];
    }

    public static final class Fixture {
        public void handle(
                @Extension final String extension) {
            // Only its annotation is used.
        }
    }

}
