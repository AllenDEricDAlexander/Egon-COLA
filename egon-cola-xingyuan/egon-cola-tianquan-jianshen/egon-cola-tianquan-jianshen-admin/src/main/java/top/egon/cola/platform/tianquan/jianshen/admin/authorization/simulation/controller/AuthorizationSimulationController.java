package top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.domain.dto.AuthorizationRoleChangeImpactRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.domain.dto.AuthorizationSimulationRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.domain.dto.RoleChangeImpactRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.domain.dto.SimulationRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.domain.vo.RoleChangeImpactResultVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.domain.vo.SimulationResultVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.service.AuthorizationSimulationService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;

/**
 * 授权模拟 HTTP 入口。
 * HTTP entry point for authorization simulation.
 */
@RestController
@RequestMapping("/api/rbac3/v1")
@Tag(name = "authorization-simulation", description = "授权模拟接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam"
)
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
    @RequiresPermission(value = "system:authorization-simulation:execute")
    @Operation(
            operationId = "rbac3-authorization-simulation-v1",
            summary = "基于一致快照执行无业务副作用的授权模拟",
            tags = {"rbac3", "simulation"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<SimulationResultVO> simulate(
            @Valid @RequestBody AuthorizationSimulationRequestDTO request,
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Trace-Id") String traceId
) {
        return ResultRecord.success(simulationService.simulate(
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
    @RequiresPermission(value = "system:authorization-simulation:execute")
    @Operation(
            operationId = "rbac3-role-change-impact-simulation-v1",
            summary = "查询带策略版本和证据校验和的角色变更影响",
            tags = {"rbac3", "simulation"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<RoleChangeImpactResultVO> simulateRoleChangeImpact(
            @Valid @RequestBody AuthorizationRoleChangeImpactRequestDTO request,
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Trace-Id") String traceId
) {
        return ResultRecord.success(simulationService.simulateRoleChangeImpact(
                new RoleChangeImpactRequestDTO(
                        request.roleId(), request.at(), requestId, traceId)));
    }
}
