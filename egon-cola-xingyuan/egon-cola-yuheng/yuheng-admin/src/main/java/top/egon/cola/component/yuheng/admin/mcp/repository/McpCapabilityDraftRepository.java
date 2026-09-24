package top.egon.cola.component.yuheng.admin.mcp.repository;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import java.time.Instant;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpCapabilityDraftBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpCapabilityRecordBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpCapabilityDraftMutationDTO;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.McpCapabilityKindEnum;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;

/**
 * 中文说明：{@code McpCapabilityDraftRepository} 是持久化公开业务端口，签名与原 {@code legacy JDBC CapabilityDraft facade} 一致，仅把 PO 返回与入参替换为 BO。
 * English summary: {@code McpCapabilityDraftRepository} is the public persistence port whose signatures mirror {@code legacy JDBC CapabilityDraft facade} with PO types replaced by BO types.
 *
 * 用法 / Usage: 由应用/领域服务按限定名注入该端口，实现类承担事务与守卫查询。/ Inject this port from the application layer; the implementation owns transactions and guarded queries.
 */
@Validated
public interface McpCapabilityDraftRepository {

    /**
     * 中文说明：执行 load 操作；该端口方法是 McpCapabilityDraftRepository 的调用入口。
     * English summary: Executes the load operation; this port method is the invocation entry point of McpCapabilityDraftRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityDraftRepository.load(...)}。
     * @param gatewayGroupId 参数；parameter.
     * @return 返回 load 的处理结果；returns the result.
     */
    McpCapabilityDraftBO load(String gatewayGroupId);

    /**
     * 中文说明：执行 save 操作；该端口方法是 McpCapabilityDraftRepository 的调用入口。
     * English summary: Executes the save operation; this port method is the invocation entry point of McpCapabilityDraftRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityDraftRepository.save(..., ..., ..., ...)}。
     * @param draft 参数；parameter.
     * @return 返回 save 的处理结果；returns the result.
     */
    McpCapabilityDraftMutationDTO save(McpCapabilityRecordBO draft, long expectedRevision, AdminActor actor, Instant now);

    /**
     * 中文说明：执行 softDelete 操作；该端口方法是 McpCapabilityDraftRepository 的调用入口。
     * English summary: Executes the softDelete operation; this port method is the invocation entry point of McpCapabilityDraftRepository.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCapabilityDraftRepository.softDelete(..., ..., ..., ..., ...)}。
     * @param kind 参数；parameter.
     * @return 返回 softDelete 的处理结果；returns the result.
     */
    McpCapabilityDraftMutationDTO softDelete(McpCapabilityKindEnum kind, String id, long expectedRevision, AdminActor actor, Instant now);

}
