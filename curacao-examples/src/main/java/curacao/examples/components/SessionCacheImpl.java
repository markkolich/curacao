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

package curacao.examples.components;

import com.google.common.cache.CacheBuilder;
import curacao.annotations.Component;
import org.apache.commons.codec.binary.StringUtils;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public final class SessionCacheImpl implements SessionCache {

    private final Map<Object, Object> cache_;

    public SessionCacheImpl() {
        cache_ = CacheBuilder.newBuilder()
            .expireAfterAccess(30L, TimeUnit.MINUTES)
            .build()
            .asMap(); // Concurrent map
    }

    @Override
    public Object getSession(
            final String id) {
        return cache_.get(id);
    }

    @Override
    public void setSession(
            final String id,
            final Object session) {
        cache_.put(id, session);
    }

    @Override
    public Object removeSession(
            final String id) {
        return cache_.remove(id);
    }

    public static String getRandomSessionId() {
        return Base64.getUrlEncoder().encodeToString(StringUtils.getBytesUtf8(UUID.randomUUID().toString()));
    }

}
