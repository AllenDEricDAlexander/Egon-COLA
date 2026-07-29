package top.egon.cola.component.accessguard.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.execution.reactive.ReactiveGuardExecutor;
import top.egon.cola.component.accessguard.execution.reactive.ReactorGuardExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AccessGuardReactiveAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AccessGuardReactiveAutoConfiguration.class))
            .withUserConfiguration(EngineConfiguration.class);

    @Test
    void reactorClasspathRegistersTheOptionalAdapter() {
        contextRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(ReactiveGuardExecutor.class)
                .hasSingleBean(ReactorGuardExecutor.class));
    }

    @Test
    void reactorAbsentKeepsTheContextLoadable() {
        contextRunner.withClassLoader(new FilteredClassLoader("reactor.core.publisher"))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(ReactiveGuardExecutor.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class EngineConfiguration {

        @Bean
        GuardEngine guardEngine() {
            return mock(GuardEngine.class);
        }
    }
}
