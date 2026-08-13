package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.assignment.application.AssignmentFacade;
import top.egon.cola.platform.rbac3.admin.runtime.application.IdempotencyService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 类型 `AssignmentController` 位于当前包内，是类型，用于承载 `Assignment Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AssignmentController` is a type in its package and carries the responsibility, state, or contract for `Assignment Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `AssignmentController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AssignmentController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/api/rbac3/v1/users/{userId}/role-assignments")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "role-assignment",
        name = "角色任职接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class AssignmentController {

    /**
     * 字段 `IDEMPOTENCY_TTL` 表示 `AssignmentController` 中与 `IDEMPOTENCY TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `IDEMPOTENCY_TTL` stores the `IDEMPOTENCY TTL`-related state, dependency, configuration, or result of `AssignmentController` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `IDEMPOTENCY_TTL` 时应保持 `AssignmentController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `IDEMPOTENCY_TTL`, preserve `AssignmentController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    /**
     * 字段 `facade` 表示 `AssignmentController` 中与 `facade` 相关的状态、依赖、配置或结果（声明类型 `AssignmentFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `facade` stores the `facade`-related state, dependency, configuration, or result of `AssignmentController` (declared type `AssignmentFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `facade` 时应保持 `AssignmentController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `facade`, preserve `AssignmentController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AssignmentFacade facade;
    /**
     * 字段 `idempotencyService` 表示 `AssignmentController` 中与 `idempotency Service` 相关的状态、依赖、配置或结果（声明类型 `IdempotencyService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idempotencyService` stores the `idempotency Service`-related state, dependency, configuration, or result of `AssignmentController` (declared type `IdempotencyService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idempotencyService` 时应保持 `AssignmentController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idempotencyService`, preserve `AssignmentController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final IdempotencyService idempotencyService;
    /**
     * 字段 `sessionStrengthPort` 表示 `AssignmentController` 中与 `session Strength Port` 相关的状态、依赖、配置或结果（声明类型 `SessionStrengthPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionStrengthPort` stores the `session Strength Port`-related state, dependency, configuration, or result of `AssignmentController` (declared type `SessionStrengthPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionStrengthPort` 时应保持 `AssignmentController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionStrengthPort`, preserve `AssignmentController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionStrengthPort sessionStrengthPort;
    /**
     * 字段 `databaseClock` 表示 `AssignmentController` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `AssignmentController` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `AssignmentController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `AssignmentController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `AssignmentController` 用于创建并初始化 `AssignmentController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AssignmentController` creates and initializes `AssignmentController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AssignmentController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AssignmentController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param facade 输入参数 `facade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyService 输入参数 `idempotencyService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionStrengthPort 输入参数 `sessionStrengthPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AssignmentController(
            AssignmentFacade facade,
            IdempotencyService idempotencyService,
            SessionStrengthPort sessionStrengthPort,
            DatabaseClock databaseClock
    ) {
        this.facade = facade;
        this.idempotencyService = idempotencyService;
        this.sessionStrengthPort = sessionStrengthPort;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `assignments` 按照 `AssignmentController` 的职责处理输入，完成 `assignments` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assignments` processes its inputs according to `AssignmentController`'s responsibility, performs the `assignments` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assignments` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assignments`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping
    @GatewayOperation(
            name = "rbac3-assignment-list-v1",
            summary = "查询用户角色任职及历史状态",
            externalAccessible = true,
            tags = {"rbac3", "assignment"})
    public ApiEnvelope<List<AssignmentFacade.AssignmentView>> assignments(
            @PathVariable String userId,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        if (!principal.userId().equals(userId)
                && !principal.hasPermission("system:role-assignment:read")) {
            throw new Rbac3RuleViolation("PERMISSION_DENIED");
        }
        return ApiEnvelope.success(facade.assignments(
                tenantId(), userId, databaseClock.transactionNow()));
    }

    /**
     * 方法 `assign` 按照 `AssignmentController` 的职责处理输入，完成 `assign` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assign` processes its inputs according to `AssignmentController`'s responsibility, performs the `assign` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assign` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assign`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping
    @RequiresRbac3Permission(permission = "system:role-assignment:manage")
    @GatewayOperation(
            name = "rbac3-assignment-create-v1",
            summary = "按完整委托策略创建角色任职",
            externalAccessible = true,
            tags = {"rbac3", "assignment"})
    public ApiEnvelope<AssignmentFacade.AssignmentResult> assign(
            @PathVariable String userId,
            @Valid @RequestBody AssignRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        Instant now = databaseClock.transactionNow();
        String operation = "POST:/users/{userId}/role-assignments";
        IdempotencyService.Claim claim = claim(
                principal, operation, idempotencyKey,
                userId + '|' + request, now);
        if (claim.outcome() == IdempotencyService.Outcome.REPLAY) {
            return ApiEnvelope.success(new AssignmentFacade.AssignmentResult(
                    claim.resourceId(), null, true, "IDEMPOTENT_REPLAY", null));
        }
        AssignmentFacade.AssignmentResult result = facade.assign(
                new AssignmentFacade.AssignRequest(
                        tenantId(), principal.userId(), userId, request.roleId(),
                        request.assignmentType(), request.validFrom(), request.validTo(),
                        request.reason(), request.ticketNo(),
                        sessionStrengthPort.authenticationStrength(
                                tenantId(), principal.sessionId(), now),
                        principal.platformAdministrator(),
                        request.expectedUserAuthVersion(), claim.recordId(), now));
        return complete(claim, result, now);
    }

    /**
     * 方法 `revoke` 按照 `AssignmentController` 的职责处理输入，完成 `revoke` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revoke` processes its inputs according to `AssignmentController`'s responsibility, performs the `revoke` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revoke` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revoke`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param assignmentId 输入参数 `assignmentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/{assignmentId}/revoke")
    @RequiresRbac3Permission(permission = "system:role-assignment:manage")
    @GatewayOperation(
            name = "rbac3-assignment-revoke-v1",
            summary = "撤销角色任职并保留历史",
            externalAccessible = true,
            tags = {"rbac3", "assignment"})
    public ApiEnvelope<AssignmentFacade.AssignmentResult> revoke(
            @PathVariable String userId,
            @PathVariable String assignmentId,
            @Valid @RequestBody ChangeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return change(userId, assignmentId, AssignmentFacade.ChangeOperation.REVOKE,
                request, idempotencyKey, principal);
    }

    /**
     * 方法 `suspend` 按照 `AssignmentController` 的职责处理输入，完成 `suspend` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `suspend` processes its inputs according to `AssignmentController`'s responsibility, performs the `suspend` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `suspend` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `suspend`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param assignmentId 输入参数 `assignmentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/{assignmentId}/suspend")
    @RequiresRbac3Permission(permission = "system:role-assignment:manage")
    @GatewayOperation(
            name = "rbac3-assignment-suspend-v1",
            summary = "暂停角色任职",
            externalAccessible = true,
            tags = {"rbac3", "assignment"})
    public ApiEnvelope<AssignmentFacade.AssignmentResult> suspend(
            @PathVariable String userId,
            @PathVariable String assignmentId,
            @Valid @RequestBody ChangeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return change(userId, assignmentId, AssignmentFacade.ChangeOperation.SUSPEND,
                request, idempotencyKey, principal);
    }

    /**
     * 方法 `resume` 按照 `AssignmentController` 的职责处理输入，完成 `resume` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resume` processes its inputs according to `AssignmentController`'s responsibility, performs the `resume` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resume` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resume`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param assignmentId 输入参数 `assignmentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/{assignmentId}/resume")
    @RequiresRbac3Permission(permission = "system:role-assignment:manage")
    @GatewayOperation(
            name = "rbac3-assignment-resume-v1",
            summary = "恢复角色任职",
            externalAccessible = true,
            tags = {"rbac3", "assignment"})
    public ApiEnvelope<AssignmentFacade.AssignmentResult> resume(
            @PathVariable String userId,
            @PathVariable String assignmentId,
            @Valid @RequestBody ChangeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return change(userId, assignmentId, AssignmentFacade.ChangeOperation.RESUME,
                request, idempotencyKey, principal);
    }

    /**
     * 方法 `change` 按照 `AssignmentController` 的职责处理输入，完成 `change` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `change` processes its inputs according to `AssignmentController`'s responsibility, performs the `change` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `change` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `change`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param assignmentId 输入参数 `assignmentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param operation 输入参数 `operation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ApiEnvelope<AssignmentFacade.AssignmentResult> change(
            String userId,
            String assignmentId,
            AssignmentFacade.ChangeOperation operation,
            ChangeRequest request,
            String idempotencyKey,
            CurrentRbac3Principal principal
    ) {
        Instant now = databaseClock.transactionNow();
        String operationCode = "POST:/users/{userId}/role-assignments/{assignmentId}/"
                + operation.name().toLowerCase(java.util.Locale.ROOT);
        IdempotencyService.Claim claim = claim(
                principal, operationCode, idempotencyKey,
                userId + '|' + assignmentId + '|' + operation + '|' + request, now);
        if (claim.outcome() == IdempotencyService.Outcome.REPLAY) {
            return ApiEnvelope.success(new AssignmentFacade.AssignmentResult(
                    claim.resourceId(), null, true, "IDEMPOTENT_REPLAY", null));
        }
        AssignmentFacade.AssignmentResult result = facade.change(
                new AssignmentFacade.ChangeRequest(
                        tenantId(), principal.userId(), userId, assignmentId, operation,
                        request.reason(), request.ticketNo(),
                        sessionStrengthPort.authenticationStrength(
                                tenantId(), principal.sessionId(), now),
                        principal.platformAdministrator(),
                        request.expectedAssignmentVersion(),
                        request.expectedUserAuthVersion(), claim.recordId(), now));
        return complete(claim, result, now);
    }

    /**
     * 方法 `claim` 按照 `AssignmentController` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `claim` processes its inputs according to `AssignmentController`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `claim` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `claim`, then continue the business flow using its result, exception, or side effect.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param operation 输入参数 `operation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param canonicalRequest 输入参数 `canonicalRequest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private IdempotencyService.Claim claim(
            CurrentRbac3Principal principal,
            String operation,
            String idempotencyKey,
            String canonicalRequest,
            Instant now
    ) {
        requireIdempotencyKey(idempotencyKey);
        return idempotencyService.claim(new IdempotencyService.Command(
                tenantId(), "USER", principal.userId(), operation,
                idempotencyKey, canonicalRequest, now.plus(IDEMPOTENCY_TTL), now));
    }

    /**
     * 方法 `complete` 按照 `AssignmentController` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `complete` processes its inputs according to `AssignmentController`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `complete` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `complete`, then continue the business flow using its result, exception, or side effect.
     *
     * @param claim 输入参数 `claim`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param result 输入参数 `result`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ApiEnvelope<AssignmentFacade.AssignmentResult> complete(
            IdempotencyService.Claim claim,
            AssignmentFacade.AssignmentResult result,
            Instant now
    ) {
        int status = result.completed() ? 200 : 503;
        idempotencyService.complete(
                claim.recordId(), "ROLE_ASSIGNMENT", result.assignmentId(), status,
                result.assignmentId() + '|' + result.reasonCode(), now);
        if (!result.completed()) {
            throw new Rbac3RuleViolation(
                    "AUTH_PROPAGATION_PENDING", List.of(result.mutationId()));
        }
        return ApiEnvelope.success(result);
    }

    /**
     * 方法 `requireIdempotencyKey` 按照 `AssignmentController` 的职责处理输入，完成 `require Idempotency Key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireIdempotencyKey` processes its inputs according to `AssignmentController`'s responsibility, performs the `require Idempotency Key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireIdempotencyKey` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireIdempotencyKey`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void requireIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 128
                || !value.chars().allMatch(character -> character >= 0x21
                && character <= 0x7e)) {
            throw new IllegalArgumentException("Idempotency-Key must be 1-128 ASCII characters");
        }
    }

    /**
     * 方法 `tenantId` 按照 `AssignmentController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `AssignmentController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 类型 `SessionStrengthPort` 位于 `AssignmentController` 内，是接口，用于承载 `Session Strength Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionStrengthPort` is an interface inside `AssignmentController` and carries the responsibility, state, or contract for `Session Strength Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionStrengthPort` 作为 `AssignmentController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionStrengthPort` as the responsibility boundary of `AssignmentController`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface SessionStrengthPort {
        /**
         * 方法 `authenticationStrength` 按照 `SessionStrengthPort` 的职责处理输入，完成 `authentication Strength` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `authenticationStrength` processes its inputs according to `SessionStrengthPort`'s responsibility, performs the `authentication Strength` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `authenticationStrength` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `authenticationStrength`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        String authenticationStrength(String tenantId, String sessionId, Instant now);
    }

    /**
     * 类型 `AssignRequest` 位于 `AssignmentController` 内，是记录类型，用于承载 `Assign Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignRequest` is a record inside `AssignmentController` and carries the responsibility, state, or contract for `Assign Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignRequest` 作为 `AssignmentController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignRequest` as the responsibility boundary of `AssignmentController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param assignmentType 记录组件 `assignmentType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignmentType` carries constructor data whose meaning is defined by the record contract.
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     * @param ticketNo 记录组件 `ticketNo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ticketNo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedUserAuthVersion 记录组件 `expectedUserAuthVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedUserAuthVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record AssignRequest(
            /**
             * 字段 `roleId` 表示 `AssignRequest` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String roleId,
            /**
             * 字段 `validFrom` 表示 `AssignRequest` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `AssignRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `AssignRequest` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `AssignRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `assignmentType` 表示 `AssignRequest` 中与 `assignment Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignmentType` stores the `assignment Type`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignmentType` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignmentType`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String assignmentType,
            /**
             * 字段 `reason` 表示 `AssignRequest` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reason,
            /**
             * 字段 `ticketNo` 表示 `AssignRequest` 中与 `ticket No` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ticketNo` stores the `ticket No`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ticketNo` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ticketNo`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ticketNo,
            /**
             * 字段 `expectedUserAuthVersion` 表示 `AssignRequest` 中与 `expected User Auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedUserAuthVersion` stores the `expected User Auth Version`-related state, dependency, configuration, or result of `AssignRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedUserAuthVersion` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedUserAuthVersion`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedUserAuthVersion
    ) {
    }

    /**
     * 类型 `ChangeRequest` 位于 `AssignmentController` 内，是记录类型，用于承载 `Change Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ChangeRequest` is a record inside `AssignmentController` and carries the responsibility, state, or contract for `Change Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ChangeRequest` 作为 `AssignmentController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ChangeRequest` as the responsibility boundary of `AssignmentController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     * @param ticketNo 记录组件 `ticketNo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ticketNo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedAssignmentVersion 记录组件 `expectedAssignmentVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedAssignmentVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expectedUserAuthVersion 记录组件 `expectedUserAuthVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedUserAuthVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record ChangeRequest(
            /**
             * 字段 `reason` 表示 `ChangeRequest` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reason,
            /**
             * 字段 `ticketNo` 表示 `ChangeRequest` 中与 `ticket No` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ticketNo` stores the `ticket No`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ticketNo` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ticketNo`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ticketNo,
            /**
             * 字段 `expectedAssignmentVersion` 表示 `ChangeRequest` 中与 `expected Assignment Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedAssignmentVersion` stores the `expected Assignment Version`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedAssignmentVersion` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedAssignmentVersion`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedAssignmentVersion,
            /**
             * 字段 `expectedUserAuthVersion` 表示 `ChangeRequest` 中与 `expected User Auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedUserAuthVersion` stores the `expected User Auth Version`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedUserAuthVersion` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedUserAuthVersion`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedUserAuthVersion
    ) {
    }
}
