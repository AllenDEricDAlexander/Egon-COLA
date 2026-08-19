package top.egon.cola.component.gateway.engine.common.provider.service;

import top.egon.cola.component.gateway.engine.common.provider.domain.PassiveHealthPolicy;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderCallOutcome;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassiveHealthTrackerTest {

    @Test
    void retryableFailuresEjectAndSuccessfulHalfOpenProbeRecovers() {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-07-25T00:00:00Z")
        );
        PassiveHealthTracker tracker = new PassiveHealthTracker(
                new PassiveHealthPolicy(
                        2,
                        2,
                        0.5,
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(20)
                ),
                clock
        );

        tracker.record("a:lease", ProviderCallOutcome.RETRYABLE_FAILURE);
        assertTrue(tracker.eligible("a:lease"));
        tracker.record("a:lease", ProviderCallOutcome.RETRYABLE_FAILURE);
        assertFalse(tracker.eligible("a:lease"));

        clock.advance(Duration.ofSeconds(5));
        assertTrue(tracker.eligible("a:lease"));
        tracker.record("a:lease", ProviderCallOutcome.SUCCESS);
        assertTrue(tracker.eligible("a:lease"));
    }

    @Test
    void businessRejectionAndCancellationDoNotEjectProvider() {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-07-25T00:00:00Z")
        );
        PassiveHealthTracker tracker = new PassiveHealthTracker(
                PassiveHealthPolicy.defaults(),
                clock
        );

        for (int index = 0; index < 20; index++) {
            tracker.record(
                    "a:lease",
                    index % 2 == 0
                            ? ProviderCallOutcome.BUSINESS_REJECTION
                            : ProviderCallOutcome.CANCELLED
            );
        }

        assertTrue(tracker.eligible("a:lease"));
    }
}
