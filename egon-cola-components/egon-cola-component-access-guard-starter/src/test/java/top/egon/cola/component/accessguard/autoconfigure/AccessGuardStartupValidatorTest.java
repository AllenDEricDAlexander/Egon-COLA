package top.egon.cola.component.accessguard.autoconfigure;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.AccessGuardAgentIntegration;
import top.egon.cola.component.accessguard.api.RateLimitGuard;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardStartupValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    AccessGuardCoreAutoConfiguration.class,
                    AccessGuardLocalStoreAutoConfiguration.class,
                    AccessGuardTimeLimitAutoConfiguration.class,
                    AccessGuardAopAutoConfiguration.class));

    @Test
    void dedicatedAnnotationCannotBindAMultiPolicyPlan() {
        contextRunner.withUserConfiguration(DedicatedGuardConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.key.hmac-secret=test-secret",
                        "egon.cola.component.access-guard.rules.multi.key.contributors[0]=GLOBAL",
                        "egon.cola.component.access-guard.rules.multi.allow-list.enabled=true",
                        "egon.cola.component.access-guard.rules.multi.rate-limit.enabled=true")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("dedicated guard"));
    }

    @Test
    void aopModeRejectsGuardedSpringConstructors() {
        contextRunner.withUserConfiguration(ConstructorGuardConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.key.hmac-secret=test-secret",
                        "egon.cola.component.access-guard.rules.constructor.key.contributors[0]=GLOBAL")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("constructor"));
    }

    @Test
    void agentModeRejectsConstructorExecutionPolicies() {
        contextRunner.withUserConfiguration(
                        ConstructorGuardConfiguration.class,
                        AgentIntegrationConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.engine=agent",
                        "egon.cola.component.access-guard.key.hmac-secret=test-secret",
                        "egon.cola.component.access-guard.rules.constructor.key.contributors[0]=GLOBAL",
                        "egon.cola.component.access-guard.rules.constructor.rejection.mode=RETURN_NULL")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining(
                                "constructors support admission and THROW rejection only"));
    }

    @Test
    void reactiveMethodWithoutRegisteredAdapterFailsStartup() {
        contextRunner.withUserConfiguration(ReactiveGuardConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.key.hmac-secret=test-secret",
                        "egon.cola.component.access-guard.rules.reactive.key.contributors[0]=GLOBAL")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("Reactor adapter"));
    }

    @Configuration(proxyBeanMethods = false)
    static class DedicatedGuardConfiguration {

        @Bean
        DedicatedGuardedService dedicatedGuardedService() {
            return new DedicatedGuardedService();
        }
    }

    static class DedicatedGuardedService {

        @RateLimitGuard("multi")
        public String draw() {
            return "ok";
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ConstructorGuardConfiguration {

        @Bean
        ConstructorGuardedService constructorGuardedService() {
            return new ConstructorGuardedService();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ReactiveGuardConfiguration {

        @Bean
        ReactiveGuardedService reactiveGuardedService() {
            return new ReactiveGuardedService();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AgentIntegrationConfiguration {

        @Bean
        AccessGuardAgentIntegration accessGuardAgentIntegration() {
            return new AccessGuardAgentIntegration() {
            };
        }
    }

    static class ConstructorGuardedService {

        @AccessGuard("constructor")
        ConstructorGuardedService() {
        }
    }

    static class ReactiveGuardedService {

        @AccessGuard("reactive")
        Mono<String> draw() {
            return Mono.just("ok");
        }
    }
}
