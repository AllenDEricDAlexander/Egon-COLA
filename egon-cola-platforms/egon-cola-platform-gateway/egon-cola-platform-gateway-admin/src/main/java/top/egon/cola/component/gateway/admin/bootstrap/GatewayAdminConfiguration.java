package top.egon.cola.component.gateway.admin.bootstrap;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
import top.egon.cola.component.gateway.admin.auth.domain.vo.*;
import top.egon.cola.component.gateway.admin.auth.service.*;
import top.egon.cola.component.gateway.admin.bootstrap.*;
import top.egon.cola.component.gateway.admin.catalog.controller.*;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.*;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.*;
import top.egon.cola.component.gateway.admin.catalog.domain.po.*;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.*;
import top.egon.cola.component.gateway.admin.catalog.repository.*;
import top.egon.cola.component.gateway.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.catalog.service.*;
import top.egon.cola.component.gateway.admin.config.*;
import top.egon.cola.component.gateway.admin.config.properties.*;
import top.egon.cola.component.gateway.admin.credential.controller.*;
import top.egon.cola.component.gateway.admin.credential.domain.dto.*;
import top.egon.cola.component.gateway.admin.credential.domain.po.*;
import top.egon.cola.component.gateway.admin.credential.domain.vo.*;
import top.egon.cola.component.gateway.admin.credential.repository.*;
import top.egon.cola.component.gateway.admin.credential.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.credential.service.*;
import top.egon.cola.component.gateway.admin.group.controller.*;
import top.egon.cola.component.gateway.admin.group.domain.dto.*;
import top.egon.cola.component.gateway.admin.group.domain.po.*;
import top.egon.cola.component.gateway.admin.group.domain.vo.*;
import top.egon.cola.component.gateway.admin.group.repository.*;
import top.egon.cola.component.gateway.admin.group.service.*;
import top.egon.cola.component.gateway.admin.mcp.controller.*;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.*;
import top.egon.cola.component.gateway.admin.mcp.domain.enums.*;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.*;
import top.egon.cola.component.gateway.admin.mcp.domain.po.*;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.*;
import top.egon.cola.component.gateway.admin.mcp.repository.*;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.mcp.service.*;
import top.egon.cola.component.gateway.admin.observability.controller.*;
import top.egon.cola.component.gateway.admin.observability.controller.message.*;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.observability.domain.dto.*;
import top.egon.cola.component.gateway.admin.observability.domain.enums.*;
import top.egon.cola.component.gateway.admin.observability.domain.po.*;
import top.egon.cola.component.gateway.admin.observability.domain.vo.*;
import top.egon.cola.component.gateway.admin.observability.repository.*;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.observability.service.*;
import top.egon.cola.component.gateway.admin.release.controller.*;
import top.egon.cola.component.gateway.admin.release.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.release.domain.*;
import top.egon.cola.component.gateway.admin.release.domain.dto.*;
import top.egon.cola.component.gateway.admin.release.domain.enums.*;
import top.egon.cola.component.gateway.admin.release.domain.po.*;
import top.egon.cola.component.gateway.admin.release.domain.vo.*;
import top.egon.cola.component.gateway.admin.release.repository.*;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.release.service.*;
import top.egon.cola.component.gateway.admin.reporting.controller.openapi.*;
import top.egon.cola.component.gateway.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.*;
import top.egon.cola.component.gateway.admin.reporting.domain.po.*;
import top.egon.cola.component.gateway.admin.reporting.domain.vo.*;
import top.egon.cola.component.gateway.admin.reporting.repository.*;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.reporting.service.*;
import top.egon.cola.component.gateway.admin.routing.controller.*;
import top.egon.cola.component.gateway.admin.routing.domain.*;
import top.egon.cola.component.gateway.admin.routing.domain.dto.*;
import top.egon.cola.component.gateway.admin.routing.domain.po.*;
import top.egon.cola.component.gateway.admin.routing.domain.vo.*;
import top.egon.cola.component.gateway.admin.routing.repository.*;
import top.egon.cola.component.gateway.admin.routing.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.routing.service.*;
import top.egon.cola.component.gateway.admin.rule.domain.dto.*;
import top.egon.cola.component.gateway.admin.rule.domain.vo.*;
import top.egon.cola.component.gateway.admin.rule.service.*;
import top.egon.cola.component.gateway.admin.runtime.controller.*;
import top.egon.cola.component.gateway.admin.runtime.domain.dto.*;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.*;
import top.egon.cola.component.gateway.admin.runtime.service.*;
import top.egon.cola.component.gateway.admin.scope.controller.*;
import top.egon.cola.component.gateway.admin.scope.domain.*;
import top.egon.cola.component.gateway.admin.scope.domain.dto.*;
import top.egon.cola.component.gateway.admin.scope.domain.vo.*;
import top.egon.cola.component.gateway.admin.scope.service.*;
import top.egon.cola.component.gateway.admin.shared.controller.*;
import top.egon.cola.component.gateway.admin.shared.domain.*;
import top.egon.cola.component.gateway.admin.shared.domain.enums.*;
import top.egon.cola.component.gateway.admin.shared.domain.exception.*;
import top.egon.cola.component.gateway.admin.shared.domain.po.*;
import top.egon.cola.component.gateway.admin.shared.domain.vo.*;
import top.egon.cola.component.gateway.admin.shared.repository.*;
import top.egon.cola.component.gateway.admin.shared.repository.jdbc.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.gateway.admin.observability.service.GatewayCallEventIngestService;
import top.egon.cola.component.gateway.admin.observability.service.GatewayObservabilityQueryService;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayObservabilityRepository;
import top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionService;
import top.egon.cola.component.gateway.admin.credential.service.GatewaySecretProtector;
import top.egon.cola.component.gateway.admin.release.service.GatewayReleasePublicationCoordinator;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleasePublicationRepository;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleaseRepository;
import top.egon.cola.component.gateway.admin.config.GatewayAdminProperties;
import top.egon.cola.component.gateway.admin.observability.controller.message.GatewayCallEventCodec;
import top.egon.cola.component.gateway.admin.observability.controller.message.GatewayCallEventConsumerHandler;
import top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaCallEventConsumer;
import top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaConsumerMetrics;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.JdbcGatewayObservabilityRepository;
import top.egon.cola.component.gateway.admin.credential.service.AesGcmGatewaySecretProtector;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.GatewayObservabilityRetentionReaper;
import top.egon.cola.component.gateway.admin.rule.service.GatewayDdcRulePublisher;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.FileSystemMcpAppArtifactRepository;
import top.egon.cola.component.gateway.mcp.app.McpAppSecurityValidator;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

/**
 * 中文说明：{@code GatewayAdminConfiguration} 是配置类，位于当前 Gateway 模块的相关包中，负责网关管理端配置相关的职责与边界。
 * English summary: {@code GatewayAdminConfiguration} is a gateway admin configuration configuration in the current Gateway module; it owns the gateway admin configuration-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(GatewayAdminProperties.class)
public class GatewayAdminConfiguration {

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
                    "${gateway.admin.mcp.artifact-root:"
                            + "${java.io.tmpdir}/egon-cola/"
                            + "gateway-mcp-artifacts}"
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
            name = "gateway.admin.ddc.enabled",
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
            name = "gateway.admin.ddc.enabled",
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
            @Value("${gateway.admin.ddc.publish-timeout:PT30S}")
            Duration timeout) {
        return new GatewayReleasePublicationCoordinator(
                journal,
                releases,
                client,
                publisher,
                Clock.systemUTC(),
                timeout,
                properties.getDdc().getTargetBizCode(),
                properties.getDdc().getTargetAppCode()
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
            name = "gateway.admin.secrets.master-key-base64"
    )
    GatewaySecretProtector gatewaySecretProtector(
            @Value("${gateway.admin.secrets.master-key-base64}")
            String masterKey,
            @Value("${gateway.admin.secrets.key-version:v1}")
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
                    "${gateway.admin.observability.retention:PT168H}"
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
            name = "gateway.admin.observability.kafka.enabled",
            havingValue = "true"
    )
    GatewayKafkaCallEventConsumer gatewayKafkaCallEventConsumer(
            GatewayCallEventConsumerHandler handler,
            MeterRegistry meterRegistry,
            @Value(
                    "${gateway.admin.observability.kafka.bootstrap-servers}"
            ) String bootstrapServers,
            @Value(
                    "${gateway.admin.observability.kafka.topic:"
                            + "egon.gateway.call.v1}"
            ) String topic,
            @Value(
                    "${gateway.admin.observability.kafka.group-id:"
                            + "gateway-admin-call-events-v1}"
            ) String groupId,
            @Value(
                    "${gateway.admin.observability.kafka.retry-backoff:"
                            + "PT0.25S}"
            ) Duration retryBackoff,
            @Value(
                    "${gateway.admin.observability.kafka."
                            + "max-record-attempts:5}"
            ) int maxRecordAttempts) {
        return new GatewayKafkaCallEventConsumer(
                handler,
                new GatewayKafkaConsumerMetrics(
                        meterRegistry,
                        Clock.systemUTC()
                ),
                new top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO(
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
