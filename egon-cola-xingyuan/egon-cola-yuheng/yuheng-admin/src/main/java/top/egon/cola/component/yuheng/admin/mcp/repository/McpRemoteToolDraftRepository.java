package top.egon.cola.component.yuheng.admin.mcp.repository;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpRemoteToolDraftBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;

/**
 * 中文说明：{@code McpRemoteToolDraftRepository} 是持久化公开业务端口，签名与原 {@code legacy JDBC RemoteToolDraft facade} 一致，仅把 PO 返回与入参替换为 BO。
 * English summary: {@code McpRemoteToolDraftRepository} is the public persistence port whose signatures mirror {@code legacy JDBC RemoteToolDraft facade} with PO types replaced by BO types.
 *
 * 用法 / Usage: 由应用/领域服务按限定名注入该端口，实现类承担事务与守卫查询。/ Inject this port from the application layer; the implementation owns transactions and guarded queries.
 */
@Validated
public interface McpRemoteToolDraftRepository {

    /**
     * 中文说明：执行 load 操作；该端口方法是 McpRemoteToolDraftRepository 的调用入口。
     * English summary: Executes the load operation; this port method is the invocation entry point of McpRemoteToolDraftRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteToolDraftRepository.load(...)}。
     * @param gatewayGroupId 参数；parameter.
     * @return 返回 load 的处理结果；returns the result.
     */
    List<McpRemoteToolDraftBO> load(String gatewayGroupId);

    /**
     * 中文说明：执行 save 操作；该端口方法是 McpRemoteToolDraftRepository 的调用入口。
     * English summary: Executes the save operation; this port method is the invocation entry point of McpRemoteToolDraftRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteToolDraftRepository.save(..., ..., ..., ...)}。
     * @param draft 参数；parameter.
     * @return 返回 save 的处理结果；returns the result.
     */
    McpRemoteToolDraftMutationDTO save(McpRemoteToolDraftBO draft, long expectedRevision, AdminActor actor, Instant now);

    /**
     * 中文说明：执行 softDelete 操作；该端口方法是 McpRemoteToolDraftRepository 的调用入口。
     * English summary: Executes the softDelete operation; this port method is the invocation entry point of McpRemoteToolDraftRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteToolDraftRepository.softDelete(..., ..., ..., ...)}。
     * @param id 参数；parameter.
     * @return 返回 softDelete 的处理结果；returns the result.
     */
    McpRemoteToolDraftMutationDTO softDelete(String id, long expectedRevision, AdminActor actor, Instant now);

}
