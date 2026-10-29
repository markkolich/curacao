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

package curacao.examples.components.mcp;

import com.google.common.collect.ImmutableList;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.mcp.McpServer;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The examples' MCP server: one tool and one prompt, served by
 * {@link curacao.examples.controllers.McpExampleController}.
 */
@Component
public final class ExampleMcpServer {

    private static final String NAME = "curacao-examples";
    private static final String VERSION = "1.0.0";

    private final McpServer server_;

    @Injectable
    public ExampleMcpServer(
            @Nonnull final GetTimeTool getTimeTool,
            @Nonnull final MorningBriefPrompt morningBriefPrompt) {
        checkNotNull(getTimeTool, "Get time tool cannot be null.");
        checkNotNull(morningBriefPrompt, "Morning brief prompt cannot be null.");

        server_ = new McpServer.Builder(NAME, VERSION)
                .setTitle("Curacao Examples")
                .setInstructions("Use get_time for anything that depends on the current date or time.")
                .setTools(ImmutableList.of(getTimeTool))
                .setPrompts(ImmutableList.of(morningBriefPrompt))
                .build();
    }

    public McpServer getServer() {
        return server_;
    }

}
