package top.egon.cola.component.yuheng.mcp.engine.bootstrap.config;

import top.egon.cola.component.yuheng.runtime.http.service.ReactorNettyHttpUpstreamAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.observation.ObservationRegistry;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.yuheng.runtime.traffic.service.RedisTokenBucketExecutor;
import top.egon.cola.component.yuheng.runtime.traffic.adapter.RedissonRedisTokenBucketExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleCompilerStrategy;
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
import top.egon.cola.component.tianshu.api.extension.DdcInstanceMetadataContributor;
import top.egon.cola.component.tianshu.api.refresh.DdcConfigApplierRegistry;
import top.egon.cola.component.yuheng.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.yuheng.runtime.provider.domain.ActiveHealthProbePolicy;
import top.egon.cola.component.yuheng.runtime.provider.service.DirectoryProviderSelector;
import top.egon.cola.component.yuheng.runtime.provider.service.ProviderDirectory;
import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.JdbcMcpRuntimeTaskStore;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpAuditPublisher;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpEngineHttpHandler;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpRuntimeHealthIndicator;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.McpRuntimeProperties;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpTaskOperationExecutor;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpTaskServiceTokenSupplier;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpTaskWorker;
import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.MicrometerMcpTelemetry;
import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.RedisMcpSessionStore;
import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.remote.ReactorNettyRemoteMcpClient;
import top.egon.cola.component.yuheng.runtime.observability.domain.GatewayTelemetry;
import top.egon.cola.component.yuheng.runtime.operation.adapter.DefaultGatewayOperationTransport;
import top.egon.cola.component.yuheng.runtime.operation.service.EngineGatewayOperationInvoker;
import top.egon.cola.component.yuheng.runtime.operation.adapter.HttpRpcUpstreamAdapter;
import top.egon.cola.component.yuheng.runtime.rpc.adapter.RpcProviderChannelCache;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleActivationApplier;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleApplierRegistrar;
import top.egon.cola.component.yuheng.runtime.rule.repository.GatewayRuleChunkStore;
import top.egon.cola.component.yuheng.runtime.rule.adapter.json.GatewayRuleJsonCodec;
import top.egon.cola.component.yuheng.runtime.rule.repository.GatewayRuleLkgRepository;
import top.egon.cola.component.yuheng.runtime.rule.domain.GatewayRuleRuntimeStatus;
import top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.yuheng.runtime.security.domain.GatewayTransportSecurity;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayTrafficGovernance;
import top.egon.cola.component.yuheng.mcp.remote.service.McpRemoteClientPool;
import top.egon.cola.component.yuheng.mcp.task.service.McpTaskService;
import top.egon.cola.component.yuheng.mcp.common.telemetry.McpTelemetry;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.SingleFlightSnapshotLoader;
import javax.sql.DataSource;
import java.net.URI;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import top.egon.cola.component.yuheng.mcp.engine.config.McpGatewayEngineProperties;
import top.egon.cola.component.yuheng.mcp.engine.rule.domain.McpGatewayCompiledRulesDTO;
import top.egon.cola.component.yuheng.mcp.engine.rule.service.McpGatewayRuleCompilerStrategy;
import top.egon.cola.component.yuheng.mcp.engine.bootstrap.lifecycle.McpGatewayEngineRuntime;
import top.egon.cola.component.yuheng.mcp.engine.http.service.McpGatewayHttpServer;
import top.egon.cola.component.yuheng.mcp.engine.http.service.McpGatewayHttpDataPlaneHandlerAdapter;
import top.egon.cola.component.yuheng.runtime.config.GatewayRuntimeConfiguration;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;
import org.springframework.context.annotation.Import;
import org.redisson.Redisson;
import org.redisson.config.Config;
import top.egon.cola.component.yuheng.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.yuheng.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.FileSystemMcpAppArtifactReader;
import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.HttpMcpTaskServiceTokenSupplier;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpGatewayIdentityAuthenticator;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.security.JdbcMcpApprovalAdapter;
import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.security.Rbac3McpAuthorizationAdapter;
import top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityChain;
import top.egon.cola.component.yuheng.mcp.app.service.AppUiResourceDriver;
import top.egon.cola.component.yuheng.mcp.app.service.McpAppRuntime;
import top.egon.cola.component.yuheng.mcp.app.domain.McpAppSecurityValidator;
import top.egon.cola.component.yuheng.mcp.completion.service.DictionaryCompletionProvider;
import top.egon.cola.component.yuheng.mcp.completion.service.McpCompletionHandler;
import top.egon.cola.component.yuheng.mcp.completion.service.OperationCompletionProvider;
import top.egon.cola.component.yuheng.mcp.prompt.service.McpPromptDriver;
import top.egon.cola.component.yuheng.mcp.prompt.service.McpPromptsGetHandler;
import top.egon.cola.component.yuheng.mcp.prompt.service.McpPromptsListHandler;
import top.egon.cola.component.yuheng.mcp.prompt.service.OperationPromptDriver;
import top.egon.cola.component.yuheng.mcp.prompt.service.StaticPromptDriver;
import top.egon.cola.component.yuheng.mcp.prompt.domain.StrictPromptTemplate;
import top.egon.cola.component.yuheng.mcp.remote.service.McpDialectTranslator;
import top.egon.cola.component.yuheng.mcp.remote.service.McpNamespaceRouter;
import top.egon.cola.component.yuheng.mcp.remote.service.RemoteMcpCompletionProvider;
import top.egon.cola.component.yuheng.mcp.remote.service.RemoteMcpPromptDriver;
import top.egon.cola.component.yuheng.mcp.remote.service.RemoteMcpResourceDriver;
import top.egon.cola.component.yuheng.mcp.remote.service.RemoteMcpToolDriver;
import top.egon.cola.component.yuheng.mcp.resource.adapter.DatabaseSchemaResourceDriver;
import top.egon.cola.component.yuheng.mcp.resource.service.McpResourceCatalog;
import top.egon.cola.component.yuheng.mcp.resource.service.McpResourceDriver;
import top.egon.cola.component.yuheng.mcp.resource.service.McpResourceTemplatesListHandler;
import top.egon.cola.component.yuheng.mcp.resource.domain.McpResourceUriValidator;
import top.egon.cola.component.yuheng.mcp.resource.service.McpResourcesListHandler;
import top.egon.cola.component.yuheng.mcp.resource.service.McpResourcesReadHandler;
import top.egon.cola.component.yuheng.mcp.resource.adapter.ObjectStorageResourceDriver;
import top.egon.cola.component.yuheng.mcp.resource.adapter.OperationResourceDriver;
import top.egon.cola.component.yuheng.mcp.resource.adapter.StaticBlobResourceDriver;
import top.egon.cola.component.yuheng.mcp.resource.adapter.StaticTextResourceDriver;
import top.egon.cola.component.yuheng.mcp.rule.domain.CompiledMcpRules;
import top.egon.cola.component.yuheng.mcp.common.security.McpSecurityGate;
import top.egon.cola.component.yuheng.mcp.server.service.McpMethodDispatcher;
import top.egon.cola.component.yuheng.mcp.server.service.McpMethodHandler;
import top.egon.cola.component.yuheng.mcp.server.service.handler.McpDiscoverHandler;
import top.egon.cola.component.yuheng.mcp.server.service.handler.McpInitializeHandler;
import top.egon.cola.component.yuheng.mcp.server.service.handler.McpInitializedHandler;
import top.egon.cola.component.yuheng.mcp.server.service.handler.McpPingHandler;
import top.egon.cola.component.yuheng.mcp.subscription.service.McpResourceSubscribeHandler;
import top.egon.cola.component.yuheng.mcp.subscription.service.McpSubscriptionService;
import top.egon.cola.component.yuheng.mcp.subscription.service.McpSubscriptionsListenHandler;
import top.egon.cola.component.yuheng.mcp.task.service.McpTasksCancelHandler;
import top.egon.cola.component.yuheng.mcp.task.service.McpTasksGetHandler;
import top.egon.cola.component.yuheng.mcp.task.service.McpTasksUpdateHandler;
import top.egon.cola.component.yuheng.mcp.tool.service.McpResultBinder;
import top.egon.cola.component.yuheng.mcp.tool.service.McpToolCatalog;
import top.egon.cola.component.yuheng.mcp.tool.service.McpToolsCallHandler;
import top.egon.cola.component.yuheng.mcp.tool.service.McpToolsListHandler;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * 中文说明：MCP 专属装配，沿用既有协议、会话、任务和安全适配器。
 * English summary: Owns MCP ingress and state; operations invoke providers through shared transports.
 * 用法 / Usage: Loaded only by the MCP executable; no API or RPC ingress beans are imported.
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(value = "mcpGatewayEngineConfiguration", proxyBeanMethods = false)
@Import(GatewayRuntimeConfiguration.class)
@EnableConfigurationProperties({McpGatewayEngineProperties.class, McpRuntimeProperties.class})
public class McpGatewayEngineConfiguration {

    /**
     * 中文说明：执行 网关远程MCPAuthentication 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway remote mcp authentication operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayRemoteMcpAuthentication(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 网关远程MCPAuthentication 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayRemoteMcpAuthentication")
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
     * 中文说明：执行 网关远程MCP客户端池 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway remote mcp client pool operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayRemoteMcpClientPool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authentication 参数 authentication；parameter authentication。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关远程MCP客户端池 的处理结果；returns the result of the operation.
     */
    @Bean(name = "gatewayRemoteMcpClientPool", destroyMethod = "close")
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
     * 中文说明：执行 网关MCP遥测 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp telemetry operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayMcpTelemetry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param meters 参数 meters；parameter meters。
     * @param observations 参数 observations；parameter observations。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关MCP遥测 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayMcpTelemetry")
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
                    json -> log.info("MCP_RUNTIME_AUDIT {}", json)
            ));
        }
        return McpTelemetry.composite(observers);
    }

    /**
     * 中文说明：在当前 Engine 的数据目录维护独立 LKG。
     * English summary: Keeps the last-known-good repository local to this executable.
     */
    @Bean("gatewayRuleLkgRepository")
    public GatewayRuleLkgRepository gatewayRuleLkgRepository(
            McpGatewayEngineProperties properties) {
        return new GatewayRuleLkgRepository(
                Path.of(properties.dataDirectory()), properties.gatewayGroupCode());
    }

    /**
     * 中文说明：按固定编译策略装配局部原子激活，并注册同一个 DDC Active Key。
     * English summary: Wires one role-local activation pipeline and registers the shared DDC key.
     */
    @Bean("gatewayRuleActivationApplier")
    public GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> gatewayRuleActivationApplier(
            @Qualifier("ddcConfigApplierRegistry") DdcConfigApplierRegistry applierRegistry,
            @Qualifier("gatewayRuleJsonCodec") GatewayRuleJsonCodec codec,
            @Qualifier("gatewayRuleCompilerStrategy") GatewayRuleCompilerStrategy<McpGatewayCompiledRulesDTO> compiler,
            @Qualifier("gatewayRuleChunkStore") GatewayRuleChunkStore chunks,
            @Qualifier("gatewayProviderDirectory") ProviderDirectory providerDirectory,
            @Qualifier("gatewayRuleLkgRepository") GatewayRuleLkgRepository lkg,
            @Qualifier("gatewayClock") Clock gatewayClock,
            @Qualifier("gatewayTelemetry") GatewayTelemetry telemetry) {
        GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation =
                new GatewayRuleActivationApplier<>(
                        codec, compiler, chunks, providerDirectory, lkg, gatewayClock, telemetry);
        GatewayRuleApplierRegistrar.register(applierRegistry, activation, chunks);
        return activation;
    }

    /**
     * 中文说明：执行 网关MCPRedisson客户端 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp redisson client operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayMcpRedissonClient(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 网关MCP会话存储 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp session store operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayMcpSessionStore(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param redisson 参数 redisson；parameter redisson。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @param keyPrefix 参数 键Prefix；parameter key prefix。
     * @param maximumStreamLength 参数 maximumStreamLength；parameter maximum stream length。
     * @return 返回 网关MCP会话存储 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayMcpSessionStore")
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
     * 中文说明：执行 网关MCP运行时任务存储 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp runtime task store operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayMcpRuntimeTaskStore(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param dataSource 参数 dataSource；parameter data source。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @return 返回 网关MCP运行时任务存储 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayMcpRuntimeTaskStore")
    public JdbcMcpRuntimeTaskStore gatewayMcpRuntimeTaskStore(
            DataSource dataSource,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new JdbcMcpRuntimeTaskStore(dataSource, objectMapper);
    }

    /**
     * 中文说明：执行 网关MCP任务服务 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp task service operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayMcpTaskService(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param store 参数 存储；parameter store。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param gatewayClock 参数 网关Clock；parameter gateway clock。
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关MCP任务服务 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayMcpTaskService")
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
     * 补充说明 / Supplementary summary: 执行 网关MCP任务服务TokenSupplier 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the gateway mcp task service token supplier operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayMcpTaskServiceTokenSupplier(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Bean("gatewayMcpTaskServiceTokenSupplier")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.mcp.tasks.service-token",
            name = "enabled",
            havingValue = "true"
    )
    public McpTaskServiceTokenSupplier gatewayMcpTaskServiceTokenSupplier(
            @Value("${egon.cola.component.gateway.engine.mcp.tasks.service-token.scopes}")
            Set<String> scopes,
            @Value("${egon.cola.component.gateway.engine.mcp.tasks.service-token.renewal-skew}")
            Duration renewalSkew,
            @Qualifier("gatewayClock") Clock gatewayClock,
            IdpServiceOAuth2Client serviceClient,
            IdpStarterProperties idpProperties) {
        return new HttpMcpTaskServiceTokenSupplier(
                serviceClient,
                idpProperties,
                scopes,
                renewalSkew,
                gatewayClock
        );
    }

    /**
     * 中文说明：执行 网关MCP任务Worker 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp task worker operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayMcpTaskWorker(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tasks 参数 tasks；parameter tasks。
     * @param operationInvoker 参数 操作Invoker；parameter operation invoker。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param activation 参数 activation；parameter activation。
     * @param tokenSupplier 参数 tokenSupplier；parameter token supplier。
     * @param properties 参数 properties；parameter properties。
     * @param mcpProperties 参数 MCPProperties；parameter mcp properties。
     * @return 返回 网关MCP任务Worker 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayMcpTaskWorker")
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
            GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation,
            McpTaskServiceTokenSupplier tokenSupplier,
            McpGatewayEngineProperties properties,
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
                properties.nodeId(),
                mcpProperties.getTasks().getLeaseDuration(),
                mcpProperties.getTasks().getPollInterval()
        );
    }

    /**
     * 中文说明：执行 网关MCPHttp处理器 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp http handler operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayMcpHttpHandler(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
    @Bean("gatewayMcpHttpHandler")
    @ConditionalOnBean(RedisMcpSessionStore.class)
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.mcp",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public McpEngineHttpHandler gatewayMcpHttpHandler(
            GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation,
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
            McpGatewayEngineProperties properties,
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
                        properties.nodeId(),
                        gatewayClock
                ),
                objectMapper,
                gatewayClock,
                sessionTtl,
                streamWait,
                Math.toIntExact(Math.min(
                        Math.min(
                                properties.listener().maximumRequestBytes(),
                                mcpProperties.getMaximumRequestBytes()
                        ),
                        Integer.MAX_VALUE
                ))
        );
    }

    /**
     * 中文说明：执行 read数据库模式 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read database schema operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.readDatabaseSchema(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 网关操作Invoker 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway operation invoker operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayOperationInvoker(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param http 参数 http；parameter http。
     * @param rpc 参数 rpc；parameter rpc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param properties 参数 properties；parameter properties。
     * @return 返回 网关操作Invoker 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayOperationInvoker")
    public EngineGatewayOperationInvoker gatewayOperationInvoker(
            GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation,
            DirectoryProviderSelector providerSelector,
            GatewayTrafficGovernance trafficGovernance,
            ReactorNettyHttpUpstreamAdapter http,
            HttpRpcUpstreamAdapter rpc,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            McpGatewayEngineProperties properties) {
        long maximumRequestBytes = properties.outbound().maximumRequestBytes();
        long maximumResponseBytes = properties.outbound().maximumResponseBytes();
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
                properties.outbound().requestTimeout(),
                maximumRequestBytes,
                maximumResponseBytes
        );
    }

    /**
     * 中文说明：执行 registerCertificateExpiry 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the register certificate expiry operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.registerCertificateExpiry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 网关MCP运行时健康Indicator 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway mcp runtime health indicator operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.gatewayMcpRuntimeHealthIndicator(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @param sessionStores 参数 会话Stores；parameter session stores。
     * @param taskServices 参数 任务Services；parameter task services。
     * @param properties 参数 properties；parameter properties。
     * @param remoteClients 参数 远程Clients；parameter remote clients。
     * @return 返回 网关MCP运行时健康Indicator 的处理结果；returns the result of the operation.
     */
    @Bean("gatewayMcpRuntimeHealthIndicator")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.engine.mcp",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public McpRuntimeHealthIndicator gatewayMcpRuntimeHealthIndicator(
            GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation,
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
     * 中文说明：执行 值 操作；该方法是 {@code McpGatewayEngineConfiguration} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the value operation; this method is the invocation entry point on {@code McpGatewayEngineConfiguration} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayEngineConfiguration.value(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 值 的处理结果；returns the result of the operation.
     */
    private String value(String value) {
        return value == null ? "" : value;
    }

    @Bean(name = {"gatewayRuleCompilerStrategy", "mcpGatewayRuleCompilerStrategy"})
    public McpGatewayRuleCompilerStrategy mcpGatewayRuleCompilerStrategy() {
        return new McpGatewayRuleCompilerStrategy();
    }

    @Bean("gatewayRedisTokenBucketExecutor")
    public RedisTokenBucketExecutor gatewayRedisTokenBucketExecutor(
            @Qualifier("gatewayMcpRedissonClient") RedissonClient redisson) {
        return new RedissonRedisTokenBucketExecutor(redisson);
    }

    @Bean("gatewayActiveHealthProbePolicy")
    public ActiveHealthProbePolicy gatewayActiveHealthProbePolicy(McpGatewayEngineProperties properties) {
        var configured = properties.activeHealth();
        return new ActiveHealthProbePolicy(
                configured.enabled(), configured.interval(), configured.jitterRatio(), configured.timeout(),
                configured.maximumConcurrency(), configured.failureThreshold(), configured.successThreshold(),
                configured.httpMethod(), configured.httpPath(), Set.copyOf(configured.httpSuccessStatuses()),
                configured.rpcServiceName(), configured.rpcConnectFallback());
    }

    @Bean(name = "gatewayHttpUpstreamAdapter", destroyMethod = "close")
    public ReactorNettyHttpUpstreamAdapter gatewayHttpUpstreamAdapter(McpGatewayEngineProperties properties) {
        var outbound = properties.outbound();
        return new ReactorNettyHttpUpstreamAdapter(
                outbound.maxConnections(), outbound.pendingAcquireMaxCount(), outbound.idleTimeout());
    }

    @Bean(name = "gatewayRpcProviderChannels", destroyMethod = "close")
    public RpcProviderChannelCache gatewayRpcProviderChannels(McpGatewayEngineProperties properties) {
        return new RpcProviderChannelCache(properties.outbound().channelDrainTimeout(),
                transportSecurity(properties.outbound().rpcTls()));
    }

    @Bean("mcpGatewayListenerProperties")
    public McpGatewayEngineProperties.ListenerProperties mcpGatewayListenerProperties(
            McpGatewayEngineProperties properties) {
        return properties.listener();
    }

    @Bean("mcpGatewayHttpDataPlaneHandlerAdapter")
    public McpGatewayHttpDataPlaneHandlerAdapter mcpGatewayHttpDataPlaneHandlerAdapter(
            @Qualifier("gatewayMcpHttpHandler") McpEngineHttpHandler handler,
            McpGatewayEngineProperties properties) {
        return new McpGatewayHttpDataPlaneHandlerAdapter(handler, properties.listener());
    }

    @Bean(name = "mcpGatewayHttpServer", destroyMethod = "close")
    public McpGatewayHttpServer mcpGatewayHttpServer(
            @Qualifier("mcpGatewayListenerProperties") McpGatewayEngineProperties.ListenerProperties properties,
            @Qualifier("mcpGatewayHttpDataPlaneHandlerAdapter") McpGatewayHttpDataPlaneHandlerAdapter handler) {
        return new McpGatewayHttpServer(properties, handler);
    }

    @Bean("mcpGatewayEngineRuntime")
    public McpGatewayEngineRuntime mcpGatewayEngineRuntime(
            @Qualifier("mcpGatewayHttpServer") McpGatewayHttpServer server,
            @Qualifier("gatewayRuleActivationApplier") GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation,
            @Qualifier("gatewayProviderDirectory") ProviderDirectory directory,
            @Qualifier("gatewayMcpRuntimeHealthIndicator") McpRuntimeHealthIndicator health) {
        return new McpGatewayEngineRuntime(server, activation, directory, health);
    }

    @Bean("gatewayRuntimeMetadata")
    public DdcInstanceMetadataContributor gatewayRuntimeMetadata(
            @Qualifier("gatewayRuleActivationApplier") GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation) {
        return () -> {
            GatewayRuleRuntimeStatus status = activation.status();
            return Map.of(
                    "gateway.engine.role", GatewayEngineRoleEnum.MCP.name(),
                    "activeReleaseId", value(status.activeReleaseId()),
                    "activeRuleVersion", Long.toString(status.activeDdcVersion()),
                    "activeRuleChecksum", value(status.artifactSha256()),
                    "lastApplyStatus", status.lastStage().name(),
                    "lastAckAt", status.updatedAt().toString());
        };
    }

    @Bean("mcpGatewayEngineHealthIndicator")
    public HealthIndicator mcpGatewayEngineHealthIndicator(
            @Qualifier("mcpGatewayEngineRuntime") McpGatewayEngineRuntime runtime,
            @Qualifier("gatewayRuleActivationApplier") GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation) {
        return () -> {
            Health.Builder health = !runtime.running() ? Health.down()
                    : runtime.ready() ? Health.up() : Health.status("OUT_OF_SERVICE");
            GatewayRuleRuntimeStatus status = activation.status();
            return health.withDetail("role", GatewayEngineRoleEnum.MCP.name())
                    .withDetail("running", runtime.running()).withDetail("ready", runtime.ready())
                    .withDetail("ruleStage", status.lastStage().name())
                    .withDetail("activeReleaseId", value(status.activeReleaseId()))
                    .withDetail("activeRuleVersion", status.activeDdcVersion())
                    .withDetail("activeRuleChecksum", value(status.artifactSha256())).build();
        };
    }

    @Bean("gatewayTlsCertificateMetrics")
    public MeterBinder gatewayTlsCertificateMetrics(McpGatewayEngineProperties properties) {
        GatewayTransportSecurity listener = transportSecurity(properties.listener().tls());
        GatewayTransportSecurity outbound = transportSecurity(properties.outbound().rpcTls());
        return registry -> {
            registerCertificateExpiry(registry, "mcp-http", listener);
            registerCertificateExpiry(registry, "mcp-outbound-rpc", outbound);
        };
    }

    private GatewayTransportSecurity transportSecurity(McpGatewayEngineProperties.TlsProperties tls) {
        return new GatewayTransportSecurity(tls.enabled(), tls.developmentPlaintext(),
                tls.certificateChainPath(), tls.privateKeyPath(), tls.trustCertificateCollectionPath(),
                tls.clientCertificateRequired());
    }

}
