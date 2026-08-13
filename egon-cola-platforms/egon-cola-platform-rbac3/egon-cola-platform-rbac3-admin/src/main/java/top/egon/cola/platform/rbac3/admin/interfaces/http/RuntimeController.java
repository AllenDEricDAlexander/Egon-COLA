package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort;
import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;

/**
 * 类型 `RuntimeController` 位于当前包内，是类型，用于承载 `Runtime Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RuntimeController` is a type in its package and carries the responsibility, state, or contract for `Runtime Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RuntimeController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RuntimeController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/api/rbac3/v1/runtime")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "authorization-runtime",
        name = "授权运行状态接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class RuntimeController {

    /**
     * 字段 `service` 表示 `RuntimeController` 中与 `service` 相关的状态、依赖、配置或结果（声明类型 `RuntimeQueryService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `service` stores the `service`-related state, dependency, configuration, or result of `RuntimeController` (declared type `RuntimeQueryService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `service` 时应保持 `RuntimeController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `service`, preserve `RuntimeController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RuntimeQueryService service;

    /**
     * 构造器 `RuntimeController` 用于创建并初始化 `RuntimeController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RuntimeController` creates and initializes `RuntimeController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RuntimeController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RuntimeController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param service 输入参数 `service`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RuntimeController(RuntimeQueryService service) {
        this.service = service;
    }

    /**
     * 方法 `status` 按照 `RuntimeController` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `RuntimeController`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/status")
    @RequiresRbac3Permission(permission = "system:authorization-runtime:read")
    @GatewayOperation(name = "rbac3-runtime-status-v1", summary = "查询授权运行状态",
            externalAccessible = true, tags = {"rbac3", "runtime"})
    public ApiEnvelope<ControlPlaneRuntimeStatusPort.RuntimeStatus> status() {
        return ApiEnvelope.success(service.status());
    }

    /**
     * 方法 `mutations` 按照 `RuntimeController` 的职责处理输入，完成 `mutations` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `mutations` processes its inputs according to `RuntimeController`'s responsibility, performs the `mutations` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `mutations` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `mutations`, then continue the business flow using its result, exception, or side effect.
     *
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param cursor 输入参数 `cursor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param limit 输入参数 `limit`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/mutations")
    @RequiresRbac3Permission(permission = "system:authorization-runtime:read")
    @GatewayOperation(name = "rbac3-runtime-mutations-v1",
            summary = "游标查询授权 Mutation Journal",
            externalAccessible = true, tags = {"rbac3", "runtime"})
    public ApiEnvelope<RuntimeQueryService.MutationPage> mutations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return ApiEnvelope.success(service.mutations(
                tenantId(), status, cursor, limit));
    }

    /**
     * 方法 `retry` 按照 `RuntimeController` 的职责处理输入，完成 `retry` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `retry` processes its inputs according to `RuntimeController`'s responsibility, performs the `retry` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `retry` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `retry`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/mutations/{mutationId}/retry")
    @RequiresRbac3Permission(permission = "system:authorization-runtime:operate")
    @GatewayOperation(name = "rbac3-runtime-mutation-retry-v1",
            summary = "按 Mutation ID 触发幂等受控恢复",
            externalAccessible = true, tags = {"rbac3", "runtime"})
    public ApiEnvelope<RuntimeQueryService.RetryResult> retry(
            @PathVariable String mutationId,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(service.retry(
                tenantId(), mutationId, principal.userId()));
    }

    /**
     * 方法 `gatewayDdcStatus` 按照 `RuntimeController` 的职责处理输入，完成 `gateway Ddc Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `gatewayDdcStatus` processes its inputs according to `RuntimeController`'s responsibility, performs the `gateway Ddc Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `gatewayDdcStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `gatewayDdcStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/gateway-ddc-status")
    @RequiresRbac3Permission(permission = "system:authorization-runtime:read")
    @GatewayOperation(name = "rbac3-runtime-gateway-ddc-status-v1",
            summary = "分别查询 Definition、DDC Lease 和 Gateway Release",
            externalAccessible = true, tags = {"rbac3", "runtime", "gateway", "ddc"})
    public ApiEnvelope<ControlPlaneRuntimeStatusPort.RuntimeStatus> gatewayDdcStatus() {
        return ApiEnvelope.success(service.gatewayDdcStatus());
    }

    /**
     * 方法 `tenantId` 按照 `RuntimeController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `RuntimeController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }
}
