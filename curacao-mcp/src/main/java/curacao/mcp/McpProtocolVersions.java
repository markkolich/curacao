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

package curacao.mcp;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * The MCP protocol versions this library speaks.
 */
public interface McpProtocolVersions {

    String V2024_11_05 = "2024-11-05";
    String V2025_03_26 = "2025-03-26";
    String V2025_06_18 = "2025-06-18";
    String V2025_11_25 = "2025-11-25";

    String LATEST = V2025_11_25;

    /**
     * Every version, newest first: what a server speaks by default.
     */
    List<String> ALL = ImmutableList.of(V2025_11_25, V2025_06_18, V2025_03_26, V2024_11_05);

    /**
     * The version a server assumes for an HTTP request without an {@code MCP-Protocol-Version}
     * header, as the specification requires for backwards compatibility.
     */
    String ASSUMED_WITHOUT_HEADER = V2025_03_26;

}
