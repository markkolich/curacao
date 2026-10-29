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

package curacao.mcp.tools;

import com.google.common.collect.ImmutableList;
import curacao.mcp.McpRequestContext;
import curacao.mcp.exceptions.CuracaoMcpException;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * A tool an MCP client can list and call.
 */
public interface McpTool {

    /**
     * The tool's unique name, e.g. {@code get_profile}.
     */
    String getName();

    /**
     * A human-readable name for clients to show. Defaults to the name with underscores as spaces,
     * capitalized: {@code get_profile} is "Get profile".
     */
    default String getTitle() {
        return StringUtils.capitalize(getName().replace('_', ' '));
    }

    /**
     * What the tool does and when to use it; the model reads this to decide whether to call it.
     */
    String getDescription();

    /**
     * The input schema's properties, as JSON Schema: property name to schema. The server wraps
     * them in an object schema.
     */
    Map<String, Object> getInputProperties();

    /**
     * The properties a call must include.
     */
    default List<String> getRequiredProperties() {
        return ImmutableList.of();
    }

    /**
     * Hints about the tool's behavior, or null for none. Clients may use them to decide whether
     * to ask before a call; they're hints, never a security boundary.
     */
    @Nullable
    default McpToolAnnotations getAnnotations() {
        return null;
    }

    /**
     * The JSON Schema of the tool's structured results, a schema of {@code "type": "object"}, or
     * null when it returns only text. A tool with an output schema returns
     * {@link McpToolResult#structured} results that match it.
     */
    @Nullable
    default Map<String, Object> getOutputSchema() {
        return null;
    }

    /**
     * Runs the tool.
     *
     * @param arguments the call's arguments, as JSON values: strings, numbers, booleans, lists
     *     and maps; empty when the call had none
     * @param context the request that made the call: headers, protocol version, and the
     *     application's attributes; {@link McpRequestContext#EMPTY} when called directly
     * @return the result to send back; {@link McpToolResult#error(String)} for a failure the
     *     model should see
     * @throws Exception on a failure; a {@link CuracaoMcpException} becomes a
     *     JSON-RPC error, and anything else an error result
     */
    McpToolResult call(
            final Map<String, Object> arguments,
            final McpRequestContext context) throws Exception;

}
