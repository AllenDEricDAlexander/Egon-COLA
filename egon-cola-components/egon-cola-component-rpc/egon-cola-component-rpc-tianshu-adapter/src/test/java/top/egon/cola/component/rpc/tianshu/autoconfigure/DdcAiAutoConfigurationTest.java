package top.egon.cola.component.rpc.tianshu.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientHandle;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory;
import top.egon.cola.component.tianshu.api.client.DdcAiFeedbackClient;
import top.egon.cola.component.tianshu.api.client.DdcAiSyncClient;
import top.egon.cola.component.tianshu.api.client.DdcPromptClient;
import top.egon.cola.component.tianshu.api.client.DdcSkillClient;
import top.egon.cola.component.tianshu.autoconfigure.DdcAiResourceAutoConfiguration;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcAiResourceProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class DdcAiAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    DdcRpcAutoConfiguration.class,
                    DdcAiResourceAutoConfiguration.class))
            .withPropertyValues(
                    "egon.cola.component.tianshu.registration-resource-uri=https://api.example/tianshu")
            .withBean(IdpServiceOAuth2Client.class, () -> mock(IdpServiceOAuth2Client.class));

    @Test
    void bothCapabilitiesDefaultToDisabledAndDoNotNeedAnRpcTarget() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DdcAiResourceProperties.class);
            assertThat(context.getBean(DdcAiResourceProperties.class).getPrompt().isEnabled()).isFalse();
            assertThat(context.getBean(DdcAiResourceProperties.class).getSkill().isEnabled()).isFalse();
            assertThat(context).doesNotHaveBean(DdcPromptClient.class);
            assertThat(context).doesNotHaveBean(DdcSkillClient.class);
            assertThat(context).doesNotHaveBean(DdcRpcClientHandle.class);
        });
    }

    @Test
    void eachCustomPortCanBeEnabledIndependentlyWithoutCreatingASecondTransport() {
        runner.withUserConfiguration(CustomPromptPort.class)
                .withPropertyValues(
                        "egon.cola.component.tianshu.enabled=false",
                        "egon.cola.component.tianshu.prompt.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DdcPromptClient.class);
                    assertThat(context).doesNotHaveBean(DdcSkillClient.class);
                    assertThat(context).doesNotHaveBean(DdcRpcClientHandle.class);
                });

        runner.withUserConfiguration(CustomSkillPort.class)
                .withPropertyValues(
                        "egon.cola.component.tianshu.enabled=false",
                        "egon.cola.component.tianshu.skill.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(DdcPromptClient.class);
                    assertThat(context).hasSingleBean(DdcSkillClient.class);
                    assertThat(context).doesNotHaveBean(DdcRpcClientHandle.class);
                });
    }

    @Test
    void bothCustomPortsOverrideTheDefaultAdaptersWhenBothSwitchesAreEnabled() {
        runner.withUserConfiguration(CustomPorts.class)
                .withPropertyValues(
                        "egon.cola.component.tianshu.prompt.enabled=true",
                        "egon.cola.component.tianshu.skill.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DdcPromptClient.class);
                    assertThat(context).hasSingleBean(DdcSkillClient.class);
                    assertThat(context).doesNotHaveBean(DdcRpcClientHandle.class);
                });
    }

    @Test
    void enabledDefaultAdapterRequiresTenantAndDirectRpcConfiguration() {
        runner.withPropertyValues(
                        "egon.cola.component.tianshu.prompt.enabled=true",
                        "egon.cola.component.tianshu.rpc.auth.runtime.access-key=runtime-ak",
                        "egon.cola.component.tianshu.rpc.auth.runtime.secret-key=runtime-sk")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "egon.cola.component.tianshu.tenant-id must be a positive Long");
                });

        runner.withPropertyValues(
                        "egon.cola.component.tianshu.prompt.enabled=true",
                        "egon.cola.component.tianshu.tenant-id=101",
                        "egon.cola.component.tianshu.rpc.auth.runtime.access-key=runtime-ak",
                        "egon.cola.component.tianshu.rpc.auth.runtime.secret-key=runtime-sk")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "egon.cola.component.tianshu.rpc.target is required");
                });
    }

    @Test
    void enabledPortFailsWhenTheDirectRpcAdapterAndCustomPortAreBothAbsent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DdcAiResourceAutoConfiguration.class))
                .withPropertyValues("egon.cola.component.tianshu.prompt.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "egon.cola.component.tianshu.prompt.enabled requires a DdcPromptClient adapter");
                });
    }

    @Test
    void enabledPromptAndSkillCreateOnlyTheirOwnLazyRpcHandles() {
        DdcRpcClientFactory factory = mock(DdcRpcClientFactory.class);
        DdcPromptClient promptClient = mock(DdcPromptClient.class);
        DdcSkillClient skillClient = mock(DdcSkillClient.class);
        when(factory.promptClient()).thenReturn(new DdcRpcClientHandle<>(promptClient, () -> { }));
        when(factory.skillClient()).thenReturn(new DdcRpcClientHandle<>(skillClient, () -> { }));

        runner.withBean(DdcRpcClientFactory.class, () -> factory)
                .withPropertyValues(
                        "egon.cola.component.tianshu.prompt.enabled=true",
                        "egon.cola.component.tianshu.skill.enabled=true",
                        "egon.cola.component.tianshu.biz-code=retail",
                        "egon.cola.component.tianshu.env=prod",
                        "egon.cola.component.tianshu.app-code=assistant-app")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DdcPromptClient.class);
                    assertThat(context).hasSingleBean(DdcSkillClient.class);
                    assertThat(context.getBeansOfType(DdcRpcClientHandle.class)).hasSize(2);
                });
        verify(factory).promptClient();
        verify(factory).skillClient();
    }

    @Test
    void feedbackAndSyncClientsAreAbsentByDefault() {
        DdcRpcClientFactory factory = mock(DdcRpcClientFactory.class);

        runner.withBean(DdcRpcClientFactory.class, () -> factory).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DdcAiFeedbackClient.class);
            assertThat(context).doesNotHaveBean(DdcAiSyncClient.class);
        });
        verify(factory, never()).feedbackClient();
        verify(factory, never()).syncClient();
    }

    @Test
    void feedbackClientIsCreatedOnlyWhenFeedbackIsEnabled() {
        DdcRpcClientFactory factory = mock(DdcRpcClientFactory.class);
        DdcAiFeedbackClient feedbackClient = mock(DdcAiFeedbackClient.class);
        when(factory.feedbackClient()).thenReturn(new DdcRpcClientHandle<>(feedbackClient, () -> { }));

        runner.withBean(DdcRpcClientFactory.class, () -> factory)
                .withPropertyValues("egon.cola.component.tianshu.ai.feedback.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("ddcAiFeedbackRpcClient");
                    assertThat(context.getBean(DdcAiFeedbackClient.class)).isSameAs(feedbackClient);
                    assertThat(context).doesNotHaveBean(DdcAiSyncClient.class);
                });
        verify(factory).feedbackClient();
        verify(factory, never()).syncClient();
    }

    @Test
    void syncClientIsCreatedWhenWorkspaceOrAuthoringIsEnabled() {
        for (String property : new String[] {
                "egon.cola.component.tianshu.ai.skill.workspace.enabled=true",
                "egon.cola.component.tianshu.ai.skill.authoring.enabled=true"}) {
            DdcRpcClientFactory factory = mock(DdcRpcClientFactory.class);
            DdcAiSyncClient syncClient = mock(DdcAiSyncClient.class);
            when(factory.syncClient()).thenReturn(new DdcRpcClientHandle<>(syncClient, () -> { }));

            runner.withBean(DdcRpcClientFactory.class, () -> factory)
                    .withPropertyValues(property)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("ddcAiSyncRpcClient");
                        assertThat(context.getBean(DdcAiSyncClient.class)).isSameAs(syncClient);
                        assertThat(context).doesNotHaveBean(DdcAiFeedbackClient.class);
                    });
            verify(factory).syncClient();
        }
    }

    @Test
    void customFeedbackAndSyncPortsSuppressTheDefaultAdapters() {
        DdcRpcClientFactory factory = mock(DdcRpcClientFactory.class);

        runner.withBean(DdcRpcClientFactory.class, () -> factory)
                .withUserConfiguration(CustomSyncPorts.class)
                .withPropertyValues(
                        "egon.cola.component.tianshu.ai.feedback.enabled=true",
                        "egon.cola.component.tianshu.ai.skill.workspace.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DdcAiFeedbackClient.class);
                    assertThat(context).hasSingleBean(DdcAiSyncClient.class);
                    assertThat(context).doesNotHaveBean("ddcAiFeedbackRpcClientHandle");
                    assertThat(context).doesNotHaveBean("ddcAiSyncRpcClientHandle");
                });
        verify(factory, never()).feedbackClient();
        verify(factory, never()).syncClient();
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSyncPorts {
        @Bean
        DdcAiFeedbackClient customDdcAiFeedbackClient() {
            return mock(DdcAiFeedbackClient.class);
        }

        @Bean
        DdcAiSyncClient customDdcAiSyncClient() {
            return mock(DdcAiSyncClient.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPromptPort {
        @Bean
        DdcPromptClient customDdcPromptClient() {
            return mock(DdcPromptClient.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSkillPort {
        @Bean
        DdcSkillClient customDdcSkillClient() {
            return mock(DdcSkillClient.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPorts {
        @Bean
        DdcPromptClient customDdcPromptClient() {
            return mock(DdcPromptClient.class);
        }

        @Bean
        DdcSkillClient customDdcSkillClient() {
            return mock(DdcSkillClient.class);
        }
    }
}
