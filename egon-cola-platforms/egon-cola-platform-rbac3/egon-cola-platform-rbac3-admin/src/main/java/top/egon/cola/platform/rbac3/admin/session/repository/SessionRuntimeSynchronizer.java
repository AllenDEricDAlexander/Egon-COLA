package top.egon.cola.platform.rbac3.admin.session.repository;

import java.time.Instant;

/**
 * 类型 `SessionRuntimeSynchronizer` 位于当前包内，是接口，用于承载 `Session Runtime Synchronizer` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SessionRuntimeSynchronizer` is an interface in its package and carries the responsibility, state, or contract for `Session Runtime Synchronizer`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Rebuilds one session runtime from authoritative PostgreSQL facts.
 */
@FunctionalInterface
public interface SessionRuntimeSynchronizer {

    /**
     * 方法 `synchronize` 按照 `SessionRuntimeSynchronizer` 的职责处理输入，完成 `synchronize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `synchronize` processes its inputs according to `SessionRuntimeSynchronizer`'s responsibility, performs the `synchronize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `synchronize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `synchronize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    void synchronize(String tenantId, String userId, String sessionId, Instant generatedAt);
}
