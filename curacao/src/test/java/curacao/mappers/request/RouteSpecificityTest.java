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

package curacao.mappers.request;

import curacao.mappers.request.matchers.CuracaoAntPathMatcher;
import curacao.mappers.request.matchers.CuracaoPathMatcher;
import curacao.mappers.request.matchers.CuracaoRegexPathMatcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public final class RouteSpecificityTest {

    @Test
    public void literalRegexBeatsOneWithAVariableTest() {
        assertMoreSpecific(regex("^/files/export$"), regex("^/files/(?<path>[a-zA-Z0-9/._-]+)$"));
    }

    @Test
    public void fewerVariablesBeatMoreTest() {
        assertMoreSpecific(regex("^/users/(?<user>[a-z]+)$"),
                regex("^/(?<section>[a-z]+)/(?<user>[a-z]+)$"));
    }

    @Test
    public void moreLiteralCharactersBreakATieTest() {
        assertMoreSpecific(regex("^/api/v1/users/(?<user>[a-z]+)$"), regex("^/api/(?<user>[a-z]+)$"));
    }

    @Test
    public void wildcardsOutsideGroupsCountTest() {
        // ".*" and "\d" are wildcards; an escaped "\." is a literal dot.
        assertMoreSpecific(regex("^/report\\.pdf$"), regex("^/report.*$"));
        assertMoreSpecific(regex("^/v1/items$"), regex("^/v\\d/items$"));
    }

    @Test
    public void nonCapturingGroupsArentVariablesTest() {
        assertEquals(0, regex("^/(?:a|b)$").compareTo(regex("^/(?:c|d)$")));
        assertMoreSpecific(regex("^/(?:a|b)$"), regex("^/(?<x>a|b)$"));
    }

    @Test
    public void equallySpecificRoutesTieTest() {
        assertEquals(0, regex("^/ambiguous/(?<a>[a-z]+)/x$").compareTo(regex("^/ambiguous/y/(?<b>[a-z]+)$")));
    }

    @Test
    public void readsAntPatternsTest() {
        assertMoreSpecific(ant("/files/export"), ant("/files/{path}"));
        assertMoreSpecific(ant("/files/{path}"), ant("/files/**"));
        assertMoreSpecific(ant("/files/*.pdf"), ant("/files/**"));
    }

    @Test
    public void unknownMatchersRankLastTest() {
        final RouteSpecificity custom = RouteSpecificity.of("anything", mock(CuracaoPathMatcher.class).getClass());
        assertMoreSpecific(regex("^/(?<a>.*)/(?<b>.*)/(?<c>.*)$"), custom);
    }

    private static void assertMoreSpecific(
            final RouteSpecificity more,
            final RouteSpecificity less) {
        assertTrue(more.compareTo(less) < 0, more + " should be more specific than " + less);
        assertTrue(less.compareTo(more) > 0, less + " should be less specific than " + more);
    }

    private static RouteSpecificity regex(
            final String pattern) {
        return RouteSpecificity.of(pattern, CuracaoRegexPathMatcher.class);
    }

    private static RouteSpecificity ant(
            final String pattern) {
        return RouteSpecificity.of(pattern, CuracaoAntPathMatcher.class);
    }

}
