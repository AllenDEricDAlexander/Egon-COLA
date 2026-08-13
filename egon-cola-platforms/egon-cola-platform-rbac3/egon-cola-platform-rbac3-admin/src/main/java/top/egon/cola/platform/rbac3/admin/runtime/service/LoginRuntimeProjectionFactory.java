package top.egon.cola.platform.rbac3.admin.runtime.service;

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
import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionRecordVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeStateVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeSessionVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.SessionSnapshotProjectionVO;

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
    public SessionSnapshotProjectionVO create(
            SessionRecordVO session,
            Instant generatedAt) {
        return create(new RuntimeStateVO(
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
    public SessionSnapshotProjectionVO create(
            RuntimeStateVO session,
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
        RuntimeSessionVO runtimeSession =
                new RuntimeSessionVO(
                        session.tenantId(), session.userId(), session.userId(),
                        session.sessionId(),
                        session.status(), session.authVersion(),
                        session.sessionVersion(), session.policyVersion(),
                        session.absoluteExpiresAt());
        return new SessionSnapshotProjectionVO(runtimeSession, snapshot);
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
    private String checksum(RuntimeStateVO session) {
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

    }
