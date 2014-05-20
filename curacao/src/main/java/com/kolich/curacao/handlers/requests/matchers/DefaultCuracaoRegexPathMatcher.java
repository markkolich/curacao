/**
 * Copyright (c) 2014 Mark S. Kolich
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

package com.kolich.curacao.handlers.requests.matchers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

public final class DefaultCuracaoRegexPathMatcher
    implements CuracaoPathMatcher {

    private static final Logger logger__ =
        getLogger(DefaultCuracaoRegexPathMatcher.class);

    /**
     * A regex for finding/extracting named captured groups in
     * another regex.
     */
    private static final Pattern NAMED_GROUPS_REGEX = Pattern
        .compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

    /**
     * Acts as an internal cache that maps a routing key to a formal
     * pre-compiled {@link Pattern}.  Routing keys are the String's used
     * inside of routing annotations.  For example, the routing key associated
     * with <tt>@RequestMapping("foo/bar/")</tt> is "foo/bar/".
     *
     * A single instance of this cache is gracefully shared by all regex
     * based path matchers.
     */
    private static final class PatternCache {

        // This makes use of the "Initialization-on-demand holder idiom" which is
        // discussed in detail here:
        // http://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
        // As such, this is totally thread safe and performant.
        private static class LazyHolder {
            private static final PatternCache instance__ = new PatternCache();
        }
        private static final PatternCache getInstance() {
            return LazyHolder.instance__;
        }

        private final Map<String,Pattern> cache_;

        private PatternCache() {
            cache_ = Maps.newLinkedHashMap();
        }

        public final synchronized Pattern getPattern(final String key) {
            Pattern p = null;
            if((p = cache_.get(key)) == null) {
                // No pattern has been compiled yet for the incoming key.
                // This may fail miserably if the regex attached to the
                // routing annotation is malformed, in which case, we will
                // bail here guaranteeing that this routing key will ~not~
                // match the path we're tasked with checking.
                p = Pattern.compile(key);
                cache_.put(key, p);
            }
            return p;
        }

    }

    @Nullable
    @Override
    public Map<String,String> match(final HttpServletRequest request,
                                    final String key,
                                    final String path) throws Exception {
        Map<String,String> result = null;
        try {
            // Load the pre-compiled pattern associated with the routing key.
            final Pattern p = PatternCache.getInstance().getPattern(key);
            // Build the matcher, and check if the regex matches the path.
            final Matcher m = p.matcher(path);
            if(m.matches()) { // required to prep matcher
                result = getNamedGroupsAndValues(p.toString(), m);
            }
        } catch (Exception e) {
            logger__.warn("Failed to match route using regex (key=" + key +
                ", path=" + path + ")", e);
        }
        return result; // Immutable
    }

    @Nonnull
    private static final ImmutableMap<String,String> getNamedGroupsAndValues(final String regex,
                                                                             final Matcher m) {
        final ImmutableSet<String> groups = getNamedGroups(regex);
        // If the provided regex has no capture groups, there's no point in
        // actually trying to build a new map to hold the results
        if(groups.isEmpty()) {
            return ImmutableMap.of(); // Empty, immutable map
        } else {
            final ImmutableMap.Builder<String, String> builder =
                ImmutableMap.builder();
            // For each extract "capture group" in the regex...
            for(final String groupName : groups) {
                final String value;
                if((value = m.group(groupName)) != null) {
                    // Only non-null values are injected into the map.
                    builder.put(groupName, value);
                }
            }
            return builder.build(); // Immutable
        }
    }

    /**
     * Given a regex as a String, returns an {@link ImmutableSet} representing
     * the named capture groups in the regex.  For example, <tt>^(?<foo>\w+)</tt>
     * would return an {@link ImmutableSet} with a single entry "foo"
     * corresponding to the named capture group "foo".
     */
    @Nonnull
    private static final ImmutableSet<String> getNamedGroups(final String regex) {
        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        final Matcher m = NAMED_GROUPS_REGEX.matcher(regex);
        while(m.find()) {
            builder.add(m.group(1));
        }
        return builder.build(); // Immutable
    }

}
