package top.egon.cola.platform.rbac3.admin.session.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;

import java.time.Instant;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.session.service.SessionManagementService;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.RevocationVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.RevokeAllVO;

/**
 * 类型 `SessionController` 位于当前包内，是类型，用于承载 `Session Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SessionController` is a type in its package and carries the responsibility, state, or contract for `Session Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `SessionController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `SessionController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/api/rbac3/v1")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "session",
        name = "会话管理接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class SessionController {

    /**
     * 字段 `managementPort` 表示 `SessionController` 中与 `management Port` 相关的状态、依赖、配置或结果（声明类型 `SessionManagementService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `managementPort` stores the `management Port`-related state, dependency, configuration, or result of `SessionController` (declared type `SessionManagementService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `managementPort` 时应保持 `SessionController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `managementPort`, preserve `SessionController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionManagementService managementPort;
    /**
     * 字段 `databaseClock` 表示 `SessionController` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `SessionController` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `SessionController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `SessionController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `SessionController` 用于创建并初始化 `SessionController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SessionController` creates and initializes `SessionController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SessionController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SessionController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param managementPort 输入参数 `managementPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public SessionController(
            SessionManagementService managementPort,
            DatabaseClock databaseClock) {
        this.managementPort = managementPort;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `mine` 按照 `SessionController` 的职责处理输入，完成 `mine` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `mine` processes its inputs according to `SessionController`'s responsibility, performs the `mine` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `mine` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `mine`, then continue the business flow using its result, exception, or side effect.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/sessions/me")
    @RequiresRbac3Permission(permission = "system:session:read")
    @GatewayOperation(
            name = "rbac3-session-list-mine-v1",
            summary = "列出当前用户的会话",
            externalAccessible = true,
            tags = {"rbac3", "session"})
    public ApiEnvelopeVO<List<SessionVO>> mine(
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(managementPort.findByUser(
                principal.tenantId(), principal.userId()));
    }

    /**
     * 方法 `revoke` 按照 `SessionController` 的职责处理输入，完成 `revoke` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revoke` processes its inputs according to `SessionController`'s responsibility, performs the `revoke` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revoke` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revoke`, then continue the business flow using its result, exception, or side effect.
     *
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/sessions/{sessionId}/revoke")
    @RequiresRbac3Permission(permission = "system:session:revoke")
    @GatewayOperation(
            name = "rbac3-session-revoke-v1",
            summary = "撤销指定租户会话",
            externalAccessible = true,
            tags = {"rbac3", "session"})
    public ApiEnvelopeVO<RevocationVO> revoke(@PathVariable String sessionId) {
        boolean changed = managementPort.revoke(
                top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext
                        .requireCurrent().effectiveTenantId(),
                sessionId,
                databaseClock.transactionNow());
        return ApiEnvelopeVO.success(new RevocationVO(true, changed));
    }

    /**
     * 方法 `revokeAll` 按照 `SessionController` 的职责处理输入，完成 `revoke All` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revokeAll` processes its inputs according to `SessionController`'s responsibility, performs the `revoke All` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revokeAll` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revokeAll`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/users/{userId}/sessions/revoke-all")
    @RequiresRbac3Permission(permission = "system:session:revoke")
    @GatewayOperation(
            name = "rbac3-session-revoke-all-v1",
            summary = "撤销指定租户用户的全部会话",
            externalAccessible = true,
            tags = {"rbac3", "session"})
    public ApiEnvelopeVO<RevokeAllVO> revokeAll(@PathVariable String userId) {
        int changed = managementPort.revokeAll(
                top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext
                        .requireCurrent().effectiveTenantId(),
                userId,
                databaseClock.transactionNow());
        return ApiEnvelopeVO.success(new RevokeAllVO(true, changed));
    }




    }
