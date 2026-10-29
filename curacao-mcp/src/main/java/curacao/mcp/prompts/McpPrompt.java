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

package curacao.mcp.prompts;

import com.google.common.collect.ImmutableList;
import curacao.mcp.McpRequestContext;
import curacao.mcp.exceptions.CuracaoMcpException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * A prompt template an MCP client can list and fetch, usually offered to the user as a starter
 * (a slash command, say).
 */
public interface McpPrompt {

    /**
     * The prompt's unique name, e.g. {@code checkup}.
     */
    String getName();

    /**
     * A human-readable name for clients to show, or null.
     */
    @Nullable
    default String getTitle() {
        return null;
    }

    /**
     * What the prompt is for, or null.
     */
    @Nullable
    default String getDescription() {
        return null;
    }

    /**
     * The arguments the prompt accepts, advertised in {@code prompts/list}. The server rejects a
     * {@code prompts/get} that leaves out a required one.
     */
    default List<McpPromptArgument> getArguments() {
        return ImmutableList.of();
    }

    /**
     * The prompt's messages, rendered when a client fetches it.
     *
     * @param arguments the client's arguments; empty when it sent none
     * @param context the request that fetched the prompt; {@link McpRequestContext#EMPTY} when
     *     called directly
     * @throws Exception on a failure; a {@link CuracaoMcpException} becomes a
     *     JSON-RPC error with its code
     */
    List<McpPromptMessage> getMessages(
            final Map<String, String> arguments,
            final McpRequestContext context) throws Exception;

}
