package top.egon.cola.component.yuheng.admin.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.tianshu.api.client.DdcManagementClient;
import top.egon.cola.component.yuheng.admin.config.GatewayAdminProperties;
import top.egon.cola.component.yuheng.admin.config.properties.GatewayAdminOpenApiProperties;
import top.egon.cola.component.yuheng.admin.credential.service.AesGcmGatewaySecretProtector;
import top.egon.cola.component.yuheng.admin.credential.service.GatewaySecretProtector;
import top.egon.cola.component.yuheng.admin.mcp.repository.filesystem.FileSystemMcpAppArtifactRepository;
import top.egon.cola.component.yuheng.admin.observability.controller.message.GatewayCallEventCodec;
import top.egon.cola.component.yuheng.admin.observability.controller.message.GatewayCallEventConsumerHandler;
import top.egon.cola.component.yuheng.admin.observability.controller.message.GatewayKafkaCallEventConsumer;
import top.egon.cola.component.yuheng.admin.observability.controller.message.GatewayKafkaConsumerMetrics;
import top.egon.cola.component.yuheng.admin.observability.controller.scheduled.GatewayObservabilityRetentionReaper;
import top.egon.cola.component.yuheng.admin.observability.repository.GatewayObservabilityRepository;
import top.egon.cola.component.yuheng.admin.observability.repository.jdbc.JdbcGatewayObservabilityRepository;
import top.egon.cola.component.yuheng.admin.observability.service.GatewayCallEventIngestService;
import top.egon.cola.component.yuheng.admin.observability.service.GatewayObservabilityQueryService;
import top.egon.cola.component.yuheng.admin.release.repository.GatewayReleasePublicationRepository;
import top.egon.cola.component.yuheng.admin.release.repository.GatewayReleaseRepository;
import top.egon.cola.component.yuheng.admin.release.service.GatewayReleasePublicationCoordinator;
import top.egon.cola.component.yuheng.admin.rule.service.GatewayDdcRulePublisher;
import top.egon.cola.component.yuheng.admin.runtime.service.GatewayProjectionService;
import top.egon.cola.component.yuheng.admin.openapi.client.GatewayOpenApiDnsPolicy;
import top.egon.cola.component.yuheng.admin.openapi.client.GatewayOpenApiTokenSupplier;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationLimits;
import top.egon.cola.component.yuheng.mcp.app.domain.McpAppSecurityValidator;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientHandle;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;

/**
 * 中文说明：{@code GatewayAdminConfiguration} 是配置类，位于当前 Gateway 模块的相关包中，负责网关管理端配置相关的职责与边界。
 * English summary: {@code GatewayAdminConfiguration} is a gateway admin configuration configuration in the current Gateway module; it owns the gateway admin configuration-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(value = "gatewayAdminConfiguration", proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties({
        GatewayAdminProperties.class,
        GatewayAdminOpenApiProperties.class
})
public class GatewayAdminConfiguration {

    /** Supplies projection timestamps regardless of whether OpenAPI discovery is enabled. */
    @Bean("gatewayProjectionClock")
    Clock gatewayProjectionClock() {
        return Clock.systemUTC();
    }

    /** Creates the bounded JDK HTTP client used by the OpenAPI fetch boundary. */
    @Bean(name = "gatewayOpenApiHttpClient")
    @ConditionalOnProperty(
            name = "yuheng.admin.openapi.enabled",
            havingValue = "true"
    )
    HttpClient gatewayOpenApiHttpClient(
            GatewayAdminOpenApiProperties properties) {
        properties.validate();
        return HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** Provides the configured JSON mapper used by the OpenAPI boundary. */
    @Bean(name = "gatewayOpenApiObjectMapper")
    @Primary
    ObjectMapper gatewayOpenApiObjectMapper() {
        return JsonMapper.builder().findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    }

    /** Binds the validated OpenAPI graph limits used by the always-available validation chain. */
    @Bean(name = "gatewayOpenApiValidationLimits")
    GatewayOpenApiValidationLimits gatewayOpenApiValidationLimits(
            GatewayAdminOpenApiProperties properties) {
        properties.validate();
        return new GatewayOpenApiValidationLimits(
                properties.getMaximumDocumentBytes(),
                properties.getMaximumOperations(),
                properties.getMaximumSchemaNodes(),
                properties.getMaximumRefDepth());
    }

    /** Provides the injected UTC clock for deterministic OpenAPI timestamps. */
    @Bean(name = "gatewayOpenApiClock")
    @ConditionalOnProperty(
            name = "yuheng.admin.openapi.enabled",
            havingValue = "true"
    )
    Clock gatewayOpenApiClock() {
        return Clock.systemUTC();
    }

    /** Binds the configured provider read timeout to the client qualifier. */
    @Bean(name = "gatewayOpenApiReadTimeout")
    @ConditionalOnProperty(
            name = "yuheng.admin.openapi.enabled",
            havingValue = "true"
    )
    Duration gatewayOpenApiReadTimeout(
            GatewayAdminOpenApiProperties properties) {
        properties.validate();
        return properties.getReadTimeout();
    }

    /** Binds the immutable document byte limit to the client qualifier. */
    @Bean(name = "gatewayOpenApiMaximumDocumentBytes")
    @ConditionalOnProperty(
            name = "yuheng.admin.openapi.enabled",
            havingValue = "true"
    )
    int gatewayOpenApiMaximumDocumentBytes(
            GatewayAdminOpenApiProperties properties) {
        properties.validate();
        return properties.getMaximumDocumentBytes();
    }

    /** Creates the DNS/CIDR policy only when the reconciler is explicitly enabled. */
    @Bean(name = "gatewayOpenApiDnsPolicy")
    @ConditionalOnProperty(
            name = "yuheng.admin.openapi.enabled",
            havingValue = "true"
    )
    GatewayOpenApiDnsPolicy gatewayOpenApiDnsPolicy(
            GatewayAdminOpenApiProperties properties) {
        properties.validate();
        return new GatewayOpenApiDnsPolicy(properties.getAllowedCidrs());
    }

    /**
     * Bridges provider reads to the existing Tianquan-Shoubing SERVICE-token facade. If
     * OAuth is not configured, the boundary fails closed and exposes no
     * credential or target details.
     */
    @Bean(name = "gatewayOpenApiTokenSupplier")
    @ConditionalOnProperty(
            name = "yuheng.admin.openapi.enabled",
            havingValue = "true"
    )
    GatewayOpenApiTokenSupplier gatewayOpenApiTokenSupplier(
            ObjectProvider<IdpServiceOAuth2Client> oauth,
            ObjectProvider<IdpStarterProperties> idpProperties,
            GatewayAdminOpenApiProperties properties) {
        properties.validate();
        IdpServiceOAuth2Client client = oauth.getIfAvailable();
        IdpStarterProperties settings = idpProperties.getIfAvailable();
        if (client == null || settings == null) {
            return (resourceUri, scope) -> {
                throw new top.egon.cola.component.yuheng.admin.openapi.client
                        .GatewayOpenApiFetchException(
                        "YUHENG_OPENAPI_OAUTH_NOT_CONFIGURED",
                        false,
                        "OpenAPI service token authorization is not configured"
                );
            };
        }
        settings.getServiceClient().validate();
        return (resourceUri, scope) -> client.authorize(
                new IdpServiceTokenRequest(
                        settings.getServiceClient().getRegistrationId(),
                        settings.getServiceClient().getAppId(),
                        resourceUri,
                        ServiceTokenContext.PLATFORM,
                        null,
                        Set.of(scope)
                )
        ).getTokenValue();
    }

    /**
     * 中文说明：执行 MCPApp制品存储 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mcp app artifact store operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.mcpAppArtifactStore(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param artifactRoot 参数 制品Root；parameter artifact root。
     * @return 返回 MCPApp制品存储 的处理结果；returns the result of the operation.
     */
    @Bean
    FileSystemMcpAppArtifactRepository mcpAppArtifactStore(
            @Value(
                    "${yuheng.admin.mcp.artifact-root:"
                            + "${java.io.tmpdir}/egon-cola/"
                            + "yuheng-mcp-artifacts}"
            ) String artifactRoot) {
        return new FileSystemMcpAppArtifactRepository(Path.of(artifactRoot));
    }

    /**
     * 中文说明：执行 MCPApp安全校验器 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mcp app security validator operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.mcpAppSecurityValidator(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 MCPApp安全校验器 的处理结果；returns the result of the operation.
     */
    @Bean
    McpAppSecurityValidator mcpAppSecurityValidator() {
        return new McpAppSecurityValidator();
    }

    /**
     * 中文说明：执行 网关DdcManagement客户端Handle 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway ddc management client handle operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewayDdcManagementClientHandle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param factory 参数 工厂；parameter factory。
     * @return 返回 网关DdcManagement客户端Handle 的处理结果；returns the result of the operation.
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(
            name = "yuheng.admin.tianshu.enabled",
            havingValue = "true"
    )
    DdcRpcClientHandle<DdcManagementClient>
            gatewayDdcManagementClientHandle(DdcRpcClientFactory factory) {
        return factory.managementClient();
    }

    /**
     * 中文说明：执行 ddcManagement客户端 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ddc management client operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.ddcManagementClient(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param handle 参数 handle；parameter handle。
     * @return 返回 ddcManagement客户端 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnProperty(
            name = "yuheng.admin.tianshu.enabled",
            havingValue = "true"
    )
    DdcManagementClient ddcManagementClient(
            @Qualifier("gatewayDdcManagementClientHandle")
            DdcRpcClientHandle<DdcManagementClient> handle) {
        return handle.client();
    }

    /**
     * 中文说明：执行 网关Ddc规则发布器 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway ddc rule publisher operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewayDdcRulePublisher(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param client 参数 客户端；parameter client。
     * @return 返回 网关Ddc规则发布器 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnBean(DdcManagementClient.class)
    GatewayDdcRulePublisher gatewayDdcRulePublisher(
            DdcManagementClient client) {
        return new GatewayDdcRulePublisher(client);
    }

    /**
     * 中文说明：执行 网关发布PublicationCoordinator 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway release publication coordinator operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewayReleasePublicationCoordinator(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param journal 参数 journal；parameter journal。
     * @param releases 参数 releases；parameter releases。
     * @param client 参数 客户端；parameter client。
     * @param publisher 参数 发布器；parameter publisher。
     * @param properties 参数 properties；parameter properties。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 网关发布PublicationCoordinator 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnBean({
            DdcManagementClient.class,
            GatewayDdcRulePublisher.class
    })
    GatewayReleasePublicationCoordinator
            gatewayReleasePublicationCoordinator(
            GatewayReleasePublicationRepository journal,
            GatewayReleaseRepository releases,
            DdcManagementClient client,
            GatewayDdcRulePublisher publisher,
            GatewayAdminProperties properties,
            @Value("${yuheng.admin.tianshu.publish-timeout:PT30S}")
            Duration timeout) {
        return new GatewayReleasePublicationCoordinator(
                journal,
                releases,
                client,
                publisher,
                Clock.systemUTC(),
                timeout,
                properties.getDdc()
        );
    }

    /**
     * 中文说明：执行 网关SecretProtector 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway secret protector operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewaySecretProtector(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param masterKey 参数 master键；parameter master key。
     * @param keyVersion 参数 键Version；parameter key version。
     * @return 返回 网关SecretProtector 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnProperty(
            name = "yuheng.admin.secrets.master-key-base64"
    )
    GatewaySecretProtector gatewaySecretProtector(
            @Value("${yuheng.admin.secrets.master-key-base64}")
            String masterKey,
            @Value("${yuheng.admin.secrets.key-version:v1}")
            String keyVersion) {
        return new AesGcmGatewaySecretProtector(
                java.util.Base64.getDecoder().decode(masterKey),
                keyVersion
        );
    }

    /**
     * 中文说明：执行 网关可观测性存储 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway observability store operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewayObservabilityStore(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param jdbcTemplate 参数 jdbc模板；parameter jdbc template。
     * @return 返回 网关可观测性存储 的处理结果；returns the result of the operation.
     */
    @Bean
    GatewayObservabilityRepository gatewayObservabilityStore(
            JdbcTemplate jdbcTemplate) {
        return new JdbcGatewayObservabilityRepository(jdbcTemplate);
    }

    /**
     * 中文说明：执行 网关调用事件Ingest服务 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway call event ingest service operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewayCallEventIngestService(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param store 参数 存储；parameter store。
     * @param retention 参数 retention；parameter retention。
     * @return 返回 网关调用事件Ingest服务 的处理结果；returns the result of the operation.
     */
    @Bean
    GatewayCallEventIngestService gatewayCallEventIngestService(
            GatewayObservabilityRepository store,
            @Value(
                    "${yuheng.admin.observability.retention:PT168H}"
            ) Duration retention) {
        return new GatewayCallEventIngestService(
                store,
                Clock.systemUTC(),
                retention
        );
    }

    /**
     * 中文说明：执行 网关可观测性Query服务 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway observability query service operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewayObservabilityQueryService(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param store 参数 存储；parameter store。
     * @param projections 参数 projections；parameter projections。
     * @return 返回 网关可观测性Query服务 的处理结果；returns the result of the operation.
     */
    @Bean
    GatewayObservabilityQueryService gatewayObservabilityQueryService(
            GatewayObservabilityRepository store,
            GatewayProjectionService projections) {
        return new GatewayObservabilityQueryService(
                store,
                Clock.systemUTC(),
                projections
        );
    }

    /**
     * 中文说明：执行 网关调用事件Codec 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway call event codec operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewayCallEventCodec(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @return 返回 网关调用事件Codec 的处理结果；returns the result of the operation.
     */
    @Bean
    GatewayCallEventCodec gatewayCallEventCodec(ObjectMapper objectMapper) {
        return new GatewayCallEventCodec(objectMapper);
    }

    /**
     * 中文说明：执行 网关调用事件消费者处理器 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway call event consumer handler operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewayCallEventConsumerHandler(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param codec 参数 codec；parameter codec。
     * @param service 参数 服务；parameter service。
     * @return 返回 网关调用事件消费者处理器 的处理结果；returns the result of the operation.
     */
    @Bean
    GatewayCallEventConsumerHandler gatewayCallEventConsumerHandler(
            GatewayCallEventCodec codec,
            GatewayCallEventIngestService service) {
        return new GatewayCallEventConsumerHandler(
                codec,
                service,
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：执行 网关可观测性RetentionReaper 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway observability retention reaper operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewayObservabilityRetentionReaper(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param service 参数 服务；parameter service。
     * @return 返回 网关可观测性RetentionReaper 的处理结果；returns the result of the operation.
     */
    @Bean
    GatewayObservabilityRetentionReaper
            gatewayObservabilityRetentionReaper(
            GatewayCallEventIngestService service) {
        return new GatewayObservabilityRetentionReaper(service);
    }

    /**
     * 中文说明：执行 网关Kafka调用事件消费者 操作；该方法是 {@code GatewayAdminConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway kafka call event consumer operation; this method is the invocation entry point on {@code GatewayAdminConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminConfiguration.gatewayKafkaCallEventConsumer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param handler 参数 处理器；parameter handler。
     * @param meterRegistry 参数 meter注册表；parameter meter registry。
     * @param bootstrapServers 参数 bootstrapServers；parameter bootstrap servers。
     * @param topic 参数 topic；parameter topic。
     * @param groupId 参数 groupId；parameter group id。
     * @param retryBackoff 参数 重试Backoff；parameter retry backoff。
     * @param maxRecordAttempts 参数 maxRecordAttempts；parameter max record attempts。
     * @return 返回 网关Kafka调用事件消费者 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnProperty(
            name = "yuheng.admin.observability.kafka.enabled",
            havingValue = "true"
    )
    GatewayKafkaCallEventConsumer gatewayKafkaCallEventConsumer(
            GatewayCallEventConsumerHandler handler,
            MeterRegistry meterRegistry,
            @Value(
                    "${yuheng.admin.observability.kafka.bootstrap-servers}"
            ) String bootstrapServers,
            @Value(
                    "${yuheng.admin.observability.kafka.topic:"
                            + "egon.yuheng.call.v1}"
            ) String topic,
            @Value(
                    "${yuheng.admin.observability.kafka.group-id:"
                            + "yuheng-admin-call-events-v1}"
            ) String groupId,
            @Value(
                    "${yuheng.admin.observability.kafka.retry-backoff:"
                            + "PT0.25S}"
            ) Duration retryBackoff,
            @Value(
                    "${yuheng.admin.observability.kafka."
                            + "max-record-attempts:5}"
            ) int maxRecordAttempts) {
        return new GatewayKafkaCallEventConsumer(
                handler,
                new GatewayKafkaConsumerMetrics(
                        meterRegistry,
                        Clock.systemUTC()
                ),
                new top.egon.cola.component.yuheng.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO(
                        bootstrapServers,
                        topic,
                        groupId,
                        Duration.ofMillis(500),
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(5),
                        retryBackoff,
                        maxRecordAttempts,
                        java.util.Map.of()
                )
        );
    }
}
