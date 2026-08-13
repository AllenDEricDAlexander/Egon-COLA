package top.egon.cola.platform.rbac3.admin.simulation.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.dto.AuthorizationRoleChangeImpactRequestDTO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.dto.AuthorizationSimulationRequestDTO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.dto.RoleChangeImpactRequestDTO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.dto.SimulationRequestDTO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.vo.RoleChangeImpactResultVO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.vo.SimulationResultVO;
import top.egon.cola.platform.rbac3.admin.simulation.service.AuthorizationSimulationService;

/**
 * 授权模拟 HTTP 入口。
 * HTTP entry point for authorization simulation.
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
public class AuthorizationSimulationController {

    /** 授权模拟服务。 / Authorization simulation service. */
    private final AuthorizationSimulationService simulationService;

    /**
     * 创建授权模拟入口。
     * Creates the authorization simulation entry point.
     *
     * @param simulationService 授权模拟服务 / authorization simulation service
     */
    public AuthorizationSimulationController(
            AuthorizationSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * 基于一致快照执行无业务副作用的授权模拟。
     * Simulates authorization on a consistent snapshot without business side effects.
     *
     * @return 授权模拟结果 / authorization simulation result
     */
    @PostMapping("/simulations/authorization")
    @RequiresRbac3Permission(permission = "system:authorization-simulation:execute")
    @GatewayOperation(name = "rbac3-authorization-simulation-v1",
            summary = "基于一致快照执行无业务副作用的授权模拟",
            externalAccessible = true, tags = {"rbac3", "simulation"})
    public ApiEnvelopeVO<SimulationResultVO> simulate(
            @Valid @RequestBody AuthorizationSimulationRequestDTO request,
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Trace-Id") String traceId,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(simulationService.simulate(
                principal,
                new SimulationRequestDTO(
                        request.decisionRequest(), request.hypothesis(), request.at(),
                        requestId, traceId)));
    }

    /**
     * 查询带策略版本和证据校验和的角色变更影响。
     * Queries role-change impact with policy version and evidence checksum.
     *
     * @return 角色变更影响结果 / role-change impact result
     */
    @PostMapping("/simulations/role-change-impact")
    @RequiresRbac3Permission(permission = "system:authorization-simulation:execute")
    @GatewayOperation(name = "rbac3-role-change-impact-simulation-v1",
            summary = "查询带策略版本和证据校验和的角色变更影响",
            externalAccessible = true, tags = {"rbac3", "simulation"})
    public ApiEnvelopeVO<RoleChangeImpactResultVO> simulateRoleChangeImpact(
            @Valid @RequestBody AuthorizationRoleChangeImpactRequestDTO request,
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Trace-Id") String traceId,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(simulationService.simulateRoleChangeImpact(
                principal,
                new RoleChangeImpactRequestDTO(
                        request.roleId(), request.at(), requestId, traceId)));
    }
}
