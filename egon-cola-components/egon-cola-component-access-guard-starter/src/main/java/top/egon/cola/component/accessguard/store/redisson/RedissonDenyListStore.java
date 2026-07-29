package top.egon.cola.component.accessguard.store.redisson;

import org.redisson.api.RedissonClient;
import top.egon.cola.component.accessguard.store.DenyListStore;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

public final class RedissonDenyListStore implements DenyListStore {

    private final RedissonListStoreSupport delegate;
    private final AccessGuardRedisKeyFactory keyFactory;

    public RedissonDenyListStore(RedissonClient client, AccessGuardRedisKeyFactory keyFactory) {
        this.delegate = new RedissonListStoreSupport(client);
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
    }

    @Override
    public boolean contains(String ruleId, String dataVersion, String keyHash) {
        return delegate.contains(keyFactory.denyList(ruleId, dataVersion), keyHash);
    }

    @Override
    public void add(String ruleId, String dataVersion, String keyHash, Duration ttl) {
        delegate.add(keyFactory.denyList(ruleId, dataVersion), keyHash, ttl);
    }

    @Override
    public void remove(String ruleId, String dataVersion, String keyHash) {
        delegate.remove(keyFactory.denyList(ruleId, dataVersion), keyHash);
    }

    @Override
    public void replace(String ruleId, String dataVersion, Set<String> keyHashes, Duration ttl) {
        delegate.replace(keyFactory.denyList(ruleId, dataVersion), keyHashes, ttl);
    }
}
