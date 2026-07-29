package top.egon.cola.component.accessguard.store.local;

import top.egon.cola.component.accessguard.store.PenaltyKey;
import top.egon.cola.component.accessguard.store.PenaltyState;
import top.egon.cola.component.accessguard.store.PenaltyStore;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class LocalPenaltyStore implements PenaltyStore {

    private final Clock clock;
    private final int maxEntries;
    private final ConcurrentHashMap<PenaltyKey, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicInteger entryCount = new AtomicInteger();

    public LocalPenaltyStore(Clock clock, int maxEntries) {
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
    }

    @Override
    public Optional<PenaltyState> current(PenaltyKey key) {
        Instant now = clock.instant();
        Entry entry = entries.computeIfPresent(key, (ignored, existing) -> {
            if (existing.expiredAt(now)) {
                entryCount.decrementAndGet();
                return null;
            }
            return existing;
        });
        return Optional.ofNullable(entry).map(value -> value.toState(now));
    }

    @Override
    public PenaltyState recordViolation(
            PenaltyKey key,
            long threshold,
            Duration violationTtl,
            Duration penaltyTtl
    ) {
        validate(threshold, violationTtl, penaltyTtl);
        Instant now = clock.instant();
        Entry updated = entries.compute(key, (ignored, existing) -> {
            if (existing == null) {
                reserve();
            }
            if (existing != null && existing.penaltyExpiresAt != null
                    && existing.penaltyExpiresAt.isAfter(now)) {
                return existing;
            }
            boolean currentWindow = existing != null
                    && existing.penaltyExpiresAt == null
                    && existing.violationExpiresAt.isAfter(now);
            long previous = currentWindow ? existing.violations : 0L;
            long violations = Math.addExact(previous, 1L);
            Instant violationExpiresAt = currentWindow
                    ? existing.violationExpiresAt
                    : now.plus(violationTtl);
            Instant penaltyExpiresAt = violations >= threshold ? now.plus(penaltyTtl) : null;
            return new Entry(violations, violationExpiresAt, penaltyExpiresAt);
        });
        return updated.toState(now);
    }

    @Override
    public int evictExpired() {
        Instant now = clock.instant();
        AtomicInteger removed = new AtomicInteger();
        entries.forEach((key, entry) -> {
            if (entry.expiredAt(now) && entries.remove(key, entry)) {
                entryCount.decrementAndGet();
                removed.incrementAndGet();
            }
        });
        return removed.get();
    }

    @Override
    public int size() {
        return entryCount.get();
    }

    private void reserve() {
        while (true) {
            int current = entryCount.get();
            if (current >= maxEntries) {
                throw new StoreOperationException("penalty store capacity exceeded");
            }
            if (entryCount.compareAndSet(current, current + 1)) {
                return;
            }
        }
    }

    private static void validate(long threshold, Duration violationTtl, Duration penaltyTtl) {
        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold must be positive");
        }
        if (violationTtl == null || violationTtl.isZero() || violationTtl.isNegative()) {
            throw new IllegalArgumentException("violationTtl must be positive");
        }
        if (penaltyTtl == null || penaltyTtl.isZero() || penaltyTtl.isNegative()) {
            throw new IllegalArgumentException("penaltyTtl must be positive");
        }
    }

    private record Entry(long violations, Instant violationExpiresAt, Instant penaltyExpiresAt) {

        private boolean expiredAt(Instant now) {
            if (penaltyExpiresAt != null) {
                return !penaltyExpiresAt.isAfter(now);
            }
            return !violationExpiresAt.isAfter(now);
        }

        private PenaltyState toState(Instant now) {
            return new PenaltyState(
                    violations,
                    penaltyExpiresAt != null && penaltyExpiresAt.isAfter(now),
                    violationExpiresAt,
                    penaltyExpiresAt);
        }
    }
}
