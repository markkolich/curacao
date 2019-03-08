/*
 * Copyright (c) 2019 Mark S. Kolich
 * http://mark.koli.ch
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

package curacao.util.helpers;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;

public final class WildcardMatchHelper {

    private static final char WILDCARD = '*';

    // Cannot instantiate
    private WildcardMatchHelper() {}

    /**
     * Performs a wildcard matching for the text and pattern provided.
     * 
     * @param text
     *            the text to be tested for matches.
     * 
     * @param pattern
     *            the pattern to be matched for. This can contain the wildcard
     *            character '*' (asterisk).
     * 
     * @return <tt>true</tt> if a match is found, <tt>false</tt> otherwise.
     */
    private static final boolean matches(String text,
                                         String pattern) {
        if (StringUtils.isEmpty(text)) {
            return false;
        }

        text += '\0';
        pattern += '\0';

        int n = pattern.length();

        boolean[] states = new boolean[n + 1];
        boolean[] old = new boolean[n + 1];
        old[0] = true;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            states = new boolean[n + 1]; // initialized to false
            for (int j = 0; j < n; j++) {
                char p = pattern.charAt(j);

                // hack to handle *'s that match 0 characters
                if (old[j] && (p == WILDCARD))
                    old[j + 1] = true;

                if (old[j] && (p == c))
                    states[j + 1] = true;
                if (old[j] && (p == WILDCARD))
                    states[j] = true;
                if (old[j] && (p == WILDCARD))
                    states[j + 1] = true;
            }
            old = states;
        }
        return states[n];
    }

    public static boolean matchesAny(final Iterable<String> patterns,
                                     final String text) {
        if (Iterables.isEmpty(patterns)) {
            return false;
        }

        for (final String string : patterns) {
            if (matches(text, string)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesAny(final String[] patterns,
                                     final String text) {
        if (patterns.length == 0) {
            return false;
        }

        for (final String string : patterns) {
            if (matches(text, string)) {
                return true;
            }
        }
        return false;
    }

}
