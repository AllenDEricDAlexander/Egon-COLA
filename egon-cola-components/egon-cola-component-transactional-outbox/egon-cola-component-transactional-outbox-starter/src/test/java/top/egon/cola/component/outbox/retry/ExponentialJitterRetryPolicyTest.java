package top.egon.cola.component.outbox.retry;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ExponentialJitterRetryPolicyTest {

    @Test
    void shouldApplyBoundedExponentialBackoffAndDeterministicJitter() {
        ExponentialJitterRetryPolicy policy = new ExponentialJitterRetryPolicy(
                Duration.ofSeconds(1), 2.0, Duration.ofSeconds(5), 0.2, () -> 0.5);

        assertThat(policy.nextDelay(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(policy.nextDelay(2)).isEqualTo(Duration.ofSeconds(2));
        assertThat(policy.nextDelay(4)).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void shouldClampNegativeAndOverflowingCalculations() {
        ExponentialJitterRetryPolicy low = new ExponentialJitterRetryPolicy(
                Duration.ofSeconds(1), 2.0, Duration.ofMinutes(5), 1.0, () -> 0.0);
        ExponentialJitterRetryPolicy high = new ExponentialJitterRetryPolicy(
                Duration.ofDays(1), Double.MAX_VALUE, Duration.ofDays(7), 1.0, () -> 1.0);

        assertThat(low.nextDelay(1)).isZero();
        assertThat(high.nextDelay(Integer.MAX_VALUE)).isEqualTo(Duration.ofDays(7));
    }
}
