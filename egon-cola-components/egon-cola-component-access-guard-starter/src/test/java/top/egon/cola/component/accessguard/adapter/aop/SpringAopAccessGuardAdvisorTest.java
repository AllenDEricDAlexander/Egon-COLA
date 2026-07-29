package top.egon.cola.component.accessguard.adapter.aop;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardAopAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardCoreAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardLocalStoreAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardReactiveAutoConfiguration;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardTimeLimitAutoConfiguration;
import top.egon.cola.component.accessguard.store.DenyListStore;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringAopAccessGuardAdvisorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    AccessGuardCoreAutoConfiguration.class,
                    AccessGuardLocalStoreAutoConfiguration.class,
                    AccessGuardTimeLimitAutoConfiguration.class,
                    AccessGuardReactiveAutoConfiguration.class,
                    AccessGuardAopAutoConfiguration.class));

    @Test
    void denyListRejectionThroughRealSpringProxyDoesNotCallTarget() {
        contextRunner.withUserConfiguration(GuardedServiceConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.key.hmac-secret=test-secret",
                        "egon.cola.component.access-guard.rules.draw.key.contributors[0]=GLOBAL",
                        "egon.cola.component.access-guard.rules.draw.deny-list.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    GuardedService service = context.getBean(GuardedService.class);

                    assertThatThrownBy(service::draw).isInstanceOf(AccessGuardRejectedException.class);
                    assertThat(service.calls()).isZero();
                });
    }

    @Test
    void methodBindingOverridesTypeBinding() throws Exception {
        GuardBindingResolver resolver = new GuardBindingResolver();

        assertThat(resolver.resolve(TypeGuardedService.class.getMethod("draw"), TypeGuardedService.class))
                .get()
                .extracting(GuardBinding::ruleId)
                .isEqualTo("method-rule");
    }

    @Test
    void reactiveProxyDoesNotEvaluateOrInvokeBeforeSubscription() {
        contextRunner.withUserConfiguration(ReactiveGuardedServiceConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.key.hmac-secret=test-secret",
                        "egon.cola.component.access-guard.rules.reactive.key.contributors[0]=GLOBAL")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ReactiveGuardedService service = context.getBean(ReactiveGuardedService.class);

                    Mono<String> guarded = service.draw();

                    assertThat(service.calls()).isZero();
                    StepVerifier.create(guarded).expectNext("ok").verifyComplete();
                    assertThat(service.calls()).isOne();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class GuardedServiceConfiguration {

        @Bean
        GuardedService guardedService() {
            return new GuardedService();
        }

        @Bean
        DenyListStore denyListStore() {
            return (ruleId, dataVersion, keyHash) -> true;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ReactiveGuardedServiceConfiguration {

        @Bean
        ReactiveGuardedService reactiveGuardedService() {
            return new ReactiveGuardedService();
        }
    }

    static class GuardedService {

        private final AtomicInteger calls = new AtomicInteger();

        @AccessGuard("draw")
        public String draw() {
            calls.incrementAndGet();
            return "ok";
        }

        int calls() {
            return calls.get();
        }
    }

    static class ReactiveGuardedService {

        private final AtomicInteger calls = new AtomicInteger();

        @AccessGuard("reactive")
        public Mono<String> draw() {
            calls.incrementAndGet();
            return Mono.just("ok");
        }

        int calls() {
            return calls.get();
        }
    }

    @AccessGuard("type-rule")
    static class TypeGuardedService {

        @AccessGuard("method-rule")
        public String draw() {
            return "ok";
        }
    }
}
