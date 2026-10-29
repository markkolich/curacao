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

package curacao.mcp.exceptions;

import curacao.exceptions.CuracaoException;

/**
 * A protocol error, answered with a JSON-RPC error carrying its code and message. Thrown by the
 * server for an unknown method, tool or prompt, and by tools and prompts for arguments they
 * can't use ({@link curacao.mcp.jsonrpc.JsonRpcError#INVALID_PARAMS}). A tool that runs and
 * fails should return {@link curacao.mcp.tools.McpToolResult#error(String)} instead, so the
 * model sees what went wrong.
 */
public final class CuracaoMcpException extends CuracaoException {

    private final int code_;

    public CuracaoMcpException(
            final int code,
            final String message) {
        super(message);
        code_ = code;
    }

    public int getCode() {
        return code_;
    }

}
