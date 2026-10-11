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

package curacao.freemarker.transformers.html;

import com.google.common.collect.ImmutableSet;
import curacao.freemarker.transformers.TextTransformer;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Minifies HTML by stripping whitespace and line breaks between tags. Note that this removes
 * every newline, including those inside {@code <pre>} blocks. It keeps no state, so one instance
 * can be shared across requests.
 */
public final class HtmlMinifier implements TextTransformer {

    /**
     * A pre-compiled set of {@link Pattern}'s used to "compress" strings of HTML
     * by removing whitespace, new lines, and other useless whitespace formatting
     * characters.  Pre-compiling the patterns ideally performs better than running
     * through a bunch of chained {@link String#replaceAll(String, String)} calls.
     */
    private static final Set<Pattern> HTML_COMPRESSOR_PATTERNS = ImmutableSet.of(
            Pattern.compile("^[\\x20\\t]+"),
            Pattern.compile("[\\x20\\t]+$"),
            Pattern.compile("\\s*(\\r\\n|\\n)+\\s*"),
            Pattern.compile("^[\\r\\n]+"));

    /**
     * Strips whitespace and other non-printable HTML characters between tags.
     *
     * @param html the string to minify
     * @return the minified response
     */
    @Nonnull
    @Override
    public String apply(
            @Nonnull final String html) {
        checkNotNull(html, "HTML cannot be null.");

        String stripped = html;
        for (final Pattern p : HTML_COMPRESSOR_PATTERNS) {
            stripped = p.matcher(stripped).replaceAll("");
        }

        return stripped;
    }

}
