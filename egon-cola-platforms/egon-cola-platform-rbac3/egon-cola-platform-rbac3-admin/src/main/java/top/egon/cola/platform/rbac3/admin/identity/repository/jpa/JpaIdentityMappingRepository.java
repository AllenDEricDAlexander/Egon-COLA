package top.egon.cola.platform.rbac3.admin.identity.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.auth.service.PasswordIdentityAuthenticator;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.ExternalIdentityStatusEnum;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserCredentialTypeEnum;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.ExternalIdentityPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserCredentialPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.MappingVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.ResolvedMembershipVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.TenantMembershipVO;
import top.egon.cola.platform.rbac3.admin.identity.repository.IdentityMappingRepository;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.tenant.domain.enums.TenantStatusEnum;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * 基于 JPA 的身份映射仓储，保留原有租户隔离、排序和事务语义。
 * JPA identity-mapping repository preserving tenant isolation, ordering, and transaction semantics.
 */
@Repository
public class JpaIdentityMappingRepository implements IdentityMappingRepository {
    private final EntityManager entityManager;

    public JpaIdentityMappingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

/**
     * 方法 `find` 按照 `JpaIdentityMappingRepository` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `find` processes its inputs according to `JpaIdentityMappingRepository`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public Optional<MappingVO> find(
            String tenantId, String identitySub) {
        return entityManager.createQuery("""
                        select i from ExternalIdentityEntity i
                         where i.tenantId = :tenantId
                           and i.identitySub = :identitySub
                        """, ExternalIdentityPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("identitySub", identitySub)
                .getResultStream()
                .findFirst()
                .map(JpaIdentityMappingRepository::toMapping);
    }

/**
     * 方法 `create` 按照 `JpaIdentityMappingRepository` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `create` processes its inputs according to `JpaIdentityMappingRepository`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public MappingVO create(
            long mappingId,
            String tenantId,
            String identitySub,
            String rbac3UserId,
            String actorId,
            java.time.Instant now) {
        Long numericTenantId = Long.valueOf(tenantId);
        Long numericUserId = Long.valueOf(rbac3UserId);
        requireActiveTenantAndUser(numericTenantId, numericUserId);
        ExternalIdentityPO entity = ExternalIdentityPO.idpMapping(
                mappingId, numericTenantId, identitySub, numericUserId, actorId, now);
        entityManager.persist(entity);
        entityManager.flush();
        return toMapping(entity);
    }

/**
     * 方法 `resolve` 按照 `JpaIdentityMappingRepository` 的职责处理输入，完成 `resolve` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resolve` processes its inputs according to `JpaIdentityMappingRepository`'s responsibility, performs the `resolve` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public Optional<ResolvedMembershipVO> resolve(
            String tenantId, String identitySub) {
        return activeMemberships(identitySub, Long.valueOf(tenantId)).stream()
                .findFirst()
                .map(JpaIdentityMappingRepository::toResolvedMembership);
    }

/**
     * 方法 `tenants` 按照 `JpaIdentityMappingRepository` 的职责处理输入，完成 `tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenants` processes its inputs according to `JpaIdentityMappingRepository`'s responsibility, performs the `tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenants`, then continue the business flow using its result, exception, or side effect.
     *
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TenantMembershipVO> tenants(String identitySub) {
        return activeMemberships(identitySub, null).stream()
                .map(row -> new TenantMembershipVO(
                        row.tenant().getId().toString(), row.tenant().getCode(),
                        row.tenant().getName(), row.user().getId().toString(),
                        row.user().getDisplayName()))
                .toList();
    }

/**
     * 方法 `activeMemberships` 按照 `JpaIdentityMappingRepository` 的职责处理输入，完成 `active Memberships` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activeMemberships` processes its inputs according to `JpaIdentityMappingRepository`'s responsibility, performs the `active Memberships` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
                .setParameter("identityStatus", ExternalIdentityStatusEnum.ACTIVE)
                .setParameter("tenantStatus", TenantStatusEnum.ACTIVE)
                .setParameter("userStatus", UserStatusEnum.ACTIVE);
        if (tenantId != null) {
            query.setParameter("tenantId", tenantId);
        }
        return query.getResultList().stream()
                .map(values -> new MembershipRow(
                        (ExternalIdentityPO) values[0],
                        (TenantPO) values[1],
                        (UserPO) values[2]))
                .toList();
    }

/**
     * 方法 `requireActiveTenantAndUser` 按照 `JpaIdentityMappingRepository` 的职责处理输入，完成 `require Active Tenant And User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireActiveTenantAndUser` processes its inputs according to `JpaIdentityMappingRepository`'s responsibility, performs the `require Active Tenant And User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireActiveTenantAndUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireActiveTenantAndUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void requireActiveTenantAndUser(Long tenantId, Long userId) {
        TenantPO tenant = entityManager.find(
                TenantPO.class, tenantId, LockModeType.PESSIMISTIC_READ);
        UserPO user = entityManager.find(
                UserPO.class, userId, LockModeType.PESSIMISTIC_READ);
        if (tenant == null || tenant.getStatus() != TenantStatusEnum.ACTIVE
                || user == null || !tenantId.equals(user.getTenantId())
                || user.getStatus() != UserStatusEnum.ACTIVE) {
            throw new IllegalStateException("active tenant user is required");
        }
    }

/**
     * 方法 `toMapping` 按照 `JpaIdentityMappingRepository` 的职责处理输入，完成 `to Mapping` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toMapping` processes its inputs according to `JpaIdentityMappingRepository`'s responsibility, performs the `to Mapping` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toMapping` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toMapping`, then continue the business flow using its result, exception, or side effect.
     *
     * @param entity 输入参数 `entity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static MappingVO toMapping(
            ExternalIdentityPO entity) {
        return new MappingVO(
                entity.getId().toString(), entity.getTenantId().toString(),
                entity.getIdentitySub(), entity.getUserId().toString(),
                entity.getStatus() == ExternalIdentityStatusEnum.ACTIVE,
                entity.getUpdatedAt());
    }

/**
     * 方法 `toResolvedMembership` 按照 `JpaIdentityMappingRepository` 的职责处理输入，完成 `to Resolved Membership` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toResolvedMembership` processes its inputs according to `JpaIdentityMappingRepository`'s responsibility, performs the `to Resolved Membership` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toResolvedMembership` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toResolvedMembership`, then continue the business flow using its result, exception, or side effect.
     *
     * @param row 输入参数 `row`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static ResolvedMembershipVO toResolvedMembership(
            MembershipRow row) {
        return new ResolvedMembershipVO(
                row.tenant().getId().toString(), row.tenant().getCode(),
                row.tenant().getName(), row.identity().getIdentitySub(),
                row.user().getId().toString(), row.user().getDisplayName(), true,
                row.user().getAuthVersion(), row.tenant().getPolicyVersion());
    }

}
