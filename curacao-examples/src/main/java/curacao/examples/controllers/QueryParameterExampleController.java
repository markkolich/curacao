/*
 * Copyright (c) 2021 Mark S. Kolich
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
import curacao.annotations.RequestMapping;
import curacao.annotations.RequestMapping.Method;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.Query;

@Controller
public final class QueryParameterExampleController {

    @RequestMapping(value = "^/api/queryparameters$", methods = Method.GET)
    public String queryParameters(
            @Query("string") final String string,
            @Query("int") final Integer integerNumber,
            @Query("long") final Long longNumber,
            @Query("char") final Character character,
            @Query(value = "boolean", required = true) final Boolean bool) {
        final StringBuilder builder = new StringBuilder();
        builder.append("Query:").append("\n");
        builder.append("string -> ").append(string).append("\n");
        builder.append("integer -> ").append(integerNumber).append("\n");
        builder.append("long -> ").append(longNumber).append("\n");
        builder.append("character -> ").append(character).append("\n");
        builder.append("boolean (required) -> ").append(bool).append("\n");
        return builder.toString();
    }

    @RequestMapping(value = "^/api/pathparameters/(?<string>[A-Za-z0-9]+)/(?<int>[0-9]+)/(?<long>[0-9]+)"
            + "/(?<char>[A-Za-z0-9]{1})/(?<boolean>[A-Za-z0-9]+)$", methods = Method.GET)
    public String pathParameters(
            @Path("string") final String string,
            @Path("int") final Integer integerNumber,
            @Path("long") final Long longNumber,
            @Path("char") final Character character,
            @Path("boolean") final Boolean bool) {
        final StringBuilder builder = new StringBuilder();
        builder.append("Path:").append("\n");
        builder.append("string -> ").append(string).append("\n");
        builder.append("integer -> ").append(integerNumber).append("\n");
        builder.append("long -> ").append(longNumber).append("\n");
        builder.append("character -> ").append(character).append("\n");
        builder.append("boolean (required) -> ").append(bool).append("\n");
        return builder.toString();
    }

}
