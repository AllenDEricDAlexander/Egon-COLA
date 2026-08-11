package top.egon.cola.component.gateway.engine;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.observation.ObservationRegistry;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
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
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.api.refresh.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.api.extension.DdcInstanceMetadataContributor;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.core.transport.GatewayTransportDefaults;
import top.egon.cola.component.gateway.core.transport.GatewayTransportSafetyLimits;
import top.egon.cola.component.gateway.engine.discovery.DdcProviderServiceRegistryAdapter;
import top.egon.cola.component.gateway.engine.discovery.DirectoryProviderSelector;
import top.egon.cola.component.gateway.engine.discovery.ActiveHealthProbePolicy;
import top.egon.cola.component.gateway.engine.discovery.ActiveHealthTracker;
import top.egon.cola.component.gateway.engine.discovery.HttpProviderActiveHealthProbe;
import top.egon.cola.component.gateway.engine.discovery.PassiveHealthPolicy;
import top.egon.cola.component.gateway.engine.discovery.PassiveHealthTracker;
import top.egon.cola.component.gateway.engine.discovery.ProviderActiveHealthMonitor;
import top.egon.cola.component.gateway.engine.discovery.ProviderDirectory;
import top.egon.cola.component.gateway.engine.discovery.RpcProviderActiveHealthProbe;
import top.egon.cola.component.gateway.engine.http.DefaultGatewayHttpDataPlaneHandler;
import top.egon.cola.component.gateway.engine.http.GatewayCompositeHttpDataPlaneHandler;
import top.egon.cola.component.gateway.engine.http.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.http.ReactorNettyHttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.http.RuleBackedHttpGatewaySecurityProcessor;
import top.egon.cola.component.gateway.engine.http.proxy.AggregatedHttpProxyStrategy;
import top.egon.cola.component.gateway.engine.http.proxy.GatewayHttpProxyStrategySelector;
import top.egon.cola.component.gateway.engine.http.proxy.StreamingHttpProxyStrategy;
import top.egon.cola.component.gateway.engine.observability.GatewayCallAccessLogger;
import top.egon.cola.component.gateway.engine.observability.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.observability.GatewayCallEventDispatcher;
import top.egon.cola.component.gateway.engine.observability.GatewayCallEventSerializer;
import top.egon.cola.component.gateway.engine.observability.GatewayCallMetricsListener;
import top.egon.cola.component.gateway.engine.observability.GatewayTelemetry;
import top.egon.cola.component.gateway.engine.observability.KafkaGatewayCallEventSink;
import top.egon.cola.component.gateway.engine.mcp.McpEngineHttpHandler;
import top.egon.cola.component.gateway.engine.mcp.McpGatewayIdentityAuthenticator;
import top.egon.cola.component.gateway.engine.mcp.JdbcMcpRuntimeTaskStore;
import top.egon.cola.component.gateway.engine.mcp.McpAuditPublisher;
import top.egon.cola.component.gateway.engine.mcp.McpRuntimeHealthIndicator;
import top.egon.cola.component.gateway.engine.mcp.McpRuntimeProperties;
import top.egon.cola.component.gateway.engine.mcp.McpTaskWorker;
import top.egon.cola.component.gateway.engine.mcp.MicrometerMcpTelemetry;
import top.egon.cola.component.gateway.engine.mcp.FileSystemMcpAppArtifactReader;
import top.egon.cola.component.gateway.engine.mcp.RedisMcpSessionStore;
import top.egon.cola.component.gateway.engine.mcp.remote.ReactorNettyRemoteMcpClient;
import top.egon.cola.component.gateway.engine.mcp.security.JdbcMcpApprovalAdapter;
import top.egon.cola.component.gateway.engine.mcp.security.Rbac3McpAuthorizationAdapter;
import top.egon.cola.component.gateway.engine.operation.DefaultGatewayOperationTransport;
import top.egon.cola.component.gateway.engine.operation.EngineGatewayOperationInvoker;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayForwarder;
import top.egon.cola.component.gateway.engine.rpc.HttpRpcUpstreamAdapter;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayHandlerRegistry;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewaySlotProperties;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewaySlotRuntime;
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndex;
import top.egon.cola.component.gateway.engine.rpc.RpcProviderChannelCache;
import top.egon.cola.component.gateway.engine.rpc.RuleBackedRpcGatewaySecurityProcessor;
import top.egon.cola.component.gateway.engine.rule.EngineGatewayRuleCompiler;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleActivationApplier;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleApplierRegistrar;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleChunkStore;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleJsonCodec;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleLkgRepository;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleRuntimeStatus;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityChain;
import top.egon.cola.component.gateway.engine.security.GatewayTransportSecurity;
import top.egon.cola.component.gateway.engine.security.TrustedClientAddressResolver;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.traffic.RedisTokenBucketExecutor;
import top.egon.cola.component.gateway.engine.traffic.RedissonRedisTokenBucketExecutor;
import top.egon.cola.component.gateway.engine.transport.GatewayTransportDispatcher;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketProxy;
import top.egon.cola.component.gateway.engine.websocket.ReactorNettyWebSocketUpstreamAdapter;
import top.egon.cola.component.gateway.mcp.completion.DictionaryCompletionProvider;
import top.egon.cola.component.gateway.mcp.completion.McpCompletionHandler;
import top.egon.cola.component.gateway.mcp.completion.OperationCompletionProvider;
import top.egon.cola.component.gateway.mcp.app.AppUiResourceDriver;
import top.egon.cola.component.gateway.mcp.app.McpAppRuntime;
import top.egon.cola.component.gateway.mcp.app.McpAppSecurityValidator;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptDriver;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptsGetHandler;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptsListHandler;
import top.egon.cola.component.gateway.mcp.prompt.OperationPromptDriver;
import top.egon.cola.component.gateway.mcp.prompt.StaticPromptDriver;
import top.egon.cola.component.gateway.mcp.prompt.StrictPromptTemplate;
import top.egon.cola.component.gateway.mcp.resource.DatabaseSchemaResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.McpResourceCatalog;
import top.egon.cola.component.gateway.mcp.resource.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.McpResourceTemplatesListHandler;
import top.egon.cola.component.gateway.mcp.resource.McpResourceUriValidator;
import top.egon.cola.component.gateway.mcp.resource.McpResourcesListHandler;
import top.egon.cola.component.gateway.mcp.resource.McpResourcesReadHandler;
import top.egon.cola.component.gateway.mcp.resource.ObjectStorageResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.OperationResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.StaticBlobResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.StaticTextResourceDriver;
import top.egon.cola.component.gateway.mcp.remote.McpDialectTranslator;
import top.egon.cola.component.gateway.mcp.remote.McpNamespaceRouter;
import top.egon.cola.component.gateway.mcp.remote.McpRemoteClientPool;
import top.egon.cola.component.gateway.mcp.remote.RemoteMcpCompletionProvider;
import top.egon.cola.component.gateway.mcp.remote.RemoteMcpPromptDriver;
import top.egon.cola.component.gateway.mcp.remote.RemoteMcpResourceDriver;
import top.egon.cola.component.gateway.mcp.remote.RemoteMcpToolDriver;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodDispatcher;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpDiscoverHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpInitializeHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpInitializedHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpPingHandler;
import top.egon.cola.component.gateway.mcp.subscription.McpResourceSubscribeHandler;
import top.egon.cola.component.gateway.mcp.subscription.McpSubscriptionService;
import top.egon.cola.component.gateway.mcp.subscription.McpSubscriptionsListenHandler;
import top.egon.cola.component.gateway.mcp.task.McpTask;
import top.egon.cola.component.gateway.mcp.task.McpTaskExecutor;
import top.egon.cola.component.gateway.mcp.task.McpTaskService;
import top.egon.cola.component.gateway.mcp.task.McpTasksCancelHandler;
import top.egon.cola.component.gateway.mcp.task.McpTasksGetHandler;
import top.egon.cola.component.gateway.mcp.task.McpTasksUpdateHandler;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;
import top.egon.cola.component.gateway.mcp.tool.McpResultBinder;
import top.egon.cola.component.gateway.mcp.tool.McpToolCatalog;
import top.egon.cola.component.gateway.mcp.tool.McpToolsCallHandler;
import top.egon.cola.component.gateway.mcp.tool.McpToolsListHandler;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;

import javax.sql.DataSource;
import java.nio.file.Path;
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

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        GatewayEngineRuntimeProperties.class,
        McpRuntimeProperties.class
})
public class GatewayEngineConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            "gateway.mcp.audit"
    );

    @Bean
    public Clock gatewayClock() {
        return Clock.systemUTC();
    }

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

    @Bean
    public GatewaySecurityCapabilityRegistry gatewaySecurityCapabilities(
            ObjectProvider<GatewayCredentialExtractor> extractors,
            ObjectProvider<GatewayAuthenticationProvider> authentications,
            ObjectProvider<GatewayAuthorizationProvider> authorizations,
            ObjectProvider<GatewayIdentityMapper> identityMappers) {
        return new GatewaySecurityCapabilityRegistry(
                extractors.orderedStream().toList(),
                authentications.orderedStream().toList(),
                authorizations.orderedStream().toList(),
                identityMappers.orderedStream().toList()
        );
    }

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

    @Bean
    public ProviderDirectory gatewayProviderDirectory(
            DdcServiceRegistryClient registry,
            @Qualifier("gatewayClock") Clock gatewayClock) {
        return new ProviderDirectory(
                new DdcProviderServiceRegistryAdapter(registry),
                gatewayClock
        );
    }

    @Bean
    public GatewayRuleChunkStore gatewayRuleChunkStore() {
        return new GatewayRuleChunkStore();
    }

    @Bean
    public PassiveHealthTracker gatewayPassiveHealthTracker(
            @Qualifier("gatewayClock") Clock gatewayClock) {
        return new PassiveHealthTracker(
                PassiveHealthPolicy.defaults(),
                gatewayClock
        );
    }

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

    @Bean
    public ActiveHealthTracker gatewayActiveHealthTracker(
            ActiveHealthProbePolicy policy) {
        return new ActiveHealthTracker(
                policy.failureThreshold(),
                policy.successThreshold()
        );
    }

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
                new top.egon.cola.component.gateway.engine.discovery
                        .ProviderCandidateFilter(
                        gatewayClock,
                        identity -> passiveHealth.eligible(identity)
                                && activeHealth.eligible(identity)
                ),
                key -> top.egon.cola.component.gateway.engine.discovery
                        .ProviderSelectionPolicy.defaults(
                        key.transport().equals("https")
                ),
                () -> activation.active() == null
                        ? Map.of()
                        : activation.active().providerPolicies()
        );
    }

    @Bean
    public GatewayTrafficGovernance gatewayTrafficGovernance(
            GatewayRuleActivationApplier activation,
            ObjectProvider<RedisTokenBucketExecutor> redis) {
        return new GatewayTrafficGovernance(
                activation::active,
                redis.getIfAvailable()
        );
    }

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

    @Bean
    public JdbcMcpRuntimeTaskStore gatewayMcpRuntimeTaskStore(
            DataSource dataSource,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new JdbcMcpRuntimeTaskStore(dataSource, objectMapper);
    }

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

    @Bean
    @ConditionalOnBean(McpTaskService.class)
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
            GatewayEngineRuntimeProperties properties,
            McpRuntimeProperties mcpProperties) {
        return new McpTaskWorker(
                tasks,
                taskExecutor(operationInvoker, objectMapper),
                properties.getNodeId(),
                mcpProperties.getTasks().getLeaseDuration(),
                mcpProperties.getTasks().getPollInterval()
        );
    }

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

    private McpTaskExecutor taskExecutor(
            EngineGatewayOperationInvoker operationInvoker,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return task -> {
            Object operation = task.inputPayload().get("operationId");
            if (!(operation instanceof String operationId)
                    || operationId.isBlank()) {
                return reactor.core.publisher.Mono.just(
                        McpTaskExecutor.Outcome.failed(Map.of(
                                "code", "MCP_TASK_OPERATION_MISSING"
                        ))
                );
            }
            Map<String, Object> pathArguments = taskArguments(
                    task.inputPayload().get("pathArguments")
            );
            Map<String, Object> queryArguments = taskArguments(
                    task.inputPayload().get("queryArguments")
            );
            Object body = taskBody(
                    task.inputPayload().get("body"),
                    task.inputPayload().get("inputResponse")
            );
            var invocation = new top.egon.cola.component.gateway.core
                    .operation.GatewayOperationInvocation(
                    new top.egon.cola.component.gateway.core.operation
                            .GatewayOperationCall(
                            operationId,
                            pathArguments,
                            queryArguments,
                            body
                    ),
                    null,
                    task.subjectId(),
                    null,
                    Map.of()
            );
            return reactor.core.publisher.Mono.from(
                    operationInvoker.invoke(invocation)
            ).map(result -> taskOutcome(result, objectMapper));
        };
    }

    private Map<String, Object> taskArguments(Object configured) {
        if (!(configured instanceof Map<?, ?> source)) {
            return Map.of();
        }
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key instanceof String name) {
                arguments.put(name, value);
            }
        });
        return Map.copyOf(arguments);
    }

    private Object taskBody(Object configured, Object inputResponse) {
        if (!(inputResponse instanceof Map<?, ?> input)) {
            return configured;
        }
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        if (configured instanceof Map<?, ?> source) {
            source.forEach((key, value) -> {
                if (key instanceof String name) {
                    body.put(name, value);
                }
            });
        }
        body.put("input", input);
        return Map.copyOf(body);
    }

    private McpTaskExecutor.Outcome taskOutcome(
            top.egon.cola.component.gateway.core.operation
                    .GatewayInvocationResult result,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        Map<String, Object> payload = taskPayload(result.body(), objectMapper);
        String inputKey = firstHeader(
                result.headers(),
                "x-egon-mcp-input-key"
        );
        if (result.statusCode() == 202 && inputKey != null) {
            return McpTaskExecutor.Outcome.inputRequired(inputKey, payload);
        }
        if (result.statusCode() >= 400) {
            return McpTaskExecutor.Outcome.failed(Map.of(
                    "code", "MCP_TASK_UPSTREAM_FAILED",
                    "status", result.statusCode()
            ));
        }
        return McpTaskExecutor.Outcome.completed(payload);
    }

    private Map<String, Object> taskPayload(
            byte[] body,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        if (body.length == 0) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    objectMapper.readTree(body);
            if (root != null && root.isObject()) {
                return objectMapper.convertValue(
                        root,
                        new com.fasterxml.jackson.core.type.TypeReference<
                                Map<String, Object>>() {
                        }
                );
            }
        } catch (Exception ignored) {
            // Non-JSON task results are returned as bounded Base64 content.
        }
        if (body.length > 1024 * 1024) {
            return Map.of("code", "MCP_TASK_RESULT_TOO_LARGE");
        }
        return Map.of(
                "contentBase64",
                java.util.Base64.getEncoder().encodeToString(body)
        );
    }

    private String firstHeader(
            Map<String, List<String>> headers,
            String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .flatMap(entry -> entry.getValue().stream())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .map(String::trim)
                .orElse(null);
    }

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

    @Bean
    public RpcProviderChannelCache gatewayRpcProviderChannels(
            GatewayEngineRuntimeProperties properties) {
        return new RpcProviderChannelCache(
                properties.getRpc().getChannelDrainTimeout(),
                transportSecurity(properties.getRpc().getTls())
        );
    }

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

    private String value(String value) {
        return value == null ? "" : value;
    }
}
