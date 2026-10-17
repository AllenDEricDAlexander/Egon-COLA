package top.egon.cola.component.tianshu.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationPolicyRepository;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiOptimizationService;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiJudgeContextLoader;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiJudgeStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiOptimizerStrategy;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcAiOptimizationConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DdcAiOptimizationConfiguration.class)
            .withBean("idpServiceOAuth2Client", IdpServiceOAuth2Client.class, () -> mock(IdpServiceOAuth2Client.class))
            .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new)
            .withBean("ddcPromptOptimizerStrategy", DdcAiOptimizerStrategy.class, () -> strategy(DdcAiKindEnum.PROMPT))
            .withBean("ddcSkillOptimizerStrategy", DdcAiOptimizerStrategy.class, () -> strategy(DdcAiKindEnum.SKILL))
            .withBean(DdcAiOptimizationConfiguration.TENANT_PROPERTIES_BEAN, DdcTenantAdminProperties.class,
                    DdcTenantAdminProperties::new)
            .withBean("ddcTenantRegistry", DdcTenantRegistry.class, () -> mock(DdcTenantRegistry.class))
            .withBean("ddcTenantContextExecutor", DdcTenantContextExecutor.class,
                    () -> mock(DdcTenantContextExecutor.class))
            .withBean("ddcAiFeedbackService", DdcAiFeedbackService.class, () -> mock(DdcAiFeedbackService.class))
            .withBean("ddcAiOptimizationService", DdcAiOptimizationService.class,
                    () -> mock(DdcAiOptimizationService.class))
            .withBean("ddcAiOptimizationPolicyRepository", DdcAiOptimizationPolicyRepository.class,
                    () -> mock(DdcAiOptimizationPolicyRepository.class))
            .withBean("ddcAiJudgeContextLoader", DdcAiJudgeContextLoader.class,
                    () -> mock(DdcAiJudgeContextLoader.class))
            .withBean("ddcDefaultAiJudgeStrategy", DdcAiJudgeStrategy.class, () -> mock(DdcAiJudgeStrategy.class))
            .withBean("ddcPublishClock", Clock.class, Clock::systemUTC);

    @Test
    void channelDisabledRegistersOnlyRetentionScannerAndRegistry() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean("ddcAiModelRestClient")
                .doesNotHaveBean("ddcAiModelPort")
                .doesNotHaveBean("ddcAiJudgeScanner")
                .doesNotHaveBean("ddcAiOptimizationScanner")
                .doesNotHaveBean("ddcAiOptimizationExecutor")
                .hasBean("ddcAiFeedbackRetentionScanner")
                .hasBean("ddcAiTenantJobRunner")
                .hasBean("ddcAiOptimizerStrategyRegistry"));
    }

    @Test
    void channelEnabledRegistersModelPortJudgeAndOptimizationScanners() {
        runner.withPropertyValues(
                        "egon.cola.component.tianshu.admin.ai.optimization.enabled=true",
                        "egon.cola.component.tianshu.admin.ai.model.base-url=http://127.0.0.1:1",
                        "egon.cola.component.tianshu.admin.ai.model.registration-id=r",
                        "egon.cola.component.tianshu.admin.ai.model.app-id=a",
                        "egon.cola.component.tianshu.admin.ai.model.resource-uri=https://yuheng.test/llm")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasBean("ddcAiModelPort")
                        .hasBean("ddcAiJudgeScanner")
                        .hasBean("ddcAiOptimizationScanner")
                        .hasBean("ddcAiOptimizationExecutor")
                        .hasBean("ddcAiFeedbackRetentionScanner")
                        .hasBean("ddcAiOptimizerStrategyRegistry"));
    }

    @Test
    void channelEnabledWithoutBaseUrlFailsFast() {
        runner.withPropertyValues("egon.cola.component.tianshu.admin.ai.optimization.enabled=true")
                .run(context -> assertThat(context.getStartupFailure())
                        .isNotNull()
                        .rootCause()
                        .hasMessageContaining("TIANSHU_AI_MODEL_BASE_URL_REQUIRED"));
    }

    @Test
    void instanceIdIsBoundedAndCarriesThePid() {
        String id = DdcAiOptimizationConfiguration.localInstanceId();

        assertThat(id).hasSizeLessThanOrEqualTo(128).endsWith(":" + ProcessHandle.current().pid());
    }

    private static DdcAiOptimizerStrategy strategy(DdcAiKindEnum kind) {
        DdcAiOptimizerStrategy strategy = mock(DdcAiOptimizerStrategy.class);
        when(strategy.kind()).thenReturn(kind);
        return strategy;
    }
}
