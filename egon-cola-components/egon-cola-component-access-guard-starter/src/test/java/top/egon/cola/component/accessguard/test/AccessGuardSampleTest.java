package top.egon.cola.component.accessguard.test;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.annotation.AccessGuard;
import top.egon.cola.component.accessguard.annotation.DoHystrix;
import top.egon.cola.component.accessguard.annotation.DoRateLimiter;
import top.egon.cola.component.accessguard.annotation.DoWhiteList;
import top.egon.cola.component.accessguard.annotation.RateLimiterAccessInterceptor;
import top.egon.cola.component.accessguard.annotation.TimeoutCircuitBreaker;
import top.egon.cola.component.accessguard.annotation.WhiteListAccessInterceptor;
import top.egon.cola.component.accessguard.aop.AccessGuardAop;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.blacklist.BlacklistStatus;
import top.egon.cola.component.accessguard.config.AccessGuardAnnotationResolver;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.key.AccessKeyResolution;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterDecision;
import top.egon.cola.component.accessguard.reject.ReflectionFallbackInvoker;
import top.egon.cola.component.accessguard.whitelist.DefaultWhiteListService;
import top.egon.cola.component.accessguard.whitelist.WhiteListDecision;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.GATEKEEPER;

class AccessGuardSampleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AccessGuardAutoConfiguration.class))
            .withUserConfiguration(SampleAccessGuardConfiguration.class);

    @Test
    void whiteListAnnotationShouldAcceptConfiguredUser() {
        contextRunner.run(context -> {
            AccessGuardRule rule = resolve(context.getBean(AccessGuardAnnotationResolver.class), "whiteList", String.class);
            WhiteListDecision decision = new DefaultWhiteListService(new AccessGuardProperties(), (ruleName, accessKeyHash) -> false)
                    .check(rule, "hash001");

            assertThat(decision.passed()).isTrue();
        });
    }

    @Test
    void doWhiteListShouldRejectThroughReturnJson() {
        contextRunner.run(context -> {
            AccessGuardRule rule = resolve(context.getBean(AccessGuardAnnotationResolver.class), "doWhiteList", String.class);
            WhiteListDecision decision = new DefaultWhiteListService(new AccessGuardProperties(), (ruleName, accessKeyHash) -> false)
                    .check(rule, "hash002");

            assertThat(decision.passed()).isFalse();
            assertThat(rule.returnJson()).isEqualTo("deny");
        });
    }

    @Test
    void rateLimiterAnnotationShouldCallFallbackOnRejection() throws NoSuchMethodException {
        SampleService target = new SampleService();
        Method method = SampleService.class.getDeclaredMethod("rateLimited", String.class);
        AccessGuardRule rule = new AccessGuardAnnotationResolver().resolve(method);
        AccessGuardContext context = new AccessGuardContext();
        context.setRuleName(rule.name());

        Object result = new ReflectionFallbackInvoker().reject(target, method, rule, context, new Object[]{"u-001"});

        assertThat(result).isEqualTo("fallback:u-001");
    }

    @Test
    void doRateLimiterShouldMapToMethodLevelGlobalLimiting() throws NoSuchMethodException {
        AccessGuardRule rule = new AccessGuardAnnotationResolver()
                .resolve(SampleService.class.getDeclaredMethod("doRateLimiter", String.class));

        assertThat(rule.key()).isEqualTo("all");
        assertThat(rule.permits()).isEqualTo(1L);
        assertThat(rule.interval()).isEqualTo(2L);
        assertThat(rule.intervalUnit()).isEqualTo(TimeUnit.SECONDS);
    }

    @Test
    void timeoutCircuitBreakerShouldReturnFallback() throws NoSuchMethodException {
        SampleService target = new SampleService();
        Method method = SampleService.class.getDeclaredMethod("timeout", String.class);
        AccessGuardRule rule = new AccessGuardAnnotationResolver().resolve(method);
        AccessGuardContext context = new AccessGuardContext();
        context.setRuleName(rule.name());

        Object result = new ReflectionFallbackInvoker().reject(target, method, rule, context, new Object[]{"u-001"});

        assertThat(result).isEqualTo("timeout:u-001");
    }

    @Test
    void doHystrixShouldReturnJson() throws NoSuchMethodException {
        SampleService target = new SampleService();
        Method method = SampleService.class.getDeclaredMethod("doHystrix", String.class);
        AccessGuardRule rule = new AccessGuardAnnotationResolver().resolve(method);

        Object result = new ReflectionFallbackInvoker().reject(target, method, rule, new AccessGuardContext(), new Object[]{"u-001"});

        assertThat(result).isEqualTo("hystrix");
    }

    @Test
    void combinedAnnotationsShouldUseFixedOrder() throws Throwable {
        Method method = SampleService.class.getDeclaredMethod("combined", String.class);
        List<String> order = new ArrayList<>();
        AccessGuardRuleResolver ruleResolver = new AccessGuardRuleResolver();
        AccessGuardAop aop = new AccessGuardAop(
                new AccessGuardProperties(),
                ruleResolver,
                (joinPoint, rule) -> new AccessKeyResolution("u-001", "u-001", "hash001"),
                (rule, accessKeyHash) -> {
                    order.add("white-list");
                    return WhiteListDecision.pass(GATEKEEPER);
                },
                new BlacklistService() {
                    @Override
                    public BlacklistStatus status(AccessGuardRule rule, AccessGuardContext context) {
                        order.add("blacklist");
                        return BlacklistStatus.none();
                    }

                    @Override
                    public BlacklistStatus incrementRejectAndMaybeBlacklist(AccessGuardRule rule, AccessGuardContext context) {
                        return BlacklistStatus.none();
                    }

                    @Override
                    public void remove(String ruleName, String accessKeyHash) {
                    }
                },
                (rule, context) -> {
                    order.add("rate-limiter");
                    return RateLimiterDecision.allow(0L);
                },
                (joinPoint, rule, context) -> {
                    order.add("timeout");
                    return joinPoint.proceed();
                },
                (joinPoint, rule, context, args) -> "reject",
                event -> {
                }
        );

        Object result = aop.around(new TestProceedingJoinPoint(new SampleService(), method, new Object[]{"u-001"}));

        assertThat(result).isEqualTo("combined:u-001");
        assertThat(order).containsExactly("white-list", "blacklist", "rate-limiter", "timeout");
    }

    private AccessGuardRule resolve(AccessGuardAnnotationResolver resolver, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        return resolver.resolve(SampleService.class.getDeclaredMethod(methodName, parameterTypes));
    }

    @Configuration
    static class SampleAccessGuardConfiguration {
    }

    static class SampleService {

        @WhiteListAccessInterceptor(name = "white-list-api", key = "userId", users = "hash001")
        String whiteList(String userId) {
            return userId;
        }

        @DoWhiteList(key = "userId", returnJson = "deny")
        String doWhiteList(String userId) {
            return userId;
        }

        @RateLimiterAccessInterceptor(name = "rate-api", key = "userId", permits = 1, interval = 1, fallbackMethod = "fallback")
        String rateLimited(String userId) {
            return userId;
        }

        @DoRateLimiter(permitsPerSecond = 0.5d, returnJson = "limited")
        String doRateLimiter(String userId) {
            return userId;
        }

        @TimeoutCircuitBreaker(name = "timeout-api", timeoutValue = 10, fallbackMethod = "timeoutFallback")
        String timeout(String userId) {
            return userId;
        }

        @DoHystrix(returnJson = "hystrix", timeoutValue = 10)
        String doHystrix(String userId) {
            return userId;
        }

        @AccessGuard(name = "combined-api", key = "userId", whitelist = true, rateLimiter = true, blacklist = true, timeoutBreaker = true)
        String combined(String userId) {
            return "combined:" + userId;
        }

        String fallback(String userId) {
            return "fallback:" + userId;
        }

        String timeoutFallback(String userId) {
            return "timeout:" + userId;
        }
    }

    static class TestProceedingJoinPoint implements ProceedingJoinPoint {

        private final Object target;
        private final Method method;
        private final Object[] args;

        TestProceedingJoinPoint(Object target, Method method, Object[] args) {
            this.target = target;
            this.method = method;
            this.args = args;
        }

        @Override
        public Object proceed() throws Throwable {
            return method.invoke(target, args);
        }

        @Override
        public Object proceed(Object[] args) throws Throwable {
            return method.invoke(target, args);
        }

        @Override
        public void set$AroundClosure(AroundClosure arc) {
        }

        @Override
        public String toShortString() {
            return method.getName();
        }

        @Override
        public String toLongString() {
            return method.toGenericString();
        }

        @Override
        public Object getThis() {
            return target;
        }

        @Override
        public Object getTarget() {
            return target;
        }

        @Override
        public Object[] getArgs() {
            return args;
        }

        @Override
        public Signature getSignature() {
            return new TestMethodSignature(method);
        }

        @Override
        public SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public String getKind() {
            return "method-execution";
        }

        @Override
        public StaticPart getStaticPart() {
            return null;
        }
    }

    record TestMethodSignature(Method method) implements MethodSignature {

        @Override
        public Class<?> getReturnType() {
            return method.getReturnType();
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public String[] getParameterNames() {
            return new String[]{"userId"};
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        @Override
        public String toShortString() {
            return method.getName();
        }

        @Override
        public String toLongString() {
            return method.toGenericString();
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public Class<?> getDeclaringType() {
            return method.getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return method.getDeclaringClass().getName();
        }
    }
}
