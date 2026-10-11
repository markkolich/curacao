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

import com.googlecode.htmlcompressor.compressor.YuiJavaScriptCompressor;
import curacao.freemarker.transformers.TextTransformer;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Compresses HTML with htmlcompressor, stripping comments and other junk and compressing inline
 * scripts with yuicompressor; typically registered after {@link HtmlMinifier}. It keeps no state
 * between calls (each call uses a new htmlcompressor), so one instance can be shared across
 * requests.
 *
 * This is the only class in this module that uses htmlcompressor and yuicompressor, which are
 * optional dependencies: applications that use it declare both.
 */
public final class HtmlCompressor implements TextTransformer {

    /**
     * Compresses the given HTML.
     *
     * @param html the HTML to compress
     * @return the compressed HTML
     */
    @Nonnull
    @Override
    public String apply(
            @Nonnull final String html) {
        checkNotNull(html, "HTML cannot be null.");

        // A new compressor on every call, so this is safe to share across requests.
        // Fully qualified: this class shares htmlcompressor's class name.
        final com.googlecode.htmlcompressor.compressor.HtmlCompressor compressor =
                new com.googlecode.htmlcompressor.compressor.HtmlCompressor();
        compressor.setJavaScriptCompressor(new YuiJavaScriptCompressor());
        compressor.setCompressJavaScript(true);

        return compressor.compress(html);
    }

}
