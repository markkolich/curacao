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

package curacao.examples.controllers;

import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.Route;
import curacao.annotations.parameters.RequestBody;
import curacao.core.servlet.HttpRequest;
import curacao.entities.CuracaoEntity;
import curacao.examples.components.mcp.ExampleMcpServer;
import curacao.mcp.http.McpHttpTransport;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.annotations.Route.Method.DELETE;
import static curacao.annotations.Route.Method.GET;
import static curacao.annotations.Route.Method.POST;

/**
 * Serves {@link ExampleMcpServer} over MCP's Streamable HTTP transport at {@code /curacao/api/mcp}.
 *
 * <p>Connect Claude Code with
 * {@code claude mcp add --transport http curacao-examples http://localhost:8080/curacao/api/mcp}.</p>
 */
@Controller
public final class McpExampleController {

    private final McpHttpTransport transport_;

    @Injectable
    public McpExampleController(
            @Nonnull final ExampleMcpServer exampleMcpServer) {
        checkNotNull(exampleMcpServer, "Example MCP server cannot be null.");

        transport_ = new McpHttpTransport.Builder(exampleMcpServer.getServer()).build();
    }

    @Route(value = "^/api/mcp$", methods = POST)
    public CuracaoEntity post(
            final HttpRequest request,
            @RequestBody final String body) {
        return transport_.post(request, body);
    }

    @Route(value = "^/api/mcp$", methods = {GET, DELETE})
    public CuracaoEntity unsupported() {
        return transport_.methodNotAllowed();
    }

}
