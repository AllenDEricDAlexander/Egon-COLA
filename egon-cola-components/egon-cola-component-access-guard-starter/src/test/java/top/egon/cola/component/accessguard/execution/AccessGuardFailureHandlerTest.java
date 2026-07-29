package top.egon.cola.component.accessguard.execution;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.exception.AccessGuardRejectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessGuardFailureHandlerTest {

    private final RuntimeException infrastructureFailure = new IllegalStateException("redis down");

    @Test
    void localFallbackLetsTheCallContinueOnTheLocalPath() {
        AccessGuardFailureHandler handler = handlerWith(FailStrategy.LOCAL_FALLBACK, true);

        assertThat(handler.failOpen(null, "rate limiter", infrastructureFailure)).isTrue();
    }

    @Test
    void localFallbackRejectsWhenDisabled() {
        AccessGuardFailureHandler handler = handlerWith(FailStrategy.LOCAL_FALLBACK, false);

        assertThatThrownBy(() -> handler.failOpen(null, "rate limiter", infrastructureFailure))
                .isInstanceOf(AccessGuardRejectedException.class)
                .hasCause(infrastructureFailure);
    }

    @Test
    void localFallbackNeverPropagatesTheRawInfrastructureFailure() {
        AccessGuardFailureHandler handler = handlerWith(FailStrategy.LOCAL_FALLBACK, false);

        assertThatThrownBy(() -> handler.failOpen(null, "white list", infrastructureFailure))
                .isNotSameAs(infrastructureFailure);
    }

    @Test
    void failOpenContinuesAndFailClosedRejects() {
        assertThat(handlerWith(FailStrategy.FAIL_OPEN, true)
                .failOpen(null, "white list", infrastructureFailure)).isTrue();

        assertThatThrownBy(() -> handlerWith(FailStrategy.FAIL_CLOSED, true)
                .failOpen(null, "white list", infrastructureFailure))
                .isInstanceOf(AccessGuardRejectedException.class);
    }

    private AccessGuardFailureHandler handlerWith(FailStrategy strategy, boolean localFallbackEnabled) {
        AccessGuardProperties properties = new AccessGuardProperties();
        properties.setFailStrategy(strategy);
        properties.getLocalFallback().setEnabled(localFallbackEnabled);
        return new AccessGuardFailureHandler(properties);
    }
}
