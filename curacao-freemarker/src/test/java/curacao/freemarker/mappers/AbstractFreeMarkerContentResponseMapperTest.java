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

package curacao.freemarker.mappers;

import com.google.common.net.MediaType;
import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpCookie;
import curacao.core.servlet.HttpResponse;
import curacao.freemarker.components.AbstractFreeMarkerContentRenderer;
import curacao.freemarker.entities.FreeMarkerContent;
import curacao.freemarker.transformers.html.HtmlMinifier;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Writer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class AbstractFreeMarkerContentResponseMapperTest {

    private static final String PAGE = "<p>\n  hi\n</p>";

    private static final class TestRenderer extends AbstractFreeMarkerContentRenderer {

        private TestRenderer() {
            super(newConfiguration());
        }

        private static Configuration newConfiguration() {
            final StringTemplateLoader loader = new StringTemplateLoader();
            loader.putTemplate("page.ftl", PAGE);
            loader.putTemplate("broken.ftl", "${missing.value}");

            final Configuration config = new Configuration(Configuration.VERSION_2_3_31);
            config.setTemplateLoader(loader);
            return config;
        }

    }

    private static final class TestMapper extends AbstractFreeMarkerContentResponseMapper<FreeMarkerContent> {

        private TestMapper() {
            super(new TestRenderer());
        }

        @Override
        public void render(
                final AsyncContext context,
                final HttpResponse response,
                @Nonnull final FreeMarkerContent content) throws Exception {
            renderFreeMarkerContent(response, content);
        }

    }

    private static final class TestResponse implements HttpResponse {

        private final ByteArrayOutputStream body_ = new ByteArrayOutputStream();
        private int status_;
        private String contentType_;

        @Override
        public Object getDelegate() {
            return this;
        }

        @Override
        public void setStatus(
                final int sc) {
            status_ = sc;
        }

        @Override
        public void setContentType(
                final String type) {
            contentType_ = type;
        }

        @Override
        public void setHeader(
                final String name,
                final String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addHeader(
                final String name,
                final String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHeader(
                final String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addCookie(
                final HttpCookie cookie) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sendRedirect(
                final String location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutputStream getOutputStream() {
            return body_;
        }

        @Override
        public Writer getWriter() {
            throw new UnsupportedOperationException();
        }

    }

    @Test
    public void noTransformersTest() throws Exception {
        final TestResponse response =
                render(new FreeMarkerContent.Builder("page.ftl", MediaType.XML_UTF_8, 404).build());

        assertEquals(PAGE, response.body_.toString(UTF_8));
        assertEquals(404, response.status_);
        assertEquals("text/xml; charset=utf-8", response.contentType_);
    }

    @Test
    public void contentTransformersTest() throws Exception {
        final TestResponse response = render(new FreeMarkerContent.Builder("page.ftl")
                .withTransformers(new HtmlMinifier())
                .withTransformers(body -> body + "!")
                .build());

        // Transformers from every withTransformers() call run, in the order they were added.
        assertEquals("<p>hi</p>!", response.body_.toString(UTF_8));
        assertEquals("text/html; charset=utf-8", response.contentType_);
    }

    @Test
    public void templateFailureTest() throws Exception {
        final TestResponse missing = render(new FreeMarkerContent.Builder("nope.ftl").build());
        final TestResponse broken = render(new FreeMarkerContent.Builder("broken.ftl", 404).build());

        // A template that can't be found or fails to process becomes a plain-text 500, not an empty 200.
        for (final TestResponse response : new TestResponse[]{missing, broken}) {
            assertEquals(500, response.status_);
            assertEquals("text/plain; charset=utf-8", response.contentType_);
            assertEquals("500 Internal Server Error", response.body_.toString(UTF_8));
        }
    }

    private static TestResponse render(
            final FreeMarkerContent content) throws Exception {
        final TestResponse response = new TestResponse();
        new TestMapper().render(null, response, content);
        return response;
    }

}
