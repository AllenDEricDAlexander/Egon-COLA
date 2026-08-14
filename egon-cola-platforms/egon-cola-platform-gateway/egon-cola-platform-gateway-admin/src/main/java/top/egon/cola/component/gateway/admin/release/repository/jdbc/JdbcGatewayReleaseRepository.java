package top.egon.cola.component.gateway.admin.release.repository.jdbc;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleaseRepository;
import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.rule.domain.vo.CompiledGatewayRelease;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static top.egon.cola.component.gateway.admin.shared.repository.jdbc.GatewayJdbcParameters.timestamp;

/**
 * 中文说明：{@code JdbcGatewayReleaseRepository} 是存储组件，位于当前 Gateway 模块的相关包中，负责Jdbc网关发布存储相关的职责与边界。
 * English summary: {@code JdbcGatewayReleaseRepository} is a jdbc gateway release store store in the current Gateway module; it owns the jdbc gateway release store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcGatewayReleaseRepository implements GatewayReleaseRepository {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcGatewayReleaseRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcGatewayReleaseRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayReleaseRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayReleaseRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code JdbcGatewayReleaseRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code JdbcGatewayReleaseRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayReleaseRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayReleaseRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code JdbcGatewayReleaseRepository} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcGatewayReleaseRepository} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcGatewayReleaseRepository(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * 中文说明：执行 insert 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.insert(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param release 参数 发布；parameter release。
     * @param compiled 参数 compiled；parameter compiled。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     */
    @Override
    public void insert(
            GatewayReleasePO release,
            CompiledGatewayRelease compiled,
            int attemptNo) {
        jdbc.update("""
                INSERT INTO gateway_release(
                    id, gateway_group_id, draft_revision,
                    based_on_release_id, rollback_of_release_id, status,
                    partial_applied, change_id, validation_report,
                    structured_diff, change_reason, created_at, created_by,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, FALSE, NULL, ?::jsonb,
                          ?::jsonb, ?, ?, ?, ?)
                """,
                release.id(),
                release.gatewayGroupId(),
                release.draftRevision(),
                release.basedOnReleaseId(),
                release.rollbackOfReleaseId(),
                release.status().name(),
                json(release.validationReport()),
                json(release.structuredDiff()),
                release.changeReason(),
                timestamp(release.createdAt()),
                release.createdBy(),
                timestamp(release.updatedAt())
        );
        jdbc.update("""
                INSERT INTO gateway_release_content(
                    release_id, rule_content_sha256, artifact_sha256,
                    canonical_snapshot, activation_content, chunk_manifest,
                    snapshot_size, created_at
                ) VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?)
                """,
                release.id(),
                compiled.snapshot().ruleContentSha256(),
                compiled.snapshot().artifactSha256(),
                compiled.snapshotJson(),
                compiled.activationJson(),
                json(compiled.chunkValues()),
                compiled.snapshotJson().getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                ).length,
                timestamp(release.createdAt())
        );
        insertAttempt(
                release.id(),
                attemptNo,
                "PENDING",
                release.createdAt()
        );
    }

    /**
     * 中文说明：执行 find 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<GatewayReleasePO> find(String releaseId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, draft_revision,
                       based_on_release_id, rollback_of_release_id, status,
                       partial_applied, change_id,
                       validation_report::text AS validation_report,
                       structured_diff::text AS structured_diff,
                       change_reason, created_at, created_by, updated_at
                  FROM gateway_release
                 WHERE id = ?
                """, (result, row) -> new GatewayReleasePO(
                result.getString("id"),
                result.getString("gateway_group_id"),
                result.getLong("draft_revision"),
                result.getString("based_on_release_id"),
                result.getString("rollback_of_release_id"),
                GatewayReleaseStatus.valueOf(result.getString("status")),
                result.getBoolean("partial_applied"),
                result.getString("change_id"),
                map(result.getString("validation_report")),
                map(result.getString("structured_diff")),
                result.getString("change_reason"),
                result.getTimestamp("created_at").toInstant(),
                result.getString("created_by"),
                result.getTimestamp("updated_at").toInstant()
        ), releaseId).stream().findFirst();
    }

    /**
     * 中文说明：执行 history 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the history operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.history(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 history 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayReleasePO> history(String gatewayGroupId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, draft_revision,
                       based_on_release_id, rollback_of_release_id, status,
                       partial_applied, change_id,
                       validation_report::text AS validation_report,
                       structured_diff::text AS structured_diff,
                       change_reason, created_at, created_by, updated_at
                  FROM gateway_release
                 WHERE gateway_group_id = ?
                 ORDER BY created_at DESC
                """, (result, row) -> release(result), gatewayGroupId);
    }

    /**
     * 中文说明：执行 recoverable 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the recoverable operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.recoverable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 recoverable 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayRecoverableReleaseAttemptPO> recoverable() {
        return jdbc.query("""
                SELECT r.id AS release_id,
                       r.gateway_group_id,
                       p.attempt_no
                  FROM gateway_release r
                  JOIN gateway_release_publication p
                    ON p.release_id = r.id
                 WHERE r.status IN (
                       'PUBLISHING', 'FAILED', 'TIMEOUT', 'UNKNOWN'
                 )
                   AND p.attempt_no = (
                       SELECT MAX(candidate.attempt_no)
                         FROM gateway_release_publication candidate
                        WHERE candidate.release_id = r.id
                   )
                 GROUP BY r.id, r.gateway_group_id,
                          p.attempt_no, r.updated_at
                 ORDER BY r.updated_at
                """, (result, row) -> new GatewayRecoverableReleaseAttemptPO(
                result.getString("release_id"),
                result.getString("gateway_group_id"),
                result.getInt("attempt_no")
        ));
    }

    /**
     * 中文说明：执行 attempts 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attempts operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.attempts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 attempts 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayReleaseAttemptPO> attempts(String releaseId) {
        Map<Integer, List<GatewayReleaseTargetPO>> targets =
                new java.util.LinkedHashMap<>();
        jdbc.query("""
                SELECT attempt_no, instance_id, lease_id, status,
                       applied_version, applied_artifact_sha256, error_code,
                       observed_at
                  FROM gateway_release_target
                 WHERE release_id = ?
                 ORDER BY attempt_no, instance_id, lease_id
                """, result -> {
            targets.computeIfAbsent(
                    result.getInt("attempt_no"),
                    ignored -> new java.util.ArrayList<>()
            ).add(new GatewayReleaseTargetPO(
                    result.getString("instance_id"),
                    result.getString("lease_id"),
                    result.getString("status"),
                    (Long) result.getObject("applied_version"),
                    result.getString("applied_artifact_sha256"),
                    result.getString("error_code"),
                    result.getTimestamp("observed_at").toInstant()
            ));
        }, releaseId);
        return jdbc.query("""
                SELECT attempt_no, status, change_id, started_at,
                       completed_at, error_code, error_message
                  FROM gateway_release_attempt
                 WHERE release_id = ?
                 ORDER BY attempt_no DESC
                """, (result, row) -> new GatewayReleaseAttemptPO(
                result.getInt("attempt_no"),
                result.getString("status"),
                result.getString("change_id"),
                result.getTimestamp("started_at").toInstant(),
                result.getTimestamp("completed_at") == null
                        ? null
                        : result.getTimestamp("completed_at").toInstant(),
                result.getString("error_code"),
                result.getString("error_message"),
                List.copyOf(targets.getOrDefault(
                        result.getInt("attempt_no"),
                        List.of()
                ))
        ), releaseId);
    }

    /**
     * 中文说明：执行 latestAttempt 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the latest attempt operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.latestAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 latestAttempt 的处理结果；returns the result of the operation.
     */
    @Override
    public int latestAttempt(String releaseId) {
        Integer attempt = jdbc.queryForObject("""
                SELECT COALESCE(MAX(attempt_no), 0)
                  FROM gateway_release_attempt
                 WHERE release_id = ?
                """, Integer.class, releaseId);
        if (attempt == null || attempt == 0) {
            throw new IllegalArgumentException(
                    "release attempt was not found"
            );
        }
        return attempt;
    }

    /**
     * 中文说明：执行 loadCompiled 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load compiled operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.loadCompiled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 loadCompiled 的处理结果；returns the result of the operation.
     */
    @Override
    public CompiledGatewayRelease loadCompiled(String releaseId) {
        return jdbc.query("""
                SELECT canonical_snapshot::text AS canonical_snapshot,
                       activation_content::text AS activation_content,
                       chunk_manifest::text AS chunk_manifest
                  FROM gateway_release_content
                 WHERE release_id = ?
                """, result -> {
            if (!result.next()) {
                throw new IllegalArgumentException(
                        "release content was not found"
                );
            }
            String snapshotJson = result.getString("canonical_snapshot");
            String activationJson = result.getString("activation_content");
            return new CompiledGatewayRelease(
                    read(snapshotJson, GatewayRuleSnapshot.class),
                    snapshotJson,
                    read(activationJson, GatewayRuleActivation.class),
                    activationJson,
                    stringMap(result.getString("chunk_manifest"))
            );
        }, releaseId);
    }

    /**
     * 中文说明：执行 nextAttempt 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the next attempt operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.nextAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param now 参数 now；parameter now。
     * @return 返回 nextAttempt 的处理结果；returns the result of the operation.
     */
    @Override
    public int nextAttempt(String releaseId, Instant now) {
        Integer maximum = jdbc.queryForObject("""
                SELECT COALESCE(MAX(attempt_no), 0)
                  FROM gateway_release_attempt
                 WHERE release_id = ?
                """, Integer.class, releaseId);
        int attempt = maximum == null ? 1 : maximum + 1;
        insertAttempt(releaseId, attempt, "PENDING", now);
        return attempt;
    }

    /**
     * 中文说明：执行 beginAttempt 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the begin attempt operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.beginAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void beginAttempt(
            String releaseId,
            int attemptNo,
            Instant now) {
        jdbc.update("""
                UPDATE gateway_release_attempt
                   SET status = 'PUBLISHING', started_at = ?,
                       completed_at = NULL, error_code = NULL,
                       error_message = NULL
                 WHERE release_id = ? AND attempt_no = ?
                """, timestamp(now), releaseId, attemptNo);
        jdbc.update("""
                UPDATE gateway_release
                   SET status = 'PUBLISHING', updated_at = ?
                 WHERE id = ?
                """, timestamp(now), releaseId);
    }

    /**
     * 中文说明：执行 completeAttempt 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the complete attempt operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.completeAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param status 参数 status；parameter status。
     * @param partialApplied 参数 partialApplied；parameter partial applied。
     * @param changeId 参数 changeId；parameter change id。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param errorMessage 参数 error消息；parameter error message。
     * @param targets 参数 targets；parameter targets。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void completeAttempt(
            String releaseId,
            int attemptNo,
            GatewayReleaseStatus status,
            boolean partialApplied,
            String changeId,
            String errorCode,
            String errorMessage,
            List<GatewayReleaseTargetPO> targets,
            Instant now) {
        jdbc.update("""
                UPDATE gateway_release_attempt
                   SET status = ?, change_id = ?, completed_at = ?,
                       error_code = ?, error_message = ?
                 WHERE release_id = ? AND attempt_no = ?
                """,
                status.name(),
                changeId,
                timestamp(now),
                errorCode,
                errorMessage,
                releaseId,
                attemptNo
        );
        jdbc.update("""
                UPDATE gateway_release
                   SET status = ?, partial_applied = ?, change_id = ?,
                       updated_at = ?
                 WHERE id = ?
                """,
                status.name(),
                partialApplied,
                changeId,
                timestamp(now),
                releaseId
        );
        targets.forEach(target -> jdbc.update("""
                INSERT INTO gateway_release_target(
                    release_id, attempt_no, instance_id, lease_id, status,
                    applied_version, applied_artifact_sha256, error_code,
                    observed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (
                    release_id, attempt_no, instance_id, lease_id
                ) DO UPDATE SET status = EXCLUDED.status,
                                applied_version = EXCLUDED.applied_version,
                                applied_artifact_sha256 =
                                    EXCLUDED.applied_artifact_sha256,
                                error_code = EXCLUDED.error_code,
                                observed_at = EXCLUDED.observed_at
                """,
                releaseId,
                attemptNo,
                target.instanceId(),
                target.leaseId(),
                target.status(),
                target.appliedVersion(),
                target.appliedArtifactSha256(),
                target.errorCode(),
                timestamp(target.observedAt())
        ));
    }

    /**
     * 中文说明：执行 has发布InProgress 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the has release in progress operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.hasReleaseInProgress(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 has发布InProgress 的处理结果；returns the result of the operation.
     */
    @Override
    public boolean hasReleaseInProgress(String gatewayGroupId) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM gateway_release
                 WHERE gateway_group_id = ?
                   AND status IN (
                       'CREATED', 'VALIDATING', 'READY', 'PUBLISHING'
                   )
                """, Integer.class, gatewayGroupId);
        return count != null && count > 0;
    }

    /**
     * 中文说明：执行 insertAttempt 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert attempt operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.insertAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param status 参数 status；parameter status。
     * @param now 参数 now；parameter now。
     */
    private void insertAttempt(
            String releaseId,
            int attemptNo,
            String status,
            Instant now) {
        jdbc.update("""
                INSERT INTO gateway_release_attempt(
                    release_id, attempt_no, status, started_at
                ) VALUES (?, ?, ?, ?)
                """, releaseId, attemptNo, status, timestamp(now));
    }

    /**
     * 中文说明：执行 发布 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the release operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.release(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 发布 的处理结果；returns the result of the operation.
     */
    private GatewayReleasePO release(java.sql.ResultSet result)
            throws java.sql.SQLException {
        return new GatewayReleasePO(
                result.getString("id"),
                result.getString("gateway_group_id"),
                result.getLong("draft_revision"),
                result.getString("based_on_release_id"),
                result.getString("rollback_of_release_id"),
                GatewayReleaseStatus.valueOf(result.getString("status")),
                result.getBoolean("partial_applied"),
                result.getString("change_id"),
                map(result.getString("validation_report")),
                map(result.getString("structured_diff")),
                result.getString("change_reason"),
                result.getTimestamp("created_at").toInstant(),
                result.getString("created_by"),
                result.getTimestamp("updated_at").toInstant()
        );
    }

    /**
     * 中文说明：执行 json 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.json(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 json 的处理结果；returns the result of the operation.
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "release value cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 map 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> map(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored release metadata is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 stringMap 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string map operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.stringMap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 stringMap 的处理结果；returns the result of the operation.
     */
    private Map<String, String> stringMap(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<Map<String, String>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored chunk manifest is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code JdbcGatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code JdbcGatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleaseRepository.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param type 参数 type；parameter type。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored release content is invalid",
                    failure
            );
        }
    }
}
