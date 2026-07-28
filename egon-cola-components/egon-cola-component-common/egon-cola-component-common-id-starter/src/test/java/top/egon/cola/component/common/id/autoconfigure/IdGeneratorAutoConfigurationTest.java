package top.egon.cola.component.common.id.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import top.egon.cola.component.common.id.generator.IdGenerator;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class IdGeneratorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdGeneratorAutoConfiguration.class));

    @Test
    void createsOneGeneratorForValidConfiguration() {
        contextRunner.withPropertyValues("egon.cola.component.id.machine-id=17")
                .run(context -> assertThat(context)
                        .hasSingleBean(SnowflakeIdGenerator.class)
                        .hasSingleBean(LongIdGenerator.class)
                        .hasSingleBean(IdGenerator.class));
    }

    @Test
    void acceptsMachineIdBoundaries() {
        contextRunner.withPropertyValues("egon.cola.component.id.machine-id=0")
                .run(context -> assertThat(context).hasSingleBean(LongIdGenerator.class));
        contextRunner.withPropertyValues("egon.cola.component.id.machine-id=1023")
                .run(context -> assertThat(context).hasSingleBean(LongIdGenerator.class));
    }

    @Test
    void disabledConfigurationCreatesNoGenerator() {
        contextRunner.withPropertyValues("egon.cola.component.id.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(IdGenerator.class)
                        .doesNotHaveBean(LongIdGenerator.class));
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
        contextRunner.withPropertyValues("egon.cola.component.id.machine-id=1")
                .run(context -> assertThat(context.getBean(IdGeneratorProperties.class).getMaxClockBackward())
                        .isEqualTo(Duration.ofMillis(5)));
        contextRunner.withPropertyValues(
                        "egon.cola.component.id.machine-id=1",
                        "egon.cola.component.id.max-clock-backward=7ms")
                .run(context -> assertThat(context.getBean(IdGeneratorProperties.class).getMaxClockBackward())
                        .isEqualTo(Duration.ofMillis(7)));
    }

    @Test
    void customStringGeneratorMakesDefaultBackOff() {
        IdGenerator custom = () -> "custom";

        contextRunner.withBean(IdGenerator.class, () -> custom)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SnowflakeIdGenerator.class);
                    assertThat(context.getBean(IdGenerator.class)).isSameAs(custom);
                });
    }

    @Test
    void customLongGeneratorMakesDefaultBackOff() {
        LongIdGenerator custom = () -> 42L;

        contextRunner.withBean(LongIdGenerator.class, () -> custom)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SnowflakeIdGenerator.class);
                    assertThat(context.getBean(LongIdGenerator.class)).isSameAs(custom);
                });
    }

    @Test
    void bootMetadataRegistersAutoConfigurationWithoutComponentScanning() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
                .contains(IdGeneratorAutoConfiguration.class.getName());
        assertNull(IdGeneratorAutoConfiguration.class.getAnnotation(ComponentScan.class));
    }
}
