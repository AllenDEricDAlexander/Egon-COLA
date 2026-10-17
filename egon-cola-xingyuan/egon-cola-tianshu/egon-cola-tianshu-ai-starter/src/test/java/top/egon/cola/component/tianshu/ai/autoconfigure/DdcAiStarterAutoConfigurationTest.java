package top.egon.cola.component.tianshu.ai.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.tianshu.ai.feedback.DdcAiUsageReporter;
import top.egon.cola.component.tianshu.ai.feedback.DefaultDdcAiUsageReporter;
import top.egon.cola.component.tianshu.ai.observability.DdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.observability.MicrometerDdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.workspace.DdcAiSkillWorkspace;
import top.egon.cola.component.tianshu.api.client.DdcAiFeedbackClient;
import top.egon.cola.component.tianshu.api.client.DdcAiSyncClient;
import top.egon.cola.component.tianshu.api.client.DdcSkillClient;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;
import top.egon.cola.component.tianshu.redis.DdcRedisTopicSubscription;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @Test
    void workspaceEnabledRegistersWorkspaceServiceAndScheduler(@TempDir Path root) {
        workspaceRunner(root).run(context -> assertThat(context)
                .hasNotFailed()
                .hasBean("ddcAiSkillWorkspace")
                .hasBean("ddcAiWorkspaceReconcileScheduler")
                .hasSingleBean(DdcAiSkillWorkspace.class)
                .doesNotHaveBean("ddcAiPublicationSubscription"));
    }

    @Test
    void workspaceEnabledFailsWithoutSkillReadsOrSyncClient(@TempDir Path root) {
        runner.withPropertyValues("egon.cola.component.tianshu.ai.skill.workspace.enabled=true",
                        "egon.cola.component.tianshu.ai.skill.workspace.root=" + root)
                .run(context -> assertThat(context).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("requires egon.cola.component.tianshu.skill.enabled"));
        runner.withPropertyValues("egon.cola.component.tianshu.skill.enabled=true",
                        "egon.cola.component.tianshu.ai.skill.workspace.enabled=true",
                        "egon.cola.component.tianshu.ai.skill.workspace.root=" + root)
                .withBean(DdcSkillClient.class, () -> mock(DdcSkillClient.class))
                .run(context -> assertThat(context).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("DdcSkillClient and DdcAiSyncClient adapters"));
    }

    @Test
    void subscriptionEnabledRegistersListenerWithTenantTopic(@TempDir Path root) {
        RedissonClient redisson = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        String expected = DdcRedisKeys.aiTopic(42L, "xingyuan", "dev", "order");
        when(redisson.getTopic(expected, StringCodec.INSTANCE)).thenReturn(topic);

        workspaceRunner(root)
                .withPropertyValues("egon.cola.component.tianshu.ai.skill.workspace.subscription-enabled=true",
                        "egon.cola.component.tianshu.tenant-id=42",
                        "egon.cola.component.tianshu.biz-code=xingyuan",
                        "egon.cola.component.tianshu.env=dev",
                        "egon.cola.component.tianshu.app-code=order")
                .withBean("ddcRedissonClient", RedissonClient.class, () -> redisson)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasBean("ddcAiPublicationSubscription");
                    assertThat(context.getBean("ddcAiPublicationSubscription", DdcRedisTopicSubscription.class)
                            .isActive()).isTrue();
                    verify(topic).addListener(eq(String.class), any());
                });
    }

    @Test
    void subscriptionWithoutWorkspaceFailsFast() {
        runner.withPropertyValues("egon.cola.component.tianshu.ai.skill.workspace.subscription-enabled=true")
                .withBean("ddcRedissonClient", RedissonClient.class, () -> mock(RedissonClient.class))
                .run(context -> assertThat(context).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("requires egon.cola.component.tianshu.ai.skill.workspace.enabled"));
    }

    private ApplicationContextRunner workspaceRunner(Path root) {
        return runner.withPropertyValues("egon.cola.component.tianshu.skill.enabled=true",
                        "egon.cola.component.tianshu.ai.skill.workspace.enabled=true",
                        "egon.cola.component.tianshu.ai.skill.workspace.root=" + root,
                        "egon.cola.component.tianshu.ai.skill.workspace.reconcile-interval=1h")
                .withBean(DdcSkillClient.class, () -> mock(DdcSkillClient.class))
                .withBean(DdcAiSyncClient.class, () -> mock(DdcAiSyncClient.class));
    }
    @Test
    void authoringEnabledRegistersWatcherAndRejectsASharedRoot(@TempDir Path authoringRoot, @TempDir Path other) {
        runner.withPropertyValues("egon.cola.component.tianshu.ai.skill.authoring.enabled=true",
                        "egon.cola.component.tianshu.ai.skill.authoring.root=" + authoringRoot,
                        "egon.cola.component.tianshu.ai.skill.authoring.poll-interval=1h")
                .withBean(DdcAiSyncClient.class, () -> mock(DdcAiSyncClient.class))
                .run(context -> assertThat(context).hasNotFailed().hasBean("ddcAiSkillAuthoringWatcher")
                        .hasBean("ddcAiSkillTreeReader"));
        runner.withPropertyValues("egon.cola.component.tianshu.ai.skill.authoring.enabled=true",
                        "egon.cola.component.tianshu.ai.skill.authoring.root=" + authoringRoot,
                        "egon.cola.component.tianshu.ai.skill.workspace.root=" + authoringRoot)
                .withBean(DdcAiSyncClient.class, () -> mock(DdcAiSyncClient.class))
                .run(context -> assertThat(context).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("authoring root must differ from workspace root"));
        runner.withPropertyValues("egon.cola.component.tianshu.ai.skill.authoring.enabled=true",
                        "egon.cola.component.tianshu.ai.skill.authoring.root=" + other)
                .run(context -> assertThat(context).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("requires a DdcAiSyncClient adapter"));
    }
}
