package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;

import java.time.Instant;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

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
     * 字段 `managementPort` 表示 `SessionController` 中与 `management Port` 相关的状态、依赖、配置或结果（声明类型 `SessionManagementPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `managementPort` stores the `management Port`-related state, dependency, configuration, or result of `SessionController` (declared type `SessionManagementPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `managementPort` 时应保持 `SessionController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `managementPort`, preserve `SessionController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionManagementPort managementPort;
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
            SessionManagementPort managementPort,
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
    public ApiEnvelopeVO<List<SessionView>> mine(
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
    public ApiEnvelopeVO<RevocationView> revoke(@PathVariable String sessionId) {
        boolean changed = managementPort.revoke(
                top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext
                        .requireCurrent().effectiveTenantId(),
                sessionId,
                databaseClock.transactionNow());
        return ApiEnvelopeVO.success(new RevocationView(true, changed));
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
    public ApiEnvelopeVO<RevokeAllView> revokeAll(@PathVariable String userId) {
        int changed = managementPort.revokeAll(
                top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext
                        .requireCurrent().effectiveTenantId(),
                userId,
                databaseClock.transactionNow());
        return ApiEnvelopeVO.success(new RevokeAllView(true, changed));
    }

    /**
     * 类型 `SessionManagementPort` 位于 `SessionController` 内，是接口，用于承载 `Session Management Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionManagementPort` is an interface inside `SessionController` and carries the responsibility, state, or contract for `Session Management Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionManagementPort` 作为 `SessionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionManagementPort` as the responsibility boundary of `SessionController`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface SessionManagementPort {

        /**
         * 方法 `findByUser` 按照 `SessionManagementPort` 的职责处理输入，完成 `find By User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findByUser` processes its inputs according to `SessionManagementPort`'s responsibility, performs the `find By User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findByUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findByUser`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<SessionView> findByUser(String tenantId, String userId);

        /**
         * 方法 `revoke` 按照 `SessionManagementPort` 的职责处理输入，完成 `revoke` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `revoke` processes its inputs according to `SessionManagementPort`'s responsibility, performs the `revoke` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `revoke` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `revoke`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        boolean revoke(String tenantId, String sessionId, Instant now);

        /**
         * 方法 `revokeAll` 按照 `SessionManagementPort` 的职责处理输入，完成 `revoke All` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `revokeAll` processes its inputs according to `SessionManagementPort`'s responsibility, performs the `revoke All` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `revokeAll` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `revokeAll`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        int revokeAll(String tenantId, String userId, Instant now);
    }

    /**
     * 类型 `SessionView` 位于 `SessionController` 内，是记录类型，用于承载 `Session View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionView` is a record inside `SessionController` and carries the responsibility, state, or contract for `Session View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionView` 作为 `SessionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionView` as the responsibility boundary of `SessionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param authStrength 记录组件 `authStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authStrength` carries constructor data whose meaning is defined by the record contract.
     * @param authenticatedAt 记录组件 `authenticatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param strongAuthenticatedAt 记录组件 `strongAuthenticatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `strongAuthenticatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param lastSeenAt 记录组件 `lastSeenAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lastSeenAt` carries constructor data whose meaning is defined by the record contract.
     * @param absoluteExpiresAt 记录组件 `absoluteExpiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `absoluteExpiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record SessionView(
            /**
             * 字段 `sessionId` 表示 `SessionView` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `SessionView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `SessionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `SessionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `status` 表示 `SessionView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `SessionView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `SessionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `SessionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `sessionVersion` 表示 `SessionView` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `SessionView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `SessionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `SessionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `authStrength` 表示 `SessionView` 中与 `auth Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authStrength` stores the `auth Strength`-related state, dependency, configuration, or result of `SessionView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authStrength` 时应保持 `SessionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authStrength`, preserve `SessionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String authStrength,
            /**
             * 字段 `authenticatedAt` 表示 `SessionView` 中与 `authenticated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticatedAt` stores the `authenticated At`-related state, dependency, configuration, or result of `SessionView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticatedAt` 时应保持 `SessionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticatedAt`, preserve `SessionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant authenticatedAt,
            /**
             * 字段 `strongAuthenticatedAt` 表示 `SessionView` 中与 `strong Authenticated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `strongAuthenticatedAt` stores the `strong Authenticated At`-related state, dependency, configuration, or result of `SessionView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `strongAuthenticatedAt` 时应保持 `SessionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `strongAuthenticatedAt`, preserve `SessionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant strongAuthenticatedAt,
            /**
             * 字段 `lastSeenAt` 表示 `SessionView` 中与 `last Seen At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lastSeenAt` stores the `last Seen At`-related state, dependency, configuration, or result of `SessionView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lastSeenAt` 时应保持 `SessionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lastSeenAt`, preserve `SessionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant lastSeenAt,
            /**
             * 字段 `absoluteExpiresAt` 表示 `SessionView` 中与 `absolute Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `absoluteExpiresAt` stores the `absolute Expires At`-related state, dependency, configuration, or result of `SessionView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `absoluteExpiresAt` 时应保持 `SessionView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `absoluteExpiresAt`, preserve `SessionView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant absoluteExpiresAt
    ) {
    }

    /**
     * 类型 `RevocationView` 位于 `SessionController` 内，是记录类型，用于承载 `Revocation View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RevocationView` is a record inside `SessionController` and carries the responsibility, state, or contract for `Revocation View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RevocationView` 作为 `SessionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RevocationView` as the responsibility boundary of `SessionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param success 记录组件 `success` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `success` carries constructor data whose meaning is defined by the record contract.
     * @param stateChanged 记录组件 `stateChanged` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `stateChanged` carries constructor data whose meaning is defined by the record contract.
     */
    public record RevocationView(/**
 * 字段 `success` 表示 `RevocationView` 中与 `success` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `success` stores the `success`-related state, dependency, configuration, or result of `RevocationView` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `success` 时应保持 `RevocationView` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `success`, preserve `RevocationView`'s lifecycle, immutability, and thread-safety constraints.
 */ boolean success, /**
 * 字段 `stateChanged` 表示 `RevocationView` 中与 `state Changed` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `stateChanged` stores the `state Changed`-related state, dependency, configuration, or result of `RevocationView` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `stateChanged` 时应保持 `RevocationView` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `stateChanged`, preserve `RevocationView`'s lifecycle, immutability, and thread-safety constraints.
 */ boolean stateChanged) {
    }

    /**
     * 类型 `RevokeAllView` 位于 `SessionController` 内，是记录类型，用于承载 `Revoke All View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RevokeAllView` is a record inside `SessionController` and carries the responsibility, state, or contract for `Revoke All View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RevokeAllView` 作为 `SessionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RevokeAllView` as the responsibility boundary of `SessionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param success 记录组件 `success` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `success` carries constructor data whose meaning is defined by the record contract.
     * @param revokedCount 记录组件 `revokedCount` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `revokedCount` carries constructor data whose meaning is defined by the record contract.
     */
    public record RevokeAllView(/**
 * 字段 `success` 表示 `RevokeAllView` 中与 `success` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `success` stores the `success`-related state, dependency, configuration, or result of `RevokeAllView` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `success` 时应保持 `RevokeAllView` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `success`, preserve `RevokeAllView`'s lifecycle, immutability, and thread-safety constraints.
 */ boolean success, /**
 * 字段 `revokedCount` 表示 `RevokeAllView` 中与 `revoked Count` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `revokedCount` stores the `revoked Count`-related state, dependency, configuration, or result of `RevokeAllView` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `revokedCount` 时应保持 `RevokeAllView` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `revokedCount`, preserve `RevokeAllView`'s lifecycle, immutability, and thread-safety constraints.
 */ int revokedCount) {
    }
}
