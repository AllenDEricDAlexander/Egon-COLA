package top.egon.cola.component.accessguard.store.local;

import top.egon.cola.component.accessguard.store.AllowListStore;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;

public final class LocalAllowListStore implements AllowListStore {

    private final LocalListStoreSupport delegate;

    public LocalAllowListStore(Clock clock, int maxEntries) {
        this.delegate = new LocalListStoreSupport(clock, maxEntries);
    }

    @Override
    public boolean contains(String ruleId, String dataVersion, String keyHash) {
        return delegate.contains(ruleId, dataVersion, keyHash);
    }

    @Override
    public void add(String ruleId, String dataVersion, String keyHash, Duration ttl) {
        delegate.add(ruleId, dataVersion, keyHash, ttl);
    }

    @Override
    public void remove(String ruleId, String dataVersion, String keyHash) {
        delegate.remove(ruleId, dataVersion, keyHash);
    }

    @Override
    public void replace(String ruleId, String dataVersion, Set<String> keyHashes, Duration ttl) {
        delegate.replace(ruleId, dataVersion, keyHashes, ttl);
    }
}
