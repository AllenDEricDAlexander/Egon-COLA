package top.egon.cola.component.gateway.admin.mcp.repository.jdbc;


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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


import top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO;
/**
 * 中文说明：{@code JdbcMcpApprovalRepository} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCP审批存储相关的职责与边界。
 * English summary: {@code JdbcMcpApprovalRepository} is a jdbc mcp approval store store in the current Gateway module; it owns the jdbc mcp approval store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpApprovalRepository {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpApprovalRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpApprovalRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpApprovalRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpApprovalRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：创建 {@code JdbcMcpApprovalRepository} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpApprovalRepository} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     */
    public JdbcMcpApprovalRepository(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    /**
     * 中文说明：执行 issue 操作；该方法是 {@code JdbcMcpApprovalRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the issue operation; this method is the invocation entry point on {@code JdbcMcpApprovalRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalRepository.issue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param approval 参数 审批；parameter approval。
     */
    public void issue(McpApprovalPO approval) {
        Objects.requireNonNull(approval, "approval");
        jdbc.update("""
                INSERT INTO gateway_mcp_approval(
                    id, token_digest, subject_id, tenant_id, client_id,
                    server_code, tool_name, argument_digest, status,
                    revision, issued_at, expires_at, consumed_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0, ?, ?, NULL
                )
                """,
                approval.id(),
                approval.tokenDigest(),
                approval.subjectId(),
                approval.tenantId(),
                approval.clientId(),
                approval.serverCode(),
                approval.toolName(),
                approval.argumentDigest(),
                McpJdbcJson.timestamp(approval.issuedAt()),
                McpJdbcJson.timestamp(approval.expiresAt())
        );
    }

    /**
     * 中文说明：执行 consume 操作；该方法是 {@code JdbcMcpApprovalRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the consume operation; this method is the invocation entry point on {@code JdbcMcpApprovalRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalRepository.consume(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tokenDigest 参数 tokenDigest；parameter token digest。
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具Name；parameter tool name。
     * @param argumentDigest 参数 argumentDigest；parameter argument digest。
     * @param now 参数 now；parameter now。
     * @return 返回 consume 的处理结果；returns the result of the operation.
     */
    public boolean consume(
            String tokenDigest,
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String argumentDigest,
            Instant now) {
        return jdbc.update("""
                UPDATE gateway_mcp_approval
                   SET status = 'CONSUMED', consumed_at = ?,
                       revision = revision + 1
                 WHERE token_digest = ?
                   AND subject_id = ?
                   AND tenant_id = ?
                   AND client_id = ?
                   AND server_code = ?
                   AND tool_name = ?
                   AND argument_digest = ?
                   AND status = 'PENDING'
                   AND expires_at > ?
                """,
                McpJdbcJson.timestamp(now),
                digest(tokenDigest, "tokenDigest"),
                McpJdbcJson.required(subjectId, "subjectId"),
                McpJdbcJson.required(tenantId, "tenantId"),
                McpJdbcJson.required(clientId, "clientId"),
                McpJdbcJson.required(serverCode, "serverCode"),
                McpJdbcJson.required(toolName, "toolName"),
                digest(argumentDigest, "argumentDigest"),
                McpJdbcJson.timestamp(now)
        ) == 1;
    }

    /**
     * 中文说明：执行 find 操作；该方法是 {@code JdbcMcpApprovalRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcMcpApprovalRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalRepository.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    public Optional<McpApprovalPO> find(String id) {
        List<McpApprovalPO> values = jdbc.query("""
                SELECT id, token_digest, subject_id, tenant_id, client_id,
                       server_code, tool_name, argument_digest,
                       issued_at, expires_at
                  FROM gateway_mcp_approval
                 WHERE id = ?
                """, (result, row) -> new McpApprovalPO(
                result.getString("id"),
                result.getString("token_digest"),
                result.getString("subject_id"),
                result.getString("tenant_id"),
                result.getString("client_id"),
                result.getString("server_code"),
                result.getString("tool_name"),
                result.getString("argument_digest"),
                result.getTimestamp("issued_at").toInstant(),
                result.getTimestamp("expires_at").toInstant()
        ), id);
        return values.stream().findFirst();
    }

    /**
     * 中文说明：执行 expire 操作；该方法是 {@code JdbcMcpApprovalRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the expire operation; this method is the invocation entry point on {@code JdbcMcpApprovalRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalRepository.expire(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 expire 的处理结果；returns the result of the operation.
     */
    public int expire(Instant now) {
        return jdbc.update("""
                UPDATE gateway_mcp_approval
                   SET status = 'EXPIRED', revision = revision + 1
                 WHERE status = 'PENDING' AND expires_at <= ?
                """, McpJdbcJson.timestamp(now));
    }

    /**
     * 中文说明：执行 revoke 操作；该方法是 {@code JdbcMcpApprovalRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revoke operation; this method is the invocation entry point on {@code JdbcMcpApprovalRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalRepository.revoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @return 返回 revoke 的处理结果；returns the result of the operation.
     */
    public boolean revoke(String id, long expectedRevision) {
        return jdbc.update("""
                UPDATE gateway_mcp_approval
                   SET status = 'REVOKED', revision = revision + 1
                 WHERE id = ? AND revision = ? AND status = 'PENDING'
                """, id, expectedRevision) == 1;
    }

    /**
     * 中文说明：执行 digest 操作；该方法是 {@code JdbcMcpApprovalRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the digest operation; this method is the invocation entry point on {@code JdbcMcpApprovalRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpApprovalRepository.digest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 digest 的处理结果；returns the result of the operation.
     */
    private static String digest(String value, String field) {
        String digest = McpJdbcJson.required(value, field);
        if (digest.length() != 64) {
            throw new IllegalArgumentException(
                    field + " must contain 64 characters"
            );
        }
        return digest;
    }


}
