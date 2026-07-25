package top.egon.cola.component.gateway.engine;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.service.DdcConfigApplierRegistry;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.engine.balance.LoadBalancerType;
import top.egon.cola.component.gateway.engine.balance.ProviderLoadBalancers;
import top.egon.cola.component.gateway.engine.discovery.DdcProviderServiceRegistryAdapter;
import top.egon.cola.component.gateway.engine.discovery.DirectoryProviderSelector;
import top.egon.cola.component.gateway.engine.discovery.ProviderDirectory;
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
import top.egon.cola.component.gateway.engine.observability.KafkaGatewayCallEventSink;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayForwarder;
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
import top.egon.cola.component.gateway.engine.security.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityChain;
import top.egon.cola.component.gateway.engine.security.TrustedClientAddressResolver;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayEngineRuntimeProperties.class)
public class GatewayEngineConfiguration {

    @Bean
    public Clock gatewayClock() {
        return Clock.systemUTC();
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
    public GatewayRuleActivationApplier gatewayRuleActivationApplier(
            DdcConfigApplierRegistry applierRegistry,
            GatewaySecurityCapabilityRegistry capabilities,
            GatewayRuleChunkStore chunks,
            ProviderDirectory providerDirectory,
            GatewayEngineRuntimeProperties properties,
            Clock gatewayClock) {
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
                        gatewayClock
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
            ProviderDirectory providerDirectory) {
        return new DirectoryProviderSelector(
                providerDirectory,
                ProviderLoadBalancers.create(LoadBalancerType.ROUND_ROBIN)
        );
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
            GatewayEngineRuntimeProperties properties) {
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
                        )
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
            GatewayCallCompletionListener completionListener) {
        GatewayEngineRuntimeProperties.Http http = properties.getHttp();
        GatewayHttpEngineProperties engineProperties =
                new GatewayHttpEngineProperties(
                        new GatewayHttpEngineProperties.Listener(
                                http.isPublicEnabled(),
                                http.getPublicHost(),
                                http.getPublicPort()
                        ),
                        new GatewayHttpEngineProperties.Listener(
                                http.isInternalEnabled(),
                                http.getInternalHost(),
                                http.getInternalPort()
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
                properties.getNodeId()
        );
        return new GatewayHttpServer(engineProperties, handler);
    }

    @Bean
    public RpcProviderChannelCache gatewayRpcProviderChannels(
            GatewayEngineRuntimeProperties properties) {
        return new RpcProviderChannelCache(
                properties.getRpc().getChannelDrainTimeout()
        );
    }

    @Bean
    public RpcGatewayHandlerRegistry gatewayRpcHandlerRegistry(
            GatewayEngineRuntimeProperties properties,
            GatewayRuleActivationApplier activation,
            DirectoryProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            GatewaySecurityCapabilityRegistry capabilities,
            GatewayCallCompletionListener completionListener) {
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
                properties.getNodeId()
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
                registry
        );
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
            GatewayRuleActivationApplier activation) {
        return new GatewayEngineRuntime(
                properties,
                httpServer,
                rpcServer,
                rpcSlot,
                activation
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
}
