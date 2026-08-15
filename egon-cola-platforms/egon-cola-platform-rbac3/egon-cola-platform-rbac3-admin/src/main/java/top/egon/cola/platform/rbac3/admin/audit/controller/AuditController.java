package top.egon.cola.platform.rbac3.admin.audit.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.audit.domain.dto.QueryDTO;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditQueryPageVO;
import top.egon.cola.platform.rbac3.admin.audit.service.AuditQueryService;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.TenantContext;

import java.time.Instant;

/**
 * 审计日志查询 HTTP 入口。
 * HTTP entry point for querying audit logs.
 */
@RestController
@RequestMapping("/api/rbac3/v1")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "audit-simulation",
        name = "审计与授权模拟接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class AuditController {

    /** 审计查询服务。 / Audit query service. */
    private final AuditQueryService auditService;

    /**
     * 创建审计查询入口。
     * Creates the audit query entry point.
     *
     * @param auditService 审计查询服务 / audit query service
     */
    public AuditController(AuditQueryService auditService) {
        this.auditService = auditService;
    }

    /**
     * 按租户和精确过滤条件游标查询审计日志。
     * Queries tenant audit logs with exact filters and cursor pagination.
     *
     * @return 审计查询分页结果 / paged audit-query result
     */
    @GetMapping("/audit-logs")
    @RequiresRbac3Permission(permission = "system:audit:read")
    @GatewayOperation(name = "rbac3-audit-log-list-v1",
            summary = "按租户和精确过滤条件游标查询审计",
            externalAccessible = true, tags = {"rbac3", "audit"})
    public ApiEnvelopeVO<AuditQueryPageVO> auditLogs(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String reasonCode,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @RequestParam(required = false) String cursor,
            @RequestHeader("X-Request-Id") String auditRequestId,
            @RequestHeader("X-Trace-Id") String auditTraceId,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(auditService.query(
                new QueryDTO(
                        tenantId(), from, to, actorId, targetId, eventType,
                        outcome, reasonCode, requestId, traceId, targetType,
                        limit, cursor),
                principal.userId(), auditRequestId, auditTraceId));
    }

    /** 返回当前生效租户标识。 / Returns the effective tenant ID. */
    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }
}
