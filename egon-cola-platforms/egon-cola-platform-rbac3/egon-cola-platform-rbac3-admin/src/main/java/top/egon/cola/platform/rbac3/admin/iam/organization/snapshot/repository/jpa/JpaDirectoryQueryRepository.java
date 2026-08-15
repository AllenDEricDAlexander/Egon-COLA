package top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.po.DirectorySnapshotPO;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.po.OrgUnitPO;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.po.PositionPO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
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
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.OrgUnitUnitTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.OrgUnitStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.enums.PositionStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.position.snapshot.domain.enums.UserPositionSnapshotStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.enums.TenantStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.DirectoryQueryRepository;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.vo.TenantVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.OrgUnitVO;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.DirectorySnapshotVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.DirectoryPageVO;

/**
 * 目录读模型的 JPA 仓储，保留原 Store 的查询、排序和分页语义。
 * JPA directory-query repository preserving the original Store query, ordering, and pagination semantics.
 */
@Repository
public class JpaDirectoryQueryRepository implements DirectoryQueryRepository {
    private final EntityManager entityManager;

    public JpaDirectoryQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type, String value, String reasonCode) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new Rbac3RuleViolation(reasonCode);
        }
    }

/**
     * 方法 `findUser` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `find User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findUser` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `find User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDirectoryVO findUser(
            String tenantId,
            String userId) {
        UserPO user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
        return userView(user);
    }

/**
     * 方法 `findTenant` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `find Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findTenant` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `find Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public TenantVO findTenant(String tenantId) {
        TenantPO tenant = entityManager.find(
                TenantPO.class, Long.valueOf(tenantId));
        if (tenant == null) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return tenantView(tenant);
    }

/**
     * 方法 `findTenants` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `find Tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findTenants` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `find Tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findTenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findTenants`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param page 输入参数 `page`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param size 输入参数 `size`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public DirectoryPageVO<TenantVO>
            findTenants(String query, String status, int page, int size) {
        String normalizedQuery = nullableText(query);
        TenantStatusEnum requiredStatus = nullableEnum(
                TenantStatusEnum.class, status, "TENANT_STATUS_INVALID");
        String predicates = "";
        if (normalizedQuery != null) {
            predicates += " and (lower(t.code) like :query or lower(t.name) like :query)";
        }
        if (requiredStatus != null) {
            predicates += " and t.status = :status";
        }
        var dataQuery = entityManager.createQuery(
                "select t from TenantEntity t where 1 = 1" + predicates
                        + " order by t.code, t.id", TenantPO.class);
        var countQuery = entityManager.createQuery(
                "select count(t) from TenantEntity t where 1 = 1" + predicates,
                Long.class);
        if (normalizedQuery != null) {
            dataQuery.setParameter("query", '%' + normalizedQuery.toLowerCase(Locale.ROOT) + '%');
            countQuery.setParameter("query", '%' + normalizedQuery.toLowerCase(Locale.ROOT) + '%');
        }
        if (requiredStatus != null) {
            dataQuery.setParameter("status", requiredStatus);
            countQuery.setParameter("status", requiredStatus);
        }
        List<TenantVO> items = dataQuery
                .setFirstResult(Math.multiplyExact(page, size))
                .setMaxResults(size)
                .getResultList().stream().map(this::tenantView).toList();
        return new DirectoryPageVO<>(
                items, page, size, countQuery.getSingleResult());
    }

/**
     * 方法 `findUsers` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `find Users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findUsers` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `find Users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findUsers` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findUsers`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param positionId 输入参数 `positionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param page 输入参数 `page`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param size 输入参数 `size`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public DirectoryPageVO<UserDirectoryVO>
            findUsers(
                    String tenantId, String query, String status, String orgUnitId,
                    String positionId, int page, int size) {
        String normalizedQuery = nullableText(query);
        UserStatusEnum requiredStatus = nullableEnum(
                UserStatusEnum.class, status, "USER_STATUS_INVALID");
        Long requiredOrgUnit = nullableLong(orgUnitId, "ORG_UNIT_ID_INVALID");
        Long requiredPosition = nullableLong(positionId, "POSITION_ID_INVALID");
        String predicates = " where u.tenantId = :tenantId";
        if (normalizedQuery != null) {
            predicates += " and lower(u.identitySub) like :query";
        }
        if (requiredStatus != null) {
            predicates += " and u.status = :status";
        }
        if (requiredOrgUnit != null) {
            predicates += " and exists (select up.id from UserPositionSnapshotEntity up"
                    + " where up.tenantId = u.tenantId and up.userId = u.id"
                    + " and up.orgUnitId = :orgUnitId and up.status = :assignmentStatus)";
        }
        if (requiredPosition != null) {
            predicates += " and exists (select up.id from UserPositionSnapshotEntity up"
                    + " where up.tenantId = u.tenantId and up.userId = u.id"
                    + " and up.positionId = :positionId and up.status = :assignmentStatus)";
        }
        var dataQuery = entityManager.createQuery(
                "select u from UserEntity u" + predicates
                        + " order by u.identitySub, u.id", UserPO.class);
        var countQuery = entityManager.createQuery(
                "select count(u) from UserEntity u" + predicates, Long.class);
        dataQuery.setParameter("tenantId", Long.valueOf(tenantId));
        countQuery.setParameter("tenantId", Long.valueOf(tenantId));
        if (normalizedQuery != null) {
            String pattern = '%' + normalizedQuery.toLowerCase(Locale.ROOT) + '%';
            dataQuery.setParameter("query", pattern);
            countQuery.setParameter("query", pattern);
        }
        if (requiredStatus != null) {
            dataQuery.setParameter("status", requiredStatus);
            countQuery.setParameter("status", requiredStatus);
        }
        if (requiredOrgUnit != null) {
            dataQuery.setParameter("orgUnitId", requiredOrgUnit);
            countQuery.setParameter("orgUnitId", requiredOrgUnit);
        }
        if (requiredPosition != null) {
            dataQuery.setParameter("positionId", requiredPosition);
            countQuery.setParameter("positionId", requiredPosition);
        }
        if (requiredOrgUnit != null || requiredPosition != null) {
            dataQuery.setParameter("assignmentStatus", UserPositionSnapshotStatusEnum.ACTIVE);
            countQuery.setParameter("assignmentStatus", UserPositionSnapshotStatusEnum.ACTIVE);
        }
        List<UserDirectoryVO> items = dataQuery
                .setFirstResult(Math.multiplyExact(page, size))
                .setMaxResults(size)
                .getResultList().stream().map(this::userView).toList();
        return new DirectoryPageVO<>(
                items, page, size, countQuery.getSingleResult());
    }

/**
     * 方法 `findOrgUnits` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `find Org Units` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findOrgUnits` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `find Org Units` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findOrgUnits` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findOrgUnits`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param parentId 输入参数 `parentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrgUnitVO> findOrgUnits(
            String tenantId, String parentId, String type, String status) {
        Long requiredParent = nullableLong(parentId, "ORG_UNIT_ID_INVALID");
        OrgUnitUnitTypeEnum requiredType = nullableEnum(
                OrgUnitUnitTypeEnum.class, type, "ORG_UNIT_TYPE_INVALID");
        OrgUnitStatusEnum requiredStatus = nullableEnum(
                OrgUnitStatusEnum.class, status, "ORG_UNIT_STATUS_INVALID");
        String predicates = " where o.tenantId = :tenantId";
        if (requiredParent != null) {
            predicates += " and o.parentId = :parentId";
        }
        if (requiredType != null) {
            predicates += " and o.unitType = :type";
        }
        if (requiredStatus != null) {
            predicates += " and o.status = :status";
        }
        var query = entityManager.createQuery(
                "select o from OrgUnitEntity o" + predicates
                        + " order by o.path, o.id", OrgUnitPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId));
        if (requiredParent != null) {
            query.setParameter("parentId", requiredParent);
        }
        if (requiredType != null) {
            query.setParameter("type", requiredType);
        }
        if (requiredStatus != null) {
            query.setParameter("status", requiredStatus);
        }
        return query.getResultList().stream().map(this::orgUnitView).toList();
    }

/**
     * 方法 `findPositions` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `find Positions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findPositions` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `find Positions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findPositions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findPositions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PositionVO> findPositions(
            String tenantId, String orgUnitId, String status) {
        Long requiredOrgUnit = nullableLong(orgUnitId, "ORG_UNIT_ID_INVALID");
        PositionStatusEnum requiredStatus = nullableEnum(
                PositionStatusEnum.class, status, "POSITION_STATUS_INVALID");
        String predicates = " where p.tenantId = :tenantId";
        if (requiredOrgUnit != null) {
            predicates += " and p.orgUnitId = :orgUnitId";
        }
        if (requiredStatus != null) {
            predicates += " and p.status = :status";
        }
        var query = entityManager.createQuery(
                "select p from PositionEntity p" + predicates
                        + " order by p.code, p.id", PositionPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId));
        if (requiredOrgUnit != null) {
            query.setParameter("orgUnitId", requiredOrgUnit);
        }
        if (requiredStatus != null) {
            query.setParameter("status", requiredStatus);
        }
        return query.getResultList().stream().map(this::positionView).toList();
    }

/**
     * 方法 `findSnapshot` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `find Snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findSnapshot` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `find Snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findSnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findSnapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public DirectorySnapshotVO findSnapshot(
            String tenantId, String snapshotId) {
        DirectorySnapshotPO snapshot = entityManager.find(
                DirectorySnapshotPO.class, Long.valueOf(snapshotId));
        if (snapshot == null || !Long.valueOf(tenantId).equals(snapshot.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return new DirectorySnapshotVO(
                snapshot.getId().toString(), snapshot.getProviderCode(),
                snapshot.getSnapshotVersion(), snapshot.getChecksum(),
                snapshot.getStatus().name(), snapshot.getGeneratedAt(),
                snapshot.getReceivedAt(), snapshot.getActivatedAt(),
                snapshot.getCounts());
    }

/**
     * 方法 `requireUser` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `require User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireUser` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `require User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `tenantView` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `tenant View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantView` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `tenant View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `userView` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `user View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `userView` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `user View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `orgUnitView` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `org Unit View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `orgUnitView` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `org Unit View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `orgUnitView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `orgUnitView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param unit 输入参数 `unit`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private OrgUnitVO orgUnitView(OrgUnitPO unit) {
        return new OrgUnitVO(
                unit.getId().toString(), unit.getSnapshotId().toString(),
                unit.getUnitType().name(), unit.getCode(), unit.getName(),
                stringId(unit.getParentId()), unit.getPath(), unit.getDepth(),
                unit.getStatus().name());
    }

/**
     * 方法 `positionView` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `position View` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `positionView` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `position View` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `positionView` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `positionView`, then continue the business flow using its result, exception, or side effect.
     *
     * @param position 输入参数 `position`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private PositionVO positionView(PositionPO position) {
        return new PositionVO(
                position.getId().toString(), position.getSnapshotId().toString(),
                position.getCode(), position.getName(), position.getOrgUnitId().toString(),
                position.getStatus().name());
    }

/**
     * 方法 `nullableEnum` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `nullable Enum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `nullableEnum` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `nullable Enum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `nullableEnum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `nullableEnum`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <E> 类型参数表示可选枚举的具体类型；type parameter representing the optional enum type.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static <E extends Enum<E>> E nullableEnum(
            Class<E> type, String value, String reasonCode) {
        return nullableText(value) == null ? null : enumValue(type, value, reasonCode);
    }

/**
     * 方法 `nullableLong` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `nullable Long` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `nullableLong` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `nullable Long` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `nullableLong` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `nullableLong`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Long nullableLong(String value, String reasonCode) {
        String normalized = nullableText(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException exception) {
            throw new Rbac3RuleViolation(reasonCode);
        }
    }

/**
     * 方法 `nullableText` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `nullable Text` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `nullableText` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `nullable Text` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `nullableText` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `nullableText`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String nullableText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

/**
     * 方法 `stringId` 按照 `JpaDirectoryQueryRepository` 的职责处理输入，完成 `string Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `stringId` processes its inputs according to `JpaDirectoryQueryRepository`'s responsibility, performs the `string Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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

}
