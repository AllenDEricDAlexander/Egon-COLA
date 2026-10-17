package top.egon.cola.component.tianshu.ai.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import top.egon.cola.component.tianshu.ai.authoring.DdcAiSkillAuthoringWatcher;
import top.egon.cola.component.tianshu.ai.authoring.DdcAiSkillTreeReader;
import top.egon.cola.component.tianshu.ai.feedback.DdcAiUsageReporter;
import top.egon.cola.component.tianshu.ai.feedback.DefaultDdcAiUsageReporter;
import top.egon.cola.component.tianshu.ai.observability.DdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.observability.MicrometerDdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.properties.DdcAiAuthoringProperties;
import top.egon.cola.component.tianshu.ai.properties.DdcAiStarterProperties;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWatchedResourceDTO;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWorkspaceProperties;
import top.egon.cola.component.tianshu.ai.workspace.DdcAiPublicationListener;
import top.egon.cola.component.tianshu.ai.workspace.DdcAiSkillMaterializer;
import top.egon.cola.component.tianshu.ai.workspace.DdcAiSkillWorkspaceService;
import top.egon.cola.component.tianshu.ai.workspace.DdcAiWorkspaceReconcileScheduler;
import top.egon.cola.component.tianshu.api.client.DdcAiFeedbackClient;
import top.egon.cola.component.tianshu.api.client.DdcAiSyncClient;
import top.egon.cola.component.tianshu.api.client.DdcSkillClient;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcAiResourceProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;
import top.egon.cola.component.tianshu.redis.DdcRedisTopicSubscription;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
@EnableConfigurationProperties({DdcAiStarterProperties.class, DdcAiResourceProperties.class, DdcProperties.class,
        DdcTenantProperties.class})
public class DdcAiStarterAutoConfiguration {

    static final String FEEDBACK_PREFIX = DdcAiStarterProperties.PREFIX + ".feedback";

    static final String WORKSPACE_PREFIX = DdcAiStarterProperties.PREFIX + ".skill.workspace";

    static final String AUTHORING_PREFIX = DdcAiStarterProperties.PREFIX + ".skill.authoring";

    static final String SCOPE_PROPERTIES_BEAN =
            "egon.cola.component.tianshu-top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties";

    static final String TENANT_PROPERTIES_BEAN =
            "egon.cola.component.tianshu-top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties";

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

    @Bean("ddcAiSkillMaterializer")
    @ConditionalOnProperty(prefix = WORKSPACE_PREFIX, name = "enabled", havingValue = "true")
    public DdcAiSkillMaterializer ddcAiSkillMaterializer(ObjectProvider<ObjectMapper> objectMapper) {
        return new DdcAiSkillMaterializer(stateMapper(objectMapper), Clock.systemUTC());
    }

    @Bean(name = "ddcAiWorkspaceExecutor", destroyMethod = "shutdownNow")
    @ConditionalOnProperty(prefix = WORKSPACE_PREFIX, name = "enabled", havingValue = "true")
    public ScheduledExecutorService ddcAiWorkspaceExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("tianshu-ai-workspace-"));
    }

    @Bean("ddcAiSkillWorkspace")
    @ConditionalOnProperty(prefix = WORKSPACE_PREFIX, name = "enabled", havingValue = "true")
    public DdcAiSkillWorkspaceService ddcAiSkillWorkspace(
            @Qualifier(PROPERTIES_BEAN) DdcAiStarterProperties properties,
            @Qualifier(RESOURCE_PROPERTIES_BEAN) DdcAiResourceProperties resourceProperties,
            ObjectProvider<DdcSkillClient> skillClient,
            ObjectProvider<DdcAiSyncClient> syncClient,
            @Qualifier("ddcAiSkillMaterializer") DdcAiSkillMaterializer materializer,
            @Qualifier("ddcAiStarterMetrics") DdcAiStarterMetrics metrics) {
        DdcAiWorkspaceProperties workspace = properties.getSkill().getWorkspace();
        if (!resourceProperties.getSkill().isEnabled()) {
            throw new IllegalStateException("egon.cola.component.tianshu.ai.skill.workspace.enabled requires "
                    + "egon.cola.component.tianshu.skill.enabled");
        }
        requireWritableRoot(workspace.getRoot(), "egon.cola.component.tianshu.ai.skill.workspace.root");
        DdcSkillClient skills = skillClient.getIfAvailable();
        DdcAiSyncClient sync = syncClient.getIfAvailable();
        if (skills == null || sync == null) {
            throw new IllegalStateException("egon.cola.component.tianshu.ai.skill.workspace.enabled requires "
                    + "DdcSkillClient and DdcAiSyncClient adapters");
        }
        return new DdcAiSkillWorkspaceService(skills, sync, materializer, workspace, metrics);
    }

    @Bean(name = "ddcAiWorkspaceReconcileScheduler", initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(prefix = WORKSPACE_PREFIX, name = "enabled", havingValue = "true")
    public DdcAiWorkspaceReconcileScheduler ddcAiWorkspaceReconcileScheduler(
            @Qualifier("ddcAiSkillWorkspace") DdcAiSkillWorkspaceService workspace,
            @Qualifier(PROPERTIES_BEAN) DdcAiStarterProperties properties,
            @Qualifier("ddcAiWorkspaceExecutor") ScheduledExecutorService executor) {
        return new DdcAiWorkspaceReconcileScheduler(workspace, properties.getSkill().getWorkspace(), executor);
    }

    @Bean(name = "ddcAiPublicationSubscription", destroyMethod = "close")
    @ConditionalOnProperty(prefix = WORKSPACE_PREFIX, name = "subscription-enabled", havingValue = "true")
    public DdcRedisTopicSubscription<String> ddcAiPublicationSubscription(
            @Qualifier("ddcRedissonClient") ObjectProvider<RedissonClient> redissonClient,
            @Qualifier(SCOPE_PROPERTIES_BEAN) DdcProperties scope,
            @Qualifier(TENANT_PROPERTIES_BEAN) DdcTenantProperties tenantProperties,
            @Qualifier(PROPERTIES_BEAN) DdcAiStarterProperties properties,
            @Qualifier("ddcAiSkillWorkspace") ObjectProvider<DdcAiSkillWorkspaceService> workspace,
            @Qualifier("ddcAiWorkspaceExecutor") ObjectProvider<ScheduledExecutorService> executor,
            ObjectProvider<ObjectMapper> objectMapper) {
        DdcAiSkillWorkspaceService service = workspace.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("egon.cola.component.tianshu.ai.skill.workspace.subscription-enabled "
                    + "requires egon.cola.component.tianshu.ai.skill.workspace.enabled");
        }
        RedissonClient redisson = redissonClient.getIfAvailable();
        if (redisson == null) {
            throw new IllegalStateException("egon.cola.component.tianshu.ai.skill.workspace.subscription-enabled "
                    + "requires the ddcRedissonClient from egon.cola.component.tianshu.redis");
        }
        String topic = DdcRedisKeys.aiTopic(tenantProperties.requireTenantId(), scope.getBizCode(), scope.getEnv(),
                scope.getAppCode());
        DdcAiPublicationListener listener = new DdcAiPublicationListener(stateMapper(objectMapper), tenantProperties,
                service, watchIndex(properties.getSkill().getWorkspace().getResources()), executor.getObject());
        return new DdcRedisTopicSubscription<>(List.of(redisson.getTopic(topic, StringCodec.INSTANCE)), String.class,
                listener);
    }

    @Bean("ddcAiSkillTreeReader")
    @ConditionalOnProperty(prefix = AUTHORING_PREFIX, name = "enabled", havingValue = "true")
    public DdcAiSkillTreeReader ddcAiSkillTreeReader() {
        return new DdcAiSkillTreeReader();
    }

    @Bean(name = "ddcAiAuthoringExecutor", destroyMethod = "shutdownNow")
    @ConditionalOnProperty(prefix = AUTHORING_PREFIX, name = "enabled", havingValue = "true")
    public ScheduledExecutorService ddcAiAuthoringExecutor() {
        return Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("tianshu-ai-authoring-"));
    }

    @Bean(name = "ddcAiSkillAuthoringWatcher", initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(prefix = AUTHORING_PREFIX, name = "enabled", havingValue = "true")
    public DdcAiSkillAuthoringWatcher ddcAiSkillAuthoringWatcher(
            ObjectProvider<DdcAiSyncClient> syncClient,
            @Qualifier(PROPERTIES_BEAN) DdcAiStarterProperties properties,
            @Qualifier("ddcAiSkillTreeReader") DdcAiSkillTreeReader reader,
            @Qualifier("ddcAiStarterMetrics") DdcAiStarterMetrics metrics,
            @Qualifier("ddcAiAuthoringExecutor") ScheduledExecutorService executor,
            ObjectProvider<ObjectMapper> objectMapper) {
        DdcAiAuthoringProperties authoring = properties.getSkill().getAuthoring();
        DdcAiWorkspaceProperties workspace = properties.getSkill().getWorkspace();
        if (authoring.getRoot() == null || !Files.isDirectory(authoring.getRoot())) {
            throw new IllegalStateException(
                    "egon.cola.component.tianshu.ai.skill.authoring.root must be an existing directory");
        }
        if (workspace.getRoot() != null && workspace.getRoot().toAbsolutePath().normalize()
                .equals(authoring.getRoot().toAbsolutePath().normalize())) {
            throw new IllegalStateException("tianshu ai authoring root must differ from workspace root");
        }
        watchIndex(authoring.getResources());
        DdcAiSyncClient sync = syncClient.getIfAvailable();
        if (sync == null) {
            throw new IllegalStateException(
                    "egon.cola.component.tianshu.ai.skill.authoring.enabled requires a DdcAiSyncClient adapter");
        }
        return new DdcAiSkillAuthoringWatcher(sync, reader, authoring, stateMapper(objectMapper), metrics,
                Clock.systemUTC(), executor);
    }

    static Map<String, DdcAiWatchedResourceDTO> watchIndex(List<DdcAiWatchedResourceDTO> resources) {
        Map<String, DdcAiWatchedResourceDTO> index = new LinkedHashMap<>();
        for (DdcAiWatchedResourceDTO resource : resources) {
            if (index.putIfAbsent(resource.watchKey(), resource) != null) {
                throw new IllegalStateException("Duplicate tianshu ai watched resource: " + resource.watchKey());
            }
        }
        return index;
    }

    static void requireWritableRoot(Path root, String property) {
        if (root == null) throw new IllegalStateException(property + " is required");
        try {
            Files.createDirectories(root);
        } catch (IOException failure) {
            throw new IllegalStateException(property + " cannot be created: " + root, failure);
        }
        if (!Files.isWritable(root)) throw new IllegalStateException(property + " is not writable: " + root);
    }

    private static ObjectMapper stateMapper(ObjectProvider<ObjectMapper> objectMapper) {
        ObjectMapper base = objectMapper.getIfAvailable(ObjectMapper::new);
        return base.copy().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
