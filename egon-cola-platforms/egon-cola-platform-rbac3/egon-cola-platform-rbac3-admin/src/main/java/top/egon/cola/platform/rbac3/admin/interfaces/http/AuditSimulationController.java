package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.audit.application.AuditQueryService;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.simulation.application.AuthorizationSimulationService;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;

import java.time.Instant;

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
public class AuditSimulationController {

    private final AuditQueryService auditService;
    private final AuthorizationSimulationService simulationService;

    public AuditSimulationController(
            AuditQueryService auditService,
            AuthorizationSimulationService simulationService) {
        this.auditService = auditService;
        this.simulationService = simulationService;
    }

    @GetMapping("/audit-logs")
    @RequiresRbac3Permission(permission = "system:audit:read")
    @GatewayOperation(name = "rbac3-audit-log-list-v1",
            summary = "按租户和精确过滤条件游标查询审计",
            externalAccessible = true, tags = {"rbac3", "audit"})
    public ApiEnvelope<AuditQueryService.Page> auditLogs(
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
        return ApiEnvelope.success(auditService.query(
                new AuditQueryService.Query(
                        tenantId(), from, to, actorId, targetId, eventType,
                        outcome, reasonCode, requestId, traceId, targetType,
                        limit, cursor),
                principal.userId(), auditRequestId, auditTraceId));
    }

    @PostMapping("/simulations/authorization")
    @RequiresRbac3Permission(permission = "system:authorization-simulation:execute")
    @GatewayOperation(name = "rbac3-authorization-simulation-v1",
            summary = "基于一致快照执行无业务副作用的授权模拟",
            externalAccessible = true, tags = {"rbac3", "simulation"})
    public ApiEnvelope<AuthorizationSimulationService.SimulationResult> simulate(
            @Valid @RequestBody SimulationRequest request,
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Trace-Id") String traceId,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(simulationService.simulate(
                principal,
                new AuthorizationSimulationService.SimulationRequest(
                        request.decisionRequest(), request.hypothesis(), request.at(),
                        requestId, traceId)));
    }

    @PostMapping("/simulations/role-change-impact")
    @RequiresRbac3Permission(permission = "system:authorization-simulation:execute")
    @GatewayOperation(name = "rbac3-role-change-impact-simulation-v1",
            summary = "查询带策略版本和证据校验和的角色变更影响",
            externalAccessible = true, tags = {"rbac3", "simulation"})
    public ApiEnvelope<AuthorizationSimulationService.RoleChangeImpactResult>
            simulateRoleChangeImpact(
                    @Valid @RequestBody RoleChangeImpactRequest request,
                    @RequestHeader("X-Request-Id") String requestId,
                    @RequestHeader("X-Trace-Id") String traceId,
                    @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(simulationService.simulateRoleChangeImpact(
                principal,
                new AuthorizationSimulationService.RoleChangeImpactRequest(
                        request.roleId(), request.at(), requestId, traceId)));
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    public record SimulationRequest(
            @NotNull AuthorizationDecisionService.DecisionRequest decisionRequest,
            @NotNull AuthorizationSimulationService.Hypothesis hypothesis,
            @NotNull Instant at) {
    }

    public record RoleChangeImpactRequest(
            @NotNull String roleId,
            @NotNull Instant at) {
    }
}
