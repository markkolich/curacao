/*
 * Copyright (c) 2023 Mark S. Kolich
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

package curacao.examples.components;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.components.ComponentDestroyable;
import curacao.core.servlet.ServletContext;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public final class AsyncHttpClientComponent implements ComponentDestroyable {

    private static final Logger LOG = getLogger(AsyncHttpClientComponent.class);

    private final AsyncHttpClient asyncHttpClient_;

    @Injectable
    public AsyncHttpClientComponent(
            final ServletContext context) {
        checkNotNull(context, "Servlet context cannot be null.");
        asyncHttpClient_ = new DefaultAsyncHttpClient();
    }

    public AsyncHttpClient getClient() {
        return asyncHttpClient_;
    }

    @Override
    public void destroy() throws Exception {
        LOG.info("Inside of AsyncHttpClientComponent destroy.");
        asyncHttpClient_.close();
    }

}
