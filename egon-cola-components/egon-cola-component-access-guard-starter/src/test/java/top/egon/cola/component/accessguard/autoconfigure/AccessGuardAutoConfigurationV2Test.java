package top.egon.cola.component.accessguard.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.adapter.aop.SpringAopAccessGuardAdvisor;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.AccessGuardClient;
import top.egon.cola.component.accessguard.execution.TimeLimiter;
import top.egon.cola.component.accessguard.execution.RoutingTimeLimiter;
import top.egon.cola.component.accessguard.store.local.LocalStateCleaner;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardAutoConfigurationV2Test {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    AccessGuardCoreAutoConfiguration.class,
                    AccessGuardLocalStoreAutoConfiguration.class,
                    AccessGuardTimeLimitAutoConfiguration.class,
                    AccessGuardAopAutoConfiguration.class));

    @Test
    void disabledEngineKeepsProgrammaticClientWithoutAdvisor() {
        contextRunner.withUserConfiguration(InactiveGuardConfiguration.class)
                .withPropertyValues("egon.cola.component.access-guard.engine=DISABLED")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AccessGuardClient.class);
                    assertThat(context).doesNotHaveBean(SpringAopAccessGuardAdvisor.class);
                    assertThat(context.getBean(InactiveGuardedService.class).draw()).isEqualTo("ok");
                });
    }

    @Test
    void disabledComponentRegistersNoV2Beans() {
        contextRunner.withPropertyValues("egon.cola.component.access-guard.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AccessGuardClient.class);
                    assertThat(context).doesNotHaveBean(LocalStateCleaner.class);
                });
    }

    @Test
    void agentModeWithoutIntegrationFailsWithActionableMessage() {
        contextRunner.withPropertyValues("egon.cola.component.access-guard.engine=AGENT")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("egon-cola-component-bytecode-starter"));
    }

    @Test
    void customTimeLimiterReplacesTheManagedDefault() {
        contextRunner.withUserConfiguration(CustomTimeLimiterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(TimeLimiter.class);
                    assertThat(context.getBean(TimeLimiter.class))
                            .isSameAs(context.getBean("customTimeLimiter"));
                });
    }

    @Test
    void contextCloseReleasesLocalCleaner() {
        AtomicReference<LocalStateCleaner> cleaner = new AtomicReference<>();
        AtomicReference<RoutingTimeLimiter> timeLimiter = new AtomicReference<>();
        contextRunner.run(context -> {
            cleaner.set(context.getBean(LocalStateCleaner.class));
            timeLimiter.set(context.getBean(RoutingTimeLimiter.class));
        });

        assertThat(cleaner.get()).isNotNull();
        assertThat(cleaner.get().isClosed()).isTrue();
        assertThat(timeLimiter.get()).isNotNull();
        assertThat(timeLimiter.get().isClosed()).isTrue();
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTimeLimiterConfiguration {

        @Bean
        TimeLimiter customTimeLimiter() {
            return (invocation, config) -> invocation.continuation().execute();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class InactiveGuardConfiguration {

        @Bean
        InactiveGuardedService inactiveGuardedService() {
            return new InactiveGuardedService();
        }
    }

    static class InactiveGuardedService {

        @AccessGuard("inactive-rule")
        public String draw() {
            return "ok";
        }
    }
}
