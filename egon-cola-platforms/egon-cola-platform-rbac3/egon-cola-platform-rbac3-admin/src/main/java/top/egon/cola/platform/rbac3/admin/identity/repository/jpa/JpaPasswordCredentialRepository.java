package top.egon.cola.platform.rbac3.admin.identity.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserCredentialTypeEnum;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserCredentialPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.auth.repository.CredentialRepository;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.PasswordCredentialVO;

/**
 * 基于 JPA 的密码凭据仓储；凭据读取继续使用悲观写锁。
 * JPA password-credential repository; credential reads continue to use a pessimistic write lock.
 */
@Repository
public class JpaPasswordCredentialRepository
        implements CredentialRepository {
    private final EntityManager entityManager;
    private final DatabaseClock databaseClock;

    public JpaPasswordCredentialRepository(
            EntityManager entityManager, DatabaseClock databaseClock) {
        this.entityManager = entityManager;
        this.databaseClock = databaseClock;
    }

/**
     * 方法 `withCredential` 按照 `JpaPasswordCredentialRepository` 的职责处理输入，完成 `with Credential` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `withCredential` processes its inputs according to `JpaPasswordCredentialRepository`'s responsibility, performs the `with Credential` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `withCredential` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `withCredential`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param normalizedUsername 输入参数 `normalizedUsername`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param action 输入参数 `action`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public <T> T withCredential(
            String tenantCode,
            String normalizedUsername,
            Function<PasswordCredentialVO, T> action) {
        Objects.requireNonNull(action, "action");
        CredentialRow row = findCredential(tenantCode, normalizedUsername, LockModeType.PESSIMISTIC_WRITE)
                .orElse(null);
        return action.apply(row == null ? null : row.toPasswordCredential());
    }

/**
     * 方法 `save` 按照 `JpaPasswordCredentialRepository` 的职责处理输入，完成 `save` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `save` processes its inputs according to `JpaPasswordCredentialRepository`'s responsibility, performs the `save` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `save` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `save`, then continue the business flow using its result, exception, or side effect.
     *
     * @param credential 输入参数 `credential`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void save(PasswordCredentialVO credential) {
        CredentialRow row = findCredential(
                credential.tenantCode(),
                credential.normalizedUsername(),
                LockModeType.PESSIMISTIC_WRITE).orElseThrow();
        var entity = row.credential();
        if (credential.failureCount() == 0 && credential.lockedUntil() == null) {
            entity.recordSuccess("authentication", databaseClock.transactionNow());
        } else {
            entity.recordFailure(
                    credential.failureCount(),
                    credential.lockedUntil(),
                    "authentication",
                    databaseClock.transactionNow());
        }
    }

/**
     * 方法 `updatePasswordHash` 按照 `JpaPasswordCredentialRepository` 的职责处理输入，完成 `update Password Hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `updatePasswordHash` processes its inputs according to `JpaPasswordCredentialRepository`'s responsibility, performs the `update Password Hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `updatePasswordHash` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `updatePasswordHash`, then continue the business flow using its result, exception, or side effect.
     *
     * @param credential 输入参数 `credential`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param passwordHash 输入参数 `passwordHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param changedAt 输入参数 `changedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void updatePasswordHash(
            PasswordCredentialVO credential,
            String passwordHash,
            java.time.Instant changedAt) {
        CredentialRow row = findCredential(
                credential.tenantCode(),
                credential.normalizedUsername(),
                LockModeType.PESSIMISTIC_WRITE).orElseThrow();
        row.credential().replacePasswordHash(
                passwordHash,
                "authentication-rehash",
                databaseClock.transactionNow());
    }

/**
     * 方法 `findCredential` 按照 `JpaPasswordCredentialRepository` 的职责处理输入，完成 `find Credential` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findCredential` processes its inputs according to `JpaPasswordCredentialRepository`'s responsibility, performs the `find Credential` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findCredential` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findCredential`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param normalizedUsername 输入参数 `normalizedUsername`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param lockMode 输入参数 `lockMode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Optional<CredentialRow> findCredential(
            String tenantCode,
            String normalizedUsername,
            LockModeType lockMode) {
        List<Object[]> rows = entityManager.createQuery("""
                        select t, u, c
                          from TenantEntity t, UserEntity u, UserCredentialEntity c
                         where lower(t.code) = :tenantCode
                           and u.tenantId = t.id
                           and u.normalizedUsername = :username
                           and c.tenantId = t.id
                           and c.userId = u.id
                           and c.credentialType = :credentialType
                        """, Object[].class)
                .setParameter("tenantCode", tenantCode)
                .setParameter("username", normalizedUsername)
                .setParameter("credentialType", UserCredentialTypeEnum.PASSWORD)
                .setLockMode(lockMode)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] values = rows.getFirst();
        return Optional.of(new CredentialRow(
                (TenantPO) values[0],
                (UserPO) values[1],
                (UserCredentialPO) values[2]));
    }

}
