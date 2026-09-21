package top.egon.cola.archetype.source.web.infrastructure.service.impl;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.archetype.source.web.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.web.infrastructure.cache.OrganizationCacheKey;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Broker-free claim store with the same duplicate-and-release semantics as the Redis owner. */
@Slf4j
public final class InMemoryCommandIdempotencyServiceImpl implements CommandIdempotencyService {
    private final Set<String> claims = ConcurrentHashMap.newKeySet();

    @Override public boolean claim(String operation, String requestId) {
        return claims.add(OrganizationCacheKey.command(operation, requestId));
    }

    @Override public void release(String operation, String requestId) {
        claims.remove(OrganizationCacheKey.command(operation, requestId));
    }

    public boolean contains(String operation, String requestId) {
        return claims.contains(OrganizationCacheKey.command(operation, requestId));
    }

    public void clear() { claims.clear(); }
}
