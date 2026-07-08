package top.egon.cola.component.accessguard.ratelimiter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterRuleConversionTest {

    @Test
    void shouldConvertHalfPermitPerSecondToOnePermitEveryTwoSeconds() {
        RateLimiterRuleConversion conversion = RateLimiterRuleConversion.fromPermitsPerSecond(0.5d);

        assertThat(conversion.permits()).isEqualTo(1L);
        assertThat(conversion.interval()).isEqualTo(2L);
        assertThat(conversion.intervalUnit()).isEqualTo(TimeUnit.SECONDS);
    }
}
