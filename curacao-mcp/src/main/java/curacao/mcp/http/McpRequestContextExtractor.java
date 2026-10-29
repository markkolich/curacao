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

package curacao.mcp.http;

import curacao.core.servlet.HttpRequest;

import java.util.Map;

/**
 * Collects application data from an HTTP request (an authenticated user, a tenant, the remote
 * address from {@link HttpRequest#getDelegate()}) into the
 * {@link curacao.mcp.McpRequestContext} that tools and prompts receive.
 *
 * <p>An extractor only collects. To turn a request away (one without credentials, say), use a
 * Curacao request filter or check in the controller before calling the transport.</p>
 */
@FunctionalInterface
public interface McpRequestContextExtractor {

    /**
     * The attributes to attach to the request's context.
     *
     * @throws Exception on a failure, which the transport answers with an HTTP 500
     */
    Map<String, Object> extract(
            final HttpRequest request) throws Exception;

}
