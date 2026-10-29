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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.examples.components.JacksonComponent;
import curacao.mcp.McpRequestContext;
import curacao.mcp.exceptions.CuracaoMcpException;
import curacao.mcp.jsonrpc.JsonRpcError;
import curacao.mcp.tools.McpTool;
import curacao.mcp.tools.McpToolAnnotations;
import curacao.mcp.tools.McpToolResult;

import javax.annotation.Nonnull;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An example MCP tool: the current date and time in a time zone.
 */
@Component
public final class GetTimeTool implements McpTool {

    private static final String NAME = "get_time";

    private static final String TIME_ZONE = "time_zone";
    private static final String DATE_TIME = "date_time";

    private static final String TYPE = "type";
    private static final String OBJECT = "object";
    private static final String STRING = "string";
    private static final String PROPERTIES = "properties";
    private static final String REQUIRED = "required";
    private static final String DESCRIPTION = "description";

    private final ObjectMapper objectMapper_;

    @Injectable
    public GetTimeTool(
            @Nonnull final JacksonComponent jacksonComponent) {
        checkNotNull(jacksonComponent, "Jackson component cannot be null.");

        objectMapper_ = jacksonComponent.getMapperInstance();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "The current date and time in a time zone. Use it whenever the user asks what time it is.";
    }

    @Override
    public Map<String, Object> getInputProperties() {
        return ImmutableMap.of(
                TIME_ZONE, ImmutableMap.of(
                        TYPE, STRING,
                        DESCRIPTION, "An IANA time zone, e.g. America/Los_Angeles."));
    }

    @Override
    public List<String> getRequiredProperties() {
        return ImmutableList.of(TIME_ZONE);
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return ImmutableMap.of(
                TYPE, OBJECT,
                PROPERTIES, ImmutableMap.of(
                        TIME_ZONE, ImmutableMap.of(TYPE, STRING),
                        DATE_TIME, ImmutableMap.of(
                                TYPE, STRING,
                                DESCRIPTION, "ISO-8601, with the offset and zone.")),
                REQUIRED, ImmutableList.of(TIME_ZONE, DATE_TIME));
    }

    @Override
    public McpToolAnnotations getAnnotations() {
        return new McpToolAnnotations.Builder()
                .setReadOnlyHint(true)
                .setOpenWorldHint(false)
                .build();
    }

    @Override
    public McpToolResult call(
            final Map<String, Object> arguments,
            final McpRequestContext context) throws Exception {
        final Object timeZone = arguments.get(TIME_ZONE);
        if (!(timeZone instanceof String)) {
            // A protocol error: the call itself is malformed.
            throw new CuracaoMcpException(JsonRpcError.INVALID_PARAMS, "time_zone is required.");
        }

        final ZoneId zone;
        try {
            zone = ZoneId.of((String) timeZone);
        } catch (final DateTimeException e) {
            // A tool error: the model reads this and can try again with a valid zone.
            return new McpToolResult.Builder()
                    .setText("Unknown time zone: " + timeZone)
                    .setError(true)
                    .build();
        }

        // Structured, and the same JSON as text for clients that don't read structured content.
        final Map<String, String> now = ImmutableMap.of(
                TIME_ZONE, zone.getId(),
                DATE_TIME, ZonedDateTime.now(zone).toString());
        return new McpToolResult.Builder()
                .setStructuredContent(now, objectMapper_)
                .build();
    }

}
