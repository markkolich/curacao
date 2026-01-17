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

package curacao.util.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static curacao.CuracaoConfig.getBaseConfigPath;
import static curacao.CuracaoConfig.getConfig;

public final class ContentTypes {

    private static final Logger LOG = LoggerFactory.getLogger(ContentTypes.class);

    private static final String CONTENT_TYPES = "http.content-types";

    public static String getContentTypeForExtension(
            final String ext,
            final String defaultValue) {
        String contentType = defaultValue;
        try {
            contentType = getConfig().getConfig(getBaseConfigPath(CONTENT_TYPES)).getString(ext);
        } catch (final Exception e) {
            LOG.debug("Exception while loading content-type for extension: {}", ext, e);
        }
        return contentType;
    }

}
