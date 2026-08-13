package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 类型 `TenantUserDirectoryController` 位于当前包内，是类型，用于承载 `Tenant User Directory Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `TenantUserDirectoryController` is a type in its package and carries the responsibility, state, or contract for `Tenant User Directory Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `TenantUserDirectoryController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `TenantUserDirectoryController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/api/rbac3/v1")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "directory",
        name = "租户用户与目录接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class TenantUserDirectoryController {

    /**
     * 字段 `commandPort` 表示 `TenantUserDirectoryController` 中与 `command Port` 相关的状态、依赖、配置或结果（声明类型 `DirectoryCommandPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `commandPort` stores the `command Port`-related state, dependency, configuration, or result of `TenantUserDirectoryController` (declared type `DirectoryCommandPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `commandPort` 时应保持 `TenantUserDirectoryController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `commandPort`, preserve `TenantUserDirectoryController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DirectoryCommandPort commandPort;
    /**
     * 字段 `queryPort` 表示 `TenantUserDirectoryController` 中与 `query Port` 相关的状态、依赖、配置或结果（声明类型 `DirectoryQueryPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `queryPort` stores the `query Port`-related state, dependency, configuration, or result of `TenantUserDirectoryController` (declared type `DirectoryQueryPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `queryPort` 时应保持 `TenantUserDirectoryController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `queryPort`, preserve `TenantUserDirectoryController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DirectoryQueryPort queryPort;

    /**
     * 构造器 `TenantUserDirectoryController` 用于创建并初始化 `TenantUserDirectoryController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `TenantUserDirectoryController` creates and initializes `TenantUserDirectoryController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `TenantUserDirectoryController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `TenantUserDirectoryController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param commandPort 输入参数 `commandPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param queryPort 输入参数 `queryPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public TenantUserDirectoryController(
            DirectoryCommandPort commandPort,
            DirectoryQueryPort queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }

    /**
     * 方法 `tenants` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenants` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenants`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param page 输入参数 `page`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param size 输入参数 `size`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/platform/tenants")
    @RequiresRbac3Permission(permission = "system:tenant:read")
    @GatewayOperation(
            name = "rbac3-platform-tenant-list-v1",
            summary = "分页查询平台租户",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelope<PageView<TenantView>> tenants(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ApiEnvelope.success(queryPort.findTenants(query, status, page, size));
    }

    /**
     * 方法 `createTenant` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `create Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createTenant` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `create Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/platform/tenants")
    @RequiresRbac3Permission(permission = "system:tenant:manage")
    @GatewayOperation(
            name = "rbac3-platform-tenant-create-v1",
            summary = "创建平台租户",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelope<TenantView> createTenant(
            @Valid @RequestBody CreateTenantCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(commandPort.createTenant(command, principal.userId()));
    }

    /**
     * 方法 `changeTenantStatus` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `change Tenant Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeTenantStatus` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `change Tenant Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeTenantStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeTenantStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PutMapping("/platform/tenants/{tenantId}/status")
    @RequiresRbac3Permission(permission = "system:tenant:manage")
    @GatewayOperation(
            name = "rbac3-platform-tenant-status-v1",
            summary = "按版本变更平台租户状态",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelope<TenantView> changeTenantStatus(
            @PathVariable String tenantId,
            @Valid @RequestBody TenantStatusCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        requireTargetTenant(tenantId);
        return ApiEnvelope.success(commandPort.changeTenantStatus(
                tenantId, command, principal.userId()));
    }

    /**
     * 方法 `users` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `users` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `users` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `users`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param positionId 输入参数 `positionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param page 输入参数 `page`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param size 输入参数 `size`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/users")
    @RequiresRbac3Permission(permission = "system:user:read")
    @GatewayOperation(
            name = "rbac3-directory-user-list-v1",
            summary = "分页查询租户用户",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<PageView<UserDirectoryView>> users(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orgUnitId,
            @RequestParam(required = false) String positionId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ApiEnvelope.success(queryPort.findUsers(tenantId(), query, status,
                orgUnitId, positionId, page, size));
    }

    /**
     * 方法 `user` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `user` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `user` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `user` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `user` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `user`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/users/{userId}")
    @RequiresRbac3Permission(permission = "system:user:read")
    @GatewayOperation(
            name = "rbac3-directory-user-get-v1",
            summary = "读取租户用户详情",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<UserDirectoryView> user(@PathVariable String userId) {
        return ApiEnvelope.success(queryPort.findUser(tenantId(), userId));
    }

    /**
     * 方法 `changeUserStatus` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `change User Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeUserStatus` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `change User Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeUserStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeUserStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PutMapping("/users/{userId}/status")
    @RequiresRbac3Permission(permission = "system:user-status:manage")
    @GatewayOperation(
            name = "rbac3-directory-user-status-v1",
            summary = "按授权版本变更租户用户状态",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<UserDirectoryView> changeUserStatus(
            @PathVariable String userId,
            @Valid @RequestBody UserStatusCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(commandPort.changeUserStatus(
                tenantId(), userId, command, principal.userId()));
    }

    /**
     * 方法 `orgUnits` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `org Units` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `orgUnits` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `org Units` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `orgUnits` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `orgUnits`, then continue the business flow using its result, exception, or side effect.
     *
     * @param parentId 输入参数 `parentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/org-units")
    @RequiresRbac3Permission(permission = "system:directory:read")
    @GatewayOperation(
            name = "rbac3-directory-org-unit-list-v1",
            summary = "查询组织单元",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<List<OrgUnitView>> orgUnits(
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiEnvelope.success(queryPort.findOrgUnits(
                tenantId(), parentId, type, status));
    }

    /**
     * 方法 `positions` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `positions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `positions` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `positions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `positions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `positions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/positions")
    @RequiresRbac3Permission(permission = "system:directory:read")
    @GatewayOperation(
            name = "rbac3-directory-position-list-v1",
            summary = "查询岗位",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<List<PositionView>> positions(
            @RequestParam(required = false) String orgUnitId,
            @RequestParam(required = false) String status) {
        return ApiEnvelope.success(queryPort.findPositions(
                tenantId(), orgUnitId, status));
    }

    /**
     * 方法 `submit` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `submit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `submit` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `submit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `submit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `submit`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/internal/directory-snapshots")
    @RequiresRbac3Permission(permission = "system:directory:sync")
    @GatewayOperation(
            name = "rbac3-directory-snapshot-submit-v1",
            summary = "提交单调递增的目录快照",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<DirectorySyncView> submit(
            @Valid @RequestBody DirectorySnapshotCommand command) {
        return ApiEnvelope.success(commandPort.submit(
                TenantContext.requireCurrent().effectiveTenantId(), command));
    }

    /**
     * 方法 `snapshot` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `snapshot` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `snapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `snapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/directory-snapshots/{snapshotId}")
    @RequiresRbac3Permission(permission = "system:directory-snapshot:read")
    @GatewayOperation(
            name = "rbac3-directory-snapshot-get-v1",
            summary = "读取不可变目录快照回执",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<DirectorySnapshotView> snapshot(
            @PathVariable String snapshotId) {
        return ApiEnvelope.success(queryPort.findSnapshot(tenantId(), snapshotId));
    }

    /**
     * 方法 `tenant` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenant` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/platform/tenants/{tenantId}")
    @RequiresRbac3Permission(permission = "system:tenant:read")
    @GatewayOperation(
            name = "rbac3-platform-tenant-get-v1",
            summary = "读取平台目标租户",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelope<TenantView> tenant(@PathVariable String tenantId) {
        requireTargetTenant(tenantId);
        return ApiEnvelope.success(queryPort.findTenant(tenantId));
    }

    /**
     * 类型 `DirectoryCommandPort` 位于 `TenantUserDirectoryController` 内，是接口，用于承载 `Directory Command Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DirectoryCommandPort` is an interface inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Directory Command Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DirectoryCommandPort` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DirectoryCommandPort` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface DirectoryCommandPort {

        /**
         * 方法 `submit` 按照 `DirectoryCommandPort` 的职责处理输入，完成 `submit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `submit` processes its inputs according to `DirectoryCommandPort`'s responsibility, performs the `submit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `submit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `submit`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        DirectorySyncView submit(String tenantId, DirectorySnapshotCommand command);

        /**
         * 方法 `createTenant` 按照 `DirectoryCommandPort` 的职责处理输入，完成 `create Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `createTenant` processes its inputs according to `DirectoryCommandPort`'s responsibility, performs the `create Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `createTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `createTenant`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        TenantView createTenant(CreateTenantCommand command, String actorId);

        /**
         * 方法 `changeTenantStatus` 按照 `DirectoryCommandPort` 的职责处理输入，完成 `change Tenant Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `changeTenantStatus` processes its inputs according to `DirectoryCommandPort`'s responsibility, performs the `change Tenant Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `changeTenantStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `changeTenantStatus`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        TenantView changeTenantStatus(
                String tenantId, TenantStatusCommand command, String actorId);

        /**
         * 方法 `changeUserStatus` 按照 `DirectoryCommandPort` 的职责处理输入，完成 `change User Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `changeUserStatus` processes its inputs according to `DirectoryCommandPort`'s responsibility, performs the `change User Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `changeUserStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `changeUserStatus`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        UserDirectoryView changeUserStatus(
                String tenantId, String userId, UserStatusCommand command, String actorId);
    }

    /**
     * 类型 `DirectoryQueryPort` 位于 `TenantUserDirectoryController` 内，是接口，用于承载 `Directory Query Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DirectoryQueryPort` is an interface inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Directory Query Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DirectoryQueryPort` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DirectoryQueryPort` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface DirectoryQueryPort {

        /**
         * 方法 `findUser` 按照 `DirectoryQueryPort` 的职责处理输入，完成 `find User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findUser` processes its inputs according to `DirectoryQueryPort`'s responsibility, performs the `find User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findUser`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        UserDirectoryView findUser(String tenantId, String userId);

        /**
         * 方法 `findTenant` 按照 `DirectoryQueryPort` 的职责处理输入，完成 `find Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findTenant` processes its inputs according to `DirectoryQueryPort`'s responsibility, performs the `find Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findTenant`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        TenantView findTenant(String tenantId);

        /**
         * 方法 `findTenants` 按照 `DirectoryQueryPort` 的职责处理输入，完成 `find Tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findTenants` processes its inputs according to `DirectoryQueryPort`'s responsibility, performs the `find Tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findTenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findTenants`, then continue the business flow using its result, exception, or side effect.
         *
         * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param page 输入参数 `page`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param size 输入参数 `size`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PageView<TenantView> findTenants(String query, String status, int page, int size);

        /**
         * 方法 `findUsers` 按照 `DirectoryQueryPort` 的职责处理输入，完成 `find Users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findUsers` processes its inputs according to `DirectoryQueryPort`'s responsibility, performs the `find Users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findUsers` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findUsers`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param positionId 输入参数 `positionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param page 输入参数 `page`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param size 输入参数 `size`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PageView<UserDirectoryView> findUsers(
                String tenantId, String query, String status, String orgUnitId,
                String positionId, int page, int size);

        /**
         * 方法 `findOrgUnits` 按照 `DirectoryQueryPort` 的职责处理输入，完成 `find Org Units` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findOrgUnits` processes its inputs according to `DirectoryQueryPort`'s responsibility, performs the `find Org Units` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findOrgUnits` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findOrgUnits`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param parentId 输入参数 `parentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<OrgUnitView> findOrgUnits(
                String tenantId, String parentId, String type, String status);

        /**
         * 方法 `findPositions` 按照 `DirectoryQueryPort` 的职责处理输入，完成 `find Positions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findPositions` processes its inputs according to `DirectoryQueryPort`'s responsibility, performs the `find Positions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findPositions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findPositions`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<PositionView> findPositions(
                String tenantId, String orgUnitId, String status);

        /**
         * 方法 `findSnapshot` 按照 `DirectoryQueryPort` 的职责处理输入，完成 `find Snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findSnapshot` processes its inputs according to `DirectoryQueryPort`'s responsibility, performs the `find Snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findSnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findSnapshot`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        DirectorySnapshotView findSnapshot(String tenantId, String snapshotId);
    }

    /**
     * 类型 `CreateTenantCommand` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Create Tenant Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CreateTenantCommand` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Create Tenant Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CreateTenantCommand` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CreateTenantCommand` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param code 记录组件 `code` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `code` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param settings 记录组件 `settings` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `settings` carries constructor data whose meaning is defined by the record contract.
     */
    public record CreateTenantCommand(
            /**
             * 字段 `code` 表示 `CreateTenantCommand` 中与 `code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `code` stores the `code`-related state, dependency, configuration, or result of `CreateTenantCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `code` 时应保持 `CreateTenantCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `code`, preserve `CreateTenantCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String code,
            /**
             * 字段 `name` 表示 `CreateTenantCommand` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `CreateTenantCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `CreateTenantCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `CreateTenantCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String name,
            /**
             * 字段 `settings` 表示 `CreateTenantCommand` 中与 `settings` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `settings` stores the `settings`-related state, dependency, configuration, or result of `CreateTenantCommand` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `settings` 时应保持 `CreateTenantCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `settings`, preserve `CreateTenantCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @GatewaySchemaField(allowArbitraryJson = true)
            @NotNull Map<String, Object> settings
    ) {
    }

    /**
     * 类型 `TenantStatusCommand` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Tenant Status Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TenantStatusCommand` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Tenant Status Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TenantStatusCommand` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TenantStatusCommand` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record TenantStatusCommand(
            /**
             * 字段 `status` 表示 `TenantStatusCommand` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `TenantStatusCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `TenantStatusCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `TenantStatusCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String status,
            /**
             * 字段 `reason` 表示 `TenantStatusCommand` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `TenantStatusCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `TenantStatusCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `TenantStatusCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String reason,
            /**
             * 字段 `expectedVersion` 表示 `TenantStatusCommand` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `TenantStatusCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `TenantStatusCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `TenantStatusCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedVersion
    ) {
    }

    /**
     * 类型 `UserStatusCommand` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `User Status Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `UserStatusCommand` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `User Status Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `UserStatusCommand` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `UserStatusCommand` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     * @param expectedAuthVersion 记录组件 `expectedAuthVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedAuthVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record UserStatusCommand(
            /**
             * 字段 `status` 表示 `UserStatusCommand` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `UserStatusCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `UserStatusCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `UserStatusCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String status,
            /**
             * 字段 `reason` 表示 `UserStatusCommand` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `UserStatusCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `UserStatusCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `UserStatusCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String reason,
            /**
             * 字段 `expectedAuthVersion` 表示 `UserStatusCommand` 中与 `expected Auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedAuthVersion` stores the `expected Auth Version`-related state, dependency, configuration, or result of `UserStatusCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedAuthVersion` 时应保持 `UserStatusCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedAuthVersion`, preserve `UserStatusCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedAuthVersion
    ) {
    }

    /**
     * 类型 `DirectorySnapshotCommand` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Directory Snapshot Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DirectorySnapshotCommand` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Directory Snapshot Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DirectorySnapshotCommand` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DirectorySnapshotCommand` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param providerCode 记录组件 `providerCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `providerCode` carries constructor data whose meaning is defined by the record contract.
     * @param snapshotVersion 记录组件 `snapshotVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotVersion` carries constructor data whose meaning is defined by the record contract.
     * @param checksum 记录组件 `checksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `checksum` carries constructor data whose meaning is defined by the record contract.
     * @param generatedAt 记录组件 `generatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `generatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param payload 记录组件 `payload` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `payload` carries constructor data whose meaning is defined by the record contract.
     */
    public record DirectorySnapshotCommand(
            /**
             * 字段 `providerCode` 表示 `DirectorySnapshotCommand` 中与 `provider Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `providerCode` stores the `provider Code`-related state, dependency, configuration, or result of `DirectorySnapshotCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `providerCode` 时应保持 `DirectorySnapshotCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `providerCode`, preserve `DirectorySnapshotCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String providerCode,
            /**
             * 字段 `snapshotVersion` 表示 `DirectorySnapshotCommand` 中与 `snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotVersion` stores the `snapshot Version`-related state, dependency, configuration, or result of `DirectorySnapshotCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotVersion` 时应保持 `DirectorySnapshotCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotVersion`, preserve `DirectorySnapshotCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long snapshotVersion,
            /**
             * 字段 `checksum` 表示 `DirectorySnapshotCommand` 中与 `checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `checksum` stores the `checksum`-related state, dependency, configuration, or result of `DirectorySnapshotCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `checksum` 时应保持 `DirectorySnapshotCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `checksum`, preserve `DirectorySnapshotCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String checksum,
            /**
             * 字段 `generatedAt` 表示 `DirectorySnapshotCommand` 中与 `generated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `generatedAt` stores the `generated At`-related state, dependency, configuration, or result of `DirectorySnapshotCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `generatedAt` 时应保持 `DirectorySnapshotCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `generatedAt`, preserve `DirectorySnapshotCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant generatedAt,
            /**
             * 字段 `payload` 表示 `DirectorySnapshotCommand` 中与 `payload` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `payload` stores the `payload`-related state, dependency, configuration, or result of `DirectorySnapshotCommand` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `payload` 时应保持 `DirectorySnapshotCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `payload`, preserve `DirectorySnapshotCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            @GatewaySchemaField(allowArbitraryJson = true)
            @NotNull Map<String, Object> payload
    ) {
    }

    /**
     * 类型 `DirectorySyncView` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Directory Sync View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DirectorySyncView` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Directory Sync View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DirectorySyncView` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DirectorySyncView` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param snapshotId 记录组件 `snapshotId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotId` carries constructor data whose meaning is defined by the record contract.
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param counts 记录组件 `counts` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `counts` carries constructor data whose meaning is defined by the record contract.
     * @param affectedUserCount 记录组件 `affectedUserCount` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `affectedUserCount` carries constructor data whose meaning is defined by the record contract.
     */
    public record DirectorySyncView(
            /**
             * 字段 `snapshotId` 表示 `DirectorySyncView` 中与 `snapshot Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotId` stores the `snapshot Id`-related state, dependency, configuration, or result of `DirectorySyncView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotId` 时应保持 `DirectorySyncView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotId`, preserve `DirectorySyncView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotId,
            /**
             * 字段 `outcome` 表示 `DirectorySyncView` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `DirectorySyncView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outcome` 时应保持 `DirectorySyncView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outcome`, preserve `DirectorySyncView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String outcome,
            /**
             * 字段 `counts` 表示 `DirectorySyncView` 中与 `counts` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Long&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `counts` stores the `counts`-related state, dependency, configuration, or result of `DirectorySyncView` (declared type `Map&lt;String, Long&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `counts` 时应保持 `DirectorySyncView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `counts`, preserve `DirectorySyncView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, Long> counts,
            /**
             * 字段 `affectedUserCount` 表示 `DirectorySyncView` 中与 `affected User Count` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `affectedUserCount` stores the `affected User Count`-related state, dependency, configuration, or result of `DirectorySyncView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `affectedUserCount` 时应保持 `DirectorySyncView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `affectedUserCount`, preserve `DirectorySyncView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long affectedUserCount
    ) {
    }

    /**
     * 类型 `UserDirectoryView` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `User Directory View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `UserDirectoryView` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `User Directory View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `UserDirectoryView` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `UserDirectoryView` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param username 记录组件 `username` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `username` carries constructor data whose meaning is defined by the record contract.
     * @param displayName 记录组件 `displayName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `displayName` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param primaryOrgUnitId 记录组件 `primaryOrgUnitId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `primaryOrgUnitId` carries constructor data whose meaning is defined by the record contract.
     * @param primaryPositionId 记录组件 `primaryPositionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `primaryPositionId` carries constructor data whose meaning is defined by the record contract.
     * @param directorySnapshotVersion 记录组件 `directorySnapshotVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `directorySnapshotVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record UserDirectoryView(
            /**
             * 字段 `userId` 表示 `UserDirectoryView` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `UserDirectoryView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `UserDirectoryView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `UserDirectoryView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `username` 表示 `UserDirectoryView` 中与 `username` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `username` stores the `username`-related state, dependency, configuration, or result of `UserDirectoryView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `username` 时应保持 `UserDirectoryView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `username`, preserve `UserDirectoryView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String username,
            /**
             * 字段 `displayName` 表示 `UserDirectoryView` 中与 `display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `displayName` stores the `display Name`-related state, dependency, configuration, or result of `UserDirectoryView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `displayName` 时应保持 `UserDirectoryView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `displayName`, preserve `UserDirectoryView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String displayName,
            /**
             * 字段 `status` 表示 `UserDirectoryView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `UserDirectoryView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `UserDirectoryView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `UserDirectoryView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `authVersion` 表示 `UserDirectoryView` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `UserDirectoryView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `UserDirectoryView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `UserDirectoryView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `primaryOrgUnitId` 表示 `UserDirectoryView` 中与 `primary Org Unit Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `primaryOrgUnitId` stores the `primary Org Unit Id`-related state, dependency, configuration, or result of `UserDirectoryView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `primaryOrgUnitId` 时应保持 `UserDirectoryView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `primaryOrgUnitId`, preserve `UserDirectoryView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String primaryOrgUnitId,
            /**
             * 字段 `primaryPositionId` 表示 `UserDirectoryView` 中与 `primary Position Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `primaryPositionId` stores the `primary Position Id`-related state, dependency, configuration, or result of `UserDirectoryView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `primaryPositionId` 时应保持 `UserDirectoryView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `primaryPositionId`, preserve `UserDirectoryView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String primaryPositionId,
            /**
             * 字段 `directorySnapshotVersion` 表示 `UserDirectoryView` 中与 `directory Snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `directorySnapshotVersion` stores the `directory Snapshot Version`-related state, dependency, configuration, or result of `UserDirectoryView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `directorySnapshotVersion` 时应保持 `UserDirectoryView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `directorySnapshotVersion`, preserve `UserDirectoryView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long directorySnapshotVersion
    ) {
    }

    /**
     * 类型 `TenantView` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Tenant View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TenantView` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Tenant View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TenantView` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TenantView` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantCode 记录组件 `tenantCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantCode` carries constructor data whose meaning is defined by the record contract.
     * @param tenantName 记录组件 `tenantName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantName` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param settings 记录组件 `settings` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `settings` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record TenantView(
            /**
             * 字段 `tenantId` 表示 `TenantView` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `TenantView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `TenantView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `TenantView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `tenantCode` 表示 `TenantView` 中与 `tenant Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantCode` stores the `tenant Code`-related state, dependency, configuration, or result of `TenantView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantCode` 时应保持 `TenantView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantCode`, preserve `TenantView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantCode,
            /**
             * 字段 `tenantName` 表示 `TenantView` 中与 `tenant Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantName` stores the `tenant Name`-related state, dependency, configuration, or result of `TenantView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantName` 时应保持 `TenantView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantName`, preserve `TenantView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantName,
            /**
             * 字段 `status` 表示 `TenantView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `TenantView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `TenantView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `TenantView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `settings` 表示 `TenantView` 中与 `settings` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `settings` stores the `settings`-related state, dependency, configuration, or result of `TenantView` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `settings` 时应保持 `TenantView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `settings`, preserve `TenantView`'s lifecycle, immutability, and thread-safety constraints.
             */
            @GatewaySchemaField(allowArbitraryJson = true)
            Map<String, Object> settings,
            /**
             * 字段 `version` 表示 `TenantView` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `TenantView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `TenantView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `TenantView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {
    }

    /**
     * 类型 `OrgUnitView` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Org Unit View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OrgUnitView` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Org Unit View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OrgUnitView` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OrgUnitView` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param orgUnitId 记录组件 `orgUnitId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `orgUnitId` carries constructor data whose meaning is defined by the record contract.
     * @param snapshotId 记录组件 `snapshotId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotId` carries constructor data whose meaning is defined by the record contract.
     * @param type 记录组件 `type` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `type` carries constructor data whose meaning is defined by the record contract.
     * @param code 记录组件 `code` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `code` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param parentId 记录组件 `parentId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `parentId` carries constructor data whose meaning is defined by the record contract.
     * @param path 记录组件 `path` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `path` carries constructor data whose meaning is defined by the record contract.
     * @param depth 记录组件 `depth` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `depth` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     */
    public record OrgUnitView(
            /**
             * 字段 `orgUnitId` 表示 `OrgUnitView` 中与 `org Unit Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `orgUnitId` stores the `org Unit Id`-related state, dependency, configuration, or result of `OrgUnitView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `orgUnitId` 时应保持 `OrgUnitView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `orgUnitId`, preserve `OrgUnitView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String orgUnitId,
            /**
             * 字段 `snapshotId` 表示 `OrgUnitView` 中与 `snapshot Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotId` stores the `snapshot Id`-related state, dependency, configuration, or result of `OrgUnitView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotId` 时应保持 `OrgUnitView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotId`, preserve `OrgUnitView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotId,
            /**
             * 字段 `type` 表示 `OrgUnitView` 中与 `type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `type` stores the `type`-related state, dependency, configuration, or result of `OrgUnitView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `type` 时应保持 `OrgUnitView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `type`, preserve `OrgUnitView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String type,
            /**
             * 字段 `code` 表示 `OrgUnitView` 中与 `code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `code` stores the `code`-related state, dependency, configuration, or result of `OrgUnitView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `code` 时应保持 `OrgUnitView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `code`, preserve `OrgUnitView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String code,
            /**
             * 字段 `name` 表示 `OrgUnitView` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `OrgUnitView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `OrgUnitView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `OrgUnitView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String name,
            /**
             * 字段 `parentId` 表示 `OrgUnitView` 中与 `parent Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `parentId` stores the `parent Id`-related state, dependency, configuration, or result of `OrgUnitView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `parentId` 时应保持 `OrgUnitView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `parentId`, preserve `OrgUnitView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String parentId,
            /**
             * 字段 `path` 表示 `OrgUnitView` 中与 `path` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `path` stores the `path`-related state, dependency, configuration, or result of `OrgUnitView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `path` 时应保持 `OrgUnitView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `path`, preserve `OrgUnitView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String path,
            /**
             * 字段 `depth` 表示 `OrgUnitView` 中与 `depth` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `depth` stores the `depth`-related state, dependency, configuration, or result of `OrgUnitView` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `depth` 时应保持 `OrgUnitView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `depth`, preserve `OrgUnitView`'s lifecycle, immutability, and thread-safety constraints.
             */
            int depth,
            /**
             * 字段 `status` 表示 `OrgUnitView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `OrgUnitView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `OrgUnitView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `OrgUnitView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status
    ) {
    }

    /**
     * 类型 `PositionView` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Position View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PositionView` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Position View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PositionView` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PositionView` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param positionId 记录组件 `positionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `positionId` carries constructor data whose meaning is defined by the record contract.
     * @param snapshotId 记录组件 `snapshotId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotId` carries constructor data whose meaning is defined by the record contract.
     * @param code 记录组件 `code` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `code` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param orgUnitId 记录组件 `orgUnitId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `orgUnitId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     */
    public record PositionView(
            /**
             * 字段 `positionId` 表示 `PositionView` 中与 `position Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `positionId` stores the `position Id`-related state, dependency, configuration, or result of `PositionView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `positionId` 时应保持 `PositionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `positionId`, preserve `PositionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String positionId,
            /**
             * 字段 `snapshotId` 表示 `PositionView` 中与 `snapshot Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotId` stores the `snapshot Id`-related state, dependency, configuration, or result of `PositionView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotId` 时应保持 `PositionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotId`, preserve `PositionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotId,
            /**
             * 字段 `code` 表示 `PositionView` 中与 `code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `code` stores the `code`-related state, dependency, configuration, or result of `PositionView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `code` 时应保持 `PositionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `code`, preserve `PositionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String code,
            /**
             * 字段 `name` 表示 `PositionView` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `PositionView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `PositionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `PositionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String name,
            /**
             * 字段 `orgUnitId` 表示 `PositionView` 中与 `org Unit Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `orgUnitId` stores the `org Unit Id`-related state, dependency, configuration, or result of `PositionView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `orgUnitId` 时应保持 `PositionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `orgUnitId`, preserve `PositionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String orgUnitId,
            /**
             * 字段 `status` 表示 `PositionView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `PositionView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `PositionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `PositionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status
    ) {
    }

    /**
     * 类型 `DirectorySnapshotView` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Directory Snapshot View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DirectorySnapshotView` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Directory Snapshot View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DirectorySnapshotView` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DirectorySnapshotView` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param snapshotId 记录组件 `snapshotId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotId` carries constructor data whose meaning is defined by the record contract.
     * @param providerCode 记录组件 `providerCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `providerCode` carries constructor data whose meaning is defined by the record contract.
     * @param snapshotVersion 记录组件 `snapshotVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotVersion` carries constructor data whose meaning is defined by the record contract.
     * @param checksum 记录组件 `checksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `checksum` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param generatedAt 记录组件 `generatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `generatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param receivedAt 记录组件 `receivedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `receivedAt` carries constructor data whose meaning is defined by the record contract.
     * @param activatedAt 记录组件 `activatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param counts 记录组件 `counts` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `counts` carries constructor data whose meaning is defined by the record contract.
     */
    public record DirectorySnapshotView(
            /**
             * 字段 `snapshotId` 表示 `DirectorySnapshotView` 中与 `snapshot Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotId` stores the `snapshot Id`-related state, dependency, configuration, or result of `DirectorySnapshotView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotId` 时应保持 `DirectorySnapshotView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotId`, preserve `DirectorySnapshotView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotId,
            /**
             * 字段 `providerCode` 表示 `DirectorySnapshotView` 中与 `provider Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `providerCode` stores the `provider Code`-related state, dependency, configuration, or result of `DirectorySnapshotView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `providerCode` 时应保持 `DirectorySnapshotView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `providerCode`, preserve `DirectorySnapshotView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String providerCode,
            /**
             * 字段 `snapshotVersion` 表示 `DirectorySnapshotView` 中与 `snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotVersion` stores the `snapshot Version`-related state, dependency, configuration, or result of `DirectorySnapshotView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotVersion` 时应保持 `DirectorySnapshotView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotVersion`, preserve `DirectorySnapshotView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long snapshotVersion,
            /**
             * 字段 `checksum` 表示 `DirectorySnapshotView` 中与 `checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `checksum` stores the `checksum`-related state, dependency, configuration, or result of `DirectorySnapshotView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `checksum` 时应保持 `DirectorySnapshotView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `checksum`, preserve `DirectorySnapshotView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String checksum,
            /**
             * 字段 `status` 表示 `DirectorySnapshotView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `DirectorySnapshotView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `DirectorySnapshotView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `DirectorySnapshotView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `generatedAt` 表示 `DirectorySnapshotView` 中与 `generated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `generatedAt` stores the `generated At`-related state, dependency, configuration, or result of `DirectorySnapshotView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `generatedAt` 时应保持 `DirectorySnapshotView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `generatedAt`, preserve `DirectorySnapshotView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant generatedAt,
            /**
             * 字段 `receivedAt` 表示 `DirectorySnapshotView` 中与 `received At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `receivedAt` stores the `received At`-related state, dependency, configuration, or result of `DirectorySnapshotView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `receivedAt` 时应保持 `DirectorySnapshotView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `receivedAt`, preserve `DirectorySnapshotView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant receivedAt,
            /**
             * 字段 `activatedAt` 表示 `DirectorySnapshotView` 中与 `activated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activatedAt` stores the `activated At`-related state, dependency, configuration, or result of `DirectorySnapshotView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activatedAt` 时应保持 `DirectorySnapshotView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activatedAt`, preserve `DirectorySnapshotView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant activatedAt,
            /**
             * 字段 `counts` 表示 `DirectorySnapshotView` 中与 `counts` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `counts` stores the `counts`-related state, dependency, configuration, or result of `DirectorySnapshotView` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `counts` 时应保持 `DirectorySnapshotView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `counts`, preserve `DirectorySnapshotView`'s lifecycle, immutability, and thread-safety constraints.
             */
            @GatewaySchemaField(allowArbitraryJson = true)
            Map<String, Object> counts
    ) {
    }

    /**
     * 类型 `PageView` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Page View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PageView` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Page View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PageView` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PageView` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param <T> 类型参数表示分页元素的具体类型；type parameter representing the page element type.
     * @param items 记录组件 `items` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `items` carries constructor data whose meaning is defined by the record contract.
     * @param page 记录组件 `page` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `page` carries constructor data whose meaning is defined by the record contract.
     * @param size 记录组件 `size` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `size` carries constructor data whose meaning is defined by the record contract.
     * @param total 记录组件 `total` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `total` carries constructor data whose meaning is defined by the record contract.
     */
    public record PageView<T>(
            /**
             * 字段 `items` 表示 `PageView` 中与 `items` 相关的状态、依赖、配置或结果（声明类型 `List&lt;T&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `items` stores the `items`-related state, dependency, configuration, or result of `PageView` (declared type `List&lt;T&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `items` 时应保持 `PageView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `items`, preserve `PageView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<T> items,
            /**
             * 字段 `page` 表示 `PageView` 中与 `page` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `page` stores the `page`-related state, dependency, configuration, or result of `PageView` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `page` 时应保持 `PageView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `page`, preserve `PageView`'s lifecycle, immutability, and thread-safety constraints.
             */
            int page,
            /**
             * 字段 `size` 表示 `PageView` 中与 `size` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `size` stores the `size`-related state, dependency, configuration, or result of `PageView` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `size` 时应保持 `PageView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `size`, preserve `PageView`'s lifecycle, immutability, and thread-safety constraints.
             */
            int size,
            /**
             * 字段 `total` 表示 `PageView` 中与 `total` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `total` stores the `total`-related state, dependency, configuration, or result of `PageView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `total` 时应保持 `PageView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `total`, preserve `PageView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long total
    ) {
    }

    /**
     * 方法 `tenantId` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `requireTargetTenant` 按照 `TenantUserDirectoryController` 的职责处理输入，完成 `require Target Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireTargetTenant` processes its inputs according to `TenantUserDirectoryController`'s responsibility, performs the `require Target Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireTargetTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireTargetTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void requireTargetTenant(String tenantId) {
        if (!TenantContext.requireCurrent().effectiveTenantId().equals(tenantId)) {
            throw new Rbac3RuleViolation("TENANT_CONTEXT_INVALID");
        }
    }
}
