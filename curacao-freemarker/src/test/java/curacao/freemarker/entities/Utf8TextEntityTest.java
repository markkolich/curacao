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

package curacao.freemarker.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class Utf8TextEntityTest {

    @Test
    public void noTransformersTest() throws Exception {
        final String body = "<html>\n  <!-- kept -->\n</html>";
        final Utf8TextEntity entity = new Utf8TextEntity.Builder(MediaType.HTML_UTF_8)
                .setBody(body)
                .build();

        assertEquals(body, render(entity));
        assertEquals("text/html; charset=utf-8", entity.getContentType());
        assertEquals(200, entity.getStatus());
    }

    @Test
    public void transformersRunInOrderTest() throws Exception {
        final Utf8TextEntity entity = new Utf8TextEntity.Builder(MediaType.XML_UTF_8)
                .setStatus(404)
                .setBody("<a>x</a>")
                .addTransformer(body -> body + "1")
                .addTransformers(ImmutableList.of(body -> body + "2", body -> body.toUpperCase(Locale.ROOT)))
                .build();

        assertEquals("<A>X</A>12", render(entity));
        assertEquals(404, entity.getStatus());
    }

    @Test
    public void invalidEntityTest() {
        assertThrows(IllegalArgumentException.class,
                () -> new Utf8TextEntity.Builder(MediaType.HTML_UTF_8).setStatus(199));
        assertThrows(NullPointerException.class,
                () -> new Utf8TextEntity.Builder(MediaType.HTML_UTF_8).build());
    }

    @Test
    public void failingTransformersAreSkippedTest() throws Exception {
        final Utf8TextEntity entity = new Utf8TextEntity.Builder(MediaType.HTML_UTF_8)
                .setBody("a")
                .addTransformer(body -> body + "b")
                .addTransformer(body -> null)
                .addTransformer(body -> {
                    throw new IllegalStateException("boom");
                })
                .addTransformer(body -> body + "c")
                .build();

        // The null and throwing transformers are skipped; the ones around them still run.
        assertEquals("abc", render(entity));
    }

    private static String render(
            final Utf8TextEntity entity) throws Exception {
        final StringWriter writer = new StringWriter();
        entity.toWriter(writer);
        return writer.toString();
    }

}
