package top.egon.cola.component.yuheng.admin.mcp.repository;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpManagedToolOverrideBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpManagedToolDraftMutationDTO;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;

/**
 * 中文说明：{@code McpManagedToolOverrideRepository} 是持久化公开业务端口，签名与原 {@code legacy JDBC ManagedToolOverride facade} 一致，仅把 PO 返回与入参替换为 BO。
 * English summary: {@code McpManagedToolOverrideRepository} is the public persistence port whose signatures mirror {@code legacy JDBC ManagedToolOverride facade} with PO types replaced by BO types.
 *
 * 用法 / Usage: 由应用/领域服务按限定名注入该端口，实现类承担事务与守卫查询。/ Inject this port from the application layer; the implementation owns transactions and guarded queries.
 */
@Validated
public interface McpManagedToolOverrideRepository {

    /**
     * 中文说明：执行 load 操作；该端口方法是 McpManagedToolOverrideRepository 的调用入口。
     * English summary: Executes the load operation; this port method is the invocation entry point of McpManagedToolOverrideRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpManagedToolOverrideRepository.load(...)}。
     * @param gatewayGroupId 参数；parameter.
     * @return 返回 load 的处理结果；returns the result.
     */
    List<McpManagedToolOverrideBO> load(String gatewayGroupId);

    /**
     * 中文说明：执行 save 操作；该端口方法是 McpManagedToolOverrideRepository 的调用入口。
     * English summary: Executes the save operation; this port method is the invocation entry point of McpManagedToolOverrideRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpManagedToolOverrideRepository.save(..., ..., ..., ...)}。
     * @param override 参数；parameter.
     * @return 返回 save 的处理结果；returns the result.
     */
    McpManagedToolDraftMutationDTO save(McpManagedToolOverrideBO override, long expectedRevision, AdminActor actor, Instant now);

    /**
     * 中文说明：执行 delete 操作；该端口方法是 McpManagedToolOverrideRepository 的调用入口。
     * English summary: Executes the delete operation; this port method is the invocation entry point of McpManagedToolOverrideRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpManagedToolOverrideRepository.delete(..., ..., ..., ...)}。
     * @param toolId 参数；parameter.
     * @return 返回 delete 的处理结果；returns the result.
     */
    McpManagedToolDraftMutationDTO delete(String toolId, String gatewayGroupId, String operationId, long expectedRevision);

}
