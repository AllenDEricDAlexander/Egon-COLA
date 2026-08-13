package top.egon.cola.platform.rbac3.admin.snapshot.application;

import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 类型 `LoginRuntimeProjectionFactory` 位于当前包内，是类型，用于承载 `Login Runtime Projection Factory` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `LoginRuntimeProjectionFactory` is a type in its package and carries the responsibility, state, or contract for `Login Runtime Projection Factory`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Builds the non-business capability snapshot required immediately after login.
 */
public final class LoginRuntimeProjectionFactory {

    /**
     * 字段 `RBAC3_APPLICATION_CODE` 表示 `LoginRuntimeProjectionFactory` 中与 `RBAC3 APPLICATION CODE` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `RBAC3_APPLICATION_CODE` stores the `RBAC3 APPLICATION CODE`-related state, dependency, configuration, or result of `LoginRuntimeProjectionFactory` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `RBAC3_APPLICATION_CODE` 时应保持 `LoginRuntimeProjectionFactory` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `RBAC3_APPLICATION_CODE`, preserve `LoginRuntimeProjectionFactory`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String RBAC3_APPLICATION_CODE = "rbac3-admin";
    /**
     * 字段 `SESSION_CAPABILITIES` 表示 `LoginRuntimeProjectionFactory` 中与 `SESSION CAPABILITIES` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `SESSION_CAPABILITIES` stores the `SESSION CAPABILITIES`-related state, dependency, configuration, or result of `LoginRuntimeProjectionFactory` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `SESSION_CAPABILITIES` 时应保持 `LoginRuntimeProjectionFactory` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `SESSION_CAPABILITIES`, preserve `LoginRuntimeProjectionFactory`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Set<String> SESSION_CAPABILITIES = Set.of(
            "system:role-activation:read",
            "system:role-activation:use",
            "system:session:logout");

    /**
     * 方法 `create` 按照 `LoginRuntimeProjectionFactory` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `create` processes its inputs according to `LoginRuntimeProjectionFactory`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public SessionSnapshotProjector.Projection create(
            SessionFacade.SessionRecord session,
            Instant generatedAt) {
        return create(new RuntimeState(
                session.tenantId(), session.userId(), session.sessionId(),
                session.status().name(), session.authVersion(), session.sessionVersion(),
                session.policyVersion(), session.absoluteExpiresAt()), generatedAt);
    }

    /**
     * 方法 `create` 按照 `LoginRuntimeProjectionFactory` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `create` processes its inputs according to `LoginRuntimeProjectionFactory`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public SessionSnapshotProjector.Projection create(
            RuntimeState session,
            Instant generatedAt) {
        List<AppAuthorizationContext> contexts = "ACTIVE".equals(session.status())
                ? List.of(new AppAuthorizationContext(
                session.tenantId(), RBAC3_APPLICATION_CODE,
                List.of(), List.of(), List.of(), SESSION_CAPABILITIES,
                Map.of(), Map.of(), List.of(), null))
                : List.of();
        SessionAuthorizationSnapshot snapshot = new SessionAuthorizationSnapshot(
                session.sessionId(), session.authVersion(), session.sessionVersion(),
                session.policyVersion(), contexts, checksum(session), generatedAt);
        SessionSnapshotProjector.RuntimeSession runtimeSession =
                new SessionSnapshotProjector.RuntimeSession(
                        session.tenantId(), session.userId(), session.userId(),
                        session.sessionId(),
                        session.status(), session.authVersion(),
                        session.sessionVersion(), session.policyVersion(),
                        session.absoluteExpiresAt());
        return new SessionSnapshotProjector.Projection(runtimeSession, snapshot);
    }

    /**
     * 方法 `checksum` 按照 `LoginRuntimeProjectionFactory` 的职责处理输入，完成 `checksum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `checksum` processes its inputs according to `LoginRuntimeProjectionFactory`'s responsibility, performs the `checksum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `checksum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `checksum`, then continue the business flow using its result, exception, or side effect.
     *
     * @param session 输入参数 `session`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String checksum(RuntimeState session) {
        String canonical = "session|" + session.tenantId() + '|' + session.userId()
                + '|' + session.sessionId() + '|' + session.authVersion()
                + '|' + session.sessionVersion() + '|' + session.policyVersion()
                + '|' + session.status() + '|'
                + ("ACTIVE".equals(session.status())
                ? SESSION_CAPABILITIES.stream().sorted().toList() : List.of());
        try {
            return "sha256:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(
                            canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * 类型 `RuntimeState` 位于 `LoginRuntimeProjectionFactory` 内，是记录类型，用于承载 `Runtime State` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeState` is a record inside `LoginRuntimeProjectionFactory` and carries the responsibility, state, or contract for `Runtime State`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeState` 作为 `LoginRuntimeProjectionFactory` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeState` as the responsibility boundary of `LoginRuntimeProjectionFactory`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param absoluteExpiresAt 记录组件 `absoluteExpiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `absoluteExpiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record RuntimeState(
            /**
             * 字段 `tenantId` 表示 `RuntimeState` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RuntimeState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RuntimeState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RuntimeState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `RuntimeState` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `RuntimeState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `RuntimeState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `RuntimeState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `RuntimeState` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `RuntimeState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `RuntimeState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `RuntimeState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `status` 表示 `RuntimeState` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `RuntimeState` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `RuntimeState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `RuntimeState`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `authVersion` 表示 `RuntimeState` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `RuntimeState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `RuntimeState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `RuntimeState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `RuntimeState` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `RuntimeState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `RuntimeState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `RuntimeState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `RuntimeState` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RuntimeState` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RuntimeState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RuntimeState`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `absoluteExpiresAt` 表示 `RuntimeState` 中与 `absolute Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `absoluteExpiresAt` stores the `absolute Expires At`-related state, dependency, configuration, or result of `RuntimeState` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `absoluteExpiresAt` 时应保持 `RuntimeState` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `absoluteExpiresAt`, preserve `RuntimeState`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant absoluteExpiresAt
    ) {
    }
}
