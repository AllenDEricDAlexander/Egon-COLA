package top.egon.cola.component.yuheng.admin.mcp.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpApprovalRequestDTO;
import top.egon.cola.component.yuheng.admin.mcp.domain.vo.McpApprovalOwnerVO;
import top.egon.cola.component.yuheng.admin.mcp.domain.vo.McpApprovalVO;

/**
 * 中文说明：{@code McpApprovalAdminService} 是 MCP 一次性审批凭据的管理面业务合同，承担令牌签发、摘要绑定与
 * 到期时间编排；接口层只交付已校验的身份归属，业务写事务与限定协作者由实现类持有。
 * English summary: {@code McpApprovalAdminService} is the management-plane business contract for one-time MCP approval
 * tokens; it owns token issuance, digest binding and expiry orchestration, while the implementation holds the write
 * transaction and its qualified collaborators.
 *
 * 用法 / Usage: 由 {@code McpApprovalController} 在解析出受信归属后调用；/ Call it from the controller after the trusted
 * owner has been resolved from the authenticated principal.
 */
@Validated
public interface McpApprovalAdminService {

    /**
     * 中文说明：为已通过校验的请求签发一次性审批凭据，明文令牌只在返回值中出现一次，持久层只保存绑定摘要。
     * English summary: Issues a one-time approval credential for a validated request; the plaintext token appears once in
     * the returned value and only its bound digest is persisted.
     *
     * 用法 / Usage: {@code mcpApprovalAdminServiceImpl.issue(request, owner)}。
     * @param request 参数 审批请求；parameter the approval request.
     * @param owner 参数 已验证的受信归属；parameter the verified trusted owner.
     * @return 返回 一次性审批凭据；returns the issued approval credential.
     */
    McpApprovalVO issue(
            @Valid @NotNull McpApprovalRequestDTO request,
            @NotNull McpApprovalOwnerVO owner
    );
}
