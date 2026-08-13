package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.contract.activation.ActiveRoleSetView;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesRequest;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesResult;
import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidateView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 类型 `RoleActivationController` 位于当前包内，是类型，用于承载 `Role Activation Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleActivationController` is a type in its package and carries the responsibility, state, or contract for `Role Activation Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RoleActivationController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RoleActivationController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/api/rbac3/v1/auth")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "role-activation",
        name = "当前会话角色激活接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class RoleActivationController {

    /**
     * 字段 `candidateService` 表示 `RoleActivationController` 中与 `candidate Service` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationCandidateService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `candidateService` stores the `candidate Service`-related state, dependency, configuration, or result of `RoleActivationController` (declared type `RoleActivationCandidateService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `candidateService` 时应保持 `RoleActivationController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `candidateService`, preserve `RoleActivationController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationCandidateService candidateService;
    /**
     * 字段 `facade` 表示 `RoleActivationController` 中与 `facade` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `facade` stores the `facade`-related state, dependency, configuration, or result of `RoleActivationController` (declared type `RoleActivationFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `facade` 时应保持 `RoleActivationController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `facade`, preserve `RoleActivationController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationFacade facade;
    /**
     * 字段 `databaseClock` 表示 `RoleActivationController` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `RoleActivationController` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `RoleActivationController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `RoleActivationController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `RoleActivationController` 用于创建并初始化 `RoleActivationController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleActivationController` creates and initializes `RoleActivationController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleActivationController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleActivationController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param candidateService 输入参数 `candidateService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param facade 输入参数 `facade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RoleActivationController(
            RoleActivationCandidateService candidateService,
            RoleActivationFacade facade,
            DatabaseClock databaseClock
    ) {
        this.candidateService = candidateService;
        this.facade = facade;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `candidates` 按照 `RoleActivationController` 的职责处理输入，完成 `candidates` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `candidates` processes its inputs according to `RoleActivationController`'s responsibility, performs the `candidates` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `candidates` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `candidates`, then continue the business flow using its result, exception, or side effect.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/role-activation-candidates")
    @RequiresRbac3Permission(permission = "system:role-activation:read")
    @GatewayOperation(
            name = "rbac3-role-activation-candidates-v1",
            summary = "查询当前会话可激活的规范根角色",
            externalAccessible = true,
            tags = {"rbac3", "role-activation"})
    public ApiEnvelope<RoleActivationCandidateView> candidates(
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return ApiEnvelope.success(candidateService.candidates(
                tenantId(), principal.userId(), databaseClock.transactionNow()));
    }

    /**
     * 方法 `current` 按照 `RoleActivationController` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `current` processes its inputs according to `RoleActivationController`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/role-activations")
    @RequiresRbac3Permission(permission = "system:role-activation:read")
    @GatewayOperation(
            name = "rbac3-role-activation-current-v1",
            summary = "查询当前会话已激活的规范根角色",
            externalAccessible = true,
            tags = {"rbac3", "role-activation"})
    public ApiEnvelope<ActiveRoleSetView> current(
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return ApiEnvelope.success(facade.current(
                tenantId(), principal.identitySub(), principal.userId(),
                principal.sessionId()));
    }

    /**
     * 方法 `replace` 按照 `RoleActivationController` 的职责处理输入，完成 `replace` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `replace` processes its inputs according to `RoleActivationController`'s responsibility, performs the `replace` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `replace` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `replace`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PutMapping("/role-activations")
    @RequiresRbac3Permission(permission = "system:role-activation:use")
    @GatewayOperation(
            name = "rbac3-role-activation-replace-v1",
            summary = "原子替换当前会话激活角色集合",
            externalAccessible = true,
            tags = {"rbac3", "role-activation"})
    public ApiEnvelope<ReplaceActiveRolesResult> replace(
            @Valid @RequestBody ReplaceActiveRolesRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        String commandId = activationCommandId(principal.sessionId(), request);
        return ApiEnvelope.success(facade.replace(new RoleActivationFacade.ReplaceCommand(
                tenantId(), principal.identitySub(), principal.userId(),
                principal.sessionId(), request.roleIds(),
                request.expectedContextVersion(), principal.userId(), commandId)));
    }

    /**
     * 方法 `tenantId` 按照 `RoleActivationController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `RoleActivationController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `activationCommandId` 按照 `RoleActivationController` 的职责处理输入，完成 `activation Command Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activationCommandId` processes its inputs according to `RoleActivationController`'s responsibility, performs the `activation Command Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activationCommandId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activationCommandId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String activationCommandId(
            String sessionId,
            ReplaceActiveRolesRequest request) {
        String canonicalRoles = request.roleIds().stream()
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        String canonical = sessionId + '|' + request.expectedContextVersion()
                + '|' + canonicalRoles;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return "role-activation:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
