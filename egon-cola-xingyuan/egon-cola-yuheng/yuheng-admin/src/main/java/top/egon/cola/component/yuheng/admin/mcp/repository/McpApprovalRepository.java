package top.egon.cola.component.yuheng.admin.mcp.repository;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Optional;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpApprovalBO;

/**
 * 中文说明：{@code McpApprovalRepository} 是持久化公开业务端口，签名与原 {@code JdbcMcpApprovalRepository} 一致，仅把 PO 返回与入参替换为 BO。
 * English summary: {@code McpApprovalRepository} is the public persistence port whose signatures mirror {@code JdbcMcpApprovalRepository} with PO types replaced by BO types.
 *
 * 用法 / Usage: 由应用/领域服务按限定名注入该端口，实现类承担事务与守卫查询。/ Inject this port from the application layer; the implementation owns transactions and guarded queries.
 */
@Validated
public interface McpApprovalRepository {

    /**
     * 中文说明：执行 issue 操作；该端口方法是 McpApprovalRepository 的调用入口。
     * English summary: Executes the issue operation; this port method is the invocation entry point of McpApprovalRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalRepository.issue(...)}。
     * @param approval 参数；parameter.
     */
    void issue(McpApprovalBO approval);

    /**
     * 中文说明：执行 consume 操作；该端口方法是 McpApprovalRepository 的调用入口。
     * English summary: Executes the consume operation; this port method is the invocation entry point of McpApprovalRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalRepository.consume(..., ..., ..., ..., ..., ..., ..., ...)}。
     * @param tokenDigest 参数；parameter.
     * @return 返回 consume 的处理结果；returns the result.
     */
    boolean consume(String tokenDigest, String subjectId, String tenantId, String clientId, String serverCode, String toolName, String argumentDigest, Instant now);

    /**
     * 中文说明：执行 find 操作；该端口方法是 McpApprovalRepository 的调用入口。
     * English summary: Executes the find operation; this port method is the invocation entry point of McpApprovalRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalRepository.find(...)}。
     * @param id 参数；parameter.
     * @return 返回 find 的处理结果；returns the result.
     */
    Optional<McpApprovalBO> find(String id);

    /**
     * 中文说明：执行 expire 操作；该端口方法是 McpApprovalRepository 的调用入口。
     * English summary: Executes the expire operation; this port method is the invocation entry point of McpApprovalRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalRepository.expire(...)}。
     * @param now 参数；parameter.
     * @return 返回 expire 的处理结果；returns the result.
     */
    int expire(Instant now);

    /**
     * 中文说明：执行 revoke 操作；该端口方法是 McpApprovalRepository 的调用入口。
     * English summary: Executes the revoke operation; this port method is the invocation entry point of McpApprovalRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalRepository.revoke(..., ...)}。
     * @param id 参数；parameter.
     * @return 返回 revoke 的处理结果；returns the result.
     */
    boolean revoke(String id, long expectedRevision);

}
