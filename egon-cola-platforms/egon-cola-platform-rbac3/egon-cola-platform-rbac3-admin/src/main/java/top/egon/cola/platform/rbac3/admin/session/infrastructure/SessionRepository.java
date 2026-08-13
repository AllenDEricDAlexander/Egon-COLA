package top.egon.cola.platform.rbac3.admin.session.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity;

import java.time.Instant;
import java.util.Optional;

/**
 * 类型 `SessionRepository` 位于当前包内，是接口，用于承载 `Session Repository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SessionRepository` is an interface in its package and carries the responsibility, state, or contract for `Session Repository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `SessionRepository` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `SessionRepository` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
public interface SessionRepository extends JpaRepository<SessionEntity, Long> {

    /**
     * 方法 `findByTenantIdAndSessionId` 按照 `SessionRepository` 的职责处理输入，完成 `find By Tenant Id And Session Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findByTenantIdAndSessionId` processes its inputs according to `SessionRepository`'s responsibility, performs the `find By Tenant Id And Session Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findByTenantIdAndSessionId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findByTenantIdAndSessionId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    Optional<SessionEntity> findByTenantIdAndSessionId(Long tenantId, Long sessionId);

    /**
     * 方法 `lockByTenantIdAndSessionId` 按照 `SessionRepository` 的职责处理输入，完成 `lock By Tenant Id And Session Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `lockByTenantIdAndSessionId` processes its inputs according to `SessionRepository`'s responsibility, performs the `lock By Tenant Id And Session Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `lockByTenantIdAndSessionId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `lockByTenantIdAndSessionId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionEntity s where s.tenantId = :tenantId and s.sessionId = :sessionId")
    Optional<SessionEntity> lockByTenantIdAndSessionId(
            @Param("tenantId") Long tenantId,
            @Param("sessionId") Long sessionId);

    /**
     * 方法 `revokeAllActiveForUser` 按照 `SessionRepository` 的职责处理输入，完成 `revoke All Active For User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revokeAllActiveForUser` processes its inputs according to `SessionRepository`'s responsibility, performs the `revoke All Active For User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revokeAllActiveForUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revokeAllActiveForUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param currentStatus 输入参数 `currentStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextStatus 输入参数 `nextStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Modifying
    @Query("""
            update SessionEntity s
               set s.status = :nextStatus,
                   s.sessionVersion = s.sessionVersion + 1,
                   s.revokedAt = :now,
                   s.revokeReason = :reason
             where s.tenantId = :tenantId and s.userId = :userId
               and s.status = :currentStatus
            """)
    int revokeAllActiveForUser(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("reason") String reason,
            @Param("now") Instant now,
            @Param("currentStatus") SessionEntity.Status currentStatus,
            @Param("nextStatus") SessionEntity.Status nextStatus);
}
