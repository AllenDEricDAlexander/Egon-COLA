package top.egon.cola.component.ddc.admin.security.openapi;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DdcNonceCache {

    private final int maximumSize;

    private final Map<String, Long> entries = new LinkedHashMap<>();

    public DdcNonceCache(int maximumSize) {
        if (maximumSize <= 0) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }
        this.maximumSize = maximumSize;
    }

    public synchronized boolean markIfAbsent(String key, long now, long timeToLiveMillis) {
        removeExpired(now);
        Long expiresAt = entries.get(key);
        if (expiresAt != null && expiresAt >= now) {
            return false;
        }
        while (entries.size() >= maximumSize) {
            Iterator<String> iterator = entries.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        entries.put(key, now + timeToLiveMillis);
        return true;
    }

    private void removeExpired(long now) {
        entries.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
