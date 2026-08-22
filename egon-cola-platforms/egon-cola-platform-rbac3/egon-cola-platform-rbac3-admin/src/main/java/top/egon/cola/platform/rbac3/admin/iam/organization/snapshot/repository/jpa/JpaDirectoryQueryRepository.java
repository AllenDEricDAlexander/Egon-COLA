package top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.OrgUnitStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.OrgUnitUnitTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.po.OrgUnitPO;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.DirectoryPageVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.OrgUnitVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.po.DirectorySnapshotPO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.DirectorySnapshotVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.DirectoryQueryRepository;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.enums.PositionStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.po.PositionPO;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.iam.position.snapshot.domain.enums.UserPositionSnapshotStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.List;
import java.util.Locale;

/** 目录读模型仓储；所有查询直接按外部 tenantId 作用域，不读取 tenant catalog。 */
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

    @Override
    @Transactional(readOnly = true)
    public UserDirectoryVO findUser(String tenantId, String userId) {
        UserPO user = requireUser(Long.valueOf(tenantId), Long.valueOf(userId));
        return userView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public DirectoryPageVO<UserDirectoryVO> findUsers(
            String tenantId,
            String query,
            String status,
            String orgUnitId,
            String positionId,
            int page,
            int size) {
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
        return new DirectoryPageVO<>(items, page, size, countQuery.getSingleResult());
    }

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

    @Override
    @Transactional(readOnly = true)
    public DirectorySnapshotVO findSnapshot(String tenantId, String snapshotId) {
        DirectorySnapshotPO snapshot = entityManager.find(
                DirectorySnapshotPO.class, Long.valueOf(snapshotId));
        if (snapshot == null || !Long.valueOf(tenantId).equals(snapshot.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return new DirectorySnapshotVO(
                snapshot.getId().toString(), snapshot.getProviderCode(),
                snapshot.getSnapshotVersion(), snapshot.getChecksum(),
                snapshot.getStatus().name(), snapshot.getGeneratedAt(),
                snapshot.getReceivedAt(), snapshot.getActivatedAt(), snapshot.getCounts());
    }

    private UserPO requireUser(Long tenantId, Long userId) {
        UserPO user = entityManager.find(UserPO.class, userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return user;
    }

    private UserDirectoryVO userView(UserPO user) {
        return new UserDirectoryVO(
                user.getId().toString(), user.getIdentitySub(),
                user.getStatus().name(), user.getAuthVersion());
    }

    private OrgUnitVO orgUnitView(OrgUnitPO unit) {
        return new OrgUnitVO(
                unit.getId().toString(), stringId(unit.getSnapshotId()),
                unit.getUnitType().name(), unit.getCode(), unit.getName(),
                stringId(unit.getParentId()), unit.getPath(), unit.getDepth(),
                unit.getStatus().name());
    }

    private PositionVO positionView(PositionPO position) {
        return new PositionVO(
                position.getId().toString(), stringId(position.getSnapshotId()),
                position.getCode(), position.getName(), position.getOrgUnitId().toString(),
                position.getStatus().name());
    }

    private static <E extends Enum<E>> E nullableEnum(
            Class<E> type, String value, String reasonCode) {
        return nullableText(value) == null ? null : enumValue(type, value, reasonCode);
    }

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

    private static String nullableText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String stringId(Long value) {
        return value == null ? null : value.toString();
    }
}
