package top.egon.cola.platform.rbac3.admin.identity.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.auth.application.PasswordIdentityAuthenticator;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.ExternalIdentityEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserCredentialEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserEntity;
import top.egon.cola.platform.rbac3.admin.identity.application.IdentityMappingFacade;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * 类型 `IdentityRepositories` 位于当前包内，是类型，用于承载 `Identity Repositories` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `IdentityRepositories` is a type in its package and carries the responsibility, state, or contract for `Identity Repositories`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Tenant-safe identity persistence adapter. Credential reads use a database row lock.
 */
@Repository
public class IdentityRepositories implements PasswordIdentityAuthenticator.CredentialStore,
        IdentityMappingFacade.MappingStore {

    /**
     * 字段 `entityManager` 表示 `IdentityRepositories` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `IdentityRepositories` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `IdentityRepositories` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `IdentityRepositories`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `databaseClock` 表示 `IdentityRepositories` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `IdentityRepositories` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `IdentityRepositories` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `IdentityRepositories`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `IdentityRepositories` 用于创建并初始化 `IdentityRepositories` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `IdentityRepositories` creates and initializes `IdentityRepositories`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `IdentityRepositories` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `IdentityRepositories`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public IdentityRepositories(EntityManager entityManager, DatabaseClock databaseClock) {
        this.entityManager = entityManager;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `findTenantByCode` 按照 `IdentityRepositories` 的职责处理输入，完成 `find Tenant By Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findTenantByCode` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `find Tenant By Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findTenantByCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findTenantByCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Transactional(readOnly = true)
    public Optional<TenantEntity> findTenantByCode(String tenantCode) {
        return entityManager.createQuery(
                        "select t from TenantEntity t where lower(t.code) = :code",
                        TenantEntity.class)
                .setParameter("code", tenantCode.toLowerCase(java.util.Locale.ROOT))
                .getResultStream()
                .findFirst();
    }

    /**
     * 方法 `find` 按照 `IdentityRepositories` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `find` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `find` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `find`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityMappingFacade.Mapping> find(
            String tenantId, String identitySub) {
        return entityManager.createQuery("""
                        select i from ExternalIdentityEntity i
                         where i.tenantId = :tenantId
                           and i.identitySub = :identitySub
                        """, ExternalIdentityEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("identitySub", identitySub)
                .getResultStream()
                .findFirst()
                .map(IdentityRepositories::toMapping);
    }

    /**
     * 方法 `create` 按照 `IdentityRepositories` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `create` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
     *
     * @param mappingId 输入参数 `mappingId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rbac3UserId 输入参数 `rbac3UserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public IdentityMappingFacade.Mapping create(
            long mappingId,
            String tenantId,
            String identitySub,
            String rbac3UserId,
            String actorId,
            java.time.Instant now) {
        Long numericTenantId = Long.valueOf(tenantId);
        Long numericUserId = Long.valueOf(rbac3UserId);
        requireActiveTenantAndUser(numericTenantId, numericUserId);
        ExternalIdentityEntity entity = ExternalIdentityEntity.idpMapping(
                mappingId, numericTenantId, identitySub, numericUserId, actorId, now);
        entityManager.persist(entity);
        entityManager.flush();
        return toMapping(entity);
    }

    /**
     * 方法 `resolve` 按照 `IdentityRepositories` 的职责处理输入，完成 `resolve` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resolve` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `resolve` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resolve` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resolve`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityMappingFacade.ResolvedMembership> resolve(
            String tenantId, String identitySub) {
        return activeMemberships(identitySub, Long.valueOf(tenantId)).stream()
                .findFirst()
                .map(IdentityRepositories::toResolvedMembership);
    }

    /**
     * 方法 `tenants` 按照 `IdentityRepositories` 的职责处理输入，完成 `tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenants` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenants`, then continue the business flow using its result, exception, or side effect.
     *
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public List<IdentityMappingFacade.TenantMembership> tenants(String identitySub) {
        return activeMemberships(identitySub, null).stream()
                .map(row -> new IdentityMappingFacade.TenantMembership(
                        row.tenant().getId().toString(), row.tenant().getCode(),
                        row.tenant().getName(), row.user().getId().toString(),
                        row.user().getDisplayName()))
                .toList();
    }

    /**
     * 方法 `withCredential` 按照 `IdentityRepositories` 的职责处理输入，完成 `with Credential` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `withCredential` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `with Credential` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
            Function<PasswordIdentityAuthenticator.PasswordCredential, T> action) {
        Objects.requireNonNull(action, "action");
        CredentialRow row = findCredential(tenantCode, normalizedUsername, LockModeType.PESSIMISTIC_WRITE)
                .orElse(null);
        return action.apply(row == null ? null : row.toPasswordCredential());
    }

    /**
     * 方法 `save` 按照 `IdentityRepositories` 的职责处理输入，完成 `save` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `save` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `save` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `save` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `save`, then continue the business flow using its result, exception, or side effect.
     *
     * @param credential 输入参数 `credential`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    @Transactional
    public void save(PasswordIdentityAuthenticator.PasswordCredential credential) {
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
     * 方法 `updatePasswordHash` 按照 `IdentityRepositories` 的职责处理输入，完成 `update Password Hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `updatePasswordHash` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `update Password Hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
            PasswordIdentityAuthenticator.PasswordCredential credential,
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
     * 方法 `findCredential` 按照 `IdentityRepositories` 的职责处理输入，完成 `find Credential` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findCredential` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `find Credential` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
                .setParameter("credentialType", UserCredentialEntity.CredentialType.PASSWORD)
                .setLockMode(lockMode)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] values = rows.getFirst();
        return Optional.of(new CredentialRow(
                (TenantEntity) values[0],
                (UserEntity) values[1],
                (UserCredentialEntity) values[2]));
    }

    /**
     * 方法 `activeMemberships` 按照 `IdentityRepositories` 的职责处理输入，完成 `active Memberships` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activeMemberships` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `active Memberships` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activeMemberships` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activeMemberships`, then continue the business flow using its result, exception, or side effect.
     *
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<MembershipRow> activeMemberships(
            String identitySub, Long tenantId) {
        String tenantPredicate = tenantId == null ? "" : " and t.id = :tenantId";
        var query = entityManager.createQuery("""
                        select i, t, u
                          from ExternalIdentityEntity i, TenantEntity t, UserEntity u
                         where i.identitySub = :identitySub
                           and i.status = :identityStatus
                           and t.id = i.tenantId
                           and t.status = :tenantStatus
                           and u.tenantId = i.tenantId
                           and u.id = i.userId
                           and u.status = :userStatus
                        """ + tenantPredicate + " order by t.id", Object[].class)
                .setParameter("identitySub", identitySub)
                .setParameter("identityStatus", ExternalIdentityEntity.Status.ACTIVE)
                .setParameter("tenantStatus", TenantEntity.Status.ACTIVE)
                .setParameter("userStatus", UserEntity.Status.ACTIVE);
        if (tenantId != null) {
            query.setParameter("tenantId", tenantId);
        }
        return query.getResultList().stream()
                .map(values -> new MembershipRow(
                        (ExternalIdentityEntity) values[0],
                        (TenantEntity) values[1],
                        (UserEntity) values[2]))
                .toList();
    }

    /**
     * 方法 `requireActiveTenantAndUser` 按照 `IdentityRepositories` 的职责处理输入，完成 `require Active Tenant And User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireActiveTenantAndUser` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `require Active Tenant And User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireActiveTenantAndUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireActiveTenantAndUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void requireActiveTenantAndUser(Long tenantId, Long userId) {
        TenantEntity tenant = entityManager.find(
                TenantEntity.class, tenantId, LockModeType.PESSIMISTIC_READ);
        UserEntity user = entityManager.find(
                UserEntity.class, userId, LockModeType.PESSIMISTIC_READ);
        if (tenant == null || tenant.getStatus() != TenantEntity.Status.ACTIVE
                || user == null || !tenantId.equals(user.getTenantId())
                || user.getStatus() != UserEntity.Status.ACTIVE) {
            throw new IllegalStateException("active tenant user is required");
        }
    }

    /**
     * 方法 `toMapping` 按照 `IdentityRepositories` 的职责处理输入，完成 `to Mapping` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toMapping` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `to Mapping` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toMapping` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toMapping`, then continue the business flow using its result, exception, or side effect.
     *
     * @param entity 输入参数 `entity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static IdentityMappingFacade.Mapping toMapping(
            ExternalIdentityEntity entity) {
        return new IdentityMappingFacade.Mapping(
                entity.getId().toString(), entity.getTenantId().toString(),
                entity.getIdentitySub(), entity.getUserId().toString(),
                entity.getStatus() == ExternalIdentityEntity.Status.ACTIVE,
                entity.getUpdatedAt());
    }

    /**
     * 方法 `toResolvedMembership` 按照 `IdentityRepositories` 的职责处理输入，完成 `to Resolved Membership` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toResolvedMembership` processes its inputs according to `IdentityRepositories`'s responsibility, performs the `to Resolved Membership` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toResolvedMembership` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toResolvedMembership`, then continue the business flow using its result, exception, or side effect.
     *
     * @param row 输入参数 `row`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static IdentityMappingFacade.ResolvedMembership toResolvedMembership(
            MembershipRow row) {
        return new IdentityMappingFacade.ResolvedMembership(
                row.tenant().getId().toString(), row.tenant().getCode(),
                row.tenant().getName(), row.identity().getIdentitySub(),
                row.user().getId().toString(), row.user().getDisplayName(), true,
                row.user().getAuthVersion(), row.tenant().getPolicyVersion());
    }

    /**
     * 类型 `MembershipRow` 位于 `IdentityRepositories` 内，是记录类型，用于承载 `Membership Row` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MembershipRow` is a record inside `IdentityRepositories` and carries the responsibility, state, or contract for `Membership Row`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MembershipRow` 作为 `IdentityRepositories` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MembershipRow` as the responsibility boundary of `IdentityRepositories`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param identity 记录组件 `identity` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identity` carries constructor data whose meaning is defined by the record contract.
     * @param tenant 记录组件 `tenant` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenant` carries constructor data whose meaning is defined by the record contract.
     * @param user 记录组件 `user` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `user` carries constructor data whose meaning is defined by the record contract.
     */
    private record MembershipRow(
            /**
             * 字段 `identity` 表示 `MembershipRow` 中与 `identity` 相关的状态、依赖、配置或结果（声明类型 `ExternalIdentityEntity`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identity` stores the `identity`-related state, dependency, configuration, or result of `MembershipRow` (declared type `ExternalIdentityEntity`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identity` 时应保持 `MembershipRow` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identity`, preserve `MembershipRow`'s lifecycle, immutability, and thread-safety constraints.
             */
            ExternalIdentityEntity identity,
            /**
             * 字段 `tenant` 表示 `MembershipRow` 中与 `tenant` 相关的状态、依赖、配置或结果（声明类型 `TenantEntity`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenant` stores the `tenant`-related state, dependency, configuration, or result of `MembershipRow` (declared type `TenantEntity`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenant` 时应保持 `MembershipRow` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenant`, preserve `MembershipRow`'s lifecycle, immutability, and thread-safety constraints.
             */
            TenantEntity tenant,
            /**
             * 字段 `user` 表示 `MembershipRow` 中与 `user` 相关的状态、依赖、配置或结果（声明类型 `UserEntity`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `user` stores the `user`-related state, dependency, configuration, or result of `MembershipRow` (declared type `UserEntity`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `user` 时应保持 `MembershipRow` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `user`, preserve `MembershipRow`'s lifecycle, immutability, and thread-safety constraints.
             */
            UserEntity user
    ) {
    }

    /**
     * 类型 `CredentialRow` 位于 `IdentityRepositories` 内，是记录类型，用于承载 `Credential Row` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CredentialRow` is a record inside `IdentityRepositories` and carries the responsibility, state, or contract for `Credential Row`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CredentialRow` 作为 `IdentityRepositories` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CredentialRow` as the responsibility boundary of `IdentityRepositories`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenant 记录组件 `tenant` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenant` carries constructor data whose meaning is defined by the record contract.
     * @param user 记录组件 `user` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `user` carries constructor data whose meaning is defined by the record contract.
     * @param credential 记录组件 `credential` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `credential` carries constructor data whose meaning is defined by the record contract.
     */
    private record CredentialRow(
            /**
             * 字段 `tenant` 表示 `CredentialRow` 中与 `tenant` 相关的状态、依赖、配置或结果（声明类型 `TenantEntity`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenant` stores the `tenant`-related state, dependency, configuration, or result of `CredentialRow` (declared type `TenantEntity`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenant` 时应保持 `CredentialRow` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenant`, preserve `CredentialRow`'s lifecycle, immutability, and thread-safety constraints.
             */
            TenantEntity tenant,
            /**
             * 字段 `user` 表示 `CredentialRow` 中与 `user` 相关的状态、依赖、配置或结果（声明类型 `UserEntity`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `user` stores the `user`-related state, dependency, configuration, or result of `CredentialRow` (declared type `UserEntity`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `user` 时应保持 `CredentialRow` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `user`, preserve `CredentialRow`'s lifecycle, immutability, and thread-safety constraints.
             */
            UserEntity user,
            /**
             * 字段 `credential` 表示 `CredentialRow` 中与 `credential` 相关的状态、依赖、配置或结果（声明类型 `UserCredentialEntity`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `credential` stores the `credential`-related state, dependency, configuration, or result of `CredentialRow` (declared type `UserCredentialEntity`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `credential` 时应保持 `CredentialRow` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `credential`, preserve `CredentialRow`'s lifecycle, immutability, and thread-safety constraints.
             */
            UserCredentialEntity credential
    ) {

        /**
         * 方法 `toPasswordCredential` 按照 `CredentialRow` 的职责处理输入，完成 `to Password Credential` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `toPasswordCredential` processes its inputs according to `CredentialRow`'s responsibility, performs the `to Password Credential` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `toPasswordCredential` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `toPasswordCredential`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PasswordIdentityAuthenticator.PasswordCredential toPasswordCredential() {
            boolean active = tenant.getStatus() == TenantEntity.Status.ACTIVE
                    && user.getStatus() == UserEntity.Status.ACTIVE
                    && credential.getStatus() != UserCredentialEntity.Status.DISABLED
                    && credential.getStatus() != UserCredentialEntity.Status.EXPIRED;
            return new PasswordIdentityAuthenticator.PasswordCredential(
                    tenant.getCode(),
                    user.getNormalizedUsername(),
                    user.getId().toString(),
                    credential.getPasswordHash(),
                    credential.getFailedAttempts(),
                    credential.getLockedUntil(),
                    active);
        }
    }
}
