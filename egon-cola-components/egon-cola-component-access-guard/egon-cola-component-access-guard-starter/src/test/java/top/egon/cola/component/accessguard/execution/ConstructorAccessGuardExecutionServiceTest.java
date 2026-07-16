package top.egon.cola.component.accessguard.execution;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.annotation.AccessGuard;
import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.annotation.TimeoutExecutorType;
import top.egon.cola.component.accessguard.annotation.WhiteListMode;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.event.AccessGuardEvent;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;
import top.egon.cola.component.accessguard.exception.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.key.AccessKeyResolution;
import top.egon.cola.component.accessguard.key.ExecutableAccessKeyResolver;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterDecision;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor;
import top.egon.cola.component.accessguard.whitelist.WhiteListService;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConstructorAccessGuardExecutionServiceTest {

    @Test
    void rejectsBeforeInitializationAndPublishesOneSanitizedEvent() throws Exception {
        Constructor<?> constructor = GuardedConstructor.class.getConstructor(String.class);
        AccessGuardRule rule = rule(FailStrategy.FAIL_OPEN, true);
        AccessGuardRuleResolver rules = mock(AccessGuardRuleResolver.class);
        when(rules.resolve(constructor)).thenReturn(rule);
        RateLimiterExecutor rateLimiter = mock(RateLimiterExecutor.class);
        when(rateLimiter.tryAcquire(org.mockito.ArgumentMatchers.eq(rule),
                org.mockito.ArgumentMatchers.any())).thenReturn(
                RateLimiterDecision.reject("capacity"));
        List<AccessGuardEvent> events = new ArrayList<>();
        ConstructorAccessGuardExecutionService service = service(
                FailStrategy.FAIL_OPEN, rules,
                (executable, arguments, ignored) -> new AccessKeyResolution(
                        arguments[0].toString(), arguments[0].toString(), "hashed-only"),
                rateLimiter,
                events::add
        );

        ConstructorGuardResult result = service.evaluate(constructor, new Object[]{"secret"});

        assertFalse(result.allowed());
        assertTrue(result.throwable() instanceof AccessGuardRejectedException);
        assertEquals(1, events.size());
        assertEquals("hashed-only", events.getFirst().accessKeyHash());
        assertFalse(events.getFirst().toString().contains("secret"));
    }

    @Test
    void appliesFailOpenAndFailClosedToConstructorInfrastructureFailure() throws Exception {
        Constructor<?> constructor = GuardedConstructor.class.getConstructor(String.class);
        AccessGuardRuleResolver rules = mock(AccessGuardRuleResolver.class);
        when(rules.resolve(constructor)).thenAnswer(invocation ->
                rule(FailStrategy.GLOBAL_DEFAULT, false));
        ExecutableAccessKeyResolver failingKeys = (executable, arguments, rule) -> {
            throw new IllegalStateException("key-store-down");
        };

        assertTrue(service(FailStrategy.FAIL_OPEN, rules, failingKeys,
                mock(RateLimiterExecutor.class), event -> {
                }).evaluate(constructor, new Object[]{"value"}).allowed());
        assertThrows(AccessGuardRejectedException.class, () -> service(
                FailStrategy.FAIL_CLOSED, rules, failingKeys,
                mock(RateLimiterExecutor.class), event -> {
                }).evaluate(constructor, new Object[]{"value"}));
    }

    private ConstructorAccessGuardExecutionService service(
            FailStrategy globalStrategy,
            AccessGuardRuleResolver ruleResolver,
            ExecutableAccessKeyResolver keyResolver,
            RateLimiterExecutor rateLimiter,
            AccessGuardEventPublisher publisher
    ) {
        AccessGuardProperties properties = new AccessGuardProperties();
        properties.setFailStrategy(globalStrategy);
        return new ConstructorAccessGuardExecutionService(
                properties,
                ruleResolver,
                new ConstructorAccessGuardValidator(),
                keyResolver,
                mock(WhiteListService.class),
                mock(BlacklistService.class),
                rateLimiter,
                publisher,
                new AccessGuardFailureHandler(properties)
        );
    }

    private AccessGuardRule rule(FailStrategy strategy, boolean rateLimited) {
        return new AccessGuardRule(
                "constructor", "all", "", false, List.of(), WhiteListMode.GATEKEEPER,
                rateLimited, 1, 1, TimeUnit.SECONDS, false, 0, Duration.ZERO, false,
                false, Duration.ZERO, TimeoutExecutorType.THREAD_POOL, false, true,
                "", "", strategy
        );
    }

    static class GuardedConstructor {

        @AccessGuard(rateLimiter = true)
        public GuardedConstructor(String value) {
        }
    }
}
