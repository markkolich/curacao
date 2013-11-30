package com.kolich.curacao.examples.components;

import com.google.common.cache.CacheBuilder;
import com.kolich.curacao.annotations.Component;
import com.kolich.curacao.handlers.components.CuracaoComponent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.kolich.common.util.crypt.Base64Utils.encodeBase64URLSafe;

@Component
public final class SessionCache implements CuracaoComponent {

    private final Map<Object,Object> cache_;

    public SessionCache() {
        cache_ = CacheBuilder.newBuilder()
            .expireAfterAccess(30L, TimeUnit.MINUTES)
            .build()
            .asMap();
    }

    @Override
    public void initialize() throws Exception {
        // Nothing, intentional.
    }

    @Override
    public void destroy() throws Exception {
        // Nothing, intentional.
    }

    public Object getSession(final String id) {
        return cache_.get(id);
    }

    public void setSession(final String id, final Object session) {
        cache_.put(id, session);
    }

    public Object removeSession(final String id) {
        return cache_.remove(id);
    }

    public String getRandomSessionId() {
        return encodeBase64URLSafe(UUID.randomUUID().toString());
    }

}
