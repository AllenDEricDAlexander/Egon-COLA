package top.egon.cola.component.yuheng.admin.mcp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpApprovalRequestDTO;
import top.egon.cola.component.yuheng.admin.mcp.domain.vo.McpApprovalOwnerVO;
import top.egon.cola.component.yuheng.admin.mcp.domain.vo.McpApprovalVO;
import top.egon.cola.component.yuheng.admin.mcp.service.McpApprovalAdminService;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

/**
 * 中文说明：{@code McpApprovalController} 是 MCP 一次性审批凭据的接口控制器，只负责把受信主体解析为归属并交付给
 * {@link McpApprovalAdminService}；令牌签发、摘要绑定与写事务编排在业务层，原 HTTP 路径、状态码与响应字段不变。
 * English summary: {@code McpApprovalController} is the interface controller for one-time MCP approval tokens; it only
 * resolves the authenticated principal into an owner and hands the request to {@link McpApprovalAdminService}. Token
 * issuance, digest binding and the write transaction live in the business layer, and the original HTTP path, status code
 * and response fields are unchanged.
 *
 * 用法 / Usage: 通过 Spring MVC 暴露的 {@code POST /api/v1/yuheng/admin/mcp/approvals} 调用；/ Invoked through the exposed
 * HTTP entry point.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/yuheng/admin/mcp/approvals")
@PreAuthorize("hasAnyAuthority('CAP_yuheng:mcp:approve','CAP_*')")
@Tag(name = "yuheng-admin")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "yuheng-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        interfaceGroupCode = "yuheng-admin")
@RequiredArgsConstructor
public class McpApprovalController {

    /**
     * 中文说明：保存 审批业务合同 对应的依赖值；字段类型为 {@code McpApprovalAdminService}，由
     * {@code McpApprovalController} 在其生命周期内读取或更新。
     * English summary: Holds the dependency represented by the approval business contract; its type is
     * {@code McpApprovalAdminService}, and {@code McpApprovalController} reads or updates it during its lifecycle.
     */
    @Qualifier("mcpApprovalAdminServiceImpl")
    private final McpApprovalAdminService approvalAdminService;

    /**
     * 中文说明：执行 issue 操作；该方法是 {@code McpApprovalController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the issue operation; this method is the invocation entry point on
     * {@code McpApprovalController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalController.issue(request, authentication)}。
     * @param request 参数 请求；parameter request。
     * @param authentication 参数 authentication；parameter authentication。
     * @return 返回 issue 的处理结果；returns the result of the operation.
     */
    @Operation(operationId = "admin.mcpApprovalController.issue")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public McpApprovalVO issue(
            @Valid @RequestBody McpApprovalRequestDTO request,
            Authentication authentication) {
        return approvalAdminService.issue(request, owner(authentication));
    }

    /**
     * 中文说明：执行 owner 操作；该方法是 {@code McpApprovalController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the owner operation; this method is the invocation entry point on
     * {@code McpApprovalController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 由所属类型的受控入口调用；/ Call it from the owning type's controlled entry points.
     * @param authentication 参数 authentication；parameter authentication。
     * @return 返回 owner 的处理结果；returns the result of the operation.
     */
    private McpApprovalOwnerVO owner(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "YUHENG_ADMIN_AUTHENTICATION_REQUIRED"
            );
        }
        if (authentication.getPrincipal()
                instanceof IdentityPrincipal principal) {
            return new McpApprovalOwnerVO(
                    principal.subject(),
                    principal.tenantId(),
                    principal.audience().stream()
                            .sorted()
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                    "YUHENG_ADMIN_RESOURCE_AUDIENCE_REQUIRED"
                            ))
            );
        }
        throw new IllegalStateException(
                "YUHENG_ADMIN_IDENTITY_PRINCIPAL_REQUIRED"
        );
    }
}
