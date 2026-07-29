package top.egon.cola.component.accessguard.store;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public interface GuardStoreContract {

    String HASH_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    String HASH_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    ListStoreFixture fixture();

    @Test
    default void membershipIsIsolatedByDataVersion() {
        ListStoreFixture store = fixture();
        store.add("draw", "v1", HASH_A, Duration.ZERO);

        assertThat(store.contains("draw", "v1", HASH_A)).isTrue();
        assertThat(store.contains("draw", "v2", HASH_A)).isFalse();
    }

    @Test
    default void replaceIsAtomicAtTheVersionBoundary() {
        ListStoreFixture store = fixture();
        store.add("draw", "v1", HASH_A, Duration.ZERO);
        store.replace("draw", "v1", Set.of(HASH_B), Duration.ZERO);

        assertThat(store.contains("draw", "v1", HASH_A)).isFalse();
        assertThat(store.contains("draw", "v1", HASH_B)).isTrue();
    }

    interface ListStoreFixture {

        boolean contains(String ruleId, String dataVersion, String keyHash);

        void add(String ruleId, String dataVersion, String keyHash, Duration ttl);

        void replace(String ruleId, String dataVersion, Set<String> keyHashes, Duration ttl);
    }
}
