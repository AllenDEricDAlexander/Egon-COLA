package top.egon.cola.component.gateway.engine;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.observation.ObservationRegistry;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.service.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.DdcInstanceMetadataContributor;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
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
import top.egon.cola.component.gateway.engine.http.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.http.ReactorNettyHttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.http.RuleBackedHttpGatewaySecurityProcessor;
import top.egon.cola.component.gateway.engine.observability.GatewayCallAccessLogger;
import top.egon.cola.component.gateway.engine.observability.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.observability.GatewayCallEventDispatcher;
import top.egon.cola.component.gateway.engine.observability.GatewayCallEventSerializer;
import top.egon.cola.component.gateway.engine.observability.GatewayCallMetricsListener;
import top.egon.cola.component.gateway.engine.observability.GatewayTelemetry;
import top.egon.cola.component.gateway.engine.observability.KafkaGatewayCallEventSink;
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

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayEngineRuntimeProperties.class)
public class GatewayEngineConfiguration {

    @Bean
    public Clock gatewayClock() {
        return Clock.systemUTC();
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
    public ProviderDirectory gatewayProviderDirectory(
            DdcServiceRegistryClient registry,
            Clock gatewayClock) {
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
            Clock gatewayClock) {
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
            GatewayRuleChunkStore chunks,
            ProviderDirectory providerDirectory,
            GatewayEngineRuntimeProperties properties,
            Clock gatewayClock,
            GatewayTelemetry telemetry) {
        GatewayRuleActivationApplier activation =
                new GatewayRuleActivationApplier(
                        new GatewayRuleJsonCodec(),
                        new EngineGatewayRuleCompiler(capabilities),
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
            Clock gatewayClock) {
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
            GatewayRuleActivationApplier activation,
            DirectoryProviderSelector providerSelector,
            ReactorNettyHttpUpstreamAdapter upstream,
            GatewaySecurityCapabilityRegistry capabilities,
            GatewayCallCompletionListener completionListener,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream,
            PassiveHealthTracker passiveHealth,
            GatewayTelemetry telemetry) {
        GatewayEngineRuntimeProperties.Http http = properties.getHttp();
        GatewayHttpEngineProperties engineProperties =
                new GatewayHttpEngineProperties(
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
                        http.getUpstreamPendingAcquireMaxCount()
                );
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
                telemetry
        );
        return new GatewayHttpServer(engineProperties, handler);
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
    public RpcGatewayHandlerRegistry gatewayRpcHandlerRegistry(
            GatewayEngineRuntimeProperties properties,
            GatewayRuleActivationApplier activation,
            DirectoryProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            GatewaySecurityCapabilityRegistry capabilities,
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

    @Bean
    public RpcGatewaySlotRuntime gatewayRpcSlotRuntime(
            DdcServiceRegistryClient registry,
            GatewayEngineRuntimeProperties properties) {
        GatewayEngineRuntimeProperties.Rpc rpc = properties.getRpc();
        return new RpcGatewaySlotRuntime(
                registry,
                new RpcGatewaySlotProperties(
                        rpc.isEnabled(),
                        properties.getEnv(),
                        properties.getNamespace(),
                        properties.getInstanceId(),
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
                )
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
    public HealthIndicator gatewayEngineHealthIndicator(
            GatewayEngineRuntime runtime,
            GatewayRuleActivationApplier activation) {
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
                    .build();
        };
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
