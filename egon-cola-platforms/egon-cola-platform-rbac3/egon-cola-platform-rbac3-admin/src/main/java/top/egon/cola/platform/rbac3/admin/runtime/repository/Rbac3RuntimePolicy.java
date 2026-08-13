package top.egon.cola.platform.rbac3.admin.runtime.repository;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.Rbac3RuntimePolicySnapshotVO;

/**
 * 类型 `Rbac3RuntimePolicy` 位于当前包内，是接口，用于承载 `Rbac3 Runtime Policy` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3RuntimePolicy` is an interface in its package and carries the responsibility, state, or contract for `Rbac3 Runtime Policy`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Supplies one complete runtime-policy snapshot to each RBAC3 command.
 */
@FunctionalInterface
public interface Rbac3RuntimePolicy {

    /**
     * 方法 `current` 按照 `Rbac3RuntimePolicy` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `current` processes its inputs according to `Rbac3RuntimePolicy`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    Rbac3RuntimePolicySnapshotVO current();

    }
