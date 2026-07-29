package top.egon.cola.component.accessguard.store.local;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.store.PenaltyKey;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalPenaltyStoreTest {

    private static final Instant INSTANT = Instant.parse("2026-07-29T00:00:00Z");
    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);

    @Test
    void thresholdTransitionAndTtlAreAtomic() {
        MutableClock clock = new MutableClock(INSTANT);
        LocalPenaltyStore store = new LocalPenaltyStore(clock, 100);

        assertThat(store.recordViolation(key("user"), 3, ONE_MINUTE, TEN_MINUTES).active()).isFalse();
        assertThat(store.recordViolation(key("user"), 3, ONE_MINUTE, TEN_MINUTES).active()).isFalse();
        assertThat(store.recordViolation(key("user"), 3, ONE_MINUTE, TEN_MINUTES).active()).isTrue();
        clock.advance(TEN_MINUTES.plusMillis(1));

        assertThat(store.current(key("user"))).isEmpty();
    }

    @Test
    void counterExpiresBeforeThreshold() {
        MutableClock clock = new MutableClock(INSTANT);
        LocalPenaltyStore store = new LocalPenaltyStore(clock, 100);
        store.recordViolation(key("user"), 3, ONE_MINUTE, TEN_MINUTES);
        clock.advance(ONE_MINUTE.plusMillis(1));

        assertThat(store.recordViolation(key("user"), 3, ONE_MINUTE, TEN_MINUTES).violations())
                .isEqualTo(1L);
    }

    @Test
    void rejectsNewEntryWhenBoundedCapacityIsFull() {
        LocalPenaltyStore store = new LocalPenaltyStore(new MutableClock(INSTANT), 1);
        store.recordViolation(key("first"), 3, ONE_MINUTE, TEN_MINUTES);

        assertThatThrownBy(() -> store.recordViolation(key("second"), 3, ONE_MINUTE, TEN_MINUTES))
                .isInstanceOf(StoreOperationException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void stateVersionIsolatesPenaltyState() {
        LocalPenaltyStore store = new LocalPenaltyStore(new MutableClock(INSTANT), 10);
        store.recordViolation(key("user"), 1, ONE_MINUTE, TEN_MINUTES);

        assertThat(store.current(new PenaltyKey("draw", "state-v2", hash("user")))).isEmpty();
    }

    @Test
    void concurrentThresholdCrossingCreatesOneActivePenalty() throws Exception {
        LocalPenaltyStore store = new LocalPenaltyStore(new MutableClock(INSTANT), 10);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?>[] futures = IntStream.range(0, 100)
                    .mapToObj(ignored -> executor.submit(() ->
                            store.recordViolation(key("user"), 50, ONE_MINUTE, TEN_MINUTES)))
                    .toArray(Future<?>[]::new);
            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertThat(store.current(key("user"))).hasValueSatisfying(state -> {
            assertThat(state.active()).isTrue();
            assertThat(state.violations()).isEqualTo(50L);
        });
    }

    private static PenaltyKey key(String suffix) {
        return new PenaltyKey("draw", "state-v1", hash(suffix));
    }

    private static String hash(String suffix) {
        return String.format("%064x", Math.abs(suffix.hashCode()));
    }

    static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
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
