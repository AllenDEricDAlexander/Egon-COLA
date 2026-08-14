package top.egon.cola.platform.rbac3.admin.directory.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.DirectorySnapshotPO;
import top.egon.cola.platform.rbac3.admin.directory.domain.DirectorySnapshotProcessor;
import top.egon.cola.platform.rbac3.admin.directory.repository.jpa.DirectorySnapshotMaterializer;
import top.egon.cola.platform.rbac3.admin.directory.repository.jpa.JpaDirectorySnapshotRepository;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.contract.activation.ActivationRoot;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.SnapshotModelVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.DirectorySnapshotStatusEnum;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.MaterializationResultVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.IngestionResultVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.DirectorySnapshotOutcomeEnum;
import top.egon.cola.platform.rbac3.admin.tenant.domain.enums.TenantStatusEnum;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.directory.repository.DirectoryCommandRepository;
import top.egon.cola.platform.rbac3.admin.tenant.domain.dto.CreateTenantCommandDTO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.dto.TenantStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.identity.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.dto.DirectorySnapshotCommandDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectorySyncVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.vo.TenantVO;

/**
 * 目录写模型的 JPA 仓储。用户状态变更只更新 RBAC 用户及其授权版本，令牌生命周期由 IdP 管理。
 * JPA directory-command repository. User status changes update only the RBAC user and authorization version;
 * token lifecycle is owned by the IdP.
 */
@Repository
public class JpaDirectoryCommandRepository implements DirectoryCommandRepository {
    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;
    private final JpaDirectorySnapshotRepository directorySnapshotStore;
    private final DirectorySnapshotMaterializer directorySnapshotMaterializer;
    private final DirectorySnapshotProcessor directorySnapshotProcessor =
            new DirectorySnapshotProcessor();

    public JpaDirectoryCommandRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock,
            JpaDirectorySnapshotRepository directorySnapshotStore,
            DirectorySnapshotMaterializer directorySnapshotMaterializer) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
        this.directorySnapshotStore = directorySnapshotStore;
        this.directorySnapshotMaterializer = directorySnapshotMaterializer;
    }

/**
     * 方法 `submit` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `submit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `submit` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `submit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `submit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `submit`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public DirectorySyncVO submit(
            String tenantId,
            DirectorySnapshotCommandDTO command) {
        Instant now = databaseClock.transactionNow();
        SnapshotModelVO model =
                directorySnapshotProcessor.validate(command.payload(), command.generatedAt());
        DirectorySnapshotPO entity = new DirectorySnapshotPO(
                idGenerator.nextLongId(), Long.valueOf(tenantId), command.providerCode(),
                command.snapshotVersion(), command.checksum(), command.generatedAt(),
                command.payload(), "directory-sync", now);
        IngestionResultVO result = directorySnapshotStore.accept(entity);
        Map<String, Object> counts;
        long affectedUsers;
        if (result.outcome() == DirectorySnapshotOutcomeEnum.ACCEPTED) {
            MaterializationResultVO materialization =
                    directorySnapshotMaterializer.apply(
                            Long.valueOf(tenantId), entity.getId(), command.snapshotVersion(),
                            model, "directory-sync", now);
            counts = new LinkedHashMap<>(model.counts());
            counts.putAll(materialization.counts());
            entity.validate(counts, "directory-sync", now);
            archiveCurrentSnapshot(
                    Long.valueOf(tenantId), command.providerCode(), entity.getId(), now);
            entity.activate("directory-sync", now);
            affectedUsers = materialization.affectedUserCount();
        } else {
            DirectorySnapshotPO existing = entityManager.find(
                    DirectorySnapshotPO.class, result.snapshotId());
            counts = existing == null ? Map.of() : existing.getCounts();
            affectedUsers = numericCount(counts, "affectedUsers");
        }
        return new DirectorySyncVO(
                result.snapshotId().toString(), result.outcome().name(),
                longCounts(counts), affectedUsers);
    }

/**
     * 方法 `createTenant` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `create Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createTenant` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `create Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public TenantVO createTenant(
            CreateTenantCommandDTO command,
            String actorId) {
        String normalizedCode = command.code().trim().toLowerCase(Locale.ROOT);
        boolean exists = !entityManager.createQuery("""
                        select t.id from TenantEntity t where lower(t.code) = :code
                        """, Long.class)
                .setParameter("code", normalizedCode)
                .setMaxResults(1)
                .getResultList().isEmpty();
        if (exists) {
            throw new Rbac3RuleViolation("TENANT_CODE_CONFLICT");
        }
        Instant now = databaseClock.transactionNow();
        TenantPO tenant = new TenantPO(
                idGenerator.nextLongId(), normalizedCode, command.name(), actorId, now);
        tenant.configure(command.settings(), actorId, now);
        entityManager.persist(tenant);
        return tenantView(tenant);
    }

/**
     * 方法 `changeTenantStatus` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `change Tenant Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeTenantStatus` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `change Tenant Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeTenantStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeTenantStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public TenantVO changeTenantStatus(
            String tenantId,
            TenantStatusCommandDTO command,
            String actorId) {
        TenantPO tenant = entityManager.find(
                TenantPO.class, Long.valueOf(tenantId), LockModeType.PESSIMISTIC_WRITE);
        if (tenant == null) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        TenantStatusEnum nextStatus = enumValue(
                TenantStatusEnum.class, command.status(), "TENANT_STATUS_INVALID");
        Instant now = databaseClock.transactionNow();
        tenant.changeStatus(nextStatus, command.expectedVersion(), command.reason(), actorId, now);
        return tenantView(tenant);
    }

/**
     * 方法 `changeUserStatus` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `change User Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeUserStatus` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `change User Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeUserStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeUserStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional
    public UserDirectoryVO changeUserStatus(
            String tenantId,
            String userId,
            UserStatusCommandDTO command,
            String actorId) {
        UserPO user = entityManager.find(
                UserPO.class, Long.valueOf(userId), LockModeType.PESSIMISTIC_WRITE);
        if (user == null || !Long.valueOf(tenantId).equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        UserStatusEnum nextStatus = enumValue(
                UserStatusEnum.class, command.status(), "USER_STATUS_INVALID");
        Instant now = databaseClock.transactionNow();
        user.changeStatus(nextStatus, command.reason(), command.expectedAuthVersion(),
                actorId, now);
        return userView(user);
    }

/**
     * 方法 `requireUser` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `require User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireUser` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `require User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private UserPO requireUser(Long tenantId, Long userId) {
        UserPO user = entityManager.find(UserPO.class, userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return user;
    }

/**
     * 方法 `tenantView` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `tenant View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantView` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `tenant View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenant 输入参数 `tenant`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private TenantVO tenantView(TenantPO tenant) {
        return new TenantVO(
                tenant.getId().toString(), tenant.getCode(), tenant.getName(),
                tenant.getStatus().name(), tenant.getSettings(), tenant.getVersion());
    }

/**
     * 方法 `userView` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `user View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `userView` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `user View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `userView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `userView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param user 输入参数 `user`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private UserDirectoryVO userView(UserPO user) {
        return new UserDirectoryVO(
                user.getId().toString(), user.getIdentitySub(),
                user.getStatus().name(), user.getAuthVersion());
    }

/**
     * 方法 `archiveCurrentSnapshot` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `archive Current Snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `archiveCurrentSnapshot` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `archive Current Snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `archiveCurrentSnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `archiveCurrentSnapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param providerCode 输入参数 `providerCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param incomingSnapshotId 输入参数 `incomingSnapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void archiveCurrentSnapshot(
            Long tenantId,
            String providerCode,
            Long incomingSnapshotId,
            Instant now) {
        List<DirectorySnapshotPO> active = entityManager.createQuery("""
                        select s from DirectorySnapshotEntity s
                         where s.tenantId = :tenantId
                           and s.providerCode = :providerCode
                           and s.status = :status
                           and s.id <> :incomingSnapshotId
                        """, DirectorySnapshotPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("providerCode", providerCode)
                .setParameter("status", DirectorySnapshotStatusEnum.ACTIVE)
                .setParameter("incomingSnapshotId", incomingSnapshotId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        active.forEach(snapshot -> snapshot.archive("directory-sync", now));
    }

/**
     * 方法 `enumValue` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `enum Value` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `enumValue` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `enum Value` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `enumValue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `enumValue`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <E> 类型参数表示待解析的枚举类型；type parameter representing the enum type to resolve.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static <E extends Enum<E>> E enumValue(
            Class<E> type, String value, String reasonCode) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new Rbac3RuleViolation(reasonCode);
        }
    }

/**
     * 方法 `stringId` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `string Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `stringId` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `string Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `stringId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `stringId`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String stringId(Long value) {
        return value == null ? null : value.toString();
    }

/**
     * 方法 `longCounts` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `long Counts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `longCounts` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `long Counts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `longCounts` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `longCounts`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<String, Long> longCounts(Map<String, Object> values) {
        Map<String, Long> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (value instanceof Number number) {
                result.put(key, number.longValue());
            }
        });
        return Map.copyOf(result);
    }

/**
     * 方法 `numericCount` 按照 `JpaDirectoryCommandRepository` 的职责处理输入，完成 `numeric Count` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `numericCount` processes its inputs according to `JpaDirectoryCommandRepository`'s responsibility, performs the `numeric Count` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `numericCount` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `numericCount`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long numericCount(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }

}
