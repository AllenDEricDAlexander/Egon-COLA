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
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiModelPort;
import top.egon.cola.component.tianshu.admin.service.ai.model.YuhengLlmModelAdapter;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;

import java.net.http.HttpClient;
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

    @Bean(name = "ddcAiOptimizationExecutor", destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = CHANNEL_PREFIX, name = "enabled", havingValue = "true")
    public ExecutorService ddcAiOptimizationExecutor(@Qualifier(PROPERTIES_BEAN) DdcAiOptimizationProperties properties) {
        return Executors.newFixedThreadPool(properties.getOptimization().getMaxConcurrentRuns(),
                new CustomizableThreadFactory("tianshu-ai-opt-"));
    }
}
