package top.egon.cola.platform.rbac3.admin.audit.repository;

import java.time.Instant;
import java.util.Map;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditEventVO;

/**
 * 类型 `AuditPort` 位于当前包内，是接口，用于承载 `Audit Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuditPort` is an interface in its package and carries the responsibility, state, or contract for `Audit Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `AuditPort` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AuditPort` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
public interface AuditPort {

    /**
     * 方法 `append` 按照 `AuditPort` 的职责处理输入，完成 `append` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `append` processes its inputs according to `AuditPort`'s responsibility, performs the `append` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `append` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `append`, then continue the business flow using its result, exception, or side effect.
     *
     * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    void append(AuditEventVO event);
}
