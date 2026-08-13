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
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.simulation.application.AuthorizationSimulationService;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;

import java.time.Instant;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

/**
 * 类型 `AuditSimulationController` 位于当前包内，是类型，用于承载 `Audit Simulation Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuditSimulationController` is a type in its package and carries the responsibility, state, or contract for `Audit Simulation Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `AuditSimulationController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AuditSimulationController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
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
public class AuditSimulationController {

    /**
     * 字段 `auditService` 表示 `AuditSimulationController` 中与 `audit Service` 相关的状态、依赖、配置或结果（声明类型 `AuditQueryService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `auditService` stores the `audit Service`-related state, dependency, configuration, or result of `AuditSimulationController` (declared type `AuditQueryService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `auditService` 时应保持 `AuditSimulationController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `auditService`, preserve `AuditSimulationController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuditQueryService auditService;
    /**
     * 字段 `simulationService` 表示 `AuditSimulationController` 中与 `simulation Service` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationSimulationService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `simulationService` stores the `simulation Service`-related state, dependency, configuration, or result of `AuditSimulationController` (declared type `AuthorizationSimulationService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `simulationService` 时应保持 `AuditSimulationController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `simulationService`, preserve `AuditSimulationController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationSimulationService simulationService;

    /**
     * 构造器 `AuditSimulationController` 用于创建并初始化 `AuditSimulationController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuditSimulationController` creates and initializes `AuditSimulationController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuditSimulationController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuditSimulationController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param auditService 输入参数 `auditService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param simulationService 输入参数 `simulationService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuditSimulationController(
            AuditQueryService auditService,
            AuthorizationSimulationService simulationService) {
        this.auditService = auditService;
        this.simulationService = simulationService;
    }

    /**
     * 方法 `auditLogs` 按照 `AuditSimulationController` 的职责处理输入，完成 `audit Logs` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `auditLogs` processes its inputs according to `AuditSimulationController`'s responsibility, performs the `audit Logs` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `auditLogs` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `auditLogs`, then continue the business flow using its result, exception, or side effect.
     *
     * @param from 输入参数 `from`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param to 输入参数 `to`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param targetId 输入参数 `targetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param outcome 输入参数 `outcome`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param targetType 输入参数 `targetType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param limit 输入参数 `limit`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param cursor 输入参数 `cursor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param auditRequestId 输入参数 `auditRequestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param auditTraceId 输入参数 `auditTraceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/audit-logs")
    @RequiresRbac3Permission(permission = "system:audit:read")
    @GatewayOperation(name = "rbac3-audit-log-list-v1",
            summary = "按租户和精确过滤条件游标查询审计",
            externalAccessible = true, tags = {"rbac3", "audit"})
    public ApiEnvelopeVO<AuditQueryService.Page> auditLogs(
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
                new AuditQueryService.Query(
                        tenantId(), from, to, actorId, targetId, eventType,
                        outcome, reasonCode, requestId, traceId, targetType,
                        limit, cursor),
                principal.userId(), auditRequestId, auditTraceId));
    }

    /**
     * 方法 `simulate` 按照 `AuditSimulationController` 的职责处理输入，完成 `simulate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `simulate` processes its inputs according to `AuditSimulationController`'s responsibility, performs the `simulate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `simulate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `simulate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/simulations/authorization")
    @RequiresRbac3Permission(permission = "system:authorization-simulation:execute")
    @GatewayOperation(name = "rbac3-authorization-simulation-v1",
            summary = "基于一致快照执行无业务副作用的授权模拟",
            externalAccessible = true, tags = {"rbac3", "simulation"})
    public ApiEnvelopeVO<AuthorizationSimulationService.SimulationResult> simulate(
            @Valid @RequestBody SimulationRequest request,
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Trace-Id") String traceId,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(simulationService.simulate(
                principal,
                new AuthorizationSimulationService.SimulationRequest(
                        request.decisionRequest(), request.hypothesis(), request.at(),
                        requestId, traceId)));
    }

    /**
     * 方法 `simulateRoleChangeImpact` 按照 `AuditSimulationController` 的职责处理输入，完成 `simulate Role Change Impact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `simulateRoleChangeImpact` processes its inputs according to `AuditSimulationController`'s responsibility, performs the `simulate Role Change Impact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `simulateRoleChangeImpact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `simulateRoleChangeImpact`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/simulations/role-change-impact")
    @RequiresRbac3Permission(permission = "system:authorization-simulation:execute")
    @GatewayOperation(name = "rbac3-role-change-impact-simulation-v1",
            summary = "查询带策略版本和证据校验和的角色变更影响",
            externalAccessible = true, tags = {"rbac3", "simulation"})
    public ApiEnvelopeVO<AuthorizationSimulationService.RoleChangeImpactResult>
            simulateRoleChangeImpact(
                    @Valid @RequestBody RoleChangeImpactRequest request,
                    @RequestHeader("X-Request-Id") String requestId,
                    @RequestHeader("X-Trace-Id") String traceId,
                    @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(simulationService.simulateRoleChangeImpact(
                principal,
                new AuthorizationSimulationService.RoleChangeImpactRequest(
                        request.roleId(), request.at(), requestId, traceId)));
    }

    /**
     * 方法 `tenantId` 按照 `AuditSimulationController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `AuditSimulationController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    /**
     * 类型 `SimulationRequest` 位于 `AuditSimulationController` 内，是记录类型，用于承载 `Simulation Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SimulationRequest` is a record inside `AuditSimulationController` and carries the responsibility, state, or contract for `Simulation Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SimulationRequest` 作为 `AuditSimulationController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SimulationRequest` as the responsibility boundary of `AuditSimulationController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param decisionRequest 记录组件 `decisionRequest` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `decisionRequest` carries constructor data whose meaning is defined by the record contract.
     * @param hypothesis 记录组件 `hypothesis` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `hypothesis` carries constructor data whose meaning is defined by the record contract.
     * @param at 记录组件 `at` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `at` carries constructor data whose meaning is defined by the record contract.
     */
    public record SimulationRequest(
            /**
             * 字段 `decisionRequest` 表示 `SimulationRequest` 中与 `decision Request` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationDecisionService.DecisionRequest`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `decisionRequest` stores the `decision Request`-related state, dependency, configuration, or result of `SimulationRequest` (declared type `AuthorizationDecisionService.DecisionRequest`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `decisionRequest` 时应保持 `SimulationRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `decisionRequest`, preserve `SimulationRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull AuthorizationDecisionService.DecisionRequest decisionRequest,
            /**
             * 字段 `hypothesis` 表示 `SimulationRequest` 中与 `hypothesis` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationSimulationService.Hypothesis`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `hypothesis` stores the `hypothesis`-related state, dependency, configuration, or result of `SimulationRequest` (declared type `AuthorizationSimulationService.Hypothesis`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `hypothesis` 时应保持 `SimulationRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `hypothesis`, preserve `SimulationRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull AuthorizationSimulationService.Hypothesis hypothesis,
            /**
             * 字段 `at` 表示 `SimulationRequest` 中与 `at` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `at` stores the `at`-related state, dependency, configuration, or result of `SimulationRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `at` 时应保持 `SimulationRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `at`, preserve `SimulationRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant at) {
    }

    /**
     * 类型 `RoleChangeImpactRequest` 位于 `AuditSimulationController` 内，是记录类型，用于承载 `Role Change Impact Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleChangeImpactRequest` is a record inside `AuditSimulationController` and carries the responsibility, state, or contract for `Role Change Impact Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleChangeImpactRequest` 作为 `AuditSimulationController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleChangeImpactRequest` as the responsibility boundary of `AuditSimulationController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param at 记录组件 `at` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `at` carries constructor data whose meaning is defined by the record contract.
     */
    public record RoleChangeImpactRequest(
            /**
             * 字段 `roleId` 表示 `RoleChangeImpactRequest` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `RoleChangeImpactRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `RoleChangeImpactRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `RoleChangeImpactRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull String roleId,
            /**
             * 字段 `at` 表示 `RoleChangeImpactRequest` 中与 `at` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `at` stores the `at`-related state, dependency, configuration, or result of `RoleChangeImpactRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `at` 时应保持 `RoleChangeImpactRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `at`, preserve `RoleChangeImpactRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant at) {
    }
}
