package top.egon.cola.component.gateway.admin.release.repository.jdbc;


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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleasePublicationRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static top.egon.cola.component.gateway.admin.shared.repository.jdbc.GatewayJdbcParameters.timestamp;

/**
 * 中文说明：{@code JdbcGatewayReleasePublicationRepository} 是存储组件，位于当前 Gateway 模块的相关包中，负责Jdbc网关发布Publication存储相关的职责与边界。
 * English summary: {@code JdbcGatewayReleasePublicationRepository} is a jdbc gateway release publication store store in the current Gateway module; it owns the jdbc gateway release publication store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcGatewayReleasePublicationRepository
        implements GatewayReleasePublicationRepository {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcGatewayReleasePublicationRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcGatewayReleasePublicationRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayReleasePublicationRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayReleasePublicationRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：创建 {@code JdbcGatewayReleasePublicationRepository} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcGatewayReleasePublicationRepository} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     */
    public JdbcGatewayReleasePublicationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 中文说明：执行 insertAll 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert all operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.insertAll(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operations 参数 operations；parameter operations。
     */
    @Override
    @Transactional
    public void insertAll(List<GatewayReleasePublicationPO> operations) {
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException(
                    "publication operations must not be empty"
            );
        }
        operations.forEach(this::insert);
    }

    /**
     * 中文说明：执行 findAttempt 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find attempt operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.findAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @return 返回 findAttempt 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayReleasePublicationPO> findAttempt(
            String releaseId,
            int attemptNo) {
        return jdbc.query("""
                SELECT release_id, attempt_no, phase_order, phase_type,
                       config_key, content_value, content_sha256,
                       expected_version, change_id, ddc_target_version,
                       ddc_status, error_code, error_message,
                       created_at, updated_at
                  FROM gateway_release_publication
                 WHERE release_id = ? AND attempt_no = ?
                 ORDER BY phase_order
                """, (result, row) -> publication(result),
                releaseId, attemptNo);
    }

    /**
     * 中文说明：执行 nextIncomplete 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the next incomplete operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.nextIncomplete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @return 返回 nextIncomplete 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<GatewayReleasePublicationPO> nextIncomplete(
            String releaseId,
            int attemptNo) {
        return jdbc.query("""
                SELECT release_id, attempt_no, phase_order, phase_type,
                       config_key, content_value, content_sha256,
                       expected_version, change_id, ddc_target_version,
                       ddc_status, error_code, error_message,
                       created_at, updated_at
                  FROM gateway_release_publication
                 WHERE release_id = ? AND attempt_no = ?
                   AND ddc_status <> 'SUCCESS'
                 ORDER BY phase_order
                 LIMIT 1
                """, (result, row) -> publication(result),
                releaseId, attemptNo).stream().findFirst();
    }

    /**
     * 中文说明：执行 findChunkCleanupCandidates 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find chunk cleanup candidates operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.findChunkCleanupCandidates(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param successorActivatedBefore 参数 successorActivatedBefore；parameter successor activated before。
     * @return 返回 findChunkCleanupCandidates 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayChunkCleanupCandidatePO> findChunkCleanupCandidates(
            Instant successorActivatedBefore) {
        return jdbc.query("""
                SELECT publication.change_id,
                       publication.release_id,
                       'gateway-engine-' || (
                           content.canonical_snapshot
                               -> 'content' ->> 'gatewayGroupCode'
                       ) AS app_code,
                       content.canonical_snapshot
                           -> 'content' ->> 'env' AS env,
                       content.canonical_snapshot
                           -> 'content' ->> 'namespace' AS namespace,
                       publication.config_key,
                       publication.ddc_target_version
                  FROM gateway_release_publication publication
                  JOIN gateway_release old_release
                    ON old_release.id = publication.release_id
                  JOIN gateway_release_content content
                    ON content.release_id = publication.release_id
                 WHERE publication.phase_type = 'CHUNK'
                   AND publication.ddc_status = 'SUCCESS'
                   AND publication.ddc_target_version IS NOT NULL
                   AND COALESCE(publication.error_code, '')
                       <> 'CHUNK_GC_DELETED'
                   AND NOT EXISTS (
                       SELECT 1
                         FROM gateway_draft active_draft
                        WHERE active_draft.based_on_release_id =
                              publication.release_id
                   )
                   AND EXISTS (
                       SELECT 1
                         FROM gateway_release successor
                         JOIN gateway_release_publication activation
                           ON activation.release_id = successor.id
                          AND activation.phase_type = 'ACTIVATION'
                          AND activation.ddc_status = 'SUCCESS'
                        WHERE successor.gateway_group_id =
                              old_release.gateway_group_id
                          AND successor.created_at > old_release.created_at
                          AND activation.updated_at <= ?
                   )
                 ORDER BY old_release.created_at, publication.phase_order
                """, (result, row) -> new GatewayChunkCleanupCandidatePO(
                result.getString("change_id"),
                result.getString("release_id"),
                result.getString("app_code"),
                result.getString("env"),
                result.getString("namespace"),
                result.getString("config_key"),
                result.getLong("ddc_target_version")
        ), timestamp(successorActivatedBefore));
    }

    /**
     * 中文说明：执行 resolveDocument 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve document operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.resolveDocument(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param expectedVersion 参数 expectedVersion；parameter expected version。
     * @param documentContent 参数 documentContent；parameter document content。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void resolveDocument(
            String changeId,
            long expectedVersion,
            String documentContent,
            Instant now) {
        if (expectedVersion < 0) {
            throw new IllegalArgumentException(
                    "expectedVersion must not be negative"
            );
        }
        if (documentContent == null || documentContent.isBlank()) {
            throw new IllegalArgumentException(
                    "documentContent must not be blank"
            );
        }
        int changed = jdbc.update("""
                UPDATE gateway_release_publication
                   SET expected_version = ?, content_value = ?,
                       ddc_status = 'RESOLVED', updated_at = ?
                 WHERE change_id = ? AND ddc_status <> 'SUCCESS'
                """, expectedVersion, documentContent,
                timestamp(now), changeId);
        requireChanged(
                changed,
                "successful publication cannot be resolved again"
        );
    }

    /**
     * 中文说明：执行 markSubmitted 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mark submitted operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.markSubmitted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void markSubmitted(String changeId, Instant now) {
        int changed = jdbc.update("""
                UPDATE gateway_release_publication
                   SET ddc_status = 'SUBMITTED', updated_at = ?
                 WHERE change_id = ? AND ddc_status = 'RESOLVED'
                   AND expected_version IS NOT NULL
                """, timestamp(now), changeId);
        requireChanged(
                changed,
                "publication must be RESOLVED before submission"
        );
    }

    /**
     * 中文说明：执行 markResult 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mark result operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.markResult(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param targetVersion 参数 targetVersion；parameter target version。
     * @param status 参数 status；parameter status。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param errorMessage 参数 error消息；parameter error message。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void markResult(
            String changeId,
            Long targetVersion,
            GatewayPublicationStatusEnum status,
            String errorCode,
            String errorMessage,
            Instant now) {
        if (status == null || !status.terminalResult()) {
            throw new IllegalArgumentException(
                    "publication result status must be terminal"
            );
        }
        if (status == GatewayPublicationStatusEnum.SUCCESS && targetVersion == null) {
            throw new IllegalArgumentException(
                    "successful publication requires targetVersion"
            );
        }
        int changed = jdbc.update("""
                UPDATE gateway_release_publication
                   SET ddc_target_version = ?, ddc_status = ?,
                       error_code = ?, error_message = ?, updated_at = ?
                 WHERE change_id = ? AND ddc_status IN (
                       'RESOLVED', 'SUBMITTED', 'PARTIAL_SUCCESS',
                       'TIMEOUT', 'UNKNOWN'
                 )
                """,
                targetVersion,
                status.name(),
                errorCode,
                errorMessage,
                timestamp(now),
                changeId
        );
        requireChanged(changed, "publication result cannot be recorded");
    }

    /**
     * 中文说明：执行 markChunkCleaned 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mark chunk cleaned operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.markChunkCleaned(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void markChunkCleaned(String changeId, Instant now) {
        int changed = jdbc.update("""
                UPDATE gateway_release_publication
                   SET error_code = 'CHUNK_GC_DELETED',
                       error_message = NULL, updated_at = ?
                 WHERE change_id = ? AND phase_type = 'CHUNK'
                   AND ddc_status = 'SUCCESS'
                   AND ddc_target_version IS NOT NULL
                """, timestamp(now), changeId);
        requireChanged(changed, "cleaned chunk publication was not found");
    }

    /**
     * 中文说明：执行 insert 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.insert(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     */
    private void insert(GatewayReleasePublicationPO operation) {
        if (operation == null) {
            throw new IllegalArgumentException(
                    "publication operation must not be null"
            );
        }
        jdbc.update("""
                INSERT INTO gateway_release_publication(
                    release_id, attempt_no, phase_order, phase_type,
                    config_key, content_value, content_sha256,
                    expected_version, change_id, ddc_target_version,
                    ddc_status, error_code, error_message,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                operation.releaseId(),
                operation.attemptNo(),
                operation.phaseOrder(),
                operation.phaseType().name(),
                operation.configKey(),
                operation.contentValue(),
                operation.contentSha256(),
                operation.expectedVersion(),
                operation.changeId(),
                operation.ddcTargetVersion(),
                operation.status().name(),
                operation.errorCode(),
                operation.errorMessage(),
                timestamp(operation.createdAt()),
                timestamp(operation.updatedAt())
        );
    }

    /**
     * 中文说明：执行 publication 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the publication operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.publication(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 publication 的处理结果；returns the result of the operation.
     */
    private GatewayReleasePublicationPO publication(ResultSet result)
            throws SQLException {
        return new GatewayReleasePublicationPO(
                result.getString("release_id"),
                result.getInt("attempt_no"),
                result.getInt("phase_order"),
                GatewayPublicationPhaseEnum.valueOf(result.getString("phase_type")),
                result.getString("config_key"),
                result.getString("content_value"),
                result.getString("content_sha256"),
                (Long) result.getObject("expected_version"),
                result.getString("change_id"),
                (Long) result.getObject("ddc_target_version"),
                GatewayPublicationStatusEnum.valueOf(result.getString("ddc_status")),
                result.getString("error_code"),
                result.getString("error_message"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()
        );
    }

    /**
     * 中文说明：执行 requireChanged 操作；该方法是 {@code JdbcGatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require changed operation; this method is the invocation entry point on {@code JdbcGatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayReleasePublicationRepository.requireChanged(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changed 参数 changed；parameter changed。
     * @param message 参数 消息；parameter message。
     */
    private void requireChanged(int changed, String message) {
        if (changed != 1) {
            throw new IllegalStateException(message);
        }
    }
}
