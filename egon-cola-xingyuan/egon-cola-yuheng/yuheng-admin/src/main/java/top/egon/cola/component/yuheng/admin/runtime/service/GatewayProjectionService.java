package top.egon.cola.component.yuheng.admin.runtime.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.egon.cola.component.tianshu.api.client.DdcManagementClient;
import top.egon.cola.component.tianshu.model.management.DdcInstanceStatus;
import top.egon.cola.component.tianshu.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.tianshu.model.management.DdcManagementInstanceQuery;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceKey;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.yuheng.admin.config.GatewayAdminProperties;
import top.egon.cola.component.yuheng.admin.group.domain.po.GatewayGroupPO;
import top.egon.cola.component.yuheng.admin.group.repository.GatewayGroupRepository;
import top.egon.cola.component.yuheng.admin.release.service.GatewayReleaseService;
import top.egon.cola.component.yuheng.admin.release.repository.GatewayReleasePublicationRepository;
import top.egon.cola.component.yuheng.admin.release.domain.dto.GatewayPublicationScopeDTO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleasePublicationPO;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationPhaseEnum;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationStatusEnum;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;
import top.egon.cola.component.yuheng.admin.runtime.domain.dto.GatewayProviderQueryDTO;
import top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO;
import top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO;
import top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayProviderInstanceVO;
import top.egon.cola.component.yuheng.admin.runtime.domain.vo.GatewayRuntimeConsistencyVO;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
@Slf4j
@RequiredArgsConstructor
@Validated
@Service("gatewayProjectionService")
public class GatewayProjectionService {

    @NonNull
    @Qualifier("gatewayGroupRepository")
    private final GatewayGroupRepository groups;
    @NonNull
    @Qualifier("gatewayReleaseService")
    private final GatewayReleaseService releases;
    @NonNull
    @Qualifier("ddcManagementClient")
    private final ObjectProvider<DdcManagementClient> clients;
    @NonNull
    @Qualifier("gatewayProjectionClock")
    private final Clock clock;
    @NonNull
    @Qualifier("gateway.admin-top.egon.cola.component.yuheng.admin.config.GatewayAdminProperties")
    private final GatewayAdminProperties properties;
    @NonNull
    @Qualifier("gatewayEngineRoleConsistencyStrategy")
    private final GatewayEngineRoleConsistencyStrategy roleStrategy;
    @NonNull
    @Qualifier("jdbcGatewayReleasePublicationRepository")
    private final GatewayReleasePublicationRepository publications;

    private final Map<String, GatewayProjectionEnvelopeVO<?>> cache = new ConcurrentHashMap<>();


    /** Captures the validated bootstrap target once, preserving the former constructor semantics. */
    @PostConstruct
    void validateBootstrap() {
        properties.getDdc().targets("bootstrap-validation");
    }

    /**
     * 中文说明：执行 引擎Nodes 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the engine nodes operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.engineNodes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 引擎Nodes 的处理结果；returns the result of the operation.
     */
    public GatewayProjectionEnvelopeVO<List<DdcManagementConfigClientInstance>>
    engineNodes(@NotBlank String gatewayGroupId) {
        GatewayGroupPO group = group(gatewayGroupId);
        var history = releases.history(gatewayGroupId);
        List<GatewayReleasePublicationPO> journal = history.isEmpty()
                ? List.of() : journal(history.getFirst());
        return engineNodes(group, targetScopes(group, journal));
    }

    /**
     * 中文说明：执行 services 操作；该方法是 {@code GatewayProjectionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the services operation; this method is the invocation entry point on {@code GatewayProjectionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionService.services(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 services 的处理结果；returns the result of the operation.
     */
    public GatewayProjectionEnvelopeVO<DdcManagementServiceCatalog> services(
            GatewayProviderQueryDTO query) {
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
    public GatewayProjectionEnvelopeVO<List<GatewayProviderInstanceVO>> instances(
            GatewayProviderQueryDTO query) {
        String key = "instances:" + query;
        return load(key, "DDC_SERVICE_REGISTRY", () -> {
            DdcManagementServiceSnapshot snapshot = client()
                    .getInstances(query.ddc());
            if (snapshot.serviceKey() == null) {
                return List.of();
            }
            return snapshot.instances().stream()
                    .map(instance -> projection(
                            snapshot.serviceKey(),
                            instance,
                            snapshot.observedAt()
                    ))
                    .toList();
        });
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
    public GatewayProjectionEnvelopeVO<List<GatewayProviderInstanceVO>> instances(
            String bizCode,
            String appCode,
            String env,
            String namespace) {
        String key = "instances:" + bizCode + ":" + appCode + ":"
                + env + ":" + namespace;
        return load(key, "DDC_SERVICE_REGISTRY", () -> {
            List<GatewayProviderInstanceVO> result = new ArrayList<>();
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
    public GatewayRuntimeConsistencyVO runtimeConsistency(@NotBlank String gatewayGroupId) {
        List<top.egon.cola.component.yuheng.admin.release.domain.vo.GatewayReleaseVO> history =
                releases.history(gatewayGroupId);
        top.egon.cola.component.yuheng.admin.release.domain.vo.GatewayReleaseVO target = history.isEmpty()
                ? null
                : history.getFirst();
        GatewayGroupPO group = group(gatewayGroupId);
        List<GatewayReleasePublicationPO> journal = target == null ? List.of() : journal(target);
        GatewayProjectionEnvelopeVO<List<DdcManagementConfigClientInstance>> nodes =
                engineNodes(group, targetScopes(group, journal));
        top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseAttemptPO attempt = latestSuccessfulAttempt(
                target
        );
        Map<GatewayEngineRoleEnum, GatewayRuleExpectation> expectations =
                expectations(target, attempt, journal);
        List<DdcManagementConfigClientInstance> onlineNodes = nodes.value().stream()
                .filter(this::online).toList();
        List<GatewayEngineNodeConsistencyVO> nodeStates = onlineNodes.stream()
                .map(node -> nodeConsistency(
                        node,
                        target,
                        expectations
                ))
                .toList();
        long ready = nodeStates.stream()
                .filter(node -> "CONSISTENT".equals(node.status()))
                .count();
        return new GatewayRuntimeConsistencyVO(
                target == null ? null : target.releaseId(),
                target == null ? null : target.status().name(),
                nodeStates.size(),
                ready,
                target != null
                        && "SUCCESS".equals(target.status().name())
                        && !nodes.stale()
                        && !nodeStates.isEmpty()
                        && roleStrategy.missingRoles(onlineNodes).isEmpty()
                        && !roleStrategy.hasUnknownRole(onlineNodes)
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
    private top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseAttemptPO latestSuccessfulAttempt(
            top.egon.cola.component.yuheng.admin.release.domain.vo.GatewayReleaseVO release) {
        if (release == null) {
            return null;
        }
        return release.attempts().stream()
                .filter(attempt -> "SUCCESS".equals(attempt.status()))
                .max(java.util.Comparator.comparingInt(
                        top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseAttemptPO::attemptNo
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
    private GatewayEngineNodeConsistencyVO nodeConsistency(
            DdcManagementConfigClientInstance node,
            top.egon.cola.component.yuheng.admin.release.domain.vo.GatewayReleaseVO release,
            Map<GatewayEngineRoleEnum, GatewayRuleExpectation> expectations) {
        if (!online(node)) {
            return nodeState(node, "NOT_READY", "NODE_OFFLINE");
        }
        if (roleStrategy.roleOf(node).isEmpty()) {
            String role = node.metadata().get(GatewayEngineRoleConsistencyStrategy.ROLE_METADATA_KEY);
            return nodeState(node, "NOT_READY", role == null || role.isBlank() ? "ROLE_MISSING" : "ROLE_UNKNOWN");
        }
        if (release == null || release.status() !=
                top.egon.cola.component.yuheng.admin.release.domain.enums
                        .GatewayReleaseStatus.SUCCESS) {
            return nodeState(node, "INCONSISTENT", "RELEASE_NOT_READY");
        }
        Map<String, String> metadata = node.metadata();
        if (!release.releaseId().equals(metadata.get("activeReleaseId"))) {
            return nodeState(node, "INCONSISTENT", "RELEASE_MISMATCH");
        }
        GatewayRuleExpectation expectation = expectations.get(roleStrategy.roleOf(node).orElseThrow());
        if (expectation == null) {
            return nodeState(node, "INCONSISTENT", "ACK_MISSING");
        }
        GatewayPublicationScopeDTO scope = expectation.targetScope();
        if (!scope.bizCode().equals(node.bizCode()) || !scope.env().equals(node.env())
                || !scope.appCode().equals(node.appCode())) {
            return nodeState(node, "INCONSISTENT", "TARGET_SCOPE_MISMATCH");
        }
        Long version = longValue(metadata.get("activeRuleVersion"));
        if (version == null || version < expectation.version()) {
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
    private Map<GatewayEngineRoleEnum, GatewayRuleExpectation> expectations(
            top.egon.cola.component.yuheng.admin.release.domain.vo.GatewayReleaseVO release,
            top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseAttemptPO attempt,
            List<GatewayReleasePublicationPO> journal) {
        if (release == null || attempt == null || journal.isEmpty()
                || journal.stream().anyMatch(phase -> phase.targetScope() == null
                || phase.status() != GatewayPublicationStatusEnum.SUCCESS)) {
            return Map.of();
        }
        String checksum = releases.artifactSha256(release.releaseId()).orElse(null);
        if (checksum == null || checksum.isBlank()) {
            return Map.of();
        }
        Map<GatewayEngineRoleEnum, GatewayRuleExpectation> result =
                new java.util.EnumMap<>(GatewayEngineRoleEnum.class);
        for (GatewayReleasePublicationPO activation : journal) {
            if (activation.phaseType() != GatewayPublicationPhaseEnum.ACTIVATION) {
                continue;
            }
            if (activation.ddcTargetVersion() == null || result.put(
                    activation.targetScope().engineRole(),
                    new GatewayRuleExpectation(activation.ddcTargetVersion(), checksum,
                            activation.targetScope())) != null) {
                return Map.of();
            }
        }
        return result.size() == 2 ? Map.copyOf(result) : Map.of();
    }

    /**
     * 中文说明：节点目录按发布时冻结的双目标查询；尚未发布时展示当前引导目标。
     * English summary: Queries both frozen release targets, or bootstrap targets before the first publication.
     */
    private List<GatewayPublicationScopeDTO> targetScopes(
            GatewayGroupPO group, List<GatewayReleasePublicationPO> journal) {
        List<GatewayPublicationScopeDTO> frozen = journal.stream()
                .map(GatewayReleasePublicationPO::targetScope).filter(Objects::nonNull).distinct().toList();
        return frozen.isEmpty() ? properties.getDdc().targets(group.getEnv()) : frozen;
    }

    private List<GatewayReleasePublicationPO> journal(
            top.egon.cola.component.yuheng.admin.release.domain.vo.GatewayReleaseVO release) {
        return release.attempts().stream().max(java.util.Comparator.comparingInt(
                top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseAttemptPO::attemptNo))
                .map(attempt -> publications.findAttemptMetadata(release.releaseId(), attempt.attemptNo()))
                .orElse(List.of());
    }

    private GatewayProjectionEnvelopeVO<List<DdcManagementConfigClientInstance>> engineNodes(
            GatewayGroupPO group, List<GatewayPublicationScopeDTO> scopes) {
        String key = "engine:" + group.getId() + ":" + scopes;
        return load(key, "DDC_CONFIG_CLIENT", () -> {
            List<DdcManagementConfigClientInstance> result = new ArrayList<>();
            for (GatewayPublicationScopeDTO scope : scopes) {
                result.addAll(client().getConfigClients(new DdcManagementInstanceQuery(
                        scope.bizCode(), scope.env(), scope.appCode())));
            }
            return List.copyOf(result);
        });
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
    private GatewayEngineNodeConsistencyVO nodeState(
            DdcManagementConfigClientInstance node,
            String status,
            String reason) {
        Map<String, String> metadata = node.metadata();
        return new GatewayEngineNodeConsistencyVO(
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
    public GatewayProjectionCounts scopeCounts(
            String bizCode,
            String appCode,
            String env,
            String namespace) {
        long totalEngines = 0;
        long readyEngines = 0;
        long inconsistentGroups = 0;
        boolean stale = false;
        List<GatewayGroupPO> scopedGroups = groups
                .findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(
                        env,
                        namespace
                );
        for (GatewayGroupPO group : scopedGroups) {
            if (!group.isEnabled()) {
                continue;
            }
            GatewayRuntimeConsistencyVO consistency =
                    runtimeConsistency(group.getId());
            totalEngines += consistency.engineNodeCount();
            readyEngines += consistency.readyEngineNodeCount();
            inconsistentGroups += consistency.consistent() ? 0 : 1;
            stale = stale || consistency.stale();
        }
        GatewayProjectionEnvelopeVO<List<GatewayProviderInstanceVO>> providers =
                instances(bizCode, appCode, env, namespace);
        long activeProviders = providers.value().stream()
                .filter(this::online)
                .count();
        return new GatewayProjectionCounts(
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
    private GatewayGroupPO group(String id) {
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
        DdcManagementClient client = clients.getIfAvailable();
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
            List<GatewayProviderInstanceVO> result,
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
    private GatewayProviderInstanceVO projection(
            DdcManagementServiceKey service,
            DdcManagementServiceInstance instance,
            Instant observedAt) {
        Map<String, String> metadata = instance.metadata();
        return new GatewayProviderInstanceVO(
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
    private boolean online(GatewayProviderInstanceVO instance) {
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
    private <T> GatewayProjectionEnvelopeVO<T> load(
            String key,
            String source,
            Supplier<T> loader) {
        try {
            GatewayProjectionEnvelopeVO<T> value = new GatewayProjectionEnvelopeVO<>(
                    loader.get(),
                    clock.instant(),
                    source,
                    false,
                    null
            );
            cache.put(key, value);
            return value;
        } catch (RuntimeException failure) {
            GatewayProjectionEnvelopeVO<T> previous =
                    (GatewayProjectionEnvelopeVO<T>) cache.get(key);
            if (previous != null) {
                return new GatewayProjectionEnvelopeVO<>(
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

}
