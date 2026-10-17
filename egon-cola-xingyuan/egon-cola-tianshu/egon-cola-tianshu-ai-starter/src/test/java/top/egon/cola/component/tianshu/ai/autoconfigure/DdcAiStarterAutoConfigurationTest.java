package top.egon.cola.component.tianshu.ai.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.tianshu.ai.feedback.DdcAiUsageReporter;
import top.egon.cola.component.tianshu.ai.feedback.DefaultDdcAiUsageReporter;
import top.egon.cola.component.tianshu.ai.observability.DdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.observability.MicrometerDdcAiStarterMetrics;
import top.egon.cola.component.tianshu.api.client.DdcAiFeedbackClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DdcAiStarterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DdcAiStarterAutoConfiguration.class));

    @Test
    void feedbackDisabledByDefaultRegistersNothing() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(DdcAiUsageReporter.class)
                .doesNotHaveBean("ddcAiFeedbackScheduler")
                .hasSingleBean(DdcAiStarterMetrics.class));
    }

    @Test
    void feedbackEnabledRegistersReporterWhenPromptEnabled() {
        runner.withPropertyValues("egon.cola.component.tianshu.prompt.enabled=true",
                        "egon.cola.component.tianshu.ai.feedback.enabled=true")
                .withBean(DdcAiFeedbackClient.class, () -> mock(DdcAiFeedbackClient.class))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasBean("ddcAiUsageReporter");
                    assertThat(context.getBean(DdcAiUsageReporter.class)).isInstanceOf(DefaultDdcAiUsageReporter.class);
                });
    }

    @Test
    void feedbackEnabledFailsWithoutFeedbackClient() {
        runner.withPropertyValues("egon.cola.component.tianshu.skill.enabled=true",
                        "egon.cola.component.tianshu.ai.feedback.enabled=true")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause().hasMessageContaining("requires a DdcAiFeedbackClient adapter"));
    }

    @Test
    void feedbackEnabledFailsWhenNeitherPromptNorSkillEnabled() {
        runner.withPropertyValues("egon.cola.component.tianshu.ai.feedback.enabled=true")
                .withBean(DdcAiFeedbackClient.class, () -> mock(DdcAiFeedbackClient.class))
                .run(context -> assertThat(context).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("requires egon.cola.component.tianshu.prompt.enabled or "
                                + "egon.cola.component.tianshu.skill.enabled"));
    }

    @Test
    void metricsBindToMicrometerWhenPresentAndFallBackToNoopWithoutIt() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new).run(context -> {
            assertThat(context.getBean(DdcAiStarterMetrics.class)).isInstanceOf(MicrometerDdcAiStarterMetrics.class);
            context.getBean(DdcAiStarterMetrics.class).increment("tianshu.ai.feedback.dropped", 2, "reason", "QUEUE_FULL");
            assertThat(context.getBean(MeterRegistry.class).counter("tianshu.ai.feedback.dropped", "reason",
                    "QUEUE_FULL").count()).isEqualTo(2.0);
        });
        runner.withClassLoader(new FilteredClassLoader("io.micrometer.core"))
                .run(context -> assertThat(context.getBean(DdcAiStarterMetrics.class))
                        .isSameAs(DdcAiStarterMetrics.NOOP));
    }
}
