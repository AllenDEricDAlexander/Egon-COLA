package top.egon.cola.platform.rbac3.admin.bootstrap.application;

import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.Objects;
import java.util.Optional;

/**
 * 类型 `BootstrapQueryService` 位于当前包内，是类型，用于承载 `Bootstrap Query Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `BootstrapQueryService` is a type in its package and carries the responsibility, state, or contract for `Bootstrap Query Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Exposes business bootstrap data only for a session with active roles.
 */
public final class BootstrapQueryService {

    /**
     * 字段 `snapshotSource` 表示 `BootstrapQueryService` 中与 `snapshot Source` 相关的状态、依赖、配置或结果（声明类型 `BootstrapSnapshotSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshotSource` stores the `snapshot Source`-related state, dependency, configuration, or result of `BootstrapQueryService` (declared type `BootstrapSnapshotSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshotSource` 时应保持 `BootstrapQueryService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshotSource`, preserve `BootstrapQueryService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final BootstrapSnapshotSource snapshotSource;

    /**
     * 构造器 `BootstrapQueryService` 用于创建并初始化 `BootstrapQueryService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `BootstrapQueryService` creates and initializes `BootstrapQueryService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `BootstrapQueryService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `BootstrapQueryService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param snapshotSource 输入参数 `snapshotSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public BootstrapQueryService(BootstrapSnapshotSource snapshotSource) {
        this.snapshotSource = Objects.requireNonNull(snapshotSource, "snapshotSource");
    }

    /**
     * 方法 `query` 按照 `BootstrapQueryService` 的职责处理输入，完成 `query` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `query` processes its inputs according to `BootstrapQueryService`'s responsibility, performs the `query` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `query` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `query`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public BootstrapView query(String tenantId, String userId, String sessionId) {
        return snapshotSource.find(tenantId, userId, sessionId)
                .filter(view -> !view.activeRoleContexts().isEmpty())
                .orElseThrow(() -> new Rbac3RuleViolation("ROLE_ACTIVATION_REQUIRED"));
    }

    /**
     * 类型 `BootstrapSnapshotSource` 位于 `BootstrapQueryService` 内，是接口，用于承载 `Bootstrap Snapshot Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `BootstrapSnapshotSource` is an interface inside `BootstrapQueryService` and carries the responsibility, state, or contract for `Bootstrap Snapshot Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `BootstrapSnapshotSource` 作为 `BootstrapQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `BootstrapSnapshotSource` as the responsibility boundary of `BootstrapQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface BootstrapSnapshotSource {

        /**
         * 方法 `find` 按照 `BootstrapSnapshotSource` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `find` processes its inputs according to `BootstrapSnapshotSource`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `find` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `find`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Optional<BootstrapView> find(String tenantId, String userId, String sessionId);
    }
}
