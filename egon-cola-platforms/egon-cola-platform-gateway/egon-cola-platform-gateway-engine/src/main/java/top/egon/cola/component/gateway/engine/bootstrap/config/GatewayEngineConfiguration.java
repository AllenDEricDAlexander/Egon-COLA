package top.egon.cola.component.gateway.engine.bootstrap.config;

import top.egon.cola.component.gateway.engine.http.service.ReactorNettyHttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.rpc.service.RpcMethodIndex;

import top.egon.cola.component.gateway.engine.bootstrap.lifecycle.GatewayEngineRuntime;
import top.egon.cola.component.gateway.engine.common.config.GatewayEngineRuntimeProperties;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderSelectionPolicy;
import top.egon.cola.component.gateway.engine.common.provider.service.ProviderCandidateFilter;
import top.egon.cola.component.gateway.engine.http.adapter.HttpUpstreamAdapter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.observation.ObservationRegistry;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.api.extension.DdcInstanceMetadataContributor;
import top.egon.cola.component.ddc.api.refresh.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayCredentialRecoveryProvider;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.core.transport.GatewayTransportDefaults;
import top.egon.cola.component.gateway.core.transport.GatewayTransportSafetyLimits;
import top.egon.cola.component.gateway.engine.common.provider.domain.ActiveHealthProbePolicy;
import top.egon.cola.component.gateway.engine.common.provider.service.ActiveHealthTracker;
import top.egon.cola.component.gateway.engine.common.provider.adapter.DdcProviderServiceRegistryAdapter;
import top.egon.cola.component.gateway.engine.common.provider.service.DirectoryProviderSelector;
import top.egon.cola.component.gateway.engine.common.provider.adapter.HttpProviderActiveHealthProbe;
import top.egon.cola.component.gateway.engine.common.provider.domain.PassiveHealthPolicy;
import top.egon.cola.component.gateway.engine.common.provider.service.PassiveHealthTracker;
import top.egon.cola.component.gateway.engine.common.provider.service.ProviderActiveHealthMonitor;
import top.egon.cola.component.gateway.engine.common.provider.service.ProviderDirectory;
import top.egon.cola.component.gateway.engine.rpc.adapter.RpcProviderActiveHealthProbe;
import top.egon.cola.component.gateway.engine.http.service.DefaultGatewayHttpDataPlaneHandler;
import top.egon.cola.component.gateway.engine.http.service.GatewayCompositeHttpDataPlaneHandler;
import top.egon.cola.component.gateway.engine.http.domain.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.http.service.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.http.security.RuleBackedHttpGatewaySecurityProcessor;
import top.egon.cola.component.gateway.engine.http.proxy.service.AggregatedHttpProxyStrategy;
import top.egon.cola.component.gateway.engine.http.proxy.service.GatewayHttpProxyStrategySelector;
import top.egon.cola.component.gateway.engine.http.proxy.service.StreamingHttpProxyStrategy;
import top.egon.cola.component.gateway.engine.mcp.adapter.FileSystemMcpAppArtifactReader;
import top.egon.cola.component.gateway.engine.mcp.adapter.HttpMcpTaskServiceTokenSupplier;
import top.egon.cola.component.gateway.engine.mcp.adapter.JdbcMcpRuntimeTaskStore;
import top.egon.cola.component.gateway.engine.mcp.service.McpAuditPublisher;
import top.egon.cola.component.gateway.engine.mcp.service.McpEngineHttpHandler;
import top.egon.cola.component.gateway.engine.mcp.service.McpGatewayIdentityAuthenticator;
import top.egon.cola.component.gateway.engine.mcp.service.McpRuntimeHealthIndicator;
import top.egon.cola.component.gateway.engine.mcp.domain.McpRuntimeProperties;
import top.egon.cola.component.gateway.engine.mcp.service.McpTaskOperationExecutor;
import top.egon.cola.component.gateway.engine.mcp.service.McpTaskServiceTokenSupplier;
import top.egon.cola.component.gateway.engine.mcp.service.McpTaskWorker;
import top.egon.cola.component.gateway.engine.mcp.adapter.MicrometerMcpTelemetry;
import top.egon.cola.component.gateway.engine.mcp.adapter.RedisMcpSessionStore;
import top.egon.cola.component.gateway.engine.mcp.adapter.remote.ReactorNettyRemoteMcpClient;
import top.egon.cola.component.gateway.engine.mcp.adapter.security.JdbcMcpApprovalAdapter;
import top.egon.cola.component.gateway.engine.mcp.adapter.security.Rbac3McpAuthorizationAdapter;
import top.egon.cola.component.gateway.engine.http.common.logging.GatewayCallAccessLogger;
import top.egon.cola.component.gateway.engine.common.observability.service.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.common.observability.service.GatewayCallEventDispatcher;
import top.egon.cola.component.gateway.engine.common.observability.service.GatewayCallEventSerializer;
import top.egon.cola.component.gateway.engine.common.observability.service.GatewayCallMetricsListener;
import top.egon.cola.component.gateway.engine.common.observability.domain.GatewayTelemetry;
import top.egon.cola.component.gateway.engine.common.observability.adapter.KafkaGatewayCallEventSink;
import top.egon.cola.component.gateway.engine.operation.adapter.DefaultGatewayOperationTransport;
import top.egon.cola.component.gateway.engine.operation.service.EngineGatewayOperationInvoker;
import top.egon.cola.component.gateway.engine.operation.adapter.HttpRpcUpstreamAdapter;
import top.egon.cola.component.gateway.engine.rpc.service.RpcGatewayForwarder;
import top.egon.cola.component.gateway.engine.rpc.service.RpcGatewayHandlerRegistry;
import top.egon.cola.component.gateway.engine.rpc.service.RpcGatewayServer;
import top.egon.cola.component.gateway.engine.rpc.domain.RpcGatewaySlotProperties;
import top.egon.cola.component.gateway.engine.rpc.service.RpcGatewaySlotRuntime;
import top.egon.cola.component.gateway.engine.rpc.adapter.RpcProviderChannelCache;
import top.egon.cola.component.gateway.engine.rpc.security.RuleBackedRpcGatewaySecurityProcessor;
import top.egon.cola.component.gateway.engine.rule.service.EngineGatewayRuleCompiler;
import top.egon.cola.component.gateway.engine.rule.service.GatewayRuleActivationApplier;
import top.egon.cola.component.gateway.engine.rule.service.GatewayRuleApplierRegistrar;
import top.egon.cola.component.gateway.engine.rule.repository.GatewayRuleChunkStore;
import top.egon.cola.component.gateway.engine.rule.adapter.json.GatewayRuleJsonCodec;
import top.egon.cola.component.gateway.engine.rule.repository.GatewayRuleLkgRepository;
import top.egon.cola.component.gateway.engine.rule.domain.GatewayRuleRuntimeStatus;
import top.egon.cola.component.gateway.engine.common.security.service.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.gateway.engine.common.security.service.GatewaySecurityChain;
import top.egon.cola.component.gateway.engine.common.security.domain.GatewayTransportSecurity;
import top.egon.cola.component.gateway.engine.common.security.service.TrustedClientAddressResolver;
import top.egon.cola.component.gateway.engine.rule.service.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.common.traffic.service.RedisTokenBucketExecutor;
import top.egon.cola.component.gateway.engine.common.traffic.adapter.RedissonRedisTokenBucketExecutor;
import top.egon.cola.component.gateway.engine.http.service.GatewayTransportDispatcher;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketProxy;
import top.egon.cola.component.gateway.engine.http.websocket.adapter.ReactorNettyWebSocketUpstreamAdapter;
import top.egon.cola.component.gateway.mcp.app.service.AppUiResourceDriver;
import top.egon.cola.component.gateway.mcp.app.service.McpAppRuntime;
import top.egon.cola.component.gateway.mcp.app.domain.McpAppSecurityValidator;
import top.egon.cola.component.gateway.mcp.completion.service.DictionaryCompletionProvider;
import top.egon.cola.component.gateway.mcp.completion.service.McpCompletionHandler;
import top.egon.cola.component.gateway.mcp.completion.service.OperationCompletionProvider;
import top.egon.cola.component.gateway.mcp.prompt.service.McpPromptDriver;
import top.egon.cola.component.gateway.mcp.prompt.service.McpPromptsGetHandler;
import top.egon.cola.component.gateway.mcp.prompt.service.McpPromptsListHandler;
import top.egon.cola.component.gateway.mcp.prompt.service.OperationPromptDriver;
import top.egon.cola.component.gateway.mcp.prompt.service.StaticPromptDriver;
import top.egon.cola.component.gateway.mcp.prompt.domain.StrictPromptTemplate;
import top.egon.cola.component.gateway.mcp.remote.service.McpDialectTranslator;
import top.egon.cola.component.gateway.mcp.remote.service.McpNamespaceRouter;
import top.egon.cola.component.gateway.mcp.remote.service.McpRemoteClientPool;
import top.egon.cola.component.gateway.mcp.remote.service.RemoteMcpCompletionProvider;
import top.egon.cola.component.gateway.mcp.remote.service.RemoteMcpPromptDriver;
import top.egon.cola.component.gateway.mcp.remote.service.RemoteMcpResourceDriver;
import top.egon.cola.component.gateway.mcp.remote.service.RemoteMcpToolDriver;
import top.egon.cola.component.gateway.mcp.resource.adapter.DatabaseSchemaResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceCatalog;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceTemplatesListHandler;
import top.egon.cola.component.gateway.mcp.resource.domain.McpResourceUriValidator;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourcesListHandler;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourcesReadHandler;
import top.egon.cola.component.gateway.mcp.resource.adapter.ObjectStorageResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.adapter.OperationResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.adapter.StaticBlobResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.adapter.StaticTextResourceDriver;
import top.egon.cola.component.gateway.mcp.rule.domain.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.common.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.service.McpMethodDispatcher;
import top.egon.cola.component.gateway.mcp.server.service.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.service.handler.McpDiscoverHandler;
import top.egon.cola.component.gateway.mcp.server.service.handler.McpInitializeHandler;
import top.egon.cola.component.gateway.mcp.server.service.handler.McpInitializedHandler;
import top.egon.cola.component.gateway.mcp.server.service.handler.McpPingHandler;
import top.egon.cola.component.gateway.mcp.subscription.service.McpResourceSubscribeHandler;
import top.egon.cola.component.gateway.mcp.subscription.service.McpSubscriptionService;
import top.egon.cola.component.gateway.mcp.subscription.service.McpSubscriptionsListenHandler;
import top.egon.cola.component.gateway.mcp.task.service.McpTaskService;
import top.egon.cola.component.gateway.mcp.task.service.McpTasksCancelHandler;
import top.egon.cola.component.gateway.mcp.task.service.McpTasksGetHandler;
import top.egon.cola.component.gateway.mcp.task.service.McpTasksUpdateHandler;
import top.egon.cola.component.gateway.mcp.common.telemetry.McpTelemetry;
import top.egon.cola.component.gateway.mcp.tool.service.McpResultBinder;
import top.egon.cola.component.gateway.mcp.tool.service.McpToolCatalog;
import top.egon.cola.component.gateway.mcp.tool.service.McpToolsCallHandler;
import top.egon.cola.component.gateway.mcp.tool.service.McpToolsListHandler;
import top.egon.cola.platform.idp.starter.admission.OwnerOnlyPrivateKeyLoader;
import top.egon.cola.platform.idp.starter.admission.PrivateKeyJwtAssertionFactory;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;

import javax.sql.DataSource;
import java.net.URI;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 中文说明：{@code GatewayEngineConfiguration} 是配置类，位于当前 Gateway 模块的相关包中，负责网关引擎配置相关的职责与边界。
 * English summary: {@code GatewayEngineConfiguration} is a gateway engine configuration configuration in the current Gateway module; it owns the gateway engine configuration-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        GatewayEngineRuntimeProperties.class,
        McpRuntimeProperties.class
})
public class GatewayEngineConfiguration {

    /**
     * 中文说明：表示 LOGGER 这一固定值；它属于 {@code GatewayEngineConfiguration} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value logger; it is a state, type, or protocol value of {@code GatewayEngineConfiguration} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineConfiguration} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineConfiguration}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            "gateway.mcp.audit"
    );

    /**
     * 中文说明：执行 网关Clock 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway clock operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayClock(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 网关Clock 的处理结果；returns the result of the operation.
     */
    @Bean
    public Clock gatewayClock() {
        return Clock.systemUTC();
    }

    /**
     * 中文说明：执行 网关远程MCPAuthentication 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway remote mcp authentication operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRemoteMcpAuthentication(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 网关远程MCPAuthentication 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnMissingBean(RemoteAuthProvider.class)
    public RemoteAuthProvider gatewayRemoteMcpAuthentication() {
        return request -> {
            if (request.provider().authProfileReference() == null) {
                return reactor.core.publisher.Mono.just(
                        new RemoteAuthProvider.OutboundAuthentication(
                                Map.of(),
                                request.provider().tlsProfileReference()
                        )
                );
            }
            return reactor.core.publisher.Mono.error(
                    new IllegalStateException(
                            "remote MCP authentication profile resolver "
                                    + "is unavailable"
                    )
            );
        };
    }

    /**
     * 中文说明：执行 网关远程MCP客户端池 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway remote mcp client pool operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRemoteMcpClientPool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authentication 参数 authentication；parameter authentication。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关远程MCP客户端池 的处理结果；returns the result of the operation.
     */
    @Bean(destroyMethod = "close")
    public McpRemoteClientPool gatewayRemoteMcpClientPool(
            RemoteAuthProvider authentication,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            @Qualifier("gatewayClock") Clock gatewayClock,
            McpRuntimeProperties properties) {
        properties.validate();
        return new McpRemoteClientPool(
                provider -> new ReactorNettyRemoteMcpClient(objectMapper),
                authentication,
                gatewayClock,
                properties.getRemote().getCallTimeout(),
                properties.getRemote().getMaximumConcurrentCalls(),
                properties.getRemote().getFailureThreshold(),
                properties.getRemote().getCircuitOpenDuration()
        );
    }

    /**
     * 中文说明：执行 网关MCP遥测 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp telemetry operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayMcpTelemetry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param meters 参数 meters；parameter meters。
     * @param observations 参数 observations；parameter observations。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关MCP遥测 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnMissingBean(McpTelemetry.class)
    public McpTelemetry gatewayMcpTelemetry(
            MeterRegistry meters,
            ObservationRegistry observations,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            @Qualifier("gatewayClock") Clock gatewayClock,
            McpRuntimeProperties properties) {
        properties.validate();
        ArrayList<McpTelemetry> observers = new ArrayList<>();
        observers.add(new MicrometerMcpTelemetry(meters, observations));
        if (properties.getAudit().isEnabled()) {
            observers.add(new McpAuditPublisher(
                    objectMapper,
                    gatewayClock,
                    json -> LOGGER.info("MCP_RUNTIME_AUDIT {}", json)
            ));
        }
        return McpTelemetry.composite(observers);
    }

    /**
     * 中文说明：执行 网关遥测 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway telemetry operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayTelemetry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param observationRegistry 参数 观测注册表；parameter observation registry。
     * @param samplingProbability 参数 samplingProbability；parameter sampling probability。
     * @return 返回 网关遥测 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayTelemetry gatewayTelemetry(
            ObservationRegistry observationRegistry,
            @Value("${management.tracing.sampling.probability:0.1}")
            double samplingProbability) {
        return new GatewayTelemetry(
                observationRegistry,
                samplingProbability
        );
    }

    /**
     * 中文说明：执行 网关安全Capabilities 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway security capabilities operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewaySecurityCapabilities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param extractors 参数 extractors；parameter extractors。
     * @param authentications 参数 authentications；parameter authentications。
     * @param authorizations 参数 authorizations；parameter authorizations。
     * @param identityMappers 参数 身份Mappers；parameter identity mappers。
     * @return 返回 网关安全Capabilities 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewaySecurityCapabilityRegistry gatewaySecurityCapabilities(
            ObjectProvider<GatewayCredentialExtractor> extractors,
            ObjectProvider<GatewayAuthenticationProvider> authentications,
            ObjectProvider<GatewayAuthorizationProvider> authorizations,
            ObjectProvider<GatewayIdentityMapper> identityMappers,
            ObjectProvider<GatewayCredentialRecoveryProvider> recoveries) {
        return new GatewaySecurityCapabilityRegistry(
                extractors.orderedStream().toList(),
                authentications.orderedStream().toList(),
                authorizations.orderedStream().toList(),
                identityMappers.orderedStream().toList(),
                recoveries.orderedStream().toList()
        );
    }

    /**
     * 中文说明：执行 网关传输Defaults 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway transport defaults operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayTransportDefaults(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关传输Defaults 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayTransportDefaults gatewayTransportDefaults(
            GatewayEngineRuntimeProperties properties) {
        GatewayEngineRuntimeProperties.Http http = properties.getHttp();
        return new GatewayTransportDefaults(
                http.getMaxBodyBytes(),
                OptionalLong.of(4L * 1024 * 1024),
                Duration.ofSeconds(30),
                http.getUpstreamTimeout(),
                http.getUpstreamTimeout(),
                Optional.empty(),
                false,
                true
        );
    }

    /**
     * 中文说明：执行 网关传输SafetyLimits 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway transport safety limits operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayTransportSafetyLimits(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param http 参数 http；parameter http。
     * @return 返回 网关传输SafetyLimits 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayTransportSafetyLimits gatewayTransportSafetyLimits(
            GatewayHttpEngineProperties http) {
        return new GatewayTransportSafetyLimits(
                http.absoluteMaxRequestBodyBytes(),
                http.maxConnectTimeout(),
                http.maxResponseHeaderTimeout(),
                http.maxStreamIdleTimeout(),
                http.maxTotalTimeout(),
                http.maxWebsocketIdleTimeout(),
                http.maxWebsocketFrameBytes()
        );
    }

    /**
     * 中文说明：执行 网关Http引擎Properties 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway http engine properties operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayHttpEngineProperties(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关Http引擎Properties 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayHttpEngineProperties gatewayHttpEngineProperties(
            GatewayEngineRuntimeProperties properties) {
        GatewayEngineRuntimeProperties.Http http = properties.getHttp();
        return new GatewayHttpEngineProperties(
                new GatewayHttpEngineProperties.Listener(
                        http.isPublicEnabled(),
                        http.getPublicHost(),
                        http.getPublicPort(),
                        transportSecurity(http.getPublicTls())
                ),
                new GatewayHttpEngineProperties.Listener(
                        http.isInternalEnabled(),
                        http.getInternalHost(),
                        http.getInternalPort(),
                        transportSecurity(http.getInternalTls())
                ),
                http.getMaxHeaderCount(),
                http.getMaxHeaderBytes(),
                http.getMaxBodyBytes(),
                http.getIdleTimeout(),
                http.getDrainTimeout(),
                http.getUpstreamMaxConnections(),
                http.getUpstreamPendingAcquireMaxCount(),
                http.getAbsoluteMaxRequestBodyBytes(),
                http.getBodyLogSampleBytes(),
                http.getAbsoluteMaxBodyLogSampleBytes(),
                http.getMaxConnectTimeout(),
                http.getMaxResponseHeaderTimeout(),
                http.getMaxStreamIdleTimeout(),
                http.getMaxTotalTimeout(),
                http.getMaxWebsocketIdleTimeout(),
                http.getMaxWebsocketFrameBytes()
        );
    }

    /**
     * 中文说明：执行 网关提供方Directory 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway provider directory operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayProviderDirectory(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param registry 参数 注册表；parameter registry。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @return 返回 网关提供方Directory 的处理结果；returns the result of the operation.
     */
    @Bean
    public ProviderDirectory gatewayProviderDirectory(
            DdcServiceRegistryClient registry,
            @Qualifier("gatewayClock") Clock gatewayClock) {
        return new ProviderDirectory(
                new DdcProviderServiceRegistryAdapter(registry),
                gatewayClock
        );
    }

    /**
     * 中文说明：执行 网关规则Chunk存储 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway rule chunk store operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRuleChunkStore(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 网关规则Chunk存储 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayRuleChunkStore gatewayRuleChunkStore() {
        return new GatewayRuleChunkStore();
    }

    /**
     * 中文说明：执行 网关Passive健康Tracker 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway passive health tracker operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayPassiveHealthTracker(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @return 返回 网关Passive健康Tracker 的处理结果；returns the result of the operation.
     */
    @Bean
    public PassiveHealthTracker gatewayPassiveHealthTracker(
            @Qualifier("gatewayClock") Clock gatewayClock) {
        return new PassiveHealthTracker(
                PassiveHealthPolicy.defaults(),
                gatewayClock
        );
    }

    /**
     * 中文说明：执行 网关Active健康Probe策略 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway active health probe policy operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayActiveHealthProbePolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关Active健康Probe策略 的处理结果；returns the result of the operation.
     */
    @Bean
    public ActiveHealthProbePolicy gatewayActiveHealthProbePolicy(
            GatewayEngineRuntimeProperties properties) {
        GatewayEngineRuntimeProperties.ActiveHealth configured =
                properties.getActiveHealth();
        return new ActiveHealthProbePolicy(
                configured.isEnabled(),
                configured.getInterval(),
                configured.getJitterRatio(),
                configured.getTimeout(),
                configured.getMaximumConcurrency(),
                configured.getFailureThreshold(),
                configured.getSuccessThreshold(),
                configured.getHttpMethod(),
                configured.getHttpPath(),
                Set.copyOf(configured.getHttpSuccessStatuses()),
                configured.getRpcServiceName(),
                configured.isRpcConnectFallback()
        );
    }

    /**
     * 中文说明：执行 网关Active健康Tracker 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway active health tracker operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayActiveHealthTracker(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @return 返回 网关Active健康Tracker 的处理结果；returns the result of the operation.
     */
    @Bean
    public ActiveHealthTracker gatewayActiveHealthTracker(
            ActiveHealthProbePolicy policy) {
        return new ActiveHealthTracker(
                policy.failureThreshold(),
                policy.successThreshold()
        );
    }

    /**
     * 中文说明：执行 网关规则ActivationApplier 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway rule activation applier operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRuleActivationApplier(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applierRegistry 参数 applier注册表；parameter applier registry。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param transportDefaults 参数 传输Defaults；parameter transport defaults。
     * @param transportSafetyLimits 参数 传输SafetyLimits；parameter transport safety limits。
     * @param chunks 参数 chunks；parameter chunks。
     * @param providerDirectory 参数 提供方Directory；parameter provider directory。
     * @param properties 参数 properties；parameter properties。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @param telemetry 参数 遥测；parameter telemetry。
     * @return 返回 网关规则ActivationApplier 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayRuleActivationApplier gatewayRuleActivationApplier(
            DdcConfigApplierRegistry applierRegistry,
            GatewaySecurityCapabilityRegistry capabilities,
            GatewayTransportDefaults transportDefaults,
            GatewayTransportSafetyLimits transportSafetyLimits,
            GatewayRuleChunkStore chunks,
            ProviderDirectory providerDirectory,
            GatewayEngineRuntimeProperties properties,
            @Qualifier("gatewayClock") Clock gatewayClock,
            GatewayTelemetry telemetry) {
        GatewayRuleActivationApplier activation =
                new GatewayRuleActivationApplier(
                        new GatewayRuleJsonCodec(),
                        new EngineGatewayRuleCompiler(
                                capabilities,
                                transportDefaults,
                                transportSafetyLimits
                        ),
                        chunks,
                        providerDirectory,
                        new GatewayRuleLkgRepository(
                                Path.of(properties.getDataDirectory()),
                                properties.getGatewayGroupCode()
                        ),
                        gatewayClock,
                        telemetry
                );
        GatewayRuleApplierRegistrar.register(
                applierRegistry,
                activation,
                chunks
        );
        return activation;
    }

    /**
     * 中文说明：执行 网关提供方Selector 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway provider selector operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayProviderSelector(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param providerDirectory 参数 提供方Directory；parameter provider directory。
     * @param activation 参数 activation；parameter activation。
     * @param passiveHealth 参数 passive健康；parameter passive health。
     * @param activeHealth 参数 active健康；parameter active health。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @return 返回 网关提供方Selector 的处理结果；returns the result of the operation.
     */
    @Bean
    public DirectoryProviderSelector gatewayProviderSelector(
            ProviderDirectory providerDirectory,
            GatewayRuleActivationApplier activation,
            PassiveHealthTracker passiveHealth,
            ActiveHealthTracker activeHealth,
            @Qualifier("gatewayClock") Clock gatewayClock) {
        return new DirectoryProviderSelector(
                providerDirectory,
                DirectoryProviderSelector.defaultLoadBalancers(),
                new ProviderCandidateFilter(
                        gatewayClock,
                        identity -> passiveHealth.eligible(identity)
                                && activeHealth.eligible(identity)
                ),
                key -> ProviderSelectionPolicy.defaults(
                        key.transport().equals("https")
                ),
                () -> activation.active() == null
                        ? Map.of()
                        : activation.active().providerPolicies()
        );
    }

    /**
     * 中文说明：执行 网关流量Governance 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway traffic governance operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayTrafficGovernance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @param redis 参数 redis；parameter redis。
     * @return 返回 网关流量Governance 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayTrafficGovernance gatewayTrafficGovernance(
            GatewayRuleActivationApplier activation,
            ObjectProvider<RedisTokenBucketExecutor> redis) {
        return new GatewayTrafficGovernance(
                activation::active,
                redis.getIfAvailable()
        );
    }

    /**
     * 中文说明：执行 网关RateLimitRedisson客户端 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway rate limit redisson client operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRateLimitRedissonClient(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param address 参数 address；parameter address。
     * @param database 参数 数据库；parameter database。
     * @param password 参数 password；parameter password。
     * @return 返回 网关RateLimitRedisson客户端 的处理结果；returns the result of the operation.
     */
    @Bean(name = "gatewayRateLimitRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "gatewayRateLimitRedissonClient")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.traffic.redis",
            name = "enabled",
            havingValue = "true"
    )
    public RedissonClient gatewayRateLimitRedissonClient(
            @Value(
                    "${egon.cola.component.gateway.engine.traffic.redis.address}"
            ) String address,
            @Value(
                    "${egon.cola.component.gateway.engine.traffic.redis."
                            + "database:0}"
            ) int database,
            @Value(
                    "${egon.cola.component.gateway.engine.traffic.redis."
                            + "password:}"
            ) String password) {
        Config config = new Config();
        var server = config.useSingleServer()
                .setAddress(address)
                .setDatabase(database);
        if (password != null && !password.isBlank()) {
            server.setPassword(password);
        }
        return Redisson.create(config);
    }

    /**
     * 中文说明：执行 网关RedisTokenBucketExecutor 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway redis token bucket executor operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRedisTokenBucketExecutor(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param redisson 参数 redisson；parameter redisson。
     * @return 返回 网关RedisTokenBucketExecutor 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnBean(name = "gatewayRateLimitRedissonClient")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.traffic.redis",
            name = "enabled",
            havingValue = "true"
    )
    public RedisTokenBucketExecutor gatewayRedisTokenBucketExecutor(
            @Qualifier("gatewayRateLimitRedissonClient")
            RedissonClient redisson) {
        return new RedissonRedisTokenBucketExecutor(redisson);
    }

    /**
     * 中文说明：执行 网关MCPRedisson客户端 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp redisson client operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayMcpRedissonClient(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param address 参数 address；parameter address。
     * @param database 参数 数据库；parameter database。
     * @param password 参数 password；parameter password。
     * @return 返回 网关MCPRedisson客户端 的处理结果；returns the result of the operation.
     */
    @Bean(name = "gatewayMcpRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "gatewayMcpRedissonClient")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.mcp.redis",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public RedissonClient gatewayMcpRedissonClient(
            @Value(
                    "${egon.cola.component.gateway.engine.mcp.redis.address:"
                            + "redis://127.0.0.1:6379}"
            ) String address,
            @Value(
                    "${egon.cola.component.gateway.engine.mcp.redis."
                            + "database:0}"
            ) int database,
            @Value(
                    "${egon.cola.component.gateway.engine.mcp.redis."
                            + "password:}"
            ) String password) {
        Config config = new Config();
        var server = config.useSingleServer()
                .setAddress(address)
                .setDatabase(database);
        if (password != null && !password.isBlank()) {
            server.setPassword(password);
        }
        return Redisson.create(config);
    }

    /**
     * 中文说明：执行 网关MCP会话存储 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp session store operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayMcpSessionStore(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param redisson 参数 redisson；parameter redisson。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @param keyPrefix 参数 键Prefix；parameter key prefix。
     * @param maximumStreamLength 参数 maximumStreamLength；parameter maximum stream length。
     * @return 返回 网关MCP会话存储 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnBean(name = "gatewayMcpRedissonClient")
    public RedisMcpSessionStore gatewayMcpSessionStore(
            @Qualifier("gatewayMcpRedissonClient") RedissonClient redisson,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            @Qualifier("gatewayClock") Clock gatewayClock,
            @Value(
                    "${egon.cola.component.gateway.engine.mcp.redis."
                            + "key-prefix:gateway:mcp:}"
            ) String keyPrefix,
            @Value(
                    "${egon.cola.component.gateway.engine.mcp.redis."
                            + "stream-max-length:256}"
            ) int maximumStreamLength) {
        return new RedisMcpSessionStore(
                redisson,
                objectMapper,
                keyPrefix,
                maximumStreamLength,
                gatewayClock
        );
    }

    /**
     * 中文说明：执行 网关MCP运行时任务存储 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp runtime task store operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayMcpRuntimeTaskStore(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param dataSource 参数 dataSource；parameter data source。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @return 返回 网关MCP运行时任务存储 的处理结果；returns the result of the operation.
     */
    @Bean
    public JdbcMcpRuntimeTaskStore gatewayMcpRuntimeTaskStore(
            DataSource dataSource,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new JdbcMcpRuntimeTaskStore(dataSource, objectMapper);
    }

    /**
     * 中文说明：执行 网关MCP任务服务 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp task service operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayMcpTaskService(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param store 参数 存储；parameter store。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关MCP任务服务 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnBean(JdbcMcpRuntimeTaskStore.class)
    public McpTaskService gatewayMcpTaskService(
            JdbcMcpRuntimeTaskStore store,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            @Qualifier("gatewayClock") Clock gatewayClock,
            McpRuntimeProperties properties) {
        return new McpTaskService(
                store,
                objectMapper,
                gatewayClock,
                properties.getTasks().getLeaseDuration()
        );
    }

    /**
     * 创建异步 MCP 任务使用的 IdP SERVICE Token Adapter。
     * Creates the IdP SERVICE-token adapter used by asynchronous MCP tasks.
     * 补充说明 / Supplementary summary: 执行 网关MCP任务服务TokenSupplier 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the gateway mcp task service token supplier operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayMcpTaskServiceTokenSupplier(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.mcp.tasks.service-token",
            name = "enabled",
            havingValue = "true"
    )
    public McpTaskServiceTokenSupplier gatewayMcpTaskServiceTokenSupplier(
            @Value("${egon.cola.component.gateway.engine.mcp.tasks.service-token.token-endpoint}")
            URI tokenEndpoint,
            @Value("${egon.cola.component.gateway.engine.mcp.tasks.service-token.client-id}")
            String clientId,
            @Value("${egon.cola.component.gateway.engine.mcp.tasks.service-token.key-id}")
            String keyId,
            @Value("${egon.cola.component.gateway.engine.mcp.tasks.service-token.private-key-file}")
            Path privateKeyFile,
            @Value("${egon.cola.component.gateway.engine.mcp.tasks.service-token.scopes}")
            Set<String> scopes,
            @Value("${egon.cola.component.gateway.engine.mcp.tasks.service-token.renewal-skew}")
            Duration renewalSkew,
            @Qualifier("gatewayClock") Clock gatewayClock) {
        PrivateKeyJwtAssertionFactory assertions =
                new PrivateKeyJwtAssertionFactory(
                        clientId,
                        keyId,
                        tokenEndpoint,
                        new OwnerOnlyPrivateKeyLoader().load(privateKeyFile),
                        gatewayClock,
                        new SecureRandom()
                );
        return new HttpMcpTaskServiceTokenSupplier(
                org.springframework.web.client.RestClient.builder().build(),
                tokenEndpoint,
                assertions,
                scopes,
                renewalSkew,
                gatewayClock
        );
    }

    /**
     * 中文说明：执行 网关MCP任务Worker 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp task worker operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayMcpTaskWorker(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tasks 参数 tasks；parameter tasks。
     * @param operationInvoker 参数 操作Invoker；parameter operation invoker。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param activation 参数 activation；parameter activation。
     * @param tokenSupplier 参数 tokenSupplier；parameter token supplier。
     * @param properties 参数 properties；parameter properties。
     * @param mcpProperties 参数 MCPProperties；parameter mcp properties。
     * @return 返回 网关MCP任务Worker 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnBean({
            McpTaskService.class,
            McpTaskServiceTokenSupplier.class
    })
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.mcp",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public McpTaskWorker gatewayMcpTaskWorker(
            McpTaskService tasks,
            EngineGatewayOperationInvoker operationInvoker,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            GatewayRuleActivationApplier activation,
            McpTaskServiceTokenSupplier tokenSupplier,
            GatewayEngineRuntimeProperties properties,
            McpRuntimeProperties mcpProperties) {
        return new McpTaskWorker(
                tasks,
                new McpTaskOperationExecutor(
                        operationInvoker,
                        objectMapper,
                        serverCode -> URI.create(activation.active()
                                .mcpRules()
                                .server(serverCode)
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "MCP_TASK_SERVER_NOT_FOUND"
                                        ))
                                .resourceUri()),
                        tokenSupplier
                ),
                properties.getNodeId(),
                mcpProperties.getTasks().getLeaseDuration(),
                mcpProperties.getTasks().getPollInterval()
        );
    }

    /**
     * 中文说明：执行 网关MCPHttp处理器 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp http handler operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayMcpHttpHandler(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param operationInvoker 参数 操作Invoker；parameter operation invoker。
     * @param sessionStore 参数 会话存储；parameter session store。
     * @param taskServices 参数 任务Services；parameter task services。
     * @param snapshots 参数 snapshots；parameter snapshots。
     * @param dataSources 参数 dataSources；parameter data sources。
     * @param remoteClients 参数 远程Clients；parameter remote clients。
     * @param mcpTelemetry 参数 MCP遥测；parameter mcp telemetry。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @param properties 参数 properties；parameter properties。
     * @param mcpProperties 参数 MCPProperties；parameter mcp properties。
     * @param issuer 参数 issuer；parameter issuer。
     * @return 返回 网关MCPHttp处理器 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnBean(RedisMcpSessionStore.class)
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.mcp",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public McpEngineHttpHandler gatewayMcpHttpHandler(
            GatewayRuleActivationApplier activation,
            GatewaySecurityCapabilityRegistry capabilities,
            EngineGatewayOperationInvoker operationInvoker,
            RedisMcpSessionStore sessionStore,
            ObjectProvider<McpTaskService> taskServices,
            ObjectProvider<SingleFlightSnapshotLoader> snapshots,
            ObjectProvider<DataSource> dataSources,
            McpRemoteClientPool remoteClients,
            McpTelemetry mcpTelemetry,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            @Qualifier("gatewayClock") Clock gatewayClock,
            GatewayEngineRuntimeProperties properties,
            McpRuntimeProperties mcpProperties,
            @Value(
                    "${egon.cola.platform.idp.gateway.issuer:"
                            + "http://127.0.0.1:18120}"
            ) String issuer) {
        mcpProperties.validate();
        Duration sessionTtl = mcpProperties.getSessionTtl();
        Duration streamWait = mcpProperties.getStreamWait();
        Supplier<CompiledMcpRules> mcpRules = () -> activation.active() == null
                ? null
                : activation.active().mcpRules();
        var toolCatalog = new McpToolCatalog(mcpRules);
        SingleFlightSnapshotLoader snapshotLoader = snapshots.getIfAvailable();
        DataSource dataSource = dataSources.getIfAvailable();
        McpAuthorizationPort authorization = snapshotLoader == null
                ? request -> reactor.core.publisher.Mono.just(
                McpAuthorizationPort.Decision.denied(
                        "RBAC3_AUTHORIZATION_UNAVAILABLE",
                        0L,
                        0L,
                        0L
                ))
                : new Rbac3McpAuthorizationAdapter(
                snapshotLoader
        );
        McpApprovalPort approvals = dataSource == null
                ? request -> reactor.core.publisher.Mono.just(
                McpApprovalPort.Result.UNAVAILABLE
        )
                : new JdbcMcpApprovalAdapter(
                dataSource,
                gatewayClock
        );
        McpSecurityGate securityGate = new McpSecurityGate(
                authorization,
                approvals,
                objectMapper
        );
        McpResourceUriValidator resourceUriValidator =
                new McpResourceUriValidator();
        McpResourceCatalog resourceCatalog = new McpResourceCatalog(
                mcpRules,
                resourceUriValidator
        );
        McpDialectTranslator dialectTranslator = new McpDialectTranslator();
        McpNamespaceRouter namespaceRouter = new McpNamespaceRouter();
        RemoteMcpToolDriver remoteToolDriver = new RemoteMcpToolDriver(
                mcpRules,
                remoteClients,
                namespaceRouter,
                dialectTranslator
        );
        ArrayList<McpResourceDriver> resourceDrivers = new ArrayList<>(
                List.of(
                        new StaticTextResourceDriver(),
                        new StaticBlobResourceDriver(),
                        new OperationResourceDriver(operationInvoker),
                        new ObjectStorageResourceDriver(resourceUriValidator)
                )
        );
        McpAppRuntime appRuntime = new McpAppRuntime(
                () -> activation.active() == null
                        ? null
                        : activation.active().mcpRules(),
                new FileSystemMcpAppArtifactReader(Path.of(
                        mcpProperties.getArtifactRoot()
                )),
                new McpAppSecurityValidator()
        );
        resourceDrivers.add(new AppUiResourceDriver(appRuntime));
        resourceDrivers.add(new RemoteMcpResourceDriver(
                mcpRules,
                remoteClients,
                namespaceRouter,
                dialectTranslator
        ));
        if (dataSource != null) {
            resourceDrivers.add(new DatabaseSchemaResourceDriver(
                    (schema, objectName) -> readDatabaseSchema(
                            dataSource,
                            schema,
                            objectName,
                            objectMapper
                    ),
                    resourceUriValidator
            ));
        }
        McpSubscriptionService subscriptions = new McpSubscriptionService(
                sessionStore,
                objectMapper,
                gatewayClock,
                sessionTtl,
                streamWait
        );
        List<McpPromptDriver> promptDrivers = List.of(
                new StaticPromptDriver(new StrictPromptTemplate()),
                new OperationPromptDriver(operationInvoker),
                new RemoteMcpPromptDriver(
                        mcpRules,
                        remoteClients,
                        namespaceRouter,
                        dialectTranslator
                )
        );
        McpTaskService taskService = taskServices.getIfAvailable();
        ArrayList<McpMethodHandler> methodHandlers = new ArrayList<>(List.of(
                new McpInitializeHandler(),
                new McpInitializedHandler(),
                new McpPingHandler(),
                new McpDiscoverHandler(),
                new McpToolsListHandler(toolCatalog, objectMapper),
                new McpToolsCallHandler(
                        toolCatalog,
                        new McpResultBinder(objectMapper),
                        operationInvoker,
                        securityGate,
                        objectMapper,
                        taskService,
                        mcpRules,
                        remoteToolDriver
                ),
                new McpResourcesListHandler(
                        resourceCatalog,
                        securityGate
                ),
                new McpResourceTemplatesListHandler(
                        resourceCatalog,
                        securityGate
                ),
                new McpResourcesReadHandler(
                        resourceCatalog,
                        List.copyOf(resourceDrivers),
                        securityGate
                ),
                new McpResourceSubscribeHandler(
                        resourceCatalog,
                        subscriptions,
                        securityGate
                ),
                new McpSubscriptionsListenHandler(
                        resourceCatalog,
                        subscriptions,
                        securityGate
                ),
                new McpPromptsListHandler(
                        mcpRules,
                        securityGate
                ),
                new McpPromptsGetHandler(
                        mcpRules,
                        promptDrivers,
                        securityGate
                ),
                new McpCompletionHandler(
                        mcpRules,
                        resourceCatalog,
                        List.of(
                                new DictionaryCompletionProvider(Map.of()),
                                new OperationCompletionProvider(
                                        operationInvoker,
                                        objectMapper
                                ),
                                new RemoteMcpCompletionProvider(
                                        mcpRules,
                                        remoteClients,
                                        namespaceRouter,
                                        dialectTranslator
                                )
                        ),
                        securityGate
                )
        ));
        if (taskService != null) {
            methodHandlers.add(new McpTasksGetHandler(
                    taskService,
                    securityGate
            ));
            methodHandlers.add(new McpTasksUpdateHandler(
                    taskService,
                    securityGate
            ));
            methodHandlers.add(new McpTasksCancelHandler(
                    taskService,
                    securityGate
            ));
        }
        McpMethodDispatcher dispatcher = new McpMethodDispatcher(
                List.copyOf(methodHandlers),
                mcpTelemetry
        );
        return new McpEngineHttpHandler(
                () -> activation.active() == null
                        ? null
                        : activation.active().mcpRules(),
                dispatcher,
                sessionStore,
                sessionStore,
                new McpGatewayIdentityAuthenticator(
                        new GatewaySecurityChain(capabilities),
                        issuer,
                        properties.getNodeId(),
                        gatewayClock
                ),
                objectMapper,
                gatewayClock,
                sessionTtl,
                streamWait,
                Math.toIntExact(Math.min(
                        Math.min(
                                properties.getHttp().getMaxBodyBytes(),
                                mcpProperties.getMaximumRequestBytes()
                        ),
                        Integer.MAX_VALUE
                ))
        );
    }

    /**
     * 中文说明：执行 read数据库模式 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read database schema operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.readDatabaseSchema(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param dataSource 参数 dataSource；parameter data source。
     * @param schema 参数 模式；parameter schema。
     * @param objectName 参数 objectName；parameter object name。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @return 返回 read数据库模式 的处理结果；returns the result of the operation.
     */
    private String readDatabaseSchema(
            DataSource dataSource,
            String schema,
            String objectName,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper)
            throws Exception {
        ArrayList<Map<String, Object>> columns = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet result = connection.getMetaData().getColumns(
                     connection.getCatalog(),
                     schema,
                     objectName,
                     null
             )) {
            while (result.next()) {
                LinkedHashMap<String, Object> column = new LinkedHashMap<>();
                column.put("name", result.getString("COLUMN_NAME"));
                column.put("type", result.getString("TYPE_NAME"));
                column.put("size", result.getInt("COLUMN_SIZE"));
                column.put("nullable", result.getInt("NULLABLE")
                        != java.sql.DatabaseMetaData.columnNoNulls);
                columns.add(Map.copyOf(column));
            }
        }
        if (columns.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(Map.of(
                "schema", schema,
                "object", objectName,
                "columns", List.copyOf(columns)
        ));
    }

    /**
     * 中文说明：执行 网关HttpUpstreamAdapter 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway http upstream adapter operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayHttpUpstreamAdapter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关HttpUpstreamAdapter 的处理结果；returns the result of the operation.
     */
    @Bean
    public ReactorNettyHttpUpstreamAdapter gatewayHttpUpstreamAdapter(
            GatewayEngineRuntimeProperties properties) {
        GatewayEngineRuntimeProperties.Http http = properties.getHttp();
        return new ReactorNettyHttpUpstreamAdapter(
                http.getUpstreamMaxConnections(),
                http.getUpstreamPendingAcquireMaxCount(),
                http.getIdleTimeout()
        );
    }

    /**
     * 中文说明：执行 网关传输分发器 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway transport dispatcher operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayTransportDispatcher(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 网关传输分发器 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayTransportDispatcher gatewayTransportDispatcher() {
        return new GatewayTransportDispatcher(
                new GatewayHttpProxyStrategySelector(
                        new AggregatedHttpProxyStrategy(),
                        new StreamingHttpProxyStrategy()
                ),
                new GatewayWebSocketProxy(
                        new ReactorNettyWebSocketUpstreamAdapter(
                                reactor.netty.http.client.HttpClient.create()
                        )
                )
        );
    }

    /**
     * 中文说明：执行 网关调用补全监听器 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway call completion listener operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayCallCompletionListener(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param meterRegistry 参数 meter注册表；parameter meter registry。
     * @param dispatcher 参数 分发器；parameter dispatcher。
     * @return 返回 网关调用补全监听器 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayCallCompletionListener gatewayCallCompletionListener(
            MeterRegistry meterRegistry,
            ObjectProvider<GatewayCallEventDispatcher> dispatcher) {
        return GatewayCallCompletionListener.composite(
                new GatewayCallAccessLogger(),
                new GatewayCallMetricsListener(meterRegistry),
                dispatcher.getIfAvailable()
        );
    }

    /**
     * 中文说明：执行 网关调用事件分发器 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway call event dispatcher operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayCallEventDispatcher(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @param telemetry 参数 遥测；parameter telemetry。
     * @return 返回 网关调用事件分发器 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.kafka",
            name = "enabled",
            havingValue = "true"
    )
    public GatewayCallEventDispatcher gatewayCallEventDispatcher(
            GatewayEngineRuntimeProperties properties,
            GatewayTelemetry telemetry) {
        GatewayEngineRuntimeProperties.Kafka kafka = properties.getKafka();
        return new GatewayCallEventDispatcher(
                kafka.getMaxQueuedEvents(),
                kafka.getMaxQueuedBytes(),
                kafka.getShutdownDrain(),
                new GatewayCallEventSerializer(),
                new KafkaGatewayCallEventSink(
                        new KafkaGatewayCallEventSink.Settings(
                                kafka.getBootstrapServers(),
                                kafka.getTopic(),
                                kafka.getDeliveryTimeout(),
                                kafka.getShutdownDrain(),
                                Map.of()
                        ),
                        telemetry
                )
        );
    }

    /**
     * 中文说明：执行 网关Http服务器 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway http server operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayHttpServer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @param engineProperties 参数 引擎Properties；parameter engine properties。
     * @param activation 参数 activation；parameter activation。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstream 参数 upstream；parameter upstream。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param httpRpcUpstream 参数 httpRpcUpstream；parameter http rpc upstream。
     * @param passiveHealth 参数 passive健康；parameter passive health。
     * @param telemetry 参数 遥测；parameter telemetry。
     * @param transportDispatcher 参数 传输分发器；parameter transport dispatcher。
     * @param mcpHandlers 参数 MCPHandlers；parameter mcp handlers。
     * @return 返回 网关Http服务器 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayHttpServer gatewayHttpServer(
            GatewayEngineRuntimeProperties properties,
            GatewayHttpEngineProperties engineProperties,
            GatewayRuleActivationApplier activation,
            DirectoryProviderSelector providerSelector,
            ReactorNettyHttpUpstreamAdapter upstream,
            GatewaySecurityCapabilityRegistry capabilities,
            @Qualifier("gatewayCallCompletionListener")
            GatewayCallCompletionListener completionListener,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream,
            PassiveHealthTracker passiveHealth,
            GatewayTelemetry telemetry,
            GatewayTransportDispatcher transportDispatcher,
            ObjectProvider<McpEngineHttpHandler> mcpHandlers) {
        GatewayEngineRuntimeProperties.Http http = properties.getHttp();
        var emptyRoutes = new HttpRouteCompiler().compile(List.of());
        var security = new RuleBackedHttpGatewaySecurityProcessor(
                new GatewaySecurityChain(capabilities),
                activation::active,
                new TrustedClientAddressResolver(
                        properties.getSecurity().getTrustedProxyCidrs()
                ),
                properties.getNodeId()
        );
        var handler = new DefaultGatewayHttpDataPlaneHandler(
                new HttpRequestNormalizer(
                        http.getMaxHeaderCount(),
                        http.getMaxHeaderBytes()
                ),
                () -> activation.active() == null
                        ? emptyRoutes
                        : activation.active().httpRoutes(),
                providerSelector,
                upstream,
                http.getMaxBodyBytes(),
                http.getUpstreamTimeout(),
                security,
                completionListener,
                properties.getNodeId(),
                trafficGovernance,
                httpRpcUpstream,
                passiveHealth,
                () -> activation.active() == null
                        ? Map.of()
                        : activation.active().corsPolicies(),
                telemetry,
                properties.getEnv(),
                properties.getNamespace(),
                transportDispatcher,
                engineProperties.bodyLogSampleBytes(),
                null
        );
        McpEngineHttpHandler mcpHandler = mcpHandlers.getIfAvailable();
        return new GatewayHttpServer(engineProperties, mcpHandler == null
                ? handler
                : new GatewayCompositeHttpDataPlaneHandler(
                mcpHandler,
                handler,
                Math.toIntExact(Math.min(
                        properties.getHttp().getMaxBodyBytes(),
                        Integer.MAX_VALUE
                ))
        ));
    }

    /**
     * 中文说明：执行 网关Rpc提供方Channels 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway rpc provider channels operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRpcProviderChannels(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关Rpc提供方Channels 的处理结果；returns the result of the operation.
     */
    @Bean
    public RpcProviderChannelCache gatewayRpcProviderChannels(
            GatewayEngineRuntimeProperties properties) {
        return new RpcProviderChannelCache(
                properties.getRpc().getChannelDrainTimeout(),
                transportSecurity(properties.getRpc().getTls())
        );
    }

    /**
     * 中文说明：执行 网关提供方Active健康监控器 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway provider active health monitor operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayProviderActiveHealthMonitor(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param directory 参数 directory；parameter directory。
     * @param channels 参数 channels；parameter channels。
     * @param tracker 参数 tracker；parameter tracker。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 网关提供方Active健康监控器 的处理结果；returns the result of the operation.
     */
    @Bean(destroyMethod = "close")
    public ProviderActiveHealthMonitor gatewayProviderActiveHealthMonitor(
            ProviderDirectory directory,
            RpcProviderChannelCache channels,
            ActiveHealthTracker tracker,
            ActiveHealthProbePolicy policy) {
        return new ProviderActiveHealthMonitor(
                directory,
                Map.of(
                        ProviderProtocolType.HTTP,
                        new HttpProviderActiveHealthProbe(
                                reactor.netty.http.client.HttpClient.create()
                        ),
                        ProviderProtocolType.RPC,
                        new RpcProviderActiveHealthProbe(channels)
                ),
                tracker,
                policy
        );
    }

    /**
     * 中文说明：执行 网关HttpRpcUpstreamAdapter 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway http rpc upstream adapter operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayHttpRpcUpstreamAdapter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @param channels 参数 channels；parameter channels。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @return 返回 网关HttpRpcUpstreamAdapter 的处理结果；returns the result of the operation.
     */
    @Bean
    public HttpRpcUpstreamAdapter gatewayHttpRpcUpstreamAdapter(
            GatewayRuleActivationApplier activation,
            RpcProviderChannelCache channels,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new HttpRpcUpstreamAdapter(
                activation::active,
                channels,
                objectMapper
        );
    }

    /**
     * 中文说明：执行 网关操作Invoker 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway operation invoker operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayOperationInvoker(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param http 参数 http；parameter http。
     * @param rpc 参数 rpc；parameter rpc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关操作Invoker 的处理结果；returns the result of the operation.
     */
    @Bean
    public EngineGatewayOperationInvoker gatewayOperationInvoker(
            GatewayRuleActivationApplier activation,
            DirectoryProviderSelector providerSelector,
            GatewayTrafficGovernance trafficGovernance,
            ReactorNettyHttpUpstreamAdapter http,
            HttpRpcUpstreamAdapter rpc,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            GatewayEngineRuntimeProperties properties) {
        long maximumRequestBytes = properties.getHttp()
                .getAbsoluteMaxRequestBodyBytes();
        long maximumResponseBytes = 4L * 1024 * 1024;
        return new EngineGatewayOperationInvoker(
                activation::active,
                providerSelector,
                trafficGovernance,
                new DefaultGatewayOperationTransport(
                        http,
                        rpc,
                        maximumResponseBytes
                ),
                objectMapper,
                properties.getHttp().getUpstreamTimeout(),
                maximumRequestBytes,
                maximumResponseBytes
        );
    }

    /**
     * 中文说明：执行 网关Rpc处理器注册表 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway rpc handler registry operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRpcHandlerRegistry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @param activation 参数 activation；parameter activation。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param channels 参数 channels；parameter channels。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param passiveHealth 参数 passive健康；parameter passive health。
     * @param telemetry 参数 遥测；parameter telemetry。
     * @return 返回 网关Rpc处理器注册表 的处理结果；returns the result of the operation.
     */
    @Bean
    public RpcGatewayHandlerRegistry gatewayRpcHandlerRegistry(
            GatewayEngineRuntimeProperties properties,
            GatewayRuleActivationApplier activation,
            DirectoryProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            GatewaySecurityCapabilityRegistry capabilities,
            @Qualifier("gatewayCallCompletionListener")
            GatewayCallCompletionListener completionListener,
            GatewayTrafficGovernance trafficGovernance,
            PassiveHealthTracker passiveHealth,
            GatewayTelemetry telemetry) {
        var security = new RuleBackedRpcGatewaySecurityProcessor(
                new GatewaySecurityChain(capabilities),
                activation::active,
                properties.getNodeId()
        );
        var forwarder = new RpcGatewayForwarder(
                providerSelector,
                channels,
                properties.getRpc().getMaximumTimeout(),
                properties.getRpc().getMaxInboundMessageBytes(),
                security,
                completionListener,
                properties.getNodeId(),
                trafficGovernance,
                passiveHealth,
                telemetry
        );
        return new RpcGatewayHandlerRegistry(
                forwarder,
                () -> activation.active() == null
                        ? RpcMethodIndex.empty()
                        : activation.active().rpcMethods()
        );
    }

    /**
     * 中文说明：执行 网关Rpc服务器 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway rpc server operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRpcServer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @param registry 参数 注册表；parameter registry。
     * @return 返回 网关Rpc服务器 的处理结果；returns the result of the operation.
     */
    @Bean
    public RpcGatewayServer gatewayRpcServer(
            GatewayEngineRuntimeProperties properties,
            RpcGatewayHandlerRegistry registry) {
        return new RpcGatewayServer(
                properties.getRpc().getPort(),
                properties.getRpc().getMaxInboundMessageBytes(),
                registry,
                transportSecurity(properties.getRpc().getTls())
        );
    }

    /**
     * 中文说明：执行 网关TlsCertificateMetrics 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway tls certificate metrics operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayTlsCertificateMetrics(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关TlsCertificateMetrics 的处理结果；returns the result of the operation.
     */
    @Bean
    public MeterBinder gatewayTlsCertificateMetrics(
            GatewayEngineRuntimeProperties properties) {
        return registry -> {
            registerCertificateExpiry(
                    registry,
                    "public-http",
                    transportSecurity(properties.getHttp().getPublicTls())
            );
            registerCertificateExpiry(
                    registry,
                    "internal-http",
                    transportSecurity(properties.getHttp().getInternalTls())
            );
            registerCertificateExpiry(
                    registry,
                    "rpc",
                    transportSecurity(properties.getRpc().getTls())
            );
        };
    }

    /**
     * 创建携带 IdP 准入票据的 Gateway RPC Slot 运行时。
     * / Creates the Gateway RPC-slot runtime that carries IdP admission tickets.
     *
     * @param registry DDC 服务注册客户端 / DDC service-registry client
     * @param serviceKeyFactory 服务键工厂 / service-key factory
     * @param ddcIdentity Gateway 的 DDC 实例身份 / Gateway DDC instance identity
     * @param properties Gateway Engine 配置 / Gateway Engine configuration
     * @param admissionTickets 准入票据端口 / admission-ticket port
     * @return Gateway RPC Slot 运行时 / Gateway RPC-slot runtime
     * 补充说明 / Supplementary summary: 执行 网关Rpc槽位运行时 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the gateway rpc slot runtime operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRpcSlotRuntime(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Bean
    public RpcGatewaySlotRuntime gatewayRpcSlotRuntime(
            DdcServiceRegistryClient registry,
            DdcServiceKeyFactory serviceKeyFactory,
            DdcInstanceIdentity ddcIdentity,
            GatewayEngineRuntimeProperties properties,
            DdcAdmissionTicketSupplier admissionTickets) {
        GatewayEngineRuntimeProperties.Rpc rpc = properties.getRpc();
        return new RpcGatewaySlotRuntime(
                registry,
                serviceKeyFactory,
                new RpcGatewaySlotProperties(
                        rpc.isEnabled(),
                        properties.getEnv(),
                        properties.getNamespace(),
                        ddcIdentity.instanceId(),
                        rpc.getAdvertisedHost(),
                        rpc.getServiceName(),
                        rpc.getGroup(),
                        rpc.getVersion(),
                        properties.getGatewayGroupCode(),
                        "5.2.3",
                        "5.2.3",
                        rpc.getTls().isEnabled(),
                        rpc.getLeaseSeconds(),
                        rpc.getHeartbeatIntervalSeconds()
                ),
                admissionTickets
        );
    }

    /**
     * 中文说明：执行 网关引擎运行时 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway engine runtime operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayEngineRuntime(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param properties 参数 properties；parameter properties。
     * @param httpServer 参数 http服务器；parameter http server。
     * @param rpcServer 参数 rpc服务器；parameter rpc server。
     * @param rpcSlot 参数 rpc槽位；parameter rpc slot。
     * @param activation 参数 activation；parameter activation。
     * @param providerDirectory 参数 提供方Directory；parameter provider directory。
     * @return 返回 网关引擎运行时 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayEngineRuntime gatewayEngineRuntime(
            GatewayEngineRuntimeProperties properties,
            GatewayHttpServer httpServer,
            RpcGatewayServer rpcServer,
            RpcGatewaySlotRuntime rpcSlot,
            GatewayRuleActivationApplier activation,
            ProviderDirectory providerDirectory) {
        return new GatewayEngineRuntime(
                properties,
                httpServer,
                rpcServer,
                rpcSlot,
                activation,
                providerDirectory
        );
    }

    /**
     * 中文说明：执行 网关运行时元数据 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway runtime metadata operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayRuntimeMetadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @return 返回 网关运行时元数据 的处理结果；returns the result of the operation.
     */
    @Bean
    public DdcInstanceMetadataContributor gatewayRuntimeMetadata(
            GatewayRuleActivationApplier activation) {
        return () -> {
            GatewayRuleRuntimeStatus status = activation.status();
            return Map.of(
                    "activeReleaseId", value(status.activeReleaseId()),
                    "activeRuleVersion",
                    Long.toString(status.activeDdcVersion()),
                    "activeRuleChecksum", value(status.artifactSha256()),
                    "lastApplyStatus", status.lastStage().name(),
                    "lastAckAt", status.updatedAt().toString()
            );
        };
    }

    /**
     * 中文说明：执行 传输安全 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transport security operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.transportSecurity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tls 参数 tls；parameter tls。
     * @return 返回 传输安全 的处理结果；returns the result of the operation.
     */
    private GatewayTransportSecurity transportSecurity(
            GatewayEngineRuntimeProperties.Tls tls) {
        return new GatewayTransportSecurity(
                tls.isEnabled(),
                tls.isDevelopmentPlaintext(),
                tls.getCertificateChainPath(),
                tls.getPrivateKeyPath(),
                tls.getTrustCertificateCollectionPath(),
                tls.isClientCertificateRequired()
        );
    }

    /**
     * 中文说明：执行 registerCertificateExpiry 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the register certificate expiry operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.registerCertificateExpiry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param registry 参数 注册表；parameter registry。
     * @param listener 参数 监听器；parameter listener。
     * @param security 参数 安全；parameter security。
     */
    private void registerCertificateExpiry(
            MeterRegistry registry,
            String listener,
            GatewayTransportSecurity security) {
        if (!security.enabled()) {
            return;
        }
        registry.gauge(
                "gateway.tls.certificate.expiry.epoch.seconds",
                List.of(
                        io.micrometer.core.instrument.Tag.of(
                                "listener",
                                listener
                        )
                ),
                security,
                GatewayTransportSecurity::certificateExpiryEpochSeconds
        );
    }

    /**
     * 中文说明：执行 网关MCP运行时健康Indicator 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp runtime health indicator operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayMcpRuntimeHealthIndicator(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @param sessionStores 参数 会话Stores；parameter session stores。
     * @param taskServices 参数 任务Services；parameter task services。
     * @param properties 参数 properties；parameter properties。
     * @param remoteClients 参数 远程Clients；parameter remote clients。
     * @return 返回 网关MCP运行时健康Indicator 的处理结果；returns the result of the operation.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.mcp",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public McpRuntimeHealthIndicator gatewayMcpRuntimeHealthIndicator(
            GatewayRuleActivationApplier activation,
            ObjectProvider<RedisMcpSessionStore> sessionStores,
            ObjectProvider<McpTaskService> taskServices,
            McpRuntimeProperties properties,
            McpRemoteClientPool remoteClients) {
        return new McpRuntimeHealthIndicator(
                activation,
                sessionStores.getIfAvailable() != null,
                taskServices.getIfAvailable() != null,
                Path.of(properties.getArtifactRoot()),
                remoteClients
        );
    }

    /**
     * 中文说明：执行 网关引擎健康Indicator 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway engine health indicator operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.gatewayEngineHealthIndicator(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param runtime 参数 运行时；parameter runtime。
     * @param activation 参数 activation；parameter activation。
     * @param rpcSlot 参数 rpc槽位；parameter rpc slot。
     * @return 返回 网关引擎健康Indicator 的处理结果；returns the result of the operation.
     */
    @Bean
    public HealthIndicator gatewayEngineHealthIndicator(
            GatewayEngineRuntime runtime,
            GatewayRuleActivationApplier activation,
            RpcGatewaySlotRuntime rpcSlot) {
        return () -> {
            Health.Builder health = runtime.running()
                    ? runtime.ready()
                    ? Health.up()
                    : Health.status("OUT_OF_SERVICE")
                    : Health.down();
            return health
                    .withDetail("running", runtime.running())
                    .withDetail("ready", runtime.ready())
                    .withDetail(
                            "ruleStage",
                            activation.status().lastStage().name()
                    )
                    .withDetail(
                            "activeReleaseId",
                            activation.status().activeReleaseId() == null
                                    ? ""
                                    : activation.status().activeReleaseId()
                    )
                    .withDetail(
                            "rpcState",
                            runtime.rpcState().name()
                    )
                    .withDetail(
                            "rpcLastFailure",
                            rpcSlot.lastFailure()
                                    .map(Throwable::getMessage)
                                    .orElse("")
                    )
                    .build();
        };
    }

    /**
     * 中文说明：执行 值 操作；该方法是 {@code GatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the value operation; this method is the invocation entry point on {@code GatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineConfiguration.value(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 值 的处理结果；returns the result of the operation.
     */
    private String value(String value) {
        return value == null ? "" : value;
    }
}
