package top.egon.cola.component.accessguard.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.core.plan.GuardPlanResolver;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.observability.AccessGuardEndpoint;
import top.egon.cola.component.accessguard.observability.GuardEvent;
import top.egon.cola.component.accessguard.observability.GuardEventListener;
import top.egon.cola.component.accessguard.observability.GuardEventPublisher;
import top.egon.cola.component.accessguard.observability.LoggingGuardEventListener;
import top.egon.cola.component.accessguard.observability.MicrometerGuardEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AccessGuardObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AccessGuardObservabilityAutoConfiguration.class));

    private final ApplicationContextRunner fullContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AccessGuardCoreAutoConfiguration.class,
                    AccessGuardLocalStoreAutoConfiguration.class,
                    AccessGuardTimeLimitAutoConfiguration.class,
                    AccessGuardObservabilityAutoConfiguration.class))
            .withUserConfiguration(RecordingConfiguration.class)
            .withPropertyValues(
                    "egon.cola.component.access-guard.key.hmac-secret=secret",
                    "egon.cola.component.access-guard.rules.draw.key.contributors[0]=GLOBAL");

    @Test
    void optionalObserversAndEndpointAreRegisteredWhenDependenciesExist() {
        contextRunner.withUserConfiguration(OptionalDependencies.class)
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(GuardEventPublisher.class)
                        .hasSingleBean(LoggingGuardEventListener.class)
                        .hasSingleBean(MicrometerGuardEventListener.class)
                        .hasSingleBean(AccessGuardEndpoint.class));
    }

    @Test
    void missingMicrometerDoesNotAffectBasePublisher() {
        contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.core"))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(GuardEventPublisher.class)
                        .doesNotHaveBean(MicrometerGuardEventListener.class));
    }

    @Test
    void missingActuatorDoesNotAffectBasePublisher() {
        contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.boot.actuate.endpoint"))
                .withUserConfiguration(OptionalDependencies.class)
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(GuardEventPublisher.class)
                        .doesNotHaveBean(AccessGuardEndpoint.class));
    }

    @Test
    void fullAutoConfigurationWiresPublisherIntoTheCoreEngine() {
        fullContextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(GuardEventPublisher.class);
            GuardInvocation invocation = new GuardInvocation(
                    "draw",
                    null,
                    Object.class,
                    null,
                    new Object[0],
                    Map.of(),
                    GuardEntryType.PROGRAMMATIC,
                    GuardInvocationKind.OPERATION,
                    () -> "ok");

            context.getBean(GuardEngine.class).evaluate(invocation);

            assertThat(context.getBean(RecordingListener.class).events).hasSize(1);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class OptionalDependencies {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        GuardPlanResolver guardPlanResolver() {
            return mock(GuardPlanResolver.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RecordingConfiguration {

        @Bean
        RecordingListener recordingListener() {
            return new RecordingListener();
        }
    }

    static final class RecordingListener implements GuardEventListener {

        private final List<GuardEvent> events = new ArrayList<>();

        @Override
        public void onEvent(GuardEvent event) {
            events.add(event);
        }
    }
}
