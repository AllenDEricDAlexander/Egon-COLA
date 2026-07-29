package top.egon.cola.component.accessguard.store.local;

import org.junit.jupiter.api.Nested;
import top.egon.cola.component.accessguard.store.AllowListStore;
import top.egon.cola.component.accessguard.store.DenyListStore;
import top.egon.cola.component.accessguard.store.GuardStoreContract;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

class LocalStoreContractTest {

    @Nested
    class AllowListContract implements GuardStoreContract {

        @Override
        public ListStoreFixture fixture() {
            MutableClock clock = new MutableClock();
            return LocalStoreContractTest.fixture(new LocalAllowListStore(clock, 10), clock);
        }
    }

    @Nested
    class DenyListContract implements GuardStoreContract {

        @Override
        public ListStoreFixture fixture() {
            MutableClock clock = new MutableClock();
            return LocalStoreContractTest.fixture(new LocalDenyListStore(clock, 10), clock);
        }
    }

    private static GuardStoreContract.ListStoreFixture fixture(AllowListStore store, MutableClock clock) {
        return new GuardStoreContract.ListStoreFixture() {
            @Override
            public boolean contains(String ruleId, String dataVersion, String keyHash) {
                return store.contains(ruleId, dataVersion, keyHash);
            }

            @Override
            public void add(String ruleId, String dataVersion, String keyHash, Duration ttl) {
                store.add(ruleId, dataVersion, keyHash, ttl);
            }

            @Override
            public void replace(String ruleId, String dataVersion, Set<String> keyHashes, Duration ttl) {
                store.replace(ruleId, dataVersion, keyHashes, ttl);
            }

            @Override
            public void advance(Duration duration) {
                clock.advance(duration);
            }
        };
    }

    private static GuardStoreContract.ListStoreFixture fixture(DenyListStore store, MutableClock clock) {
        return new GuardStoreContract.ListStoreFixture() {
            @Override
            public boolean contains(String ruleId, String dataVersion, String keyHash) {
                return store.contains(ruleId, dataVersion, keyHash);
            }

            @Override
            public void add(String ruleId, String dataVersion, String keyHash, Duration ttl) {
                store.add(ruleId, dataVersion, keyHash, ttl);
            }

            @Override
            public void replace(String ruleId, String dataVersion, Set<String> keyHashes, Duration ttl) {
                store.replace(ruleId, dataVersion, keyHashes, ttl);
            }

            @Override
            public void advance(Duration duration) {
                clock.advance(duration);
            }
        };
    }

    private static final class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-07-29T00:00:00Z");

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
