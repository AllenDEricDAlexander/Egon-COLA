package top.egon.cola.platform.rbac3.admin.integration.ddc;

import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 类型 `DdcConfigClientStatusService` 位于当前包内，是类型，用于承载 `Ddc Config Client Status Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DdcConfigClientStatusService` is a type in its package and carries the responsibility, state, or contract for `Ddc Config Client Status Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Projects the independent DDC configuration-client lease without exposing its credential.
 */
public final class DdcConfigClientStatusService {

    /**
     * 字段 `FINGERPRINT_LENGTH` 表示 `DdcConfigClientStatusService` 中与 `FINGERPRINT LENGTH` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `FINGERPRINT_LENGTH` stores the `FINGERPRINT LENGTH`-related state, dependency, configuration, or result of `DdcConfigClientStatusService` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `FINGERPRINT_LENGTH` 时应保持 `DdcConfigClientStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `FINGERPRINT_LENGTH`, preserve `DdcConfigClientStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final int FINGERPRINT_LENGTH = 12;

    /**
     * 字段 `coordinator` 表示 `DdcConfigClientStatusService` 中与 `coordinator` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;DdcRuntimeCoordinator&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `coordinator` stores the `coordinator`-related state, dependency, configuration, or result of `DdcConfigClientStatusService` (declared type `Supplier&lt;DdcRuntimeCoordinator&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `coordinator` 时应保持 `DdcConfigClientStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `coordinator`, preserve `DdcConfigClientStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Supplier<DdcRuntimeCoordinator> coordinator;
    /**
     * 字段 `policy` 表示 `DdcConfigClientStatusService` 中与 `policy` 相关的状态、依赖、配置或结果（声明类型 `AtomicRbac3RuntimePolicy`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policy` stores the `policy`-related state, dependency, configuration, or result of `DdcConfigClientStatusService` (declared type `AtomicRbac3RuntimePolicy`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policy` 时应保持 `DdcConfigClientStatusService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policy`, preserve `DdcConfigClientStatusService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AtomicRbac3RuntimePolicy policy;

    /**
     * 构造器 `DdcConfigClientStatusService` 用于创建并初始化 `DdcConfigClientStatusService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DdcConfigClientStatusService` creates and initializes `DdcConfigClientStatusService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DdcConfigClientStatusService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DdcConfigClientStatusService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param coordinator 输入参数 `coordinator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DdcConfigClientStatusService(
            DdcRuntimeCoordinator coordinator,
            AtomicRbac3RuntimePolicy policy) {
        this(() -> Objects.requireNonNull(coordinator, "coordinator"), policy);
    }

    /**
     * 构造器 `DdcConfigClientStatusService` 用于创建并初始化 `DdcConfigClientStatusService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DdcConfigClientStatusService` creates and initializes `DdcConfigClientStatusService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DdcConfigClientStatusService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DdcConfigClientStatusService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param coordinator 输入参数 `coordinator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DdcConfigClientStatusService(
            ObjectProvider<DdcRuntimeCoordinator> coordinator,
            AtomicRbac3RuntimePolicy policy) {
        this(coordinator::getIfAvailable, policy);
    }

    /**
     * 构造器 `DdcConfigClientStatusService` 用于创建并初始化 `DdcConfigClientStatusService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DdcConfigClientStatusService` creates and initializes `DdcConfigClientStatusService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DdcConfigClientStatusService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DdcConfigClientStatusService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param coordinator 输入参数 `coordinator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private DdcConfigClientStatusService(
            Supplier<DdcRuntimeCoordinator> coordinator,
            AtomicRbac3RuntimePolicy policy) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    /**
     * 方法 `status` 按照 `DdcConfigClientStatusService` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `DdcConfigClientStatusService`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ControlPlaneRuntimeStatusPort.DdcConfigClientStatus status() {
        DdcRuntimeCoordinator runtime = coordinator.get();
        String state = runtime == null ? "UNKNOWN" : runtime.state().name();
        Optional<DdcLeaseSession> session = runtime == null
                ? Optional.empty()
                : runtime.currentSession().filter(value -> value.role() == DdcLeaseRole.CONFIG_CLIENT);
        AtomicRbac3RuntimePolicy.ApplyFailure failure = policy.lastApplyFailure().orElse(null);
        return new ControlPlaneRuntimeStatusPort.DdcConfigClientStatus(
                state,
                session.map(DdcLeaseSession::instanceId).orElse(null),
                session.map(DdcLeaseSession::leaseId).map(this::fingerprint).orElse(null),
                session.map(DdcLeaseSession::leaseExpireAt).orElse(null),
                policy.current().configVersions(),
                failure == null ? null : failure.key(),
                failure == null ? null : failure.targetVersion(),
                failure == null ? null : failure.errorCode());
    }

    /**
     * 方法 `ready` 按照 `DdcConfigClientStatusService` 的职责处理输入，完成 `ready` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ready` processes its inputs according to `DdcConfigClientStatusService`'s responsibility, performs the `ready` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ready` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ready`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean ready() {
        ControlPlaneRuntimeStatusPort.DdcConfigClientStatus status = status();
        return "READY".equals(status.state()) && status.instanceId() != null;
    }

    /**
     * 方法 `fingerprint` 按照 `DdcConfigClientStatusService` 的职责处理输入，完成 `fingerprint` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fingerprint` processes its inputs according to `DdcConfigClientStatusService`'s responsibility, performs the `fingerprint` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fingerprint` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fingerprint`, then continue the business flow using its result, exception, or side effect.
     *
     * @param leaseId 输入参数 `leaseId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String fingerprint(String leaseId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(leaseId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, FINGERPRINT_LENGTH);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
