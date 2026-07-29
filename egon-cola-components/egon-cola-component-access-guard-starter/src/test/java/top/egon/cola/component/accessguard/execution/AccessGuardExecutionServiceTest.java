package top.egon.cola.component.accessguard.execution;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.annotation.WhiteListMode;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.exception.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.key.AccessKeyResolution;
import top.egon.cola.component.accessguard.key.AccessKeyResolver;
import top.egon.cola.component.accessguard.reject.RejectResponseInvoker;
import top.egon.cola.component.accessguard.whitelist.WhiteListDecision;
import top.egon.cola.component.accessguard.whitelist.WhiteListService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    void rejectedCallNeverReachesBusinessMethodWhenRejectResponseFails() throws Throwable {
        var properties = new top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties();
        properties.setFailStrategy(FailStrategy.FAIL_OPEN);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(AccessGuardExecutionServiceTest.class.getDeclaredMethod("guarded"));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.getTarget()).thenReturn(this);

        AccessGuardRuleResolver ruleResolver = mock(AccessGuardRuleResolver.class);
        when(ruleResolver.resolve(any())).thenReturn(whiteListOnlyRule());

        AccessKeyResolver keyResolver = mock(AccessKeyResolver.class);
        when(keyResolver.resolve(any(), any())).thenReturn(new AccessKeyResolution("raw", "raw", "hash"));

        WhiteListService whiteListService = mock(WhiteListService.class);
        when(whiteListService.check(any(), any())).thenReturn(WhiteListDecision.reject("not on the list"));

        RejectResponseInvoker rejectResponseInvoker = mock(RejectResponseInvoker.class);
        when(rejectResponseInvoker.reject(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("cannot render reject response"));

        AccessGuardExecutionService service = new AccessGuardExecutionService(
                properties,
                ruleResolver,
                keyResolver,
                whiteListService,
                mock(top.egon.cola.component.accessguard.blacklist.BlacklistService.class),
                mock(top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor.class),
                mock(top.egon.cola.component.accessguard.circuitbreaker.TimeoutCircuitBreakerExecutor.class),
                rejectResponseInvoker,
                mock(top.egon.cola.component.accessguard.event.AccessGuardEventPublisher.class),
                new AccessGuardFailureHandler(properties)
        );

        assertThatThrownBy(() -> service.execute(joinPoint))
                .isInstanceOf(AccessGuardRejectedException.class)
                .hasRootCauseMessage("cannot render reject response");
        verify(joinPoint, never()).proceed();
    }

    @SuppressWarnings("unused")
    private void guarded() {
    }

    private static AccessGuardRule whiteListOnlyRule() {
        return new AccessGuardRule(
                "rule", "all", "", true, List.of("someone"), WhiteListMode.GATEKEEPER,
                false, 0L, 0L, TimeUnit.SECONDS,
                false, 0L, Duration.ZERO, false,
                false, Duration.ZERO, null, false, false, "", "", FailStrategy.FAIL_OPEN);
    }
}
