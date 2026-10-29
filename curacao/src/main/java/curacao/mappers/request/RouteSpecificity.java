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

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * How specific a route's pattern is, for choosing among several routes that match one request, as Spring does:
 * the pattern with fewer variables and wildcards is more specific, and of two with as many, the one with more
 * literal characters. So {@code ^/files/export$} beats {@code ^/files/(?<path>.+)$} for {@code /files/export}.
 * Patterns of a matcher other than Curacao's regex and Ant matchers can't be read, and rank after both.
 *
 * <p>Instances order from the most specific to the least: {@code a.compareTo(b) < 0} when {@code a} is more
 * specific, and 0 when neither is.
 */
public final class RouteSpecificity implements Comparable<RouteSpecificity> {

    private static final int UNKNOWN = Integer.MAX_VALUE;

    /**
     * Regex escapes that stand for a set of characters, not one literal character: {@code \d}, {@code \w} and
     * the like.
     */
    private static final String REGEX_CLASS_ESCAPES = "dDwWsShHvVpPRX";

    private final int variables_;
    private final int wildcards_;
    private final int literals_;

    private RouteSpecificity(
            final int variables,
            final int wildcards,
            final int literals) {
        variables_ = variables;
        wildcards_ = wildcards;
        literals_ = literals;
    }

    public static RouteSpecificity of(
            @Nonnull final String pattern,
            @Nonnull final Class<? extends CuracaoPathMatcher> matcher) {
        checkNotNull(pattern, "Route pattern cannot be null.");
        checkNotNull(matcher, "Route matcher cannot be null.");

        if (CuracaoRegexPathMatcher.class.equals(matcher)) {
            return ofRegex(pattern);
        } else if (CuracaoAntPathMatcher.class.equals(matcher)) {
            return ofAnt(pattern);
        }

        return new RouteSpecificity(UNKNOWN, UNKNOWN, 0);
    }

    /**
     * Reads a regex route: each capturing group is a variable; each character class, {@code .}, class escape,
     * quantifier and top-level alternation is a wildcard; and every other character outside a group is literal.
     * Anchors don't count.
     */
    private static RouteSpecificity ofRegex(
            final String pattern) {
        int variables = 0;
        int wildcards = 0;
        int literals = 0;
        int depth = 0;
        int index = 0;
        while (index < pattern.length()) {
            final char c = pattern.charAt(index);
            if (c == '\\' && index + 1 < pattern.length()) {
                if (depth == 0) {
                    if (REGEX_CLASS_ESCAPES.indexOf(pattern.charAt(index + 1)) >= 0) {
                        wildcards++;
                    } else {
                        literals++;
                    }
                }
                index += 2;
                continue;
            }
            if (c == '[') {
                if (depth == 0) {
                    wildcards++;
                }
                index = endOfCharacterClass(pattern, index);
                continue;
            }
            if (c == '(') {
                if (depth == 0 && isCapturingGroup(pattern, index)) {
                    variables++;
                }
                depth++;
            } else if (c == ')') {
                depth = Math.max(0, depth - 1);
            } else if (depth == 0) {
                if (c == '.' || c == '*' || c == '+' || c == '?' || c == '|' || c == '{') {
                    wildcards++;
                    if (c == '{') {
                        // A counted quantifier, such as {2,4}: skip to its end.
                        final int close = pattern.indexOf('}', index);
                        index = (close < 0) ? pattern.length() : close + 1;
                        continue;
                    }
                } else if (c != '^' && c != '$') {
                    literals++;
                }
            }
            index++;
        }

        return new RouteSpecificity(variables, wildcards, literals);
    }

    /**
     * Reads an Ant route: each {@code {name}} is a variable, each {@code *} or {@code ?} a wildcard and each
     * {@code **} two, and every other character is literal.
     */
    private static RouteSpecificity ofAnt(
            final String pattern) {
        int variables = 0;
        int wildcards = 0;
        int literals = 0;
        int index = 0;
        while (index < pattern.length()) {
            final char c = pattern.charAt(index);
            if (c == '{') {
                variables++;
                final int close = pattern.indexOf('}', index);
                index = (close < 0) ? pattern.length() : close + 1;
                continue;
            } else if (c == '*' && index + 1 < pattern.length() && pattern.charAt(index + 1) == '*') {
                wildcards += 2;
                index += 2;
                continue;
            } else if (c == '*' || c == '?') {
                wildcards++;
            } else {
                literals++;
            }
            index++;
        }

        return new RouteSpecificity(variables, wildcards, literals);
    }

    private static boolean isCapturingGroup(
            final String pattern,
            final int open) {
        if (open + 1 >= pattern.length() || pattern.charAt(open + 1) != '?') {
            // A plain group captures.
            return true;
        }
        // (?<name>...) captures; (?:...), lookarounds (?=, (?!, (?<=, (?<! and flags don't.
        return open + 3 < pattern.length() && pattern.charAt(open + 2) == '<'
                && pattern.charAt(open + 3) != '=' && pattern.charAt(open + 3) != '!';
    }

    /**
     * The index just past the {@code ]} that closes the character class opening at {@code open}.
     */
    private static int endOfCharacterClass(
            final String pattern,
            final int open) {
        int index = open + 1;
        // A ] right after [ or [^ is a literal ], not the end.
        if (index < pattern.length() && pattern.charAt(index) == '^') {
            index++;
        }
        if (index < pattern.length() && pattern.charAt(index) == ']') {
            index++;
        }
        while (index < pattern.length() && pattern.charAt(index) != ']') {
            index += (pattern.charAt(index) == '\\') ? 2 : 1;
        }

        return Math.min(index + 1, pattern.length());
    }

    @Override
    public int compareTo(
            @Nonnull final RouteSpecificity other) {
        // Fewer variables and wildcards: more specific.
        final int byCount = Long.compare((long) variables_ + wildcards_, (long) other.variables_ + other.wildcards_);
        if (byCount != 0) {
            return byCount;
        }
        // More literal characters: more specific.
        final int byLiterals = Integer.compare(other.literals_, literals_);
        if (byLiterals != 0) {
            return byLiterals;
        }
        // A wildcard matches more than a variable: fewer wildcards, more specific.
        return Integer.compare(wildcards_, other.wildcards_);
    }

    @Override
    public boolean equals(
            final Object obj) {
        if (!(obj instanceof RouteSpecificity)) {
            return false;
        }
        final RouteSpecificity other = (RouteSpecificity) obj;
        return variables_ == other.variables_ && wildcards_ == other.wildcards_ && literals_ == other.literals_;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * variables_ + wildcards_) + literals_;
    }

    @Override
    public String toString() {
        return String.format("variables=%d, wildcards=%d, literals=%d", variables_, wildcards_, literals_);
    }

}
