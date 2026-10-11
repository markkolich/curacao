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

package curacao.freemarker.components;

import com.google.common.collect.ImmutableMap;
import curacao.freemarker.entities.FreeMarkerContent;
import curacao.freemarker.exceptions.CuracaoFreeMarkerException;
import freemarker.template.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class AbstractFreeMarkerContentRendererTest {

    @TempDir
    private Path templateDir_;

    private static final class TestRenderer extends AbstractFreeMarkerContentRenderer {

        private TestRenderer(
                final Path templateDir) throws IOException {
            super(newConfiguration(templateDir));
        }

        private static Configuration newConfiguration(
                final Path templateDir) throws IOException {
            final Configuration config = new Configuration(Configuration.VERSION_2_3_31);
            config.setDefaultEncoding(StandardCharsets.UTF_8.name());
            config.setDirectoryForTemplateLoading(templateDir.toFile());
            return config;
        }

        @Nonnull
        @Override
        public Map<String, Object> getGlobalDataMap() {
            return ImmutableMap.of("site", "global", "title", "global title", "who", "global who");
        }

    }

    @Test
    public void mergesGlobalContentAndTemplateDataTest() throws Exception {
        Files.createDirectories(templateDir_.resolve("pages"));
        Files.writeString(templateDir_.resolve("pages/hello.ftl"),
                "<#ftl attributes={\"title\": \"template title\"}>${site}|${title}|${who}|café",
                StandardCharsets.UTF_8);

        final TestRenderer renderer = new TestRenderer(templateDir_);
        final FreeMarkerContent content = new FreeMarkerContent.Builder("pages/hello.ftl")
                .withAttr("who", "content who")
                .withAttr("title", "content title")
                .withAttr("ignored", null)
                .build();

        // Content data overrides global data; template attributes override both.
        assertEquals("global|template title|content who|café", renderer.contentToString(content));
    }

    @Test
    public void missingTemplateTest() throws Exception {
        final TestRenderer renderer = new TestRenderer(templateDir_);
        final FreeMarkerContent content = new FreeMarkerContent.Builder("nope.ftl").build();

        assertThrows(CuracaoFreeMarkerException.class, () -> renderer.contentToString(content));
    }

}
