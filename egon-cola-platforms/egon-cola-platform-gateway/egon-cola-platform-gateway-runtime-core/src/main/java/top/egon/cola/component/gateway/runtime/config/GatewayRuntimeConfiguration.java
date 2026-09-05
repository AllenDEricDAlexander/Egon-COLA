package top.egon.cola.component.gateway.runtime.config;

import top.egon.cola.component.gateway.runtime.provider.domain.ProviderSelectionPolicy;
import top.egon.cola.component.gateway.runtime.provider.service.ProviderCandidateFilter;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayCredentialRecoveryProvider;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.runtime.provider.domain.ActiveHealthProbePolicy;
import top.egon.cola.component.gateway.runtime.provider.service.ActiveHealthTracker;
import top.egon.cola.component.gateway.runtime.provider.adapter.DdcProviderServiceRegistryAdapter;
import top.egon.cola.component.gateway.runtime.provider.service.DirectoryProviderSelector;
import top.egon.cola.component.gateway.runtime.provider.adapter.HttpProviderActiveHealthProbe;
import top.egon.cola.component.gateway.runtime.provider.domain.PassiveHealthPolicy;
import top.egon.cola.component.gateway.runtime.provider.service.PassiveHealthTracker;
import top.egon.cola.component.gateway.runtime.provider.service.ProviderActiveHealthMonitor;
import top.egon.cola.component.gateway.runtime.provider.service.ProviderDirectory;
import top.egon.cola.component.gateway.runtime.rpc.adapter.RpcProviderActiveHealthProbe;
import top.egon.cola.component.gateway.runtime.observability.domain.GatewayTelemetry;
import top.egon.cola.component.gateway.runtime.operation.adapter.HttpRpcUpstreamAdapter;
import top.egon.cola.component.gateway.runtime.rpc.adapter.RpcProviderChannelCache;
import top.egon.cola.component.gateway.runtime.rule.service.GatewayRuleActivationApplier;
import top.egon.cola.component.gateway.runtime.rule.repository.GatewayRuleChunkStore;
import top.egon.cola.component.gateway.runtime.rule.adapter.json.GatewayRuleJsonCodec;
import top.egon.cola.component.gateway.runtime.security.service.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.gateway.runtime.rule.service.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.runtime.traffic.service.RedisTokenBucketExecutor;
import java.time.Clock;
import java.util.Map;

/**
 * 中文说明：两个 Engine 共用、且不读取角色启动配置的 Bean 装配。
 * English summary: Property-independent assembly; every imported context owns separate runtime state.
 * 用法 / Usage: Import from a role configuration, which supplies its own compiler, activation and transports.
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(value = "gatewayRuntimeConfiguration", proxyBeanMethods = false)
public class GatewayRuntimeConfiguration {

    /**
     * 中文说明：执行 网关Clock 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway clock operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewayClock(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 网关Clock 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayClock")
    public Clock gatewayClock() {
        return Clock.systemUTC();
    }

    /**
     * 中文说明：执行 网关遥测 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway telemetry operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewayTelemetry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param observationRegistry 参数 观测注册表；parameter observation registry。
     * @param samplingProbability 参数 samplingProbability；parameter sampling probability。
     * @return 返回 网关遥测 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayTelemetry")
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
     * 中文说明：执行 网关安全Capabilities 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway security capabilities operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewaySecurityCapabilities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param extractors 参数 extractors；parameter extractors。
     * @param authentications 参数 authentications；parameter authentications。
     * @param authorizations 参数 authorizations；parameter authorizations。
     * @param identityMappers 参数 身份Mappers；parameter identity mappers。
     * @return 返回 网关安全Capabilities 的处理结果；returns the result of the operation.
     */
    @Bean("gatewaySecurityCapabilities")
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
     * 中文说明：执行 网关提供方Directory 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway provider directory operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewayProviderDirectory(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param registry 参数 注册表；parameter registry。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @return 返回 网关提供方Directory 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayProviderDirectory")
    public ProviderDirectory gatewayProviderDirectory(
            DdcServiceRegistryClient registry,
            @Qualifier("gatewayClock") Clock gatewayClock) {
        return new ProviderDirectory(
                new DdcProviderServiceRegistryAdapter(registry),
                gatewayClock
        );
    }

    /**
     * 中文说明：执行 网关规则Chunk存储 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway rule chunk store operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewayRuleChunkStore(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 网关规则Chunk存储 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayRuleChunkStore")
    public GatewayRuleChunkStore gatewayRuleChunkStore() {
        return new GatewayRuleChunkStore();
    }

    /**
     * 中文说明：执行 网关Passive健康Tracker 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway passive health tracker operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewayPassiveHealthTracker(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @return 返回 网关Passive健康Tracker 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayPassiveHealthTracker")
    public PassiveHealthTracker gatewayPassiveHealthTracker(
            @Qualifier("gatewayClock") Clock gatewayClock) {
        return new PassiveHealthTracker(
                PassiveHealthPolicy.defaults(),
                gatewayClock
        );
    }

    /**
     * 中文说明：执行 网关Active健康Tracker 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway active health tracker operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewayActiveHealthTracker(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @return 返回 网关Active健康Tracker 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayActiveHealthTracker")
    public ActiveHealthTracker gatewayActiveHealthTracker(
            ActiveHealthProbePolicy policy) {
        return new ActiveHealthTracker(
                policy.failureThreshold(),
                policy.successThreshold()
        );
    }

    /**
     * 中文说明：保留规范化 Snapshot 的同一 JSON 编解码与校验规则。
     * English summary: Supplies the canonical snapshot codec to role-local activation.
     */
    @Bean("gatewayRuleJsonCodec")
    public GatewayRuleJsonCodec gatewayRuleJsonCodec() {
        return new GatewayRuleJsonCodec();
    }

    /**
     * 中文说明：执行 网关提供方Selector 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway provider selector operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewayProviderSelector(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param providerDirectory 参数 提供方Directory；parameter provider directory。
     * @param activation 参数 activation；parameter activation。
     * @param passiveHealth 参数 passive健康；parameter passive health。
     * @param activeHealth 参数 active健康；parameter active health。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @return 返回 网关提供方Selector 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayProviderSelector")
    public DirectoryProviderSelector gatewayProviderSelector(
            ProviderDirectory providerDirectory,
            GatewayRuleActivationApplier<?> activation,
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
     * 中文说明：执行 网关流量Governance 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway traffic governance operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewayTrafficGovernance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @param redis 参数 redis；parameter redis。
     * @return 返回 网关流量Governance 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayTrafficGovernance")
    public GatewayTrafficGovernance gatewayTrafficGovernance(
            GatewayRuleActivationApplier<?> activation,
            ObjectProvider<RedisTokenBucketExecutor> redis) {
        return new GatewayTrafficGovernance(
                activation::active,
                redis.getIfAvailable()
        );
    }

    /**
     * 中文说明：执行 网关提供方Active健康监控器 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway provider active health monitor operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewayProviderActiveHealthMonitor(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param directory 参数 directory；parameter directory。
     * @param channels 参数 channels；parameter channels。
     * @param tracker 参数 tracker；parameter tracker。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 网关提供方Active健康监控器 的处理结果；returns the result of the operation.
     */
    @Bean(name = "gatewayProviderActiveHealthMonitor", destroyMethod = "close")
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
     * 中文说明：执行 网关HttpRpcUpstreamAdapter 操作；该方法是 {@code GatewayRuntimeConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway http rpc upstream adapter operation; this method is the invocation entry point on {@code GatewayRuntimeConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuntimeConfiguration.gatewayHttpRpcUpstreamAdapter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @param channels 参数 channels；parameter channels。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @return 返回 网关HttpRpcUpstreamAdapter 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayHttpRpcUpstreamAdapter")
    public HttpRpcUpstreamAdapter gatewayHttpRpcUpstreamAdapter(
            GatewayRuleActivationApplier<?> activation,
            RpcProviderChannelCache channels,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new HttpRpcUpstreamAdapter(
                activation::active,
                channels,
                objectMapper
        );
    }
}
