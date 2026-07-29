package top.egon.cola.component.accessguard.store;

import java.time.Duration;
import java.util.Set;

@FunctionalInterface
public interface DenyListStore {

    boolean contains(String ruleId, String dataVersion, String keyHash);

    default void add(String ruleId, String dataVersion, String keyHash, Duration ttl) {
        throw new StoreOperationException("DENY_LIST_WRITE_UNSUPPORTED");
    }

    default void remove(String ruleId, String dataVersion, String keyHash) {
        throw new StoreOperationException("DENY_LIST_WRITE_UNSUPPORTED");
    }

    default void replace(String ruleId, String dataVersion, Set<String> keyHashes, Duration ttl) {
        throw new StoreOperationException("DENY_LIST_WRITE_UNSUPPORTED");
    }
}
