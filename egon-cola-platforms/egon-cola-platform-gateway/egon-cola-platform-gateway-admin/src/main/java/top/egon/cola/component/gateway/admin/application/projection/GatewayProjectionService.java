package top.egon.cola.component.gateway.admin.application.projection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.model.management.DdcInstanceStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceKey;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseService;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseStore;
import top.egon.cola.component.gateway.admin.config.GatewayAdminProperties;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 中文说明：{@code GatewayProjectionService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关投影服务相关的职责与边界。
 * English summary: {@code GatewayProjectionService} is a gateway projection service service in the current Gateway module; it owns the gateway projection service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class GatewayProjectionService {

    /**
     * 中文说明：保存 groups 对应的状态、依赖或配置值；字段类型为 {@code GatewayGroupRepository}，由 {@code GatewayProjectionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by groups; its type is {@code GatewayGroupRepository}, and {@code GatewayProjectionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayProjectionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayGroupRepository groups;

    /**
     * 中文说明：保存 releases 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleaseService}，由 {@code GatewayProjectionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by releases; its type is {@code GatewayReleaseService}, and {@code GatewayProjectionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayProjectionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayReleaseService releases;

    /**
     * 中文说明：保存 客户端 对应的状态、依赖或配置值；字段类型为 {@code DdcManagementClient}，由 {@code GatewayProjectionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client; its type is {@code DdcManagementClient}, and {@code GatewayProjectionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayProjectionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcManagementClient client;

    /**
     * 中文说明：保存 cache 对应的状态、依赖或配置值；字段类型为 {@code Map<String, ProjectionEnvelope<?>>}，由 {@code GatewayProjectionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by cache; its type is {@code Map<String, ProjectionEnvelope<?>>}, and {@code GatewayProjectionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayProjectionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, ProjectionEnvelope<?>> cache =
            new ConcurrentHashMap<>();

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayProjectionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayProjectionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayProjectionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 targetBizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by target biz code; its type is {@code String}, and {@code GatewayProjectionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayProjectionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String targetBizCode;

    /**
     * 中文说明：保存 targetAppCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by target app code; its type is {@code String}, and {@code GatewayProjectionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayProjectionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String targetAppCode;

    /**
     * 中文说明：创建 {@code GatewayProjectionService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayProjectionService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param groups 参数 groups；parameter groups。
     * @param releases 参数 releases；parameter releases。
     * @param client 参数 客户端；parameter client。
     * @param properties 参数 properties；parameter properties。
     */
    @Autowired
    public GatewayProjectionService(
            GatewayGroupRepository groups,
            GatewayReleaseService releases,
            ObjectProvider<DdcManagementClient> client,
            GatewayAdminProperties properties) {
        this(
                groups,
                releases,
                client.getIfAvailable(),
                Clock.systemUTC(),
                properties.getDdc().getTargetBizCode(),
                properties.getDdc().getTargetAppCode()
        );
    }

    /**
     * 中文说明：创建 {@code GatewayProjectionService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayProjectionService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param groups 参数 groups；parameter groups。
     * @param releases 参数 releases；parameter releases。
     * @param client 参数 客户端；parameter client。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayProjectionService(
            GatewayGroupRepository groups,
            GatewayReleaseService releases,
            DdcManagementClient client,
            Clock clock) {
        this(groups, releases, client, clock, "infra", "ge");
    }

    /**
     * 中文说明：创建 {@code GatewayProjectionService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayProjectionService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param groups 参数 groups；parameter groups。
     * @param releases 参数 releases；parameter releases。
     * @param client 参数 客户端；parameter client。
     * @param clock 参数 clock；parameter clock。
     * @param targetBizCode 参数 targetBizCode；parameter target biz code。
     * @param targetAppCode 参数 targetAppCode；parameter target app code。
     */
    GatewayProjectionService(
            GatewayGroupRepository groups,
            GatewayReleaseService releases,
            DdcManagementClient client,
            Clock clock,
            String targetBizCode,
            String targetAppCode) {
        this.groups = groups;
        this.releases = releases;
        this.client = client;
        this.clock = clock;
        this.targetBizCode = required(targetBizCode, "targetBizCode");
        this.targetAppCode = required(targetAppCode, "targetAppCode");
    }

    /**
     * 中文说明：执行 引擎Nodes 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the engine nodes operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.engineNodes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 引擎Nodes 的处理结果；returns the result of the operation.
     */
    public ProjectionEnvelope<List<DdcManagementConfigClientInstance>>
    engineNodes(String gatewayGroupId) {
        GatewayGroupEntity group = group(gatewayGroupId);
        String key = "engine:" + gatewayGroupId;
        return load(key, "DDC_CONFIG_CLIENT", () -> client()
                .getConfigClients(new DdcManagementInstanceQuery(
                        targetBizCode,
                        group.getEnv(),
                        targetAppCode
                )));
    }

    /**
     * 中文说明：执行 services 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the services operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.services(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 services 的处理结果；returns the result of the operation.
     */
    public ProjectionEnvelope<DdcManagementServiceCatalog> services(
            ProviderQuery query) {
        String key = "services:" + query;
        return load(key, "DDC_SERVICE_REGISTRY", () -> client()
                .getServiceKeys(query.ddc()));
    }

    /**
     * 中文说明：执行 instances 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instances operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.instances(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 instances 的处理结果；returns the result of the operation.
     */
    public ProjectionEnvelope<DdcManagementServiceSnapshot> instances(
            ProviderQuery query) {
        String key = "instances:" + query;
        return load(key, "DDC_SERVICE_REGISTRY", () -> client()
                .getInstances(query.ddc()));
    }

    /**
     * 中文说明：执行 instances 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instances operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.instances(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param appCode 参数 appCode；parameter app code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 instances 的处理结果；returns the result of the operation.
     */
    public ProjectionEnvelope<List<ProviderInstanceProjection>> instances(
            String bizCode,
            String appCode,
            String env,
            String namespace) {
        String key = "instances:" + bizCode + ":" + appCode + ":"
                + env + ":" + namespace;
        return load(key, "DDC_SERVICE_REGISTRY", () -> {
            List<ProviderInstanceProjection> result = new ArrayList<>();
            collectInstances(
                    result,
                    bizCode,
                    appCode,
                    env,
                    namespace,
                    "HTTP_PROVIDER",
                    "http"
            );
            collectInstances(
                    result,
                    bizCode,
                    appCode,
                    env,
                    namespace,
                    "HTTP_PROVIDER",
                    "https"
            );
            collectInstances(
                    result,
                    bizCode,
                    appCode,
                    env,
                    namespace,
                    "RPC_PROVIDER",
                    "grpc"
            );
            return List.copyOf(result);
        });
    }

    /**
     * 中文说明：执行 运行时Consistency 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the runtime consistency operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.runtimeConsistency(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 运行时Consistency 的处理结果；returns the result of the operation.
     */
    public RuntimeConsistency runtimeConsistency(String gatewayGroupId) {
        List<GatewayReleaseService.ReleaseView> history =
                releases.history(gatewayGroupId);
        GatewayReleaseService.ReleaseView target = history.isEmpty()
                ? null
                : history.getFirst();
        ProjectionEnvelope<List<DdcManagementConfigClientInstance>> nodes =
                engineNodes(gatewayGroupId);
        GatewayReleaseStore.AttemptRecord attempt = latestSuccessfulAttempt(
                target
        );
        RuleExpectation expectation = expectation(attempt);
        List<EngineNodeConsistency> nodeStates = nodes.value().stream()
                .filter(this::online)
                .map(node -> nodeConsistency(
                        node,
                        target,
                        expectation
                ))
                .toList();
        long ready = nodeStates.stream()
                .filter(node -> "CONSISTENT".equals(node.status()))
                .count();
        return new RuntimeConsistency(
                target == null ? null : target.releaseId(),
                target == null ? null : target.status().name(),
                nodeStates.size(),
                ready,
                target != null
                        && "SUCCESS".equals(target.status().name())
                        && !nodeStates.isEmpty()
                        && ready == nodeStates.size(),
                nodes.observedAt(),
                nodes.source(),
                nodes.stale(),
                nodeStates
        );
    }

    /**
     * 中文说明：执行 latestSuccessfulAttempt 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the latest successful attempt operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.latestSuccessfulAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param release 参数 发布；parameter release。
     * @return 返回 latestSuccessfulAttempt 的处理结果；returns the result of the operation.
     */
    private GatewayReleaseStore.AttemptRecord latestSuccessfulAttempt(
            GatewayReleaseService.ReleaseView release) {
        if (release == null) {
            return null;
        }
        return release.attempts().stream()
                .filter(attempt -> "SUCCESS".equals(attempt.status()))
                .max(java.util.Comparator.comparingInt(
                        GatewayReleaseStore.AttemptRecord::attemptNo
                ))
                .orElse(null);
    }

    /**
     * 中文说明：执行 nodeConsistency 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the node consistency operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.nodeConsistency(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param node 参数 node；parameter node。
     * @param release 参数 发布；parameter release。
     * @param expectation 参数 expectation；parameter expectation。
     * @return 返回 nodeConsistency 的处理结果；returns the result of the operation.
     */
    private EngineNodeConsistency nodeConsistency(
            DdcManagementConfigClientInstance node,
            GatewayReleaseService.ReleaseView release,
            RuleExpectation expectation) {
        if (!online(node)) {
            return nodeState(node, "NOT_READY", "NODE_OFFLINE");
        }
        if (release == null || release.status() !=
                top.egon.cola.component.gateway.admin.domain
                        .GatewayReleaseStatus.SUCCESS) {
            return nodeState(node, "INCONSISTENT", "RELEASE_NOT_READY");
        }
        Map<String, String> metadata = node.metadata();
        if (!release.releaseId().equals(metadata.get("activeReleaseId"))) {
            return nodeState(node, "INCONSISTENT", "RELEASE_MISMATCH");
        }
        if (expectation == null) {
            return nodeState(node, "INCONSISTENT", "ACK_MISSING");
        }
        if (!Objects.equals(
                value(expectation.version()),
                metadata.get("activeRuleVersion")
        )) {
            return nodeState(node, "INCONSISTENT", "VERSION_MISMATCH");
        }
        if (!Objects.equals(
                expectation.artifactSha256(),
                metadata.get("activeRuleChecksum")
        )) {
            return nodeState(node, "INCONSISTENT", "CHECKSUM_MISMATCH");
        }
        if (!"ACK_SUCCESS".equals(metadata.get("lastApplyStatus"))
                || instantValue(metadata.get("lastAckAt")) == null) {
            return nodeState(node, "INCONSISTENT", "APPLY_NOT_ACKED");
        }
        return nodeState(node, "CONSISTENT", null);
    }

    /**
     * 中文说明：执行 expectation 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the expectation operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.expectation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attempt 参数 attempt；parameter attempt。
     * @return 返回 expectation 的处理结果；returns the result of the operation.
     */
    private RuleExpectation expectation(
            GatewayReleaseStore.AttemptRecord attempt) {
        if (attempt == null) {
            return null;
        }
        List<GatewayReleaseStore.TargetRecord> targets = attempt.targets();
        if (targets.isEmpty() || targets.stream().anyMatch(target ->
                !"SUCCESS".equals(target.status())
                        || target.appliedVersion() == null
                        || target.appliedArtifactSha256() == null
                        || target.appliedArtifactSha256().isBlank())) {
            return null;
        }
        GatewayReleaseStore.TargetRecord first = targets.getFirst();
        boolean unanimous = targets.stream().allMatch(target ->
                Objects.equals(
                        first.appliedVersion(),
                        target.appliedVersion()
                )
                        && Objects.equals(
                        first.appliedArtifactSha256(),
                        target.appliedArtifactSha256()
                ));
        return unanimous
                ? new RuleExpectation(
                first.appliedVersion(),
                first.appliedArtifactSha256()
        )
                : null;
    }

    /**
     * 中文说明：执行 nodeState 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the node state operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.nodeState(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param node 参数 node；parameter node。
     * @param status 参数 status；parameter status。
     * @param reason 参数 reason；parameter reason。
     * @return 返回 nodeState 的处理结果；returns the result of the operation.
     */
    private EngineNodeConsistency nodeState(
            DdcManagementConfigClientInstance node,
            String status,
            String reason) {
        Map<String, String> metadata = node.metadata();
        return new EngineNodeConsistency(
                node.instanceId(),
                node.leaseId(),
                node.status(),
                status,
                reason,
                metadata.get("activeReleaseId"),
                longValue(metadata.get("activeRuleVersion")),
                metadata.get("activeRuleChecksum"),
                metadata.get("lastApplyStatus"),
                instantValue(metadata.get("lastAckAt"))
        );
    }

    /**
     * 中文说明：执行 值 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the value operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.value(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 值 的处理结果；returns the result of the operation.
     */
    private String value(Long value) {
        return value == null ? null : value.toString();
    }

    /**
     * 中文说明：执行 long值 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the long value operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.longValue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 long值 的处理结果；returns the result of the operation.
     */
    private Long longValue(String value) {
        try {
            return value == null || value.isBlank()
                    ? null
                    : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 中文说明：执行 instant值 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instant value operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.instantValue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 instant值 的处理结果；returns the result of the operation.
     */
    private Instant instantValue(String value) {
        try {
            return value == null || value.isBlank()
                    ? null
                    : Instant.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * 中文说明：执行 scopeCounts 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the scope counts operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.scopeCounts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param appCode 参数 appCode；parameter app code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 scopeCounts 的处理结果；returns the result of the operation.
     */
    public ProjectionCounts scopeCounts(
            String bizCode,
            String appCode,
            String env,
            String namespace) {
        long totalEngines = 0;
        long readyEngines = 0;
        long inconsistentGroups = 0;
        boolean stale = false;
        List<GatewayGroupEntity> scopedGroups = groups
                .findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(
                        env,
                        namespace
                );
        for (GatewayGroupEntity group : scopedGroups) {
            if (!group.isEnabled()) {
                continue;
            }
            RuntimeConsistency consistency =
                    runtimeConsistency(group.getId());
            totalEngines += consistency.engineNodeCount();
            readyEngines += consistency.readyEngineNodeCount();
            inconsistentGroups += consistency.consistent() ? 0 : 1;
            stale = stale || consistency.stale();
        }
        ProjectionEnvelope<List<ProviderInstanceProjection>> providers =
                instances(bizCode, appCode, env, namespace);
        long activeProviders = providers.value().stream()
                .filter(this::online)
                .count();
        return new ProjectionCounts(
                readyEngines,
                totalEngines,
                inconsistentGroups,
                activeProviders,
                providers.value().size() - activeProviders,
                stale || providers.stale()
        );
    }

    /**
     * 中文说明：执行 group 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the group operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.group(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 group 的处理结果；returns the result of the operation.
     */
    private GatewayGroupEntity group(String id) {
        return groups.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway group " + id + " was not found"
                ));
    }

    /**
     * 中文说明：执行 客户端 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the client operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.client(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 客户端 的处理结果；returns the result of the operation.
     */
    private DdcManagementClient client() {
        if (client == null) {
            throw new IllegalStateException(
                    "DDC management client is not configured"
            );
        }
        return client;
    }

    /**
     * 中文说明：执行 collectInstances 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the collect instances operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.collectInstances(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param appCode 参数 appCode；parameter app code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param serviceKind 参数 服务Kind；parameter service kind。
     * @param protocol 参数 protocol；parameter protocol。
     */
    private void collectInstances(
            List<ProviderInstanceProjection> result,
            String bizCode,
            String appCode,
            String env,
            String namespace,
            String serviceKind,
            String protocol) {
        DdcManagementServiceCatalog catalog = client().getServiceKeys(
                new DdcManagementServiceQuery(
                        bizCode,
                        namespace,
                        env,
                        appCode,
                        serviceKind,
                        protocol,
                        null,
                        null,
                        null
                )
        );
        for (DdcManagementServiceKey service : catalog.services()) {
            DdcManagementServiceSnapshot snapshot = client().getInstances(
                    new DdcManagementServiceQuery(
                            service.bizCode(),
                            namespace,
                            service.env(),
                            service.appCode(),
                            service.serviceKind(),
                            service.protocol(),
                            service.serviceName(),
                            service.group(),
                            service.version()
                    )
            );
            snapshot.instances().forEach(instance -> result.add(
                    projection(service, instance, snapshot.observedAt())
            ));
        }
    }

    /**
     * 中文说明：执行 投影 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the projection operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.projection(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param service 参数 服务；parameter service。
     * @param instance 参数 instance；parameter instance。
     * @param observedAt 参数 observedAt；parameter observed at。
     * @return 返回 投影 的处理结果；returns the result of the operation.
     */
    private ProviderInstanceProjection projection(
            DdcManagementServiceKey service,
            DdcManagementServiceInstance instance,
            Instant observedAt) {
        Map<String, String> metadata = instance.metadata();
        return new ProviderInstanceProjection(
                String.join(
                        ":",
                        service.serviceKind(),
                        service.protocol(),
                        service.serviceName(),
                        value(service.group()),
                        value(service.version())
                ),
                service.protocol(),
                service.serviceName(),
                service.group(),
                service.version(),
                instance.instanceId(),
                instance.leaseId(),
                instance.host(),
                instance.port(),
                metadata.get("gateway.region"),
                metadata.get("gateway.zone"),
                integer(metadata.get("gateway.weight")),
                metadata,
                definitionSetId(metadata),
                instance.normalizedStatus().name(),
                instance.expireAt(),
                observedAt
        );
    }

    /**
     * 中文说明：执行 值 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the value operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.value(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 值 的处理结果；returns the result of the operation.
     */
    private String value(String value) {
        return value == null ? "" : value;
    }

    /**
     * 中文说明：执行 定义SetId 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the definition set id operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.definitionSetId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param metadata 参数 元数据；parameter metadata。
     * @return 返回 定义SetId 的处理结果；returns the result of the operation.
     */
    private String definitionSetId(Map<String, String> metadata) {
        String canonical = metadata.get("gateway.definition-set-id");
        return canonical == null || canonical.isBlank()
                ? metadata.get("gateway.definition-set")
                : canonical;
    }

    /**
     * 中文说明：执行 integer 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the integer operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.integer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 integer 的处理结果；returns the result of the operation.
     */
    private Integer integer(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 中文说明：执行 online 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the online operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.online(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @return 返回 online 的处理结果；returns the result of the operation.
     */
    private boolean online(DdcManagementConfigClientInstance instance) {
        return instance.normalizedStatus().isAvailable(
                clock.instant(),
                instance.expireAt()
        );
    }

    /**
     * 中文说明：执行 online 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the online operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.online(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @return 返回 online 的处理结果；returns the result of the operation.
     */
    private boolean online(ProviderInstanceProjection instance) {
        return DdcInstanceStatus.fromWire(instance.status()).isAvailable(
                clock.instant(),
                instance.expireAt()
        );
    }

    /**
     * 中文说明：执行 load 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.load(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @param source 参数 source；parameter source。
     * @param loader 参数 loader；parameter loader。
     * @return 返回 load 的处理结果；returns the result of the operation.
     */
    @SuppressWarnings("unchecked")
    private <T> ProjectionEnvelope<T> load(
            String key,
            String source,
            Supplier<T> loader) {
        try {
            ProjectionEnvelope<T> value = new ProjectionEnvelope<>(
                    loader.get(),
                    clock.instant(),
                    source,
                    false,
                    null
            );
            cache.put(key, value);
            return value;
        } catch (RuntimeException failure) {
            ProjectionEnvelope<T> previous =
                    (ProjectionEnvelope<T>) cache.get(key);
            if (previous != null) {
                return new ProjectionEnvelope<>(
                        previous.value(),
                        previous.observedAt(),
                        previous.source(),
                        true,
                        bounded(failure.getMessage())
                );
            }
            throw failure;
        }
    }

    /**
     * 中文说明：执行 bounded 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bounded operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.bounded(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 bounded 的处理结果；returns the result of the operation.
     */
    private String bounded(String value) {
        if (value == null || value.isBlank()) {
            return "projection refresh failed";
        }
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    /**
     * 中文说明：{@code ProjectionEnvelope} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责投影Envelope相关的职责与边界。
     * English summary: {@code ProjectionEnvelope} is an immutable data carrier in the current Gateway module; it owns the projection envelope-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param value 参数 值；parameter value。
     * @param observedAt 参数 observedAt；parameter observed at。
     * @param source 参数 source；parameter source。
     * @param stale 参数 stale；parameter stale。
     * @param refreshError 参数 refreshError；parameter refresh error。
     */
    public record ProjectionEnvelope<T>(
            /**
             * 中文说明：保存 值 对应的状态、依赖或配置值；字段类型为 {@code T}，由 {@code GatewayProjectionService.ProjectionEnvelope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by value; its type is {@code T}, and {@code GatewayProjectionService.ProjectionEnvelope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionEnvelope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionEnvelope}; do not couple callers to its representation when the owning type exposes an API.
             */
            T value,
            /**
             * 中文说明：保存 observedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayProjectionService.ProjectionEnvelope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by observed at; its type is {@code Instant}, and {@code GatewayProjectionService.ProjectionEnvelope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionEnvelope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionEnvelope}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant observedAt,
            /**
             * 中文说明：保存 source 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProjectionEnvelope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by source; its type is {@code String}, and {@code GatewayProjectionService.ProjectionEnvelope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionEnvelope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionEnvelope}; do not couple callers to its representation when the owning type exposes an API.
             */
            String source,
            /**
             * 中文说明：保存 stale 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayProjectionService.ProjectionEnvelope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by stale; its type is {@code boolean}, and {@code GatewayProjectionService.ProjectionEnvelope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionEnvelope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionEnvelope}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean stale,
            /**
             * 中文说明：保存 refreshError 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProjectionEnvelope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by refresh error; its type is {@code String}, and {@code GatewayProjectionService.ProjectionEnvelope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionEnvelope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionEnvelope}; do not couple callers to its representation when the owning type exposes an API.
             */
            String refreshError
    ) {
    }

    /**
     * 中文说明：{@code ProviderQuery} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责提供方Query相关的职责与边界。
     * English summary: {@code ProviderQuery} is an immutable data carrier in the current Gateway module; it owns the provider query-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param appCode 参数 appCode；parameter app code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param serviceKind 参数 服务Kind；parameter service kind。
     * @param protocol 参数 protocol；parameter protocol。
     * @param serviceName 参数 服务Name；parameter service name。
     * @param group 参数 group；parameter group。
     * @param version 参数 version；parameter version。
     */
    public record ProviderQuery(
            /**
             * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code GatewayProjectionService.ProviderQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String bizCode,
            /**
             * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code GatewayProjectionService.ProviderQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String appCode,
            /**
             * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayProjectionService.ProviderQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String env,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayProjectionService.ProviderQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String namespace,
            /**
             * 中文说明：保存 服务Kind 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by service kind; its type is {@code String}, and {@code GatewayProjectionService.ProviderQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serviceKind,
            /**
             * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code GatewayProjectionService.ProviderQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String protocol,
            /**
             * 中文说明：保存 服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by service name; its type is {@code String}, and {@code GatewayProjectionService.ProviderQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serviceName,
            /**
             * 中文说明：保存 group 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by group; its type is {@code String}, and {@code GatewayProjectionService.ProviderQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String group,
            /**
             * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderQuery} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code GatewayProjectionService.ProviderQuery} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderQuery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderQuery}; do not couple callers to its representation when the owning type exposes an API.
             */
            String version
    ) {

        /**
         * 中文说明：执行 ddc 操作；该方法是 {@code GatewayProjectionService.ProviderQuery} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the ddc operation; this method is the invocation entry point on {@code GatewayProjectionService.ProviderQuery} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.ProviderQuery.ddc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 ddc 的处理结果；returns the result of the operation.
         */
        private DdcManagementServiceQuery ddc() {
            String ddcProtocol = normalize(protocol);
            if (ddcProtocol != null) {
                ddcProtocol = ddcProtocol.toLowerCase(Locale.ROOT);
            }
            if ("rpc".equals(ddcProtocol)) {
                ddcProtocol = "grpc";
            }
            String ddcServiceKind = normalize(serviceKind);
            if (ddcServiceKind == null && ddcProtocol != null) {
                ddcServiceKind = switch (ddcProtocol) {
                    case "http", "https" -> "HTTP_PROVIDER";
                    case "grpc" -> "RPC_PROVIDER";
                    default -> throw new IllegalArgumentException(
                            "serviceKind is required for protocol "
                                    + protocol
                    );
                };
            } else if (ddcServiceKind != null) {
                ddcServiceKind = ddcServiceKind.toUpperCase(Locale.ROOT);
            }
            return new DdcManagementServiceQuery(
                    bizCode,
                    namespace,
                    env,
                    appCode,
                    ddcServiceKind,
                    ddcProtocol,
                    serviceName,
                    group,
                    version
            );
        }

        /**
         * 中文说明：执行 normalize 操作；该方法是 {@code GatewayProjectionService.ProviderQuery} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the normalize operation; this method is the invocation entry point on {@code GatewayProjectionService.ProviderQuery} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.ProviderQuery.normalize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param value 参数 值；parameter value。
         * @return 返回 normalize 的处理结果；returns the result of the operation.
         */
        private static String normalize(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

    /**
     * 中文说明：{@code ProviderInstanceProjection} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责提供方Instance投影相关的职责与边界。
     * English summary: {@code ProviderInstanceProjection} is an immutable data carrier in the current Gateway module; it owns the provider instance projection-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param serviceKey 参数 服务键；parameter service key。
     * @param protocol 参数 protocol；parameter protocol。
     * @param serviceName 参数 服务Name；parameter service name。
     * @param group 参数 group；parameter group。
     * @param version 参数 version；parameter version。
     * @param instanceId 参数 instanceId；parameter instance id。
     * @param leaseId 参数 租约Id；parameter lease id。
     * @param host 参数 host；parameter host。
     * @param port 参数 port；parameter port。
     * @param region 参数 region；parameter region。
     * @param zone 参数 zone；parameter zone。
     * @param weight 参数 weight；parameter weight。
     * @param tags 参数 tags；parameter tags。
     * @param definitionSetId 参数 定义SetId；parameter definition set id。
     * @param status 参数 status；parameter status。
     * @param expireAt 参数 expireAt；parameter expire at。
     * @param observedAt 参数 observedAt；parameter observed at。
     */
    public record ProviderInstanceProjection(
            /**
             * 中文说明：保存 服务键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by service key; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serviceKey,
            /**
             * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String protocol,
            /**
             * 中文说明：保存 服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by service name; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serviceName,
            /**
             * 中文说明：保存 group 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by group; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String group,
            /**
             * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String version,
            /**
             * 中文说明：保存 instanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by instance id; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String instanceId,
            /**
             * 中文说明：保存 租约Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by lease id; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String leaseId,
            /**
             * 中文说明：保存 host 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by host; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String host,
            /**
             * 中文说明：保存 port 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by port; its type is {@code int}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            int port,
            /**
             * 中文说明：保存 region 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by region; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String region,
            /**
             * 中文说明：保存 zone 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by zone; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String zone,
            /**
             * 中文说明：保存 weight 对应的状态、依赖或配置值；字段类型为 {@code Integer}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by weight; its type is {@code Integer}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            Integer weight,
            /**
             * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code Map<String, String>}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> tags,
            /**
             * 中文说明：保存 定义SetId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by definition set id; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String definitionSetId,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String status,
            /**
             * 中文说明：保存 expireAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expire at; its type is {@code Instant}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant expireAt,
            /**
             * 中文说明：保存 observedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayProjectionService.ProviderInstanceProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by observed at; its type is {@code Instant}, and {@code GatewayProjectionService.ProviderInstanceProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProviderInstanceProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProviderInstanceProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant observedAt
    ) {

        /**
         * 中文说明：创建 {@code GatewayProjectionService.ProviderInstanceProjection} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayProjectionService.ProviderInstanceProjection} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param serviceKey 参数 服务键；parameter service key。
         * @param protocol 参数 protocol；parameter protocol。
         * @param serviceName 参数 服务Name；parameter service name。
         * @param group 参数 group；parameter group。
         * @param version 参数 version；parameter version。
         * @param instanceId 参数 instanceId；parameter instance id。
         * @param leaseId 参数 租约Id；parameter lease id。
         * @param host 参数 host；parameter host。
         * @param port 参数 port；parameter port。
         * @param region 参数 region；parameter region。
         * @param zone 参数 zone；parameter zone。
         * @param weight 参数 weight；parameter weight。
         * @param tags 参数 tags；parameter tags。
         * @param definitionSetId 参数 定义SetId；parameter definition set id。
         * @param status 参数 status；parameter status。
         * @param expireAt 参数 expireAt；parameter expire at。
         * @param observedAt 参数 observedAt；parameter observed at。
         */
        public ProviderInstanceProjection {
            tags = Map.copyOf(tags);
        }
    }

    /**
     * 中文说明：{@code RuntimeConsistency} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责运行时Consistency相关的职责与边界。
     * English summary: {@code RuntimeConsistency} is an immutable data carrier in the current Gateway module; it owns the runtime consistency-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param targetReleaseId 参数 target发布Id；parameter target release id。
     * @param targetReleaseStatus 参数 target发布Status；parameter target release status。
     * @param engineNodeCount 参数 引擎NodeCount；parameter engine node count。
     * @param readyEngineNodeCount 参数 ready引擎NodeCount；parameter ready engine node count。
     * @param consistent 参数 consistent；parameter consistent。
     * @param observedAt 参数 observedAt；parameter observed at。
     * @param source 参数 source；parameter source。
     * @param stale 参数 stale；parameter stale。
     * @param nodes 参数 nodes；parameter nodes。
     */
    public record RuntimeConsistency(
            /**
             * 中文说明：保存 target发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.RuntimeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by target release id; its type is {@code String}, and {@code GatewayProjectionService.RuntimeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuntimeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuntimeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String targetReleaseId,
            /**
             * 中文说明：保存 target发布Status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.RuntimeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by target release status; its type is {@code String}, and {@code GatewayProjectionService.RuntimeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuntimeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuntimeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String targetReleaseStatus,
            /**
             * 中文说明：保存 引擎NodeCount 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayProjectionService.RuntimeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by engine node count; its type is {@code int}, and {@code GatewayProjectionService.RuntimeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuntimeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuntimeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            int engineNodeCount,
            /**
             * 中文说明：保存 ready引擎NodeCount 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayProjectionService.RuntimeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by ready engine node count; its type is {@code long}, and {@code GatewayProjectionService.RuntimeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuntimeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuntimeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            long readyEngineNodeCount,
            /**
             * 中文说明：保存 consistent 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayProjectionService.RuntimeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by consistent; its type is {@code boolean}, and {@code GatewayProjectionService.RuntimeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuntimeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuntimeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean consistent,
            /**
             * 中文说明：保存 observedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayProjectionService.RuntimeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by observed at; its type is {@code Instant}, and {@code GatewayProjectionService.RuntimeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuntimeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuntimeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant observedAt,
            /**
             * 中文说明：保存 source 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.RuntimeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by source; its type is {@code String}, and {@code GatewayProjectionService.RuntimeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuntimeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuntimeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String source,
            /**
             * 中文说明：保存 stale 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayProjectionService.RuntimeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by stale; its type is {@code boolean}, and {@code GatewayProjectionService.RuntimeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuntimeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuntimeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean stale,
            /**
             * 中文说明：保存 nodes 对应的状态、依赖或配置值；字段类型为 {@code List<EngineNodeConsistency>}，由 {@code GatewayProjectionService.RuntimeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by nodes; its type is {@code List<EngineNodeConsistency>}, and {@code GatewayProjectionService.RuntimeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuntimeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuntimeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<EngineNodeConsistency> nodes
    ) {

        /**
         * 中文说明：创建 {@code GatewayProjectionService.RuntimeConsistency} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayProjectionService.RuntimeConsistency} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param targetReleaseId 参数 target发布Id；parameter target release id。
         * @param targetReleaseStatus 参数 target发布Status；parameter target release status。
         * @param engineNodeCount 参数 引擎NodeCount；parameter engine node count。
         * @param readyEngineNodeCount 参数 ready引擎NodeCount；parameter ready engine node count。
         * @param consistent 参数 consistent；parameter consistent。
         * @param observedAt 参数 observedAt；parameter observed at。
         * @param source 参数 source；parameter source。
         * @param stale 参数 stale；parameter stale。
         * @param nodes 参数 nodes；parameter nodes。
         */
        public RuntimeConsistency {
            nodes = List.copyOf(nodes);
        }
    }

    /**
     * 中文说明：{@code EngineNodeConsistency} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责引擎NodeConsistency相关的职责与边界。
     * English summary: {@code EngineNodeConsistency} is an immutable data carrier in the current Gateway module; it owns the engine node consistency-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param instanceId 参数 instanceId；parameter instance id。
     * @param leaseId 参数 租约Id；parameter lease id。
     * @param leaseStatus 参数 租约Status；parameter lease status。
     * @param status 参数 status；parameter status。
     * @param reason 参数 reason；parameter reason。
     * @param activeReleaseId 参数 active发布Id；parameter active release id。
     * @param activeRuleVersion 参数 active规则Version；parameter active rule version。
     * @param activeRuleChecksum 参数 active规则Checksum；parameter active rule checksum。
     * @param lastApplyStatus 参数 lastApplyStatus；parameter last apply status。
     * @param lastAckAt 参数 lastAckAt；parameter last ack at。
     */
    public record EngineNodeConsistency(
            /**
             * 中文说明：保存 instanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.EngineNodeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by instance id; its type is {@code String}, and {@code GatewayProjectionService.EngineNodeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.EngineNodeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.EngineNodeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String instanceId,
            /**
             * 中文说明：保存 租约Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.EngineNodeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by lease id; its type is {@code String}, and {@code GatewayProjectionService.EngineNodeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.EngineNodeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.EngineNodeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String leaseId,
            /**
             * 中文说明：保存 租约Status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.EngineNodeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by lease status; its type is {@code String}, and {@code GatewayProjectionService.EngineNodeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.EngineNodeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.EngineNodeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String leaseStatus,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.EngineNodeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code GatewayProjectionService.EngineNodeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.EngineNodeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.EngineNodeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String status,
            /**
             * 中文说明：保存 reason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.EngineNodeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by reason; its type is {@code String}, and {@code GatewayProjectionService.EngineNodeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.EngineNodeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.EngineNodeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String reason,
            /**
             * 中文说明：保存 active发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.EngineNodeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by active release id; its type is {@code String}, and {@code GatewayProjectionService.EngineNodeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.EngineNodeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.EngineNodeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String activeReleaseId,
            /**
             * 中文说明：保存 active规则Version 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code GatewayProjectionService.EngineNodeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by active rule version; its type is {@code Long}, and {@code GatewayProjectionService.EngineNodeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.EngineNodeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.EngineNodeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            Long activeRuleVersion,
            /**
             * 中文说明：保存 active规则Checksum 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.EngineNodeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by active rule checksum; its type is {@code String}, and {@code GatewayProjectionService.EngineNodeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.EngineNodeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.EngineNodeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String activeRuleChecksum,
            /**
             * 中文说明：保存 lastApplyStatus 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.EngineNodeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by last apply status; its type is {@code String}, and {@code GatewayProjectionService.EngineNodeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.EngineNodeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.EngineNodeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            String lastApplyStatus,
            /**
             * 中文说明：保存 lastAckAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayProjectionService.EngineNodeConsistency} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by last ack at; its type is {@code Instant}, and {@code GatewayProjectionService.EngineNodeConsistency} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.EngineNodeConsistency} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.EngineNodeConsistency}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant lastAckAt
    ) {
    }

    /**
     * 中文说明：{@code RuleExpectation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责规则Expectation相关的职责与边界。
     * English summary: {@code RuleExpectation} is an immutable data carrier in the current Gateway module; it owns the rule expectation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param version 参数 version；parameter version。
     * @param artifactSha256 参数 制品Sha256；parameter artifact sha256。
     */
    private record RuleExpectation(
            /**
             * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code GatewayProjectionService.RuleExpectation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code Long}, and {@code GatewayProjectionService.RuleExpectation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuleExpectation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuleExpectation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Long version,
            /**
             * 中文说明：保存 制品Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayProjectionService.RuleExpectation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by artifact sha256; its type is {@code String}, and {@code GatewayProjectionService.RuleExpectation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.RuleExpectation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.RuleExpectation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String artifactSha256
    ) {
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    /**
     * 中文说明：{@code ProjectionCounts} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责投影Counts相关的职责与边界。
     * English summary: {@code ProjectionCounts} is an immutable data carrier in the current Gateway module; it owns the projection counts-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param readyEngines 参数 readyEngines；parameter ready engines。
     * @param totalEngines 参数 totalEngines；parameter total engines。
     * @param inconsistentGroups 参数 inconsistentGroups；parameter inconsistent groups。
     * @param activeProviders 参数 activeProviders；parameter active providers。
     * @param abnormalProviders 参数 abnormalProviders；parameter abnormal providers。
     * @param stale 参数 stale；parameter stale。
     */
    public record ProjectionCounts(
            /**
             * 中文说明：保存 readyEngines 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayProjectionService.ProjectionCounts} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by ready engines; its type is {@code long}, and {@code GatewayProjectionService.ProjectionCounts} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
             */
            long readyEngines,
            /**
             * 中文说明：保存 totalEngines 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayProjectionService.ProjectionCounts} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by total engines; its type is {@code long}, and {@code GatewayProjectionService.ProjectionCounts} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
             */
            long totalEngines,
            /**
             * 中文说明：保存 inconsistentGroups 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayProjectionService.ProjectionCounts} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by inconsistent groups; its type is {@code long}, and {@code GatewayProjectionService.ProjectionCounts} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
             */
            long inconsistentGroups,
            /**
             * 中文说明：保存 activeProviders 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayProjectionService.ProjectionCounts} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by active providers; its type is {@code long}, and {@code GatewayProjectionService.ProjectionCounts} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
             */
            long activeProviders,
            /**
             * 中文说明：保存 abnormalProviders 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayProjectionService.ProjectionCounts} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by abnormal providers; its type is {@code long}, and {@code GatewayProjectionService.ProjectionCounts} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
             */
            long abnormalProviders,
            /**
             * 中文说明：保存 stale 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayProjectionService.ProjectionCounts} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by stale; its type is {@code boolean}, and {@code GatewayProjectionService.ProjectionCounts} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayProjectionService.ProjectionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionService.ProjectionCounts}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean stale
    ) {
    }
}
