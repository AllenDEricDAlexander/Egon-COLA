package top.egon.cola.component.common.id.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.id.generator.IdGenerator;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class IdGeneratorAutoConfigurationTest {

    private static final String BOUND_MACHINE_ID = "0";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdGeneratorAutoConfiguration.class));

    /** Binding happens once per process, so every successful context in this JVM uses one configuration. */
    private final ApplicationContextRunner bindingContextRunner = contextRunner
            .withPropertyValues("egon.cola.component.id.machine-id=" + BOUND_MACHINE_ID);

    @Test
    void validConfigurationStartsWithoutPublishingAGeneratorBean() {
        bindingContextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IdGeneratorProperties.class);
            assertThat(context).doesNotHaveBean(SnowflakeIdGenerator.class);
            assertThat(context).doesNotHaveBean(LongIdGenerator.class);
        });
    }

    @Test
    void repeatedContextWithTheSameConfigurationRebindsNothing() {
        bindingContextRunner.run(first -> assertThat(first).hasNotFailed());
        bindingContextRunner.run(second -> assertThat(second).hasNotFailed());
    }

    @Test
    void disabledConfigurationCreatesNoPropertiesAndNoBinding() {
        contextRunner.withPropertyValues("egon.cola.component.id.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(IdGeneratorProperties.class);
                    assertThat(context).doesNotHaveBean(IdGenerator.class);
                });
    }

    @Test
    void missingMachineIdFailsFast() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "egon.cola.component.id.machine-id must be configured when enabled=true");
        });
    }

    @Test
    void machineIdOutsideRangeFailsFast() {
        contextRunner.withPropertyValues("egon.cola.component.id.machine-id=-1")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "egon.cola.component.id.machine-id must be between 0 and 1023: -1");
                });
        contextRunner.withPropertyValues("egon.cola.component.id.machine-id=1024")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "egon.cola.component.id.machine-id must be between 0 and 1023: 1024");
                });
    }

    @Test
    void negativeClockBackwardFailsFast() {
        contextRunner.withPropertyValues(
                        "egon.cola.component.id.machine-id=1",
                        "egon.cola.component.id.max-clock-backward=-1ms")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "egon.cola.component.id.max-clock-backward must not be negative: PT-0.001S");
                });
    }

    @Test
    void defaultAndConfiguredDurationAreBound() {
        new ApplicationContextRunner().withUserConfiguration(PropertiesBindingConfiguration.class)
                .run(context -> assertThat(context.getBean(IdGeneratorProperties.class).getMaxClockBackward())
                        .isEqualTo(Duration.ofMillis(5)));
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesBindingConfiguration.class)
                .withPropertyValues("egon.cola.component.id.max-clock-backward=7ms")
                .run(context -> assertThat(context.getBean(IdGeneratorProperties.class).getMaxClockBackward())
                        .isEqualTo(Duration.ofMillis(7)));
    }

    @Test
    void customStringGeneratorMakesDefaultBackOff() {
        IdGenerator custom = () -> "custom";

        contextRunner.withBean(IdGenerator.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IdGenerator.class)).isSameAs(custom);
                });
    }

    @Test
    void customLongGeneratorMakesDefaultBackOff() {
        LongIdGenerator custom = new FixedLongIdGenerator();

        contextRunner.withBean(LongIdGenerator.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(LongIdGenerator.class)).isSameAs(custom);
                });
    }

    @Test
    void bootMetadataRegistersAutoConfigurationWithoutComponentScanning() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
                .contains(IdGeneratorAutoConfiguration.class.getName());
        assertNull(IdGeneratorAutoConfiguration.class.getAnnotation(ComponentScan.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(IdGeneratorProperties.class)
    static class PropertiesBindingConfiguration {
    }

    /** {@link LongIdGenerator} is no longer a functional interface, so the seam needs a named implementation. */
    private static final class FixedLongIdGenerator implements LongIdGenerator {

        @Override
        public long nextLongId() {
            return 42L;
        }

        @Override
        public String nextId() {
            return Long.toString(nextLongId());
        }
    }
}
