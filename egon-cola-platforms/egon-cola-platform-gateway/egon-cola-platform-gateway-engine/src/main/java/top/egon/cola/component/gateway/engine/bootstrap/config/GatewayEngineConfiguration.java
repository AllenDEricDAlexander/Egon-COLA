package top.egon.cola.component.gateway.engine.bootstrap.config;

import top.egon.cola.component.gateway.engine.rule.domain.ApiRpcGatewayCompiledRulesDTO;
import top.egon.cola.component.gateway.runtime.http.service.ReactorNettyHttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.rpc.service.RpcMethodIndex;
import top.egon.cola.component.gateway.engine.bootstrap.lifecycle.GatewayEngineRuntime;
import top.egon.cola.component.gateway.engine.common.config.GatewayEngineRuntimeProperties;
import top.egon.cola.component.gateway.runtime.http.adapter.HttpUpstreamAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.gateway.runtime.security.service.GatewaySecurityPolicyCompiler;
import top.egon.cola.component.gateway.runtime.rule.service.GatewayRuleCompilerStrategy;
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
import top.egon.cola.component.ddc.api.extension.DdcInstanceMetadataContributor;
import top.egon.cola.component.ddc.api.refresh.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.transport.GatewayTransportDefaults;
import top.egon.cola.component.gateway.core.transport.GatewayTransportSafetyLimits;
import top.egon.cola.component.gateway.runtime.provider.domain.ActiveHealthProbePolicy;
import top.egon.cola.component.gateway.runtime.provider.service.DirectoryProviderSelector;
import top.egon.cola.component.gateway.runtime.provider.service.PassiveHealthTracker;
import top.egon.cola.component.gateway.runtime.provider.service.ProviderDirectory;
import top.egon.cola.component.gateway.engine.http.service.DefaultGatewayHttpDataPlaneHandler;
import top.egon.cola.component.gateway.runtime.http.domain.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.http.service.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.http.security.RuleBackedHttpGatewaySecurityProcessor;
import top.egon.cola.component.gateway.engine.http.proxy.service.AggregatedHttpProxyStrategy;
import top.egon.cola.component.gateway.engine.http.proxy.service.GatewayHttpProxyStrategySelector;
import top.egon.cola.component.gateway.engine.http.proxy.service.StreamingHttpProxyStrategy;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.idp.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.component.gateway.engine.http.common.logging.GatewayCallAccessLogger;
import top.egon.cola.component.gateway.runtime.observability.service.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.runtime.observability.service.GatewayCallEventDispatcher;
import top.egon.cola.component.gateway.runtime.observability.service.GatewayCallEventSerializer;
import top.egon.cola.component.gateway.runtime.observability.service.GatewayCallMetricsListener;
import top.egon.cola.component.gateway.runtime.observability.domain.GatewayTelemetry;
import top.egon.cola.component.gateway.runtime.observability.adapter.KafkaGatewayCallEventSink;
import top.egon.cola.component.gateway.runtime.operation.adapter.HttpRpcUpstreamAdapter;
import top.egon.cola.component.gateway.engine.rpc.service.RpcGatewayForwarder;
import top.egon.cola.component.gateway.engine.rpc.service.RpcGatewayHandlerRegistry;
import top.egon.cola.component.gateway.engine.rpc.service.RpcGatewayServer;
import top.egon.cola.component.gateway.engine.rpc.domain.RpcGatewaySlotProperties;
import top.egon.cola.component.gateway.engine.rpc.service.RpcGatewaySlotRuntime;
import top.egon.cola.component.gateway.runtime.rpc.adapter.RpcProviderChannelCache;
import top.egon.cola.component.gateway.engine.rpc.security.RuleBackedRpcGatewaySecurityProcessor;
import top.egon.cola.component.gateway.engine.rule.service.ApiRpcGatewayRuleCompilerStrategy;
import top.egon.cola.component.gateway.runtime.rule.service.GatewayRuleActivationApplier;
import top.egon.cola.component.gateway.runtime.rule.service.GatewayRuleApplierRegistrar;
import top.egon.cola.component.gateway.runtime.rule.repository.GatewayRuleChunkStore;
import top.egon.cola.component.gateway.runtime.rule.adapter.json.GatewayRuleJsonCodec;
import top.egon.cola.component.gateway.runtime.rule.repository.GatewayRuleLkgRepository;
import top.egon.cola.component.gateway.runtime.rule.domain.GatewayRuleRuntimeStatus;
import top.egon.cola.component.gateway.runtime.security.service.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.gateway.runtime.security.service.GatewaySecurityChain;
import top.egon.cola.component.gateway.runtime.security.domain.GatewayTransportSecurity;
import top.egon.cola.component.gateway.runtime.security.service.TrustedClientAddressResolver;
import top.egon.cola.component.gateway.runtime.rule.service.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.runtime.traffic.service.RedisTokenBucketExecutor;
import top.egon.cola.component.gateway.runtime.traffic.adapter.RedissonRedisTokenBucketExecutor;
import top.egon.cola.component.gateway.engine.http.service.GatewayTransportDispatcher;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketProxy;
import top.egon.cola.component.gateway.engine.http.websocket.adapter.ReactorNettyWebSocketUpstreamAdapter;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.springframework.context.annotation.Import;
import top.egon.cola.component.gateway.runtime.config.GatewayRuntimeConfiguration;
import top.egon.cola.component.gateway.contract.runtime.GatewayEngineRoleEnum;

/**
 * 中文说明：{@code GatewayEngineConfiguration} 是配置类，位于当前 Gateway 模块的相关包中，负责网关引擎配置相关的职责与边界。
 * English summary: {@code GatewayEngineConfiguration} is a gateway engine configuration configuration in the current Gateway module; it owns the gateway engine configuration-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(value = "gatewayEngineConfiguration", proxyBeanMethods = false)
@Import(GatewayRuntimeConfiguration.class)
@EnableConfigurationProperties(GatewayEngineRuntimeProperties.class)
public class GatewayEngineConfiguration {

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
     * 中文说明：在当前 Engine 的数据目录维护独立 LKG。
     * English summary: Keeps the last-known-good repository local to this executable.
     */
    @Bean("gatewayRuleLkgRepository")
    public GatewayRuleLkgRepository gatewayRuleLkgRepository(
            GatewayEngineRuntimeProperties properties) {
        return new GatewayRuleLkgRepository(
                Path.of(properties.getDataDirectory()), properties.getGatewayGroupCode());
    }

    /**
     * 中文说明：使用现有安全能力集合构建策略编译器。
     * English summary: Compiles security policy using the registered capabilities.
     */
    @Bean("gatewaySecurityPolicyCompiler")
    public GatewaySecurityPolicyCompiler gatewaySecurityPolicyCompiler(
            @Qualifier("gatewaySecurityCapabilities") GatewaySecurityCapabilityRegistry capabilities) {
        return new GatewaySecurityPolicyCompiler(capabilities);
    }

    /**
     * 中文说明：中间步骤保留全部协议编译，双进程切换时收窄为 API/RPC。
     * English summary: Names the compatible mixed compiler Strategy until the atomic ownership split.
     */
    @Bean(name = {"gatewayRuleCompilerStrategy", "apiRpcGatewayRuleCompilerStrategy"})
    public ApiRpcGatewayRuleCompilerStrategy gatewayRuleCompilerStrategy(
            @Qualifier("gatewaySecurityPolicyCompiler") GatewaySecurityPolicyCompiler security,
            @Qualifier("gatewayTransportDefaults") GatewayTransportDefaults defaults,
            @Qualifier("gatewayTransportSafetyLimits") GatewayTransportSafetyLimits limits) {
        return new ApiRpcGatewayRuleCompilerStrategy(security, defaults, limits);
    }

    /**
     * 中文说明：按固定编译策略装配局部原子激活，并注册同一个 DDC Active Key。
     * English summary: Wires one role-local activation pipeline and registers the shared DDC key.
     */
    @Bean("gatewayRuleActivationApplier")
    public GatewayRuleActivationApplier<ApiRpcGatewayCompiledRulesDTO> gatewayRuleActivationApplier(
            @Qualifier("ddcConfigApplierRegistry") DdcConfigApplierRegistry applierRegistry,
            @Qualifier("gatewayRuleJsonCodec") GatewayRuleJsonCodec codec,
            @Qualifier("gatewayRuleCompilerStrategy") GatewayRuleCompilerStrategy<ApiRpcGatewayCompiledRulesDTO> compiler,
            @Qualifier("gatewayRuleChunkStore") GatewayRuleChunkStore chunks,
            @Qualifier("gatewayProviderDirectory") ProviderDirectory providerDirectory,
            @Qualifier("gatewayRuleLkgRepository") GatewayRuleLkgRepository lkg,
            @Qualifier("gatewayClock") Clock gatewayClock,
            @Qualifier("gatewayTelemetry") GatewayTelemetry telemetry) {
        GatewayRuleActivationApplier<ApiRpcGatewayCompiledRulesDTO> activation =
                new GatewayRuleActivationApplier<>(
                        codec, compiler, chunks, providerDirectory, lkg, gatewayClock, telemetry);
        GatewayRuleApplierRegistrar.register(applierRegistry, activation, chunks);
        return activation;
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
     * @return 返回 网关Http服务器 的处理结果；returns the result of the operation.
     */
    @Bean
    public GatewayHttpServer gatewayHttpServer(
            GatewayEngineRuntimeProperties properties,
            GatewayHttpEngineProperties engineProperties,
            GatewayRuleActivationApplier<ApiRpcGatewayCompiledRulesDTO> activation,
            DirectoryProviderSelector providerSelector,
            ReactorNettyHttpUpstreamAdapter upstream,
            GatewaySecurityCapabilityRegistry capabilities,
            @Qualifier("gatewayCallCompletionListener")
            GatewayCallCompletionListener completionListener,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream,
            PassiveHealthTracker passiveHealth,
            GatewayTelemetry telemetry,
            GatewayTransportDispatcher transportDispatcher) {
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
        return new GatewayHttpServer(engineProperties, handler);
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
            GatewayRuleActivationApplier<ApiRpcGatewayCompiledRulesDTO> activation,
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
     * 创建携带 IdP PLATFORM SERVICE Token 的 Gateway RPC Slot 运行时。
     * / Creates the Gateway RPC-slot runtime that carries an IdP PLATFORM SERVICE token.
     *
     * @param registry DDC 服务注册客户端 / DDC service-registry client
     * @param serviceKeyFactory 服务键工厂 / service-key factory
     * @param ddcIdentity Gateway 的 DDC 实例身份 / Gateway DDC instance identity
     * @param properties Gateway Engine 配置 / Gateway Engine configuration
     * @param serviceClient IdP OAuth2 Client facade / IdP OAuth2 Client facade
     * @param idpProperties IdP client settings / IdP client settings
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
            IdpServiceOAuth2Client serviceClient,
            IdpStarterProperties idpProperties) {
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
                serviceClient,
                idpProperties
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
    @Bean(name = {"apiRpcGatewayEngineRuntime", "gatewayEngineRuntime"})
    public GatewayEngineRuntime gatewayEngineRuntime(
            GatewayEngineRuntimeProperties properties,
            GatewayHttpServer httpServer,
            RpcGatewayServer rpcServer,
            RpcGatewaySlotRuntime rpcSlot,
            GatewayRuleActivationApplier<ApiRpcGatewayCompiledRulesDTO> activation,
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
            GatewayRuleActivationApplier<ApiRpcGatewayCompiledRulesDTO> activation) {
        return () -> {
            GatewayRuleRuntimeStatus status = activation.status();
            return Map.of(
                    "gateway.engine.role", GatewayEngineRoleEnum.API_RPC.name(),
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
            GatewayRuleActivationApplier<ApiRpcGatewayCompiledRulesDTO> activation,
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
