package top.egon.cola.platform.rbac3.admin.runtime.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeStatusVO;

/**
 * 类型 `ControlPlaneRuntimeStatusPort` 位于当前包内，是接口，用于承载 `Control Plane Runtime Status Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ControlPlaneRuntimeStatusPort` is an interface in its package and carries the responsibility, state, or contract for `Control Plane Runtime Status Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Read-only boundary for Gateway definition, DDC lease and release observations.
 */
@FunctionalInterface
public interface ControlPlaneRuntimeStatusPort {

    /**
     * 方法 `status` 按照 `ControlPlaneRuntimeStatusPort` 的职责处理输入，完成 `status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `status` processes its inputs according to `ControlPlaneRuntimeStatusPort`'s responsibility, performs the `status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `status` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `status`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    RuntimeStatusVO status();









    }
