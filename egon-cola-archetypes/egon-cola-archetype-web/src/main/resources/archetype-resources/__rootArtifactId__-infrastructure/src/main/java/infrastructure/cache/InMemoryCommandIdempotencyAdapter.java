package ${package}.infrastructure.cache;

import ${package}.domain.client.CommandIdempotencyPort;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCommandIdempotencyAdapter implements CommandIdempotencyPort {
    private final Set<String> claims = ConcurrentHashMap.newKeySet();
    @Override public boolean claim(String operation, String requestId) {
        return claims.add(OrganizationCacheKey.command(operation, requestId));
    }
    @Override public void release(String operation, String requestId) {
        claims.remove(OrganizationCacheKey.command(operation, requestId));
    }
}
