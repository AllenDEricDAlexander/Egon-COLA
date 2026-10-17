package top.egon.cola.component.tianshu.ai.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import top.egon.cola.component.tianshu.ai.feedback.DdcAiUsageReporter;
import top.egon.cola.component.tianshu.ai.feedback.DefaultDdcAiUsageReporter;
import top.egon.cola.component.tianshu.ai.observability.DdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.observability.MicrometerDdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.properties.DdcAiStarterProperties;
import top.egon.cola.component.tianshu.api.client.DdcAiFeedbackClient;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcAiResourceProperties;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 下游 AI Starter 自动配置：各能力独立开关，开启但缺少前置条件时启动失败。
 * / Auto-configuration of the downstream AI starter: every capability has its own switch and fails startup when
 * enabled without its prerequisites.
 */
@AutoConfiguration(afterName = {
        "top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcAutoConfiguration",
        "top.egon.cola.component.tianshu.autoconfigure.DdcAiResourceAutoConfiguration",
        "top.egon.cola.component.tianshu.autoconfigure.DdcRedisAutoConfiguration"
})
@EnableConfigurationProperties({DdcAiStarterProperties.class, DdcAiResourceProperties.class})
public class DdcAiStarterAutoConfiguration {

    static final String FEEDBACK_PREFIX = DdcAiStarterProperties.PREFIX + ".feedback";

    static final String PROPERTIES_BEAN = DdcAiStarterProperties.PREFIX
            + "-top.egon.cola.component.tianshu.ai.properties.DdcAiStarterProperties";

    static final String RESOURCE_PROPERTIES_BEAN =
            "egon.cola.component.tianshu-top.egon.cola.component.tianshu.autoconfigure.properties.DdcAiResourceProperties";

    @Bean("ddcAiStarterMetrics")
    @ConditionalOnMissingBean(DdcAiStarterMetrics.class)
    public DdcAiStarterMetrics ddcAiStarterMetrics() {
        return DdcAiStarterMetrics.NOOP;
    }

    @Bean(name = "ddcAiFeedbackScheduler", destroyMethod = "shutdownNow")
    @ConditionalOnProperty(prefix = FEEDBACK_PREFIX, name = "enabled", havingValue = "true")
    public ScheduledExecutorService ddcAiFeedbackScheduler() {
        return Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("tianshu-ai-feedback-"));
    }

    @Bean(name = "ddcAiUsageReporter", initMethod = "start", destroyMethod = "close")
    @ConditionalOnProperty(prefix = FEEDBACK_PREFIX, name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(DdcAiUsageReporter.class)
    public DefaultDdcAiUsageReporter ddcAiUsageReporter(
            ObjectProvider<DdcAiFeedbackClient> feedbackClient,
            @Qualifier(PROPERTIES_BEAN) DdcAiStarterProperties properties,
            @Qualifier(RESOURCE_PROPERTIES_BEAN) DdcAiResourceProperties resourceProperties,
            @Qualifier("ddcAiStarterMetrics") DdcAiStarterMetrics metrics,
            ObjectProvider<Validator> validator,
            @Qualifier("ddcAiFeedbackScheduler") ScheduledExecutorService scheduler) {
        if (!resourceProperties.getPrompt().isEnabled() && !resourceProperties.getSkill().isEnabled()) {
            throw new IllegalStateException("egon.cola.component.tianshu.ai.feedback.enabled requires "
                    + "egon.cola.component.tianshu.prompt.enabled or egon.cola.component.tianshu.skill.enabled");
        }
        DdcAiFeedbackClient client = feedbackClient.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException(
                    "egon.cola.component.tianshu.ai.feedback.enabled requires a DdcAiFeedbackClient adapter");
        }
        return new DefaultDdcAiUsageReporter(client, properties.getFeedback(), metrics,
                validator.getIfAvailable(() -> Validation.buildDefaultValidatorFactory().getValidator()), scheduler,
                DefaultDdcAiUsageReporter.Sleeper.THREAD);
    }

    /** 宿主存在 Micrometer 时以 MeterRegistry 记录计数。 / Records counters on the MeterRegistry when present. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    static class MicrometerMetricsConfiguration {

        @Bean("ddcAiStarterMetrics")
        @ConditionalOnMissingBean(DdcAiStarterMetrics.class)
        DdcAiStarterMetrics ddcAiStarterMetrics(ObjectProvider<MeterRegistry> registry) {
            return new MicrometerDdcAiStarterMetrics(registry);
        }
    }
}
