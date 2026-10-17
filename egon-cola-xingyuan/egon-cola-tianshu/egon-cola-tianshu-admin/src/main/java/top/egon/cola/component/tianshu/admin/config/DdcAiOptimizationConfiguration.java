package top.egon.cola.component.tianshu.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationPolicyRepository;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiModelPort;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiOptimizationService;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiJudgeContextLoader;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiJudgeStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiOptimizerStrategy;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.admin.service.ai.job.DdcAiFeedbackRetentionScanner;
import top.egon.cola.component.tianshu.admin.service.ai.job.DdcAiJudgeScanner;
import top.egon.cola.component.tianshu.admin.service.ai.job.DdcAiOptimizationScanner;
import top.egon.cola.component.tianshu.admin.service.ai.job.DdcAiTenantJobRunner;
import top.egon.cola.component.tianshu.admin.service.ai.model.YuhengLlmModelAdapter;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 反馈与优化通道装配；模型端口、执行器与作业仅在通道开启时创建。
 * / Wiring for the AI feedback and optimization channel; the model port, executor and jobs exist only when enabled.
 */
@Configuration(value = "ddcAiOptimizationConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(DdcAiOptimizationProperties.class)
public class DdcAiOptimizationConfiguration {

    static final String PROPERTIES_BEAN =
            "egon.cola.component.tianshu.admin.ai-top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties";

    static final String CHANNEL_PREFIX = "egon.cola.component.tianshu.admin.ai.optimization";

    static final String TENANT_PROPERTIES_BEAN =
            "egon.cola.component.tianshu.admin.tenancy-top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties";

    @Bean("ddcAiModelRestClient")
    @ConditionalOnProperty(prefix = CHANNEL_PREFIX, name = "enabled", havingValue = "true")
    public RestClient ddcAiModelRestClient(@Qualifier(PROPERTIES_BEAN) DdcAiOptimizationProperties properties) {
        DdcAiOptimizationProperties.Model model = properties.getModel();
        if (model.getBaseUrl() == null || model.getBaseUrl().isBlank()) {
            throw new IllegalStateException("TIANSHU_AI_MODEL_BASE_URL_REQUIRED");
        }
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(model.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(model.getReadTimeout());
        return RestClient.builder().baseUrl(model.getBaseUrl()).requestFactory(factory).build();
    }

    @Bean("ddcAiModelPort")
    @ConditionalOnProperty(prefix = CHANNEL_PREFIX, name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(DdcAiModelPort.class)
    public DdcAiModelPort ddcAiModelPort(
            @Qualifier("ddcAiModelRestClient") RestClient client,
            @Qualifier("idpServiceOAuth2Client") IdpServiceOAuth2Client oauth2Client,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper,
            @Qualifier(PROPERTIES_BEAN) DdcAiOptimizationProperties properties) {
        return new YuhengLlmModelAdapter(client, oauth2Client, objectMapper, properties.getModel());
    }

    @Bean("ddcAiOptimizerStrategyRegistry")
    public Map<DdcAiKindEnum, DdcAiOptimizerStrategy> ddcAiOptimizerStrategyRegistry(
            @Qualifier("ddcPromptOptimizerStrategy") DdcAiOptimizerStrategy promptStrategy,
            @Qualifier("ddcSkillOptimizerStrategy") DdcAiOptimizerStrategy skillStrategy) {
        EnumMap<DdcAiKindEnum, DdcAiOptimizerStrategy> strategies = new EnumMap<>(DdcAiKindEnum.class);
        for (DdcAiOptimizerStrategy strategy : List.of(promptStrategy, skillStrategy)) {
            if (strategies.putIfAbsent(strategy.kind(), strategy) != null) {
                throw new IllegalStateException("AI_OPTIMIZER_STRATEGY_DUPLICATE:" + strategy.kind());
            }
        }
        if (!strategies.keySet().equals(Set.of(DdcAiKindEnum.PROMPT, DdcAiKindEnum.SKILL))) {
            throw new IllegalStateException("AI_OPTIMIZER_STRATEGY_REGISTRATION_INCOMPLETE");
        }
        return Collections.unmodifiableMap(strategies);
    }

    @Bean("ddcAiTenantJobRunner")
    public DdcAiTenantJobRunner ddcAiTenantJobRunner(
            @Qualifier(TENANT_PROPERTIES_BEAN) DdcTenantAdminProperties tenantProperties,
            @Qualifier("ddcTenantRegistry") DdcTenantRegistry tenantRegistry,
            @Qualifier("ddcTenantContextExecutor") DdcTenantContextExecutor tenantContextExecutor) {
        return new DdcAiTenantJobRunner(tenantProperties, tenantRegistry, tenantContextExecutor);
    }

    @Bean("ddcAiJudgeScanner")
    @ConditionalOnProperty(prefix = CHANNEL_PREFIX, name = "enabled", havingValue = "true")
    public DdcAiJudgeScanner ddcAiJudgeScanner(
            @Qualifier("ddcAiFeedbackService") DdcAiFeedbackService feedbackService,
            @Qualifier("ddcAiOptimizationPolicyRepository") DdcAiOptimizationPolicyRepository policyRepository,
            @Qualifier("ddcAiJudgeContextLoader") DdcAiJudgeContextLoader contextLoader,
            @Qualifier("ddcDefaultAiJudgeStrategy") DdcAiJudgeStrategy strategy,
            @Qualifier("ddcAiModelPort") DdcAiModelPort modelPort,
            @Qualifier("ddcAiTenantJobRunner") DdcAiTenantJobRunner tenantJobRunner,
            @Qualifier(PROPERTIES_BEAN) DdcAiOptimizationProperties properties) {
        return new DdcAiJudgeScanner(feedbackService, policyRepository, contextLoader, strategy, modelPort,
                tenantJobRunner, properties);
    }

    @Bean(name = "ddcAiOptimizationExecutor", destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = CHANNEL_PREFIX, name = "enabled", havingValue = "true")
    public ExecutorService ddcAiOptimizationExecutor(@Qualifier(PROPERTIES_BEAN) DdcAiOptimizationProperties properties) {
        return Executors.newFixedThreadPool(properties.getOptimization().getMaxConcurrentRuns(),
                new CustomizableThreadFactory("tianshu-ai-opt-"));
    }

    @Bean("ddcAiOptimizationScanner")
    @ConditionalOnProperty(prefix = CHANNEL_PREFIX, name = "enabled", havingValue = "true")
    public DdcAiOptimizationScanner ddcAiOptimizationScanner(
            @Qualifier("ddcAiOptimizationService") DdcAiOptimizationService optimizationService,
            @Qualifier("ddcAiOptimizationExecutor") ExecutorService executor,
            @Qualifier("ddcAiTenantJobRunner") DdcAiTenantJobRunner tenantJobRunner,
            @Qualifier(PROPERTIES_BEAN) DdcAiOptimizationProperties properties,
            @Qualifier("ddcPublishClock") Clock clock) {
        return new DdcAiOptimizationScanner(optimizationService, executor, tenantJobRunner,
                properties.getOptimization().getMaxConcurrentRuns(), localInstanceId(), clock);
    }

    @Bean("ddcAiFeedbackRetentionScanner")
    public DdcAiFeedbackRetentionScanner ddcAiFeedbackRetentionScanner(
            @Qualifier("ddcAiFeedbackService") DdcAiFeedbackService feedbackService,
            @Qualifier("ddcAiTenantJobRunner") DdcAiTenantJobRunner tenantJobRunner) {
        return new DdcAiFeedbackRetentionScanner(feedbackService, tenantJobRunner);
    }

    /** 运行租约持有者：主机名加进程号，最长 128 字符。 / Run lease owner: host name plus PID, at most 128 chars. */
    static String localInstanceId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException | RuntimeException unavailable) {
            host = "unknown-host";
        }
        String id = host + ":" + ProcessHandle.current().pid();
        return id.length() <= 128 ? id : id.substring(id.length() - 128);
    }
}
