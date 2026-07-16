package top.egon.cola.component.accessguard.execution;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessGuardExecutionServiceTest {

    @Test
    void preservesBusinessThrowableIdentityWhenDisabled() throws Throwable {
        var properties = new top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties();
        properties.setEnabled(false);
        RuntimeException failure = new RuntimeException("business");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenThrow(failure);
        AccessGuardExecutionService service = new AccessGuardExecutionService(
                properties,
                mock(top.egon.cola.component.accessguard.config.AccessGuardRuleResolver.class),
                mock(top.egon.cola.component.accessguard.key.AccessKeyResolver.class),
                mock(top.egon.cola.component.accessguard.whitelist.WhiteListService.class),
                mock(top.egon.cola.component.accessguard.blacklist.BlacklistService.class),
                mock(top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor.class),
                mock(top.egon.cola.component.accessguard.circuitbreaker.TimeoutCircuitBreakerExecutor.class),
                mock(top.egon.cola.component.accessguard.reject.RejectResponseInvoker.class),
                mock(top.egon.cola.component.accessguard.event.AccessGuardEventPublisher.class),
                new AccessGuardFailureHandler(properties)
        );

        assertThatThrownBy(() -> service.execute(joinPoint)).isSameAs(failure);
    }
}
